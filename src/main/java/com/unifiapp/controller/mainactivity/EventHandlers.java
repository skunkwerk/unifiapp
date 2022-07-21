package com.unifiapp.controller.mainactivity;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.helpshift.HSAlertToRateAppListener;
import com.helpshift.Helpshift;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.MainActivity;
import com.unifiapp.R;
import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient;
import com.unifiapp.utils.Utilities;
import com.unifiapp.view.OKOrHelpDialogFragment;
import com.unifiapp.view.OperatorDialogFragment;
import com.unifiapp.vpn.data.VpnProfileDataSource;
import com.unifiapp.vpn.logic.CharonVpnService;

import de.greenrobot.event.EventBus;

public class EventHandlers
{
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    Context context;
    APIClient.API api;
    Utilities utils;
    Callbacks callbacks;
    Activity activity;
    DialogFragment okOrHelpDialog;
    DialogFragment operatorDialog;
    FragmentManager supportFragmentManager;
    Boolean isVisible;
    MainActivity main;

    public EventHandlers(SharedPreferences sharedPrefs, Context context, APIClient.API api, Utilities utils, Activity activity, FragmentManager supportFragmentManager, MainActivity main)
    {
        this.sharedPrefs = sharedPrefs;
        this.editor = sharedPrefs.edit();
        this.context = context;
        this.api = api;
        this.utils = utils;
        this.activity = activity;
        this.callbacks = new Callbacks(context, utils);
        this.main = main;
        this.supportFragmentManager = supportFragmentManager;
        EventBus.getDefault().register(this);
    }

    public void unregister()
    {
        //eventbus will continue to run in background, so we do this here & not in onPause
        EventBus.getDefault().unregister(this);
    }

    public void setVisible(Boolean visible)
    {
        isVisible = visible;
    }

    public void onEvent(Events.DisconnectFromWiFiEvent event)
    {
        WifiManager wifi = (WifiManager) main.getSystemService(Context.WIFI_SERVICE);
        wifi.disconnect();
        //we don't have to do anything else, as this will trigger the connectivityChangeReceiver,
        //which will then call WiFiDisconnectedEvent and do the deletion of the WiFiConfiguration and setting of
        //connected_to_community_hotspot to false
    }

    /**
     *
     * @param event
     */
    public void onEvent(Events.WiFiDisconnectedEvent event)
    {
        //this is only fired when we were previously connected to a community hotspot
        //disconnect from the VPN too
        Events.DisconnectFromVPNEvent disconnectFromVPNEvent = new Events.DisconnectFromVPNEvent();
        EventBus.getDefault().post(disconnectFromVPNEvent);
        //if it was a community hotspot, delete the WiFiConfiguration from the device
        int netId = sharedPrefs.getInt("connected_to_community_hotspot_net_id",-1);
        utils.deleteWiFiConfiguration(netId);

        //set connected_to_community_hotspot to false
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("connected_to_community_hotspot",false);
        editor.commit();
    }

    public void onEvent(Events.DisconnectFromVPNEvent event)
    {
        //we try/catch this, as the VPNService may not even be started
        try
        {
            Intent intent = new Intent(main, CharonVpnService.class);
            intent.putExtras(main.mProfileInfo);
            main.stopService(intent);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     * @param event
     */
    public void onEventMainThread(Events.VPNStatusUpdateEvent event)
    {
        //on the main thread, as CharonVpnService is calling this from a background thread
        //we don't use the native enum definitions as they're split among two types
        /*
        NO_MATCHING_SSL_CERTIFICATE_FOUND
        REVOKED
        DISABLED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        NO_ERROR,
		AUTH_FAILED,
		PEER_AUTH_FAILED,
		LOOKUP_FAILED,
		UNREACHABLE,
		GENERIC_ERROR*/

        String status = event.getStatus();
        Analytics.with(context).track("VPN State Changed", new Properties().putValue("state", status));

        if(status.equals("REVOKED")||status.equals("DISABLED")||status.equals("DISCONNECTING"))
        {
            //disconnect from the WiFi network too
            Events.VPNDisconnectedEvent vpnDisconnectedEvent = new Events.VPNDisconnectedEvent();
            EventBus.getDefault().post(vpnDisconnectedEvent);
        }
    }

    /**
     * Prepare the VpnService. If this succeeds the current VPN profile is started.
     * */
    public void onEvent(Events.StartVPNEvent event)
    {
        final int PREPARE_VPN_SERVICE = 0;

        Bundle profileInfo = new Bundle();
        profileInfo.putLong(VpnProfileDataSource.KEY_ID, 1);
        profileInfo.putString(VpnProfileDataSource.KEY_USERNAME, "app");
        profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, "rH8EpJb85H2A");
        Intent intent;
        try
        {
            intent = VpnService.prepare(context);
        }
        catch (IllegalStateException ex)
        {
			/* this happens if the always-on VPN feature (Android 4.2+) is activated */
            Events.VPNNotSupportedEvent vpnEvent = new Events.VPNNotSupportedEvent();
            EventBus.getDefault().post(vpnEvent);
            return;
        }
		/* store profile info until the user grants us permission */
        main.mProfileInfo = profileInfo;
        if (intent != null)
        {
            try
            {
                main.startActivityForResult(intent, PREPARE_VPN_SERVICE);
            }
            catch (ActivityNotFoundException ex)
            {
				/* it seems some devices, even though they come with Android 4,
				 * don't have the VPN components built into the system image.
				 * com.android.vpndialogs/com.android.vpndialogs.ConfirmDialog
				 * will not be found then */
                Events.VPNNotSupportedEvent vpnEvent = new Events.VPNNotSupportedEvent();
                EventBus.getDefault().post(vpnEvent);
            }
        }
        else
        {	/* user already granted permission to use VpnService */
            main.onActivityResult(PREPARE_VPN_SERVICE, -1, null);
        }
    }

    /**
     * Class representing an error message which is displayed if VpnService is
     * not supported on the current device.
     */
    public void onEvent(Events.VPNNotSupportedEvent event)
    {
        Log.d("home","vpn not supported error");
        Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(context.getResources().getString(R.string.unsupported_device_vpn_title), context.getResources().getString(R.string.unsupported_device_vpn_text), true);
        EventBus.getDefault().post(dialogEvent);
        Analytics.with(context).track("VPN unsupported on device");
    }

    public void onEvent(Events.VPNDisconnectedEvent event)
    {
        //fire the request to disconnect from the WiFi network too
        Events.DisconnectFromWiFiEvent disconnectFromWiFiEvent = new Events.DisconnectFromWiFiEvent();
        EventBus.getDefault().post(disconnectFromWiFiEvent);
    }

    public void onEvent(Events.SendRegistrationIdEvent event)
    {
        Log.d("gcm", "received sendregistrationidevent");
        final String regid = utils.getRegistrationId(context);
        try
        {
            Thread register_helpshift_thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Helpshift.registerDeviceToken(context, regid);
                    }
                    catch (Exception e)
                    {
                        Log.e("Error: Couldn't register Helpshift device token: ", "" + e);
                        Crashlytics.log(Log.ERROR, "MainActivity", "Couldn't register Helpshift device token");
                        Analytics.with(context).track("Couldn't register Helpshift device token");
                        Crashlytics.logException(e);
                    }
                }
            });
            register_helpshift_thread.start();
            //send it to our database too, so we can send them push notifications ourselves
            Log.d("reg in prefs is:\n",sharedPrefs.getString("registration_id",""));
            if(regid!=null && sharedPrefs!=null && sharedPrefs.getInt("customer_id",0)!=0 && sharedPrefs.getString("first_name","").equals("")==false && sharedPrefs.getString("last_name","").equals("")==false)
            {
                //TODO: this first call to update_customer is deprecated now
                api.update_customer(sharedPrefs.getInt("customer_id", 0), sharedPrefs.getInt("customer_id", 0), sharedPrefs.getString("first_name", ""), sharedPrefs.getString("last_name", ""), sharedPrefs.getString("email_address", ""), sharedPrefs.getString("phone_number", ""), sharedPrefs.getString("signup_date", ""), "facebook", sharedPrefs.getString("facebook_user_id", ""), regid, utils.getAppVersion(context), callbacks.update_customer_callback);
                api.register_new_arn(sharedPrefs.getInt("customer_id",0),regid,callbacks.arn_callback);
            }
        }
        catch (Exception e)
        {
            Log.e("Error: Couldn't register Helpshift device token: ", "" + e);
            Crashlytics.log(Log.ERROR, "MainActivity", "Couldn't register Helpshift device token");
            Crashlytics.logException(e);
        }
    }

    public void onEvent(Events.PromptToRateApp event)
    {
        String url = "https://play.google.com/store/apps/details?id=com.unifiapp";

        HSAlertToRateAppListener actionListener =  new HSAlertToRateAppListener()
        {
            @Override
            public void onAction(Helpshift.HS_RATE_ALERT action)
            {
                switch (action) {
                    case CLOSE:
                        Analytics.with(context).track("Prompted user for Rating, clicked Close");

                        break;
                    case FEEDBACK:
                        Analytics.with(context).track("Prompted user for Rating, clicked Send Feedback");
                        break;
                    case SUCCESS:
                        Analytics.with(context).track("Prompted user for Rating, clicked Rate");
                        editor.putBoolean("rated_app", true);
                        editor.commit();
                        break;
                    case FAIL:
                        Analytics.with(context).track("Failed to Prompt user for Rating");
                        break;
                }
            }
        };
        Boolean rated_app = sharedPrefs.getBoolean("rated_app",false);
        if(rated_app==false)
        {
            Helpshift.showAlertToRateApp(url, actionListener);
        }
    }

    public void onEvent(Events.ShowFAQEvent event)
    {
        try
        {
            String id = event.getId();
            Helpshift.showFAQSection(activity, id);
            Analytics.with(context).screen("F.A.Q.","");
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "MainActivity", "error showing FAQ section");
            Crashlytics.logException(e);
        }
    }

    public void onEvent(Events.MoveActivityToBackEvent event)
    {
        try
        {
            main.moveTaskToBack(true);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public void onEvent(Events.DisplayOperatorDialogEvent event)//onEventMainThread
    {
        try
        {
            if(isVisible==false)
            {
                //just send them a notification
                NotificationManager mNotificationManager = (NotificationManager)
                        activity.getSystemService(Context.NOTIFICATION_SERVICE);

                Intent intent = new Intent(activity, MainActivity.class);
                intent.setAction("display_dialog");
                intent.putExtra("dialog","operator");
                PendingIntent contentIntent = PendingIntent.getActivity(activity, 0,
                        intent, PendingIntent.FLAG_CANCEL_CURRENT);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(activity)
                                .setSmallIcon(R.drawable.logo_rings)
                                .setContentTitle(event.getTitle())
                                .setAutoCancel(true)
                                .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(event.getMsg()))
                                .setContentText(event.getMsg());
                mBuilder.setContentIntent(contentIntent);
                mNotificationManager.notify(-1, mBuilder.build());

                //if you try to display the dialog while the app is not visible, get this crash:
                //'java.lang.IllegalStateException: Cannot perform this action after onSaveInstanceState' exception
                //to prevent this, you can't do this if the app is not visible (ie the user ran out of patience, clicked the home button
                //you send the notification, and in the notification reader when the app resumes, you create the dialog
            }
            else
            {
                //do in-app dialog
                DisplayOperatorDialog();
            }
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "MainActivity", "error displaying operator dialog");
            Crashlytics.logException(e);
        }
    }

    public void DisplayOperatorDialog()
    {
        FragmentTransaction ft = supportFragmentManager.beginTransaction();
        Fragment prev = supportFragmentManager.findFragmentByTag("operator");
        if (prev != null)
        {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        operatorDialog = new OperatorDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getResources().getString(R.string.operator_dialog_title));//event.getTitle());
        args.putString("message", context.getResources().getString(R.string.operator_dialog_text));//event.getMsg());
        args.putBoolean("display_help_button", true);////event.getShowHelpButton());
        operatorDialog.setArguments(args);
        operatorDialog.show(ft,"operator");
    }

    public void onEvent(Events.SendCreditWithOperatorEvent event)
    {
        Log.d("received sendcreditwithoperatorevent","");
        String phone_number = sharedPrefs.getString("phone_number", "0");
        api.send_credit_with_operator(phone_number, sharedPrefs.getInt("customer_id", 0), event.getOperator(), String.valueOf(utils.getAppVersion(context)), "False", callbacks.send_credit_callback);
        Analytics.with(context).track("Requesting Prepaid Credit Transfer to Customer", new Properties().putValue("phone_number", phone_number));
        if (operatorDialog!=null)
            operatorDialog.dismiss();
    }

    public void onEvent(Events.HelpRequestedEvent event)
    {
        Log.d("received helprequestedevent","");
        Analytics.with(context).screen("Chat with Support","");
        Helpshift.showConversation(activity, utils.getHelpshiftConfig());
        if (okOrHelpDialog!=null)
            okOrHelpDialog.dismiss();
        if (operatorDialog!=null)
            operatorDialog.dismiss();
    }

    public void onEvent(Events.DisplayFragmentEvent event)
    {
        Log.d("received displayfragmentevent","");
        int fragment = event.getFragment();
        Bundle arguments = event.getArguments();
        main.displayView(fragment, arguments);
    }

    public void onEvent(Events.DisplayOKOrHelpDialogEvent event)
    {
        try
        {
            if(isVisible==true)
            {
                if(okOrHelpDialog!=null)
                    okOrHelpDialog.dismiss();//otherwise we crash with a 'Fragment already added' error
                //in foreground, do an in-app dialog
                okOrHelpDialog = new OKOrHelpDialogFragment();
                Bundle args = new Bundle();
                args.putString("title", event.getTitle());
                args.putString("message", event.getMsg());
                args.putBoolean("display_help_button", event.getShowHelpButton());
                okOrHelpDialog.setArguments(args);

                FragmentTransaction ft = supportFragmentManager.beginTransaction();
                Fragment prev = supportFragmentManager.findFragmentByTag("credit");
                if (prev != null)
                {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                okOrHelpDialog.show(ft,"credit");
            }
            else
            {
                //just send them a notification, without an in-app dialog
                NotificationManager mNotificationManager = (NotificationManager)
                        activity.getSystemService(Context.NOTIFICATION_SERVICE);

                PendingIntent contentIntent = PendingIntent.getActivity(activity, 0,
                        new Intent(activity, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(activity)
                                .setSmallIcon(R.drawable.logo_rings)
                                .setContentTitle(event.getTitle())
                                .setAutoCancel(true)
                                .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(event.getMsg()))
                                .setContentText(event.getMsg());
                mBuilder.setContentIntent(contentIntent);
                mNotificationManager.notify(-1, mBuilder.build());
            }
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "MainActivity", "error displaying OK or help dialog");
            Crashlytics.logException(e);
        }
    }
}
