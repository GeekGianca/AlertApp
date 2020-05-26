package com.android.app1;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
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

import com.android.app1.model.DeviceModel;
import com.android.app1.ui.main.SectionsPagerAdapter;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

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

    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        toolbar.setTitle(R.string.toolbar_title_home);
        setSupportActionBar(toolbar);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        dismiss();
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
                    show();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Dexter.withContext(MainActivity.this)
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
                                .check();
                    }
                });
                builder.setView(itemView);
                builder.setPositiveButton("Aceptar", null);
                builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialog.dismiss();
                    }
                });
                mDialog = builder.create();
                mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button accepted = ((AlertDialog) dialog).getButton(dialog.BUTTON_POSITIVE);
                        accepted.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
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
                                addImageToReference();
                            }
                        });
                    }
                });
                mDialog.show();

                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });
    }

    private void addImageToReference() {
        if (imageTaken != null) {
            mProgress = new ProgressDialog(this);
            mProgress.setMessage("Espere...");
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.show();
            final StorageReference reference = storageReference.child("devices/" + UUID.randomUUID().toString());
            UploadTask uploadTask = reference.putFile(imageTaken);
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    mProgress.setMessage("Subiendo archivo.\n" + (int) progress + "%");
                }
            });
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return reference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        mProgress.setMessage("Guardando...");
                        saveDeviceToJson(task.getResult().toString());
                    }
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
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mDialog.dismiss();
                        if (mProgress != null)
                            mProgress.dismiss();
                        Toast.makeText(MainActivity.this, "Se agrego exitosamente un dispositvo.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mDialog.dismiss();
                        mProgress.dismiss();
                        Toast.makeText(MainActivity.this, "Falló al registrar el dispositvo.", Toast.LENGTH_SHORT).show();
                    }
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

    public void setBadge(int index, int counter) {
        BadgeDrawable badge = tabs.getTabAt(index).getOrCreateBadge();
        badge.setVisible(true);
        badge.setNumber(counter);
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
}