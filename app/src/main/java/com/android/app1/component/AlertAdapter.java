package com.android.app1.component;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.app1.R;
import com.android.app1.model.AlertModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AlertAdapter extends RecyclerView.Adapter<AlertViewHolder> {
    private List<AlertModel> alertModelList;
    private Context context;

    public AlertAdapter(List<AlertModel> alertModelList, Context context) {
        this.alertModelList = alertModelList;
        this.context = context;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_alert_detail_layout, parent, false);
        return new AlertViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertModel a = alertModelList.get(position);
        holder.info.setText(String.format("Alerta de %s", a.getType()));
        holder.dateOut.setText(String.format("Fecha salida: %s", a.getDate()));
        holder.timeOut.setText(String.format("Hora salida: %s", a.getTime()));
        holder.deviceOut.setText("Dispositivo: " + a.getDevice());
    }

    @Override
    public int getItemCount() {
        return alertModelList.size();
    }
}

class AlertViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.info)
    TextView info;
    @BindView(R.id.deviceOut)
    TextView deviceOut;
    @BindView(R.id.dateOut)
    TextView dateOut;
    @BindView(R.id.timeOut)
    TextView timeOut;

    public AlertViewHolder(@NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
