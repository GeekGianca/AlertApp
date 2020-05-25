package com.android.app1.model;

public class AlertModel {
    private String date;
    private String time;
    private String device;

    public AlertModel(String date, String time, String device) {
        this.date = date;
        this.time = time;
        this.device = device;
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
}
