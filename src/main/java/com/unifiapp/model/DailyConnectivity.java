package com.unifiapp.model;

import java.util.Date;

import io.realm.RealmObject;

public class DailyConnectivity extends RealmObject
{
    private float data_connection_active_average;
    private float wifi_on_average;
    private float wifi_connected_average;
    private float mobile_data_connected_average;
    private Date date;
    private String anonymous_customer_id_hash;

    public String getAnonymous_customer_id_hash() {
        return anonymous_customer_id_hash;
    }

    public void setAnonymous_customer_id_hash(String anonymous_customer_id_hash) {
        this.anonymous_customer_id_hash = anonymous_customer_id_hash;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public float getData_connection_active_average() {
        return data_connection_active_average;
    }

    public void setData_connection_active_average(float data_connection_active_average) {
        this.data_connection_active_average = data_connection_active_average;
    }

    public float getWifi_on_average() {
        return wifi_on_average;
    }

    public void setWifi_on_average(float wifi_on_average) {
        this.wifi_on_average = wifi_on_average;
    }

    public float getWifi_connected_average() {
        return wifi_connected_average;
    }

    public void setWifi_connected_average(float wifi_connected_average) {
        this.wifi_connected_average = wifi_connected_average;
    }

    public float getMobile_data_connected_average() {
        return mobile_data_connected_average;
    }

    public void setMobile_data_connected_average(float mobile_data_connected_average) {
        this.mobile_data_connected_average = mobile_data_connected_average;
    }
}

