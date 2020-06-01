package com.android.app1.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlertModel {
    private String id;
    private String date;
    private String time;
    private String device;
    private String type;

    public AlertModel(String date, String time, String device, String type) {
        this.date = date;
        this.time = time;
        this.device = device;
        this.type = type;
    }

    public AlertModel() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> al = new HashMap<>();
        al.put("id", UUID.randomUUID().toString());
        al.put("device", getDevice());
        al.put("date", getDate());
        al.put("time", getTime());
        al.put("type", getType());
        return al;
    }
}
