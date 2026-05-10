package com.example.campusbiome.models;

public class WifiRouter {
    private String id;
    private String ssid;
    private float x;
    private float y;
    private int connected_devices;

    public WifiRouter() {
        // Default constructor required for calls to DataSnapshot.getValue(WifiRouter.class)
    }

    public WifiRouter(String id, String ssid, float x, float y, int connected_devices) {
        this.id = id;
        this.ssid = ssid;
        this.x = x;
        this.y = y;
        this.connected_devices = connected_devices;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getConnected_devices() {
        return connected_devices;
    }

    public void setConnected_devices(int connected_devices) {
        this.connected_devices = connected_devices;
    }
}
