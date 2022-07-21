package com.unifiapp.utils;

public class ConnectedWifi
{
    public String ssid;
    public int network_id;
    public String message;
    public Boolean success;
    public String auth;
    public String mac;
    public Boolean tethered;
    public String ip_address;
    public String local_ip_address;

    public ConnectedWifi(String ssid, int network_id, String message, Boolean success, String auth, Boolean tethered, String mac_address, String ip_address, String local_ip_address)
    {
        this.ssid = ssid;
        this.network_id = network_id;
        this.message = message;
        this.success = success;
        this.auth = auth;
        this.mac = mac_address;
        this.tethered = tethered;
        this.ip_address = ip_address;
        this.local_ip_address = local_ip_address;
    }
}