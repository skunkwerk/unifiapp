package com.unifiapp.model;

import io.realm.RealmObject;

public class AddedWiFiNetwork extends RealmObject
{
    private String ssid;
    private String mac_address;
    private int frequency;
    private float latitude;
    private float longitude;
    private float altitude;
    private String password;
    private long last_password_check_date;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getMac_address() {
        return mac_address;
    }

    public void setMac_address(String mac_address) {
        this.mac_address = mac_address;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getLast_password_check_date() {
        return last_password_check_date;
    }

    public void setLast_password_check_date(long last_password_check_date) {
        this.last_password_check_date = last_password_check_date;
    }
}