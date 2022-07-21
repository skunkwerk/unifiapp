package com.unifiapp.model;

import java.util.Date;

import io.realm.RealmObject;

public class Connectivity extends RealmObject
{
    private boolean data_connection_active;
    private boolean wifi_on;
    private String connection_type;
    private Date datetime;

    public boolean isData_connection_active() {
        return data_connection_active;
    }

    public void setData_connection_active(boolean data_connection_active) {
        this.data_connection_active = data_connection_active;
    }

    public boolean isWifi_on() {
        return wifi_on;
    }

    public void setWifi_on(boolean wifi_on) {
        this.wifi_on = wifi_on;
    }

    public String getConnection_type() {
        return connection_type;
    }

    public void setConnection_type(String connection_type) {
        this.connection_type = connection_type;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }
}

