package com.android.app1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.android.app1.common.Common;
import com.android.app1.model.AlertModel;
import com.android.app1.model.DeviceModel;
import com.android.app1.model.Out;
import com.android.app1.service.BluetoothThreadConnection;
import com.android.app1.service.MessageReceiver;
import com.android.app1.ui.main.SectionsPagerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.view_pager)
    ViewPager viewPager;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID UUIDBT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String deviceToConnect;
    Handler handlerBluetoothIn;
    final int handlerState = 0;
    private BluetoothSocket bluetoothSocket = null;
    private BluetoothThreadConnection connection;
    private StringBuilder dataStringInput = new StringBuilder();
    private DatabaseReference mDatabase;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private TextInputLayout deviceLayout;
    private TextInputEditText inputDevice;
    private TextInputLayout uuidLayout;
    private TextInputEditText inputUuid;
    private TextInputLayout layoutDescription;
    private TextInputEditText inputDescription;
    private Button imageButton;

    private final int TAKE_PHOTO_FILE = 1000;
    private AlertDialog mDialog;
    private Uri imageTaken;
    private String mCurrentPath;
    private boolean deviceExist = false;

    private ProgressDialog mProgress;
    private CharSequence[] deviceList;
    private List<String> deviceConnected;

    private MessageReceiver mReceiver = new MessageReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            List<Out> await;
            if (Paper.book().contains("await")) {
                await = Paper.book().read("await");
                String uuidDevice = MessageReceiver.msg;
                if (uuidDevice.charAt(0) == '#') {
                    List<Out> removes = new ArrayList<>();
                    AlertModel model = new AlertModel();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                    SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss", Locale.US);
                    model.setTime(stf.format(new Date()));
                    model.setDate(sdf.format(new Date()));
                    model.setType("Entrada");
                    String uuidAwait = "";
                    String newReplace = uuidDevice.replace("#", "");
                    for (Out out : await) {
                        if (out.getUuid().equals(newReplace)) {
                            uuidAwait = out.getName();
                        } else {
                            removes.add(out);
                        }
                    }
                    model.setDevice(uuidAwait);
                    if (model.getDevice() != null && !model.getDevice().equals("")) {
                        mDatabase
                                .getDatabase()
                                .getReference().child("departures")
                                .child(UUID.randomUUID().toString())
                                .setValue(model.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    Paper.book().delete("await");
                                    Paper.book().write("await", removes);
                                    Toast.makeText(context, "Se ha registrado un acceso.", Toast.LENGTH_SHORT).show();
                                });
                        Toast.makeText(context, "Alerta de entrada: " + MessageReceiver.msg, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Paper.init(this);
        toolbar.setTitle(R.string.toolbar_title_home);
        setSupportActionBar(toolbar);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        dismiss();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(sectionsPagerAdapter);
        tabs.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    dismiss();
                } else {
                    checkState();
                    show();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        fab.setOnClickListener(view -> {
            createDialogToAdd();
        });
        handlerBluetoothIn = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    dataStringInput.append(readMessage);
                    int endOfLineIndex = dataStringInput.indexOf("#");
                    if (endOfLineIndex > 0) {
                        String inputData = dataStringInput.substring(0, endOfLineIndex);
                        Log.d("Data", inputData);
                        dataStringInput.delete(0, dataStringInput.length());
                        if (inputUuid != null) {
                            inputUuid.setText(inputData);
                        }
                    }
                }
            }
        };
        verified();
    }

    private void createDialogToAdd() {
        if (bluetoothSocket != null) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this, R.style.AlertDialogTheme);
            builder.setTitle("Agregar dispositivo")
                    .setMessage("Registre su dispositivo asociado a la alarma.");
            View itemView = getLayoutInflater().inflate(R.layout.item_add_device_layout, null, false);
            deviceLayout = itemView.findViewById(R.id.device_layout);
            inputDevice = itemView.findViewById(R.id.inputDevice);
            uuidLayout = itemView.findViewById(R.id.uuid_layout);
            inputUuid = itemView.findViewById(R.id.inputUuid);
            deviceLayout = itemView.findViewById(R.id.device_layout);
            layoutDescription = itemView.findViewById(R.id.layout_description);
            inputDescription = itemView.findViewById(R.id.inputDescription);
            imageButton = itemView.findViewById(R.id.imageButton);
            inputUuid.setEnabled(false);
            inputUuid.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (Common.CURRENT_DEVICE_LIST.contains(s.toString())) {
                        deviceExist = true;
                        uuidLayout.setError("Este dispositivo ya existe.");
                    } else {
                        deviceExist = false;
                        uuidLayout.setError(null);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            imageButton.setOnClickListener(v -> Dexter.withContext(MainActivity.this)
                    .withPermissions(Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                            if (multiplePermissionsReport.areAllPermissionsGranted()) {
                                cameraShotPhoto();
                            }
                            if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                                MaterialAlertDialogBuilder build = new MaterialAlertDialogBuilder(MainActivity.this, R.style.AlertDialogTheme);
                                build.setTitle("Permisos rechazados")
                                        .setMessage("Para poder tomar una foto necesita habilitar los permisos de lectura y escritura en el dispositivo, de lo contrario seleccione una imagen de la galeria.")
                                        .setPositiveButton("Habilitar", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                Intent settingsPermissions = new Intent();
                                                settingsPermissions.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                                settingsPermissions.setData(uri);
                                                startActivity(settingsPermissions);
                                            }
                                        })
                                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .create()
                                        .show();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    })
                    .onSameThread()
                    .check());
            builder.setView(itemView);
            builder.setPositiveButton("Aceptar", null);
            builder.setNegativeButton("Cancelar", (dialog, which) -> mDialog.dismiss());
            mDialog = builder.create();
            mDialog.setOnShowListener(dialog -> {
                Button accepted = ((AlertDialog) dialog).getButton(dialog.BUTTON_POSITIVE);
                accepted.setOnClickListener(v -> {
                    if (inputDevice.getText().toString().isEmpty()) {
                        deviceLayout.setError("Debe ingresar un nombre.");
                        return;
                    } else {
                        deviceLayout.setError(null);
                    }
                    if (inputUuid.getText().toString().isEmpty()) {
                        uuidLayout.setError("Debe ingresar un nombre.");
                        return;
                    } else {
                        uuidLayout.setError(null);
                    }
                    if (inputDescription.getText().toString().isEmpty()) {
                        layoutDescription.setError("Debe ingresar un nombre.");
                        return;
                    } else {
                        layoutDescription.setError(null);
                    }
                    if (deviceExist) {
                        uuidLayout.setError("Este dispositivo ya existe");
                        return;
                    }
                    addImageToReference();
                });
            });
            mDialog.show();
        } else {
            updateListDevices();
        }
    }

    private void updateListDevices() {
        deviceConnected = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            deviceList = new CharSequence[pairedDevices.size()];
            int i = 0;
            for (BluetoothDevice device : pairedDevices) {
                deviceConnected.add(device.getAddress());
                deviceList[i] = device.getName() + "\n" + device.getAddress();
                i++;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Emparejar dispositivo")
                    .setItems(deviceList, (dialog, which) -> {
                        dialog.dismiss();
                        deviceToConnect = deviceConnected.get(which);
                        mProgress = new ProgressDialog(MainActivity.this);
                        mProgress.setMessage("Emparejando...");
                        mProgress.setCanceledOnTouchOutside(false);
                        mProgress.show();
                        connect();
                    }).create().show();
        }
    }

    private BluetoothSocket createConnectionSecure(BluetoothDevice bDevice) throws IOException {
        return bDevice.createRfcommSocketToServiceRecord(UUIDBT);
    }

    public void checkState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta el Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBt, REQUEST_ENABLE_BT);
            } else {
                updateListDevices();
            }
        }
    }

    private void verified() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.BLUETOOTH)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        Toast.makeText(MainActivity.this, "Permisos de Bluetooth garantizados", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        if (permissionDeniedResponse.isPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    private void addImageToReference() {
        if (imageTaken != null) {
            mProgress = new ProgressDialog(this);
            mProgress.setMessage("Espere...");
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.show();
            final StorageReference reference = storageReference.child("devices/" + UUID.randomUUID().toString());
            UploadTask uploadTask = reference.putFile(imageTaken);
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                mProgress.setMessage("Subiendo archivo.\n" + (int) progress + "%");
            });
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return reference.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    mProgress.setMessage("Guardando...");
                    saveDeviceToJson(task.getResult().toString());
                }
            });
        } else {
            saveDeviceToJson(null);
        }
    }

    private void saveDeviceToJson(String imageUrl) {
        DeviceModel model = new DeviceModel();
        model.setId(UUID.randomUUID().toString());
        model.setName(inputDevice.getText().toString());
        model.setDescription(inputDescription.getText().toString());
        model.setUuid(inputUuid.getText().toString());
        model.setLinkImage(imageUrl);
        mDatabase.child("devices").child(model.getId()).setValue(model.toMap())
                .addOnSuccessListener(aVoid -> {
                    mDialog.dismiss();
                    if (mProgress != null)
                        mProgress.dismiss();
                    Toast.makeText(MainActivity.this, "Se agrego exitosamente un dispositvo.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    mDialog.dismiss();
                    mProgress.dismiss();
                    Toast.makeText(MainActivity.this, "Falló al registrar el dispositvo.", Toast.LENGTH_SHORT).show();
                });
    }

    private void cameraShotPhoto() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//obtiene el intento
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);//se declara un nombre en tiempo de ejecucion
            String filename = String.format("IMG_%s", sdf.format(new Date()));//se le asigna el nombre completo
            File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);//se obtiene el directorio de imagenes
            File imageFile = File.createTempFile(filename, ".jpg", storageDirectory);//se crea el archivo como temporal
            mCurrentPath = imageFile.getAbsolutePath();//se obtiene la ruta
            Uri tempUriFile = FileProvider.getUriForFile(this,//se crea un uri con los datos
                    "com.android.app1",
                    imageFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempUriFile);//se llama a la camara
            startActivityForResult(intent, TAKE_PHOTO_FILE);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void addGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        imageTaken = contentUri;
        Toast.makeText(this, "Foto capturada.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PHOTO_FILE && resultCode == RESULT_OK) {
            addGallery();
        }
    }

    public void dismiss() {
        fab.hide();
    }

    public void show() {
        fab.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Necesita los permisos");
        builder.setMessage("Para poder realizar los registros adecuadamente, debe facilitar el acceso del dispositivo al modulo de bluetooth.");
        builder.setPositiveButton("Ir a configuracion", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(SMS_RECEIVED));
    }

    private void connect() {
        if (deviceToConnect != null) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceToConnect);
            try {
                bluetoothSocket = createConnectionSecure(device);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                connection = new BluetoothThreadConnection(bluetoothSocket, handlerBluetoothIn, this, handlerState);
                connection.start();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
            mProgress.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
    }

    private void disconnect() {
        try {
            if (bluetoothSocket != null)
                bluetoothSocket.close();
            Log.d("onPause", "Connection Close");
        } catch (IOException e) {
            Log.d("IOException On Pause", e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
        unregisterReceiver(mReceiver);
    }
}