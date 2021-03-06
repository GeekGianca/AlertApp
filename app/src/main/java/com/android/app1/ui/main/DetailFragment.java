package com.android.app1.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.app1.MainActivity;
import com.android.app1.R;
import com.android.app1.component.AlertAdapter;
import com.android.app1.model.AlertModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class DetailFragment extends Fragment {
    private static final String TAG = "DetailFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";
    private Context context;
    private FirebaseDatabase mDatabase;

    @BindView(R.id.refreshSms)
    SwipeRefreshLayout refreshSms;
    @BindView(R.id.listSms)
    RecyclerView listSms;

    private List<AlertModel> modelList;

    public DetailFragment() {
    }

    public static Fragment newInstance(int position) {
        DetailFragment fragment = new DetailFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, position);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, root);
        listSms.setHasFixedSize(true);
        LinearLayoutManager manager = new LinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        listSms.setLayoutManager(manager);
        refreshSms.setOnRefreshListener(() -> {
            refreshSms.setRefreshing(true);
            //checkPermissions();
            loadFromFirebase();
        });
        return root;
    }

    private void checkPermissions() {
        Dexter.withContext(context)
                .withPermissions(Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            Toast.makeText(context, "Permisos de mensajes garantizados.", Toast.LENGTH_SHORT).show();
                        }
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            MaterialAlertDialogBuilder build = new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme);
                            build.setTitle("Permisos rechazados")
                                    .setMessage("Para poder tomar una foto necesita habilitar los permisos de lectura y escritura en el dispositivo, de lo contrario seleccione una imagen de la galeria.")
                                    .setPositiveButton("Habilitar", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            Intent settingsPermissions = new Intent();
                                            settingsPermissions.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
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

    private void loadSms() {
        modelList = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int indexBody = cursor.getColumnIndex("body");
        int indexAddress = cursor.getColumnIndex("address");
        int indexDate = cursor.getColumnIndex("date");
        if (indexBody < 0 || !cursor.moveToFirst()) return;
        AlertModel model;
        do {
            Log.d(TAG, cursor.getString(indexAddress));
            if (cursor.getString(indexAddress).equals("322603614")) {
                TimeZone timeZoneUTC = TimeZone.getDefault();
                int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
                SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat simpleFormat2 = new SimpleDateFormat("HH:mm:ss", Locale.US);
                Date date = new Date(cursor.getLong(indexDate) + offsetFromUTC);
                model = new AlertModel();
                model.setDate(simpleFormat.format(date));
                model.setDevice(cursor.getString(indexBody));
                model.setTime(simpleFormat2.format(date));
                modelList.add(model);
            }
        } while (cursor.moveToNext());
        AlertAdapter adapter = new AlertAdapter(modelList, context);
        listSms.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        refreshSms.setRefreshing(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        checkPermissions();
        mDatabase = FirebaseDatabase.getInstance();
        loadFromFirebase();
    }

    private void loadFromFirebase() {
        modelList = new ArrayList<>();
        mDatabase.getReference("departures").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                modelList.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    AlertModel model = postSnapshot.getValue(AlertModel.class);
                    modelList.add(model);
                }
                AlertAdapter adapter = new AlertAdapter(modelList, context);
                listSms.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                refreshSms.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
