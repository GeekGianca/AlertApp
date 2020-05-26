package com.android.app1.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.app1.MainActivity;
import com.android.app1.R;
import com.android.app1.component.DeviceAdapter;
import com.android.app1.component.Injected;
import com.android.app1.component.ViewModelProviderFactory;
import com.android.app1.model.AlertModel;
import com.android.app1.model.DeviceModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {
    private static final String TAG = "PlaceholderFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    private Context context;
    private PageViewModel pageViewModel;
    private ViewModelProviderFactory factory;
    private SwipeRefreshLayout refresh;
    private RecyclerView listDevices;
    private FirebaseDatabase mDatabase;
    private List<DeviceModel> deviceModelList;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        refresh = root.findViewById(R.id.refresh);
        listDevices = root.findViewById(R.id.listDevices);
        listDevices.setHasFixedSize(true);
        LinearLayoutManager manager = new LinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        listDevices.setLayoutManager(manager);

        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh.setRefreshing(true);
                loadDevices();
            }
        });
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        factory = Injected.viewModelFactory(context);
        pageViewModel = new ViewModelProvider(this, factory).get(PageViewModel.class);
        pageViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                Log.d(TAG, "Result: " + s);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
        loadDevices();
    }

    private void loadDevices() {
        refresh.setRefreshing(true);
        deviceModelList = new ArrayList<>();
        mDatabase = FirebaseDatabase.getInstance();
        mDatabase.getReference("devices")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        deviceModelList.clear();
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            DeviceModel model = postSnapshot.getValue(DeviceModel.class);
                            deviceModelList.add(model);
                        }
                        addToAdapter();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(context, "Error al cargar los datos.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addToAdapter() {
        DeviceAdapter adapter = new DeviceAdapter(deviceModelList, context);
        listDevices.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        refresh.setRefreshing(false);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals("Permitir salida")) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage("3226036144", null, deviceModelList.get(item.getOrder()).getUuid(), null, null);
                AlertModel model = new AlertModel();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss", Locale.US);
                model.setTime(stf.format(new Date()));
                model.setDate(sdf.format(new Date()));
                model.setDevice(deviceModelList.get(item.getOrder()).getName());
                mDatabase.getReference().child("departures").child(UUID.randomUUID().toString())
                        .setValue(model.toMap())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(context, "Se ha solicitado el permiso.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(context, "Error al solicitar permiso en el dispositivo.", Toast.LENGTH_SHORT).show();
            }
        } else if (item.getTitle().equals("Remover")) {
            DeviceModel model = deviceModelList.get(item.getOrder());
            Query deviceQ = mDatabase.getReference("devices").orderByChild("id").equalTo(model.getId());
            deviceQ.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot appleSnapshot: dataSnapshot.getChildren()) {
                        appleSnapshot.getRef().removeValue();
                    }
                    Toast.makeText(context, "Se elimino correctamente.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(context, "Error al eliminar.", Toast.LENGTH_SHORT).show();
                }
            });

        }
        return super.onContextItemSelected(item);
    }
}