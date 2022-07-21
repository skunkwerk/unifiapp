package com.unifiapp.receiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.helpshift.Helpshift;
import com.unifiapp.service.GcmIntentService;

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver
{
    public static final String PROPERTY_REG_ID = "registration_id";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("gcm","in onReceive");
        if(intent!=null && intent.getExtras()!=null && intent.getExtras().getString("origin")!=null)
        {
            if(intent.getExtras().getString("origin").equals("helpshift"))
            {
                Helpshift.handlePush(context, intent);
            }
        }
        else
            Log.d("gcm","in onReceive, intent or origin is null");
        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);

        /*String action = intent.getAction();
        else if(action.equals("HS_TOKEN_SEND"))
        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            final String regId = sharedPref.getString(PROPERTY_REG_ID, "");
            Log.d("gcm", "token inside intentreceiver: " + regId.toString());
            if(!regId.equals(""))
            {
                Helpshift.registerDeviceToken(context, regId);
            }
            else
            {
                Helpshift.registerDeviceToken(context, "unreg");
            }
        }
        else if(action.equals("HS_ON_MESSAGE"))
        {
            Log.d("gcm", "GCMIntentReceiver - Message Received " + intent.toString());
            Helpshift.handlePush(context, intent);
        }*/
    }
}
