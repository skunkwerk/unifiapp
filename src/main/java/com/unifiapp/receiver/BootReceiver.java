package com.unifiapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.unifiapp.service.ConnectToWifi;
import com.unifiapp.service.Statistics;

public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        //we double check here for only boot complete event
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED))
        {
            Intent statsService = new Intent(context, Statistics.class);
            context.startService(statsService);
            Intent wifiService = new Intent(context, ConnectToWifi.class);
            context.startService(wifiService);
        }
    }
}
