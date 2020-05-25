package com.android.app1.component;

import android.content.Context;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.app1.R;
import com.android.app1.model.DeviceModel;
import com.bumptech.glide.Glide;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> {
    private List<DeviceModel> modelList;
    private Context context;

    public DeviceAdapter(List<DeviceModel> modelList, Context context) {
        this.modelList = modelList;
        this.context = context;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_device_register_layout, parent, false);
        return new DeviceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceModel d = modelList.get(position);
        if (d.getLinkImage() != null) {
            Glide.with(context)
                    .load(d.getLinkImage())
                    .into(holder.contentImage);
        } else {
            holder.contentImage.setImageResource(R.drawable.ic_image_not_found);
        }
        holder.titleDevice.setText(d.getName());
        holder.idDevice.setText(d.getUuid());
        holder.position.setText(d.getDescription());
    }

    @Override
    public int getItemCount() {
        return modelList.size();
    }
}

class DeviceViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

    @BindView(R.id.contentImage)
    ImageView contentImage;
    @BindView(R.id.titleDevice)
    TextView titleDevice;
    @BindView(R.id.idDevice)
    TextView idDevice;
    @BindView(R.id.position)
    TextView position;

    DeviceViewHolder(@NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        itemView.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Opciones");
        menu.add(0, 0, getAdapterPosition(), "Permitir salida");
        menu.add(0, 1, getAdapterPosition(), "Remover");
    }
}
