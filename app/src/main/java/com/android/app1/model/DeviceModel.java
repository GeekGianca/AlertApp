package com.android.app1.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeviceModel {
    private String id;
    private String name;
    private String uuid;
    private String description;
    private String linkImage;

    public DeviceModel(String id, String name, String uuid, String description, String linkImage) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
        this.description = description;
        this.linkImage = linkImage;
    }

    public DeviceModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkImage() {
        return linkImage;
    }

    public void setLinkImage(String linkImage) {
        this.linkImage = linkImage;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> mapDevice = new HashMap<>();
        mapDevice.put("id", UUID.randomUUID().toString());
        mapDevice.put("name", getName());
        mapDevice.put("uuid", getUuid());
        mapDevice.put("description", getDescription());
        mapDevice.put("linkImage", getLinkImage());
        return mapDevice;
    }
}
