package com.unifiapp.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class NetworkCluster implements ClusterItem
{
    private final LatLng mPosition;
    public String mac_address;
    public String ssid;

    public NetworkCluster(double lat, double lng, String mac_address, String ssid)
    {
        mPosition = new LatLng(lat, lng);
        this.mac_address = mac_address;
        this.ssid = ssid;
    }

    @Override
    public LatLng getPosition()
    {
        return mPosition;
    }
}