package com.unifiapp.utils;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class BlackListWhiteList
{
    List<String> blacklisted_macs;
    List<String> whitelisted_macs;
    Context context;

    public BlackListWhiteList(Context context)
    {
        this.context = context;
        readBlackListWhiteListFiles();
    }

    public void readBlackListWhiteListFiles()
    {
        //original file from https://code.wireshark.org/review/gitweb?p=wireshark.git;a=blob_plain;f=manuf;hb=HEAD
        blacklisted_macs = new ArrayList<String>();
        whitelisted_macs = new ArrayList<String>();
        try
        {
            InputStream fis = context.getAssets().open("serialized_blacklist");
            ObjectInputStream ois = new ObjectInputStream(fis);
            blacklisted_macs = (ArrayList) ois.readObject();
            ois.close();
            fis.close();

            fis = context.getAssets().open("serialized_whitelist");
            ois = new ObjectInputStream(fis);
            whitelisted_macs = (ArrayList) ois.readObject();
            ois.close();
            fis.close();
        }
        catch(Exception e)
        {
            Crashlytics.log(Log.ERROR, "ShareFragment", "could not read black/whitelist files");
            Crashlytics.logException(e);
        }
    }

    public Boolean mac_blacklisted(String mac_address)
    {
        if (blacklisted_macs.contains(mac_address.substring(0,8).toUpperCase()))
            return true;
        return false;
    }

    public Boolean mac_whitelisted(String mac_address)
    {
        if (whitelisted_macs.contains(mac_address.substring(0,8).toUpperCase()))
            return true;
        return false;
    }
}
