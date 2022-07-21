package com.unifiapp.model;

import io.realm.RealmObject;

public class WiFiNetworkAccess extends RealmObject
{
    private String ssid;
    private String mac_address;
    private String encrypted_password;
    private String authentication_algorithm;

    public String getAuthentication_algorithm() {
        return authentication_algorithm;
    }

    public void setAuthentication_algorithm(String authentication_algorithm) {
        this.authentication_algorithm = authentication_algorithm;
    }

    public String getEncrypted_password() {
        return encrypted_password;
    }

    public void setEncrypted_password(String encrypted_password) {
        this.encrypted_password = encrypted_password;
    }

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

}