package com.unifiapp.model;

import java.util.Date;

import io.realm.RealmObject;

public class DataUsage extends RealmObject
{
    private float data_uploaded_mobile;
    private float data_downloaded_mobile;
    private float data_uploaded_wifi;
    private float data_downloaded_wifi;
    private Date datetime;
    private float latitude;
    private float longitude;
    private float altitude;
    private String anonymous_customer_id_hash;

    public float getData_uploaded_mobile() {
        return data_uploaded_mobile;
    }

    public void setData_uploaded_mobile(float data_uploaded_mobile) {
        this.data_uploaded_mobile = data_uploaded_mobile;
    }

    public float getData_downloaded_mobile() {
        return data_downloaded_mobile;
    }

    public void setData_downloaded_mobile(float data_downloaded_mobile) {
        this.data_downloaded_mobile = data_downloaded_mobile;
    }

    public float getData_uploaded_wifi() {
        return data_uploaded_wifi;
    }

    public void setData_uploaded_wifi(float data_uploaded_wifi) {
        this.data_uploaded_wifi = data_uploaded_wifi;
    }

    public float getData_downloaded_wifi() {
        return data_downloaded_wifi;
    }

    public void setData_downloaded_wifi(float data_downloaded_wifi) {
        this.data_downloaded_wifi = data_downloaded_wifi;
    }

    public String getAnonymous_customer_id_hash() {
        return anonymous_customer_id_hash;
    }

    public void setAnonymous_customer_id_hash(String anonymous_customer_id_hash) {
        this.anonymous_customer_id_hash = anonymous_customer_id_hash;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
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
}