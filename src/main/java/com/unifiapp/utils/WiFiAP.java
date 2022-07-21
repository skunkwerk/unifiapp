package com.unifiapp.utils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

public class WiFiAP
{
    Context context;
    Utilities utils;

    public WiFiAP(Context context, Utilities utils)
    {
        this.context = context;
        this.utils = utils;
    }

    //check whether WiFi hotspot on or off
    public boolean isAPOn()
    {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try
        {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
        return false;
    }

    // turn WiFi hotspot on or off
    public boolean configAPState(Boolean enable)
    {
        //Log.d("in configAPState with enable:" + enable.toString(), "WiFiAP");
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try
        {
            //check to see whether it's necessary to make this change
            if(isAPOn() && enable==false || !isAPOn() && enable==true)
            {
                if(enable)
                    wifimanager.setWifiEnabled(!enable);//turn off WiFi before turning on the AP
                Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(wifimanager, wificonfiguration, enable);
                if(!enable)
                    wifimanager.setWifiEnabled(!enable);//turn on WiFi after disabling the AP
            }
            else
            {
                //Log.d("not toggling AP state","WiFiAP");
            }
            return true;
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
        return false;
    }
}