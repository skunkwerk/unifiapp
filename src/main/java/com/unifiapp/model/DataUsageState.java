package com.unifiapp.model;

import java.util.Date;

import io.realm.RealmObject;

public class DataUsageState extends RealmObject
{
    private float last_recorded_all_received_bytes_value;
    private float last_recorded_all_transmitted_bytes_value;
    private float last_recorded_mobile_received_bytes_value;
    private float last_recorded_mobile_transmitted_bytes_value;
    private Date datetime;

    public float getLast_recorded_all_received_bytes_value() {
        return last_recorded_all_received_bytes_value;
    }

    public void setLast_recorded_all_received_bytes_value(float last_recorded_all_received_bytes_value) {
        this.last_recorded_all_received_bytes_value = last_recorded_all_received_bytes_value;
    }

    public float getLast_recorded_all_transmitted_bytes_value() {
        return last_recorded_all_transmitted_bytes_value;
    }

    public void setLast_recorded_all_transmitted_bytes_value(float last_recorded_all_transmitted_bytes_value) {
        this.last_recorded_all_transmitted_bytes_value = last_recorded_all_transmitted_bytes_value;
    }

    public float getLast_recorded_mobile_received_bytes_value() {
        return last_recorded_mobile_received_bytes_value;
    }

    public void setLast_recorded_mobile_received_bytes_value(float last_recorded_mobile_received_bytes_value) {
        this.last_recorded_mobile_received_bytes_value = last_recorded_mobile_received_bytes_value;
    }

    public float getLast_recorded_mobile_transmitted_bytes_value() {
        return last_recorded_mobile_transmitted_bytes_value;
    }

    public void setLast_recorded_mobile_transmitted_bytes_value(float last_recorded_mobile_transmitted_bytes_value) {
        this.last_recorded_mobile_transmitted_bytes_value = last_recorded_mobile_transmitted_bytes_value;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }
}