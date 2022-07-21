package com.unifiapp.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.MainActivity;
import com.unifiapp.R;
import com.unifiapp.events.Events;
import com.unifiapp.events.Events.StartAPEvent;
import com.unifiapp.events.Events.StopAPEvent;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIClient.API;
import com.unifiapp.model.APIClient.WifiRouter;
import com.unifiapp.model.APIClient.WifiRouterAccess;
import com.unifiapp.model.APIFactory;
import com.unifiapp.model.AddedWiFiNetwork;
import com.unifiapp.model.WiFiNetworkAccess;
import com.unifiapp.utils.MACList;
import com.unifiapp.utils.Utilities;
import com.unifiapp.utils.WiFiAP;

import java.lang.reflect.Method;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.greenrobot.event.EventBus;
import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

//querying the database for the password and connecting to a wifi network are blocking calls
//that need to be done in a background thread
//even though the service is in its own process, you still can't
//make network calls on its main thread

public class ConnectToWifi extends Service
{	
	private NetworkInfo wifiInfo;
    private Utilities utils;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;
    private APIClient.API api;
    private Realm realm;
    private WifiRouterAccess routerAccess;
    private Context context;
    WifiManager wifi;
    private MACList macList;
    ConnectivityManager connManager;
    ScheduledThreadPoolExecutor threadPoolExecutor;
    ScheduledThreadPoolExecutor wifiThreadPoolExecutor;
    private long previous_wifi_scan_received_time;
    Boolean wifi_scan_receiver_registered;
    ScanResult highestSignalResult;
    Boolean connectionSuccessful;
    int netId;
    NetworkInfo activeNetwork;
    String loggedAlreadyConnectedMacAddress;
    String loggedFoundMatchForMacAddress;
    String loggedNotConnectingFailedWarningMacAddress;
    String loggedWifiNotOn;
    String loggedMacNotInCustomerRouters;
    String loggedScannedNetworkAddedByCustomer;
    boolean callbackCheckPassword;
    BroadcastReceiver powerReceiver;
    Boolean power_receiver_registered;
    Boolean ScreenOn;
    WiFiAP wifiAP;
	
	public ConnectToWifi()
	{
        previous_wifi_scan_received_time = 0;
	}

    @Override
    public void onCreate()
    {
        super.onCreate();
        context = this;
        try
        {
            Fabric.with(this, new Crashlytics());
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            utils = new Utilities(context);
            if(utils!=null)
            {
                utils.setupDatabase("ConnectToWifi");
            }
            else
            {
                utils.logMessage("MAJOR ISSUE - utils is null","ConnectToWifi");
            }
            if(sharedPrefs!=null)
            {
                editor = sharedPrefs.edit();
            }
            else
            {
                utils.logMessage("MAJOR ISSUE - sharedPrefs is null","ConnectToWifi");
            }
            macList = new MACList(context, sharedPrefs);
            //check to see when we last synced the MAC list with the server
            if(macList!=null)
            {
                macList.syncWithServer();
            }
            else
            {
                utils.logMessage("MAJOR ISSUE - macList is null","ConnectToWifi");
            }
            APIFactory factory = new APIFactory();
            if(factory!=null)
            {
                api = factory.getAPI();
            }
            else
            {
                utils.logMessage("MAJOR ISSUE - factory is null","ConnectToWifi");
            }
            realm = Realm.getInstance(context, false);//we set auto-refresh off, otherwise we get the erorr: java.lang.IllegalStateException: Cannot set auto-refresh in a Thread without a Looper
            syncOfflineNetworkAccess();
            EventBus.getDefault().register(this);
            startWiFiScan();
            setupTimer();
            //todo: turn back on
            //wifiAP = new WiFiAP(context, utils);
            //setupAP();
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public void setupTimer()
    {
        try
        {
            threadPoolExecutor = new ScheduledThreadPoolExecutor(1);//1 thread
            threadPoolExecutor.scheduleWithFixedDelay(new Runnable()
            {
                @Override
                public void run()
                {
                    macList.syncWithServer();
                    syncOfflineNetworkAccess();
                    syncAddedNetworks();
                }
            }, 0, 1, TimeUnit.HOURS);//run it every hour, after the end of the previous call to run()
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public void syncOfflineNetworkAccess()
    {
        try
        {
            //only do the sync if the screen is off (ie in background)
            //TODO: remove if you're not in Delhi/Bay Area once performance issues fixed
            Log.d("in WiFi", "sync offline network access");
            float latitude = sharedPrefs.getFloat("coarse_latitude",0);
            float longitude = sharedPrefs.getFloat("coarse_longitude",0);
            long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
            long last_run_time = sharedPrefs.getLong("last_offline_network_access_sync_date",0);
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();
            if((now - last_run_time) > 600 && isScreenOn==false &&
                    ((Math.abs(latitude-28.6100)<=0.5 && Math.abs(longitude-77.2300)<=0.5) ||
                            (Math.abs(latitude-37.7833)<=1.5 && Math.abs(-1*longitude-122.4167)<=1.5)))
            {
                //6 hours in seconds
                api.offline_network_access("mumbai", last_run_time, offline_callback);//TODO: mumbai
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public void syncAddedNetworks()
    {
        try
        {
            long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
            long last_run_time = sharedPrefs.getLong("last_customer_routers_added_sync_date",0);
            if(now - last_run_time > 21600) //6 hours in seconds
            {
                api.customer_routers(sharedPrefs.getInt("customer_id",0), customerRoutersCallback);
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /*

     */
    public void setupAP()
    {
        //todo: turn back on
        /*IntentFilter PowerIntentFilter = new IntentFilter();
        PowerIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        PowerIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        BroadcastReceiver powerReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent i)
            {
                // charger is connected
                if (i.getAction().equals(Intent.ACTION_POWER_CONNECTED))
                {
                    //Log.d("power connected","ConnectToWiFi");
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    boolean isScreenOn = pm.isScreenOn();
                    if(isScreenOn==false)
                    {
                        StartAPEvent APEvent = new StartAPEvent();
                        EventBus.getDefault().post(APEvent);
                    }
                }
                // charger is disconnected
                if (i.getAction().equals(Intent.ACTION_POWER_DISCONNECTED))
                {
                    //Log.d("power disconnected","ConnectToWiFi");
                    StopAPEvent APEvent = new StopAPEvent();
                    EventBus.getDefault().post(APEvent);
                }
            }
        };

        IntentFilter ScreenIntentFilter = new IntentFilter();
        ScreenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        ScreenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver screenReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if(intent.getAction().equals(Intent.ACTION_SCREEN_ON))
                {
                    //NEED TO SEE IF POWER ON/OFF - CAN'T RELY ON VARIABLES
                    //Log.d("screen on","ConnectToWiFi");
                    StopAPEvent APEvent = new StopAPEvent();
                    EventBus.getDefault().post(APEvent);
                }
                else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                {
                    //Log.d("screen off","ConnectToWiFi");
                    Intent powerIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    int plugged = powerIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    if(plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB)
                    {
                        StartAPEvent APEvent = new StartAPEvent();
                        EventBus.getDefault().post(APEvent);
                    }
                }
            }
        };

        // registering receivers
        registerReceiver(powerReceiver, PowerIntentFilter);
        registerReceiver(screenReceiver, ScreenIntentFilter);*/
    }

    public void startWiFiScan()
    {
        try
        {
            if(utils.isDeviceRooted()==false)
            {
                this.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                wifi_scan_receiver_registered = true;
                wifiThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);//1 thread
                wifiThreadPoolExecutor.scheduleWithFixedDelay(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d("in WiFi", "starting WiFi scan");
                        wifi.startScan();
                        //we don't care if the previous call to startScan finished or not
                    }
                }, 0, 1, TimeUnit.MINUTES);//run it every minute, after the end of the previous call to run()
            }
            else
            {
                Analytics.with(getApplicationContext()).track("Device is Rooted - not attempting to scan for WiFi networks");
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

	void attemptConnection(ArrayList<ScanResult> wifiNetworks, boolean checkPassword, String currentMacAddress)
	{
		// do stuff here in background
		try
        {
            routerAccess = null;
            callbackCheckPassword = checkPassword;
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            connectionSuccessful = false;
            highestSignalResult = null;
            netId = -1;

            API api = new APIFactory().getAPI();

            //we've already checked the MAC list, so there's a high probability the database has this network

            //TODO:
            //if the MAC address matches with any network the user themselves has shared, don't attempt to connect
            //as the user may have manually chosen not to connect to that network, and we don't want them going through the VPN
            //for a network they already have access to
            //unfortunately, we only store them by SSID locally, and the WiFiConfigurations on the device are also only by SSID
            //which poses an issue, as many SSIDs are common among many routers

            //if checkPassword==true, then use the currentMacAddress to find which of the wifiNetworks to connect to
            //as a user may have submitted two networks that are both in range, or only one, but someone else has submitted the other, etc.

            activeNetwork = connManager.getActiveNetworkInfo();
            if((wifiInfo!=null && wifiInfo.isConnectedOrConnecting()==false) || (activeNetwork!=null && activeNetwork.getType()!=ConnectivityManager.TYPE_WIFI))
            {
                utils.logMessage("Found community MAC in WiFi scan results, and not connected to WiFi - attempting to connect", "ConnectToWifi", new Properties().putValue("check_password",checkPassword));
                //query the REST API for the password with each router's MAC address
                //if we have multiple access points we can connect to, we pick the one with the highest signal level (RSSI)
                //TODO: change this criteria later, to use the one we have the password for, ie keep trying until one connects
                int highestLevel = 0;
                if(checkPassword==true && currentMacAddress!=null)
                {
                    for(ScanResult result : wifiNetworks)
                    {
                        if(result.BSSID.toUpperCase().equals(currentMacAddress.toUpperCase()))
                        {
                            highestSignalResult = result;
                            break;
                        }
                    }
                }
                else
                {
                    if(wifiNetworks.size()>0)
                    {
                        highestSignalResult = wifiNetworks.get(0);
                        highestLevel = wifiNetworks.get(0).level;
                    }
                    for(ScanResult result : wifiNetworks)
                    {
                        if(result.level > highestLevel)
                        {
                            highestLevel = result.level;
                            highestSignalResult = result;
                        }
                    }
                }
                if(highestSignalResult!=null)
                {
                    int time_elapsed = 0;
                    //if we are connected to 2G/3G, then get access from the API
                    //otherwise, get access from the offline data
                    if(activeNetwork!=null && activeNetwork.getType()!=ConnectivityManager.TYPE_WIFI)
                    {
                        utils.logMessage("Active data network - querying API for password","ConnectToWifi",new Properties().putValue("mac_address",highestSignalResult.BSSID).putValue("check_password",checkPassword));
                        api.router_access(highestSignalResult.BSSID.toUpperCase(), callback);

                        //WifiRouterAccess result = api.router_access(highestSignalResult.BSSID); synchronous call has EOFException issues
                        //see: https://github.com/square/retrofit/issues/397, http://stackoverflow.com/questions/22351400/retrofit-gives-eofexception-only-the-first-time
                        //could try OkHTTP library, but don't want to bloat APK size
                        //instead we just use an asynchronous call, and wait for a response
                    }
                    else if (activeNetwork==null)
                    {
                        utils.logMessage("No active data network - querying offline network access for password","ConnectToWifi",new Properties().putValue("mac_address",highestSignalResult.BSSID).putValue("check_password", checkPassword));
                        RealmQuery<WiFiNetworkAccess> query = realm.where(WiFiNetworkAccess.class);
                        query.equalTo("mac_address", highestSignalResult.BSSID.toUpperCase());
                        // Execute the query:
                        RealmResults<WiFiNetworkAccess> results = query.findAll();
                        if(results.size()>0)
                        {
                            utils.logMessage("Got access credentials to community hotspot", "ConnectToWifi", new Properties().putValue("check_password", checkPassword));
                            WiFiNetworkAccess result = results.first();
                            //convert the DB result to an API result for compatibility
                            routerAccess = new WifiRouterAccess(result.getSsid(), result.getMac_address(), result.getEncrypted_password(), result.getAuthentication_algorithm());
                            makeConnection(checkPassword);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
	}

    public void makeConnection(final boolean checkPassword)
    {
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    utils.logMessage("Got password for scanned WiFi router","ConnectToWifi",new Properties().putValue("mac_address",highestSignalResult.BSSID));
                    //two ways to determine Wifi connectivity:
                    //ConnectivityManager/getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()
                    //or wifiManager.getConnectionInfo().getSupplicantState() and see if not associated/associating/in handshake
                    //the former is easier, so we'll choose that
                    if((wifiInfo!=null && wifiInfo.isConnectedOrConnecting()==false) || (activeNetwork!=null && activeNetwork.getType()!=ConnectivityManager.TYPE_WIFI))
                    {
                        String plaintextPassword = decryptPassword(routerAccess.password);
                        netId = connectToRouter(utils.stripQuotesSSID(routerAccess.ssid), plaintextPassword, routerAccess.authentication_algorithm);
                        connectionSuccessful = waitForConnection(connManager, wifiInfo, utils.stripQuotesSSID(routerAccess.ssid), routerAccess.bssid);//wait for up to 10 seconds for the connection to the WiFi network
                        storeResult(checkPassword);
                        sendResult(checkPassword);
                    }
                    else
                    {
                        utils.logMessage("ConnectToWifi","Not attempting to connect to community hotspot because already connected to WiFi",new Properties().putValue("mac_address",highestSignalResult.BSSID));
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public void storeResult(boolean checkPassword)
    {
        try
        {
            if(highestSignalResult!=null && netId!=-1 && connectionSuccessful==true && checkPassword==false)
            {
                Log.d("ConnectToWifi","connection successful!");
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putBoolean("connected_to_community_hotspot",true);
                editor.putString("connected_to_community_hotspot_ssid", highestSignalResult.SSID);
                editor.putString("connected_to_community_hotspot_mac_address",highestSignalResult.BSSID);
                editor.putInt("connected_to_community_hotspot_net_id",netId);

                //reset the counters for currently_connected_community_hotspot stats
                long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
                editor.putLong("currently_connected_community_hotspot_start_time", now);

                //we don't bother differentiating between all & mobile stats, as they will be one and the same when we're on WiFi
                float all_received_bytes = TrafficStats.getTotalRxBytes();
                float all_transmitted_bytes = TrafficStats.getTotalTxBytes();
                if(all_received_bytes!=TrafficStats.UNSUPPORTED && all_transmitted_bytes!=TrafficStats.UNSUPPORTED)
                {
                    editor.putFloat("currently_connected_community_hotspot_received_bytes",all_received_bytes);
                    editor.putFloat("currently_connected_community_hotspot_transmitted_bytes",all_transmitted_bytes);
                }
                editor.commit();
            }

        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public void sendResult(boolean checkPassword)
    {
        try
        {
            if(highestSignalResult!=null && checkPassword==false)
            {
                //start the MainActivity (if it's not already running and we succesfully connected)
                //as that contains the event handler to start the VPN
                //and send it to the background
                //if(sharedPrefs.getBoolean("mainactivity_active",false)==false) - was not working if MainActivity shuts down
                if(MainActivity.active==false && connectionSuccessful==true)
                {
                    Log.d("ConnectToWifi","mainactivity not active - starting");
                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(mainIntent);

                    //now, wait until the activity has started
                    int elapsed_time = 0;
                    while(sharedPrefs.getBoolean("mainactivity_active",false)==false && elapsed_time<10)
                    {
                        android.os.SystemClock.sleep(1000);//wait for 1 second
                        elapsed_time += 1;
                        Log.d("ConnectToWifi","waiting for MainActivity to start");
                    }
                    if(sharedPrefs.getBoolean("mainactivity_active",false)==false)
                    {
                        utils.logMessage("Timeout starting MainActivity","ConnectToWifi");
                    }

                    Events.MoveActivityToBackEvent backgroundEvent = new Events.MoveActivityToBackEvent();
                    EventBus.getDefault().post(backgroundEvent);
                }
                //log whether we successfully connected & need to start vpn, or were unable to (password changed?)
                logConnectionResult(connectionSuccessful, highestSignalResult.BSSID, highestSignalResult.SSID);
            }
            else if(highestSignalResult!=null && checkPassword==true)
            {
                api.password_check_results(highestSignalResult.BSSID, connectionSuccessful, passwordCheckCallback);

                //store last_check_time
                RealmQuery<AddedWiFiNetwork> query = realm.where(AddedWiFiNetwork.class);
                query.equalTo("mac_address", highestSignalResult.BSSID.toUpperCase());
                RealmResults<AddedWiFiNetwork> results = query.findAll();
                AddedWiFiNetwork myNetwork;
                if(results.size()>0)
                {
                    //if the connection fails - and the user is the one who submitted that network - prompt to update password
                    if(connectionSuccessful==false)
                    {
                        //just send them a notification
                        NotificationManager mNotificationManager = (NotificationManager)
                                getSystemService(Context.NOTIFICATION_SERVICE);

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setAction("display_screen");
                        intent.putExtra("screen","update_password");
                        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                                intent, PendingIntent.FLAG_CANCEL_CURRENT);

                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(this)
                                        .setSmallIcon(R.drawable.logo_rings)
                                        .setContentTitle(getResources().getString(R.string.update_password_title))
                                        .setAutoCancel(true)
                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                .bigText(getResources().getString(R.string.update_password_text)))
                                        .setContentText("");
                        mBuilder.setContentIntent(contentIntent);
                        mNotificationManager.notify(-1, mBuilder.build());
                        //notification that takes them to update password page
                        utils.logMessage("Prompted user to update WiFi password", "ConnectToWiFi", new Properties().
                                putValue("ssid", highestSignalResult.SSID).
                                putValue("mac_address", highestSignalResult.BSSID));
                    }
                    realm.beginTransaction();
                    myNetwork = results.first();
                    long now = System.currentTimeMillis() / 1000; // seconds since epoch (UTC)
                    myNetwork.setLast_password_check_date(now);
                    realm.commitTransaction();
                }
            }
            //otherwise, we didn't attempt to connect, so don't fire any event
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public Callback<WifiRouterAccess> callback = new Callback<WifiRouterAccess>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "ConnectToWifi", "callback");
        }

        @Override
        public void success(WifiRouterAccess result, Response response)
        {
            Log.d("WifiRouterAccess","returned result");
            utils.logMessage("Got access credentials to community hotspot", "ConnectToWifi");
            routerAccess = result;
            makeConnection(callbackCheckPassword);
        }
    };
	
	public int connectToRouter(String ssid, String password, String authentication_algorithm)
	{
		try
        {
            Log.d("ConnectToWiFi", "in connectToRouter");

            //debug test to ensure android isn't using the other configuration to connect to the network when UA requests
            /*WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            List<WifiConfiguration> networkList = wifi.getConfiguredNetworks();
            for( WifiConfiguration i : networkList )
            {
                wifi.removeNetwork(i.networkId);
            }*/

            Context context = getApplicationContext();//could also use 'this'
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = String.format("\"%s\"", ssid);
            if (authentication_algorithm.equals("WEP"))
            {
                Log.d("ConnectToWiFi", "inside wep auth");
                //In case of WEP, if your password is in hex, you do not need to surround it with quotes
                conf.wepKeys[0] = String.format("\"%s\"", password);
                conf.wepTxKeyIndex = 0;
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            }
            else if(authentication_algorithm.equals("WPA"))
            {
                conf.preSharedKey = String.format("\"%s\"", password);
                conf.status = WifiConfiguration.Status.ENABLED;
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);//0
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);//1
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);//2
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);//3
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);//1
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);//1
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);//2
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);//0
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);//1
                Log.d("ConnectToWiFi", "configuring with WPA password");
            }
            else if(authentication_algorithm.equals("OPEN"))
            {
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int netId = wifiManager.addNetwork(conf);
            if(netId!=-1)
            {
                wifiManager.saveConfiguration();
                wifiManager.disconnect();
                Boolean enabled = wifiManager.enableNetwork(netId, true);
                utils.logMessage("WiFi network enabled attempt","ConnectToWiFi",new Properties().putValue("result",enabled));
                Boolean reconnectResult = wifiManager.reconnect();
                utils.logMessage("WiFi network reconnect attempt","ConnectToWiFi",new Properties().putValue("result",reconnectResult));
            }
            utils.logMessage("connectToRouter got netId","ConnectToWifi",new Properties().putValue("netId",netId));
            return netId;
        }
        catch (Exception e)
        {
            utils.logException(e);
            return -1;
        }
    }

    /**
     *
     * @param ssid
     * @param mac_address
     * @return
     */
    public Boolean waitForConnection(ConnectivityManager connManager, NetworkInfo networkInfo, String ssid, String mac_address)
    {
        try
        {
            Log.d("ConnectToWifi","waitForConnection");
            NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
            Context context = getApplicationContext();//could also use 'this'
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int responseCode = 0;
            int elapsed_time = 0;
            //while loop here, 10 seconds
            while(elapsed_time<10  && ((activeNetwork!=null && activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) || (responseCode!=200)))//(networkInfo.isConnected()==false || activeNetwork==null || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI || wifiInfo==null || wifiInfo.getSSID()==null || wifiInfo.getSSID()=="<unknown ssid>" || !TextUtils.isEmpty(wifiInfo.getSSID())))
            {
                android.os.SystemClock.sleep(1000);//wait for 1 second
                elapsed_time += 1;
                responseCode = utils.verifyInternetConnectivity(wifiManager,context,"",false);
                Log.d("ConnectToWifi","response code:" + String.valueOf(responseCode));
            }
            //now, if we're connected to WiFi, verify the mac_address matches
            //Android 5 has issues - mac_address is 00:00:00:00:00:00 and ssid 0x even 10 seconds after connecting
            activeNetwork = connManager.getActiveNetworkInfo();
            String activeNetworkType = (activeNetwork!=null) ? activeNetwork.getTypeName() : null;
            String activeNetworkDetailedState = (activeNetwork!=null) ? activeNetwork.getDetailedState().toString() : null;
            utils.logMessage("waitForConnection to community WiFi network state","ConnectToWifi",new Properties()
                    .putValue("mac_address",wifiInfo.getBSSID())
                    .putValue("ssid",wifiInfo.getSSID())
                    .putValue("active network",activeNetworkType)
                    .putValue("supplicant state",wifiInfo.getSupplicantState())
                    .putValue("active network detailed state",activeNetworkDetailedState)
                    .putValue("response code",responseCode));
            if(responseCode==200 && activeNetwork!=null && activeNetwork.getType()==ConnectivityManager.TYPE_WIFI)// && wifiInfo!=null)// && wifiInfo.getBSSID()!=null && wifiInfo.getBSSID().equals(mac_address))
                return true;
            else
                return false;
        }
        catch (Exception e)
        {
            utils.logException(e);
            return false;
        }
    }

    /**
     *
     * @param ciphertext
     * @return
     */
    public String decryptPassword(String ciphertext)
    {
        String plaintextPassword = "";
        try
        {
            String key_hex = "3f552a117a9d0aafc30ed15abed75bc2";//shared secret
            byte[] key_bytes = hexStringToByteArray(key_hex);
            byte[] ciphertext_bytes = hexStringToByteArray(ciphertext);
            byte[] iv = Arrays.copyOfRange(ciphertext_bytes, 0, 16);
            byte[] rest = Arrays.copyOfRange(ciphertext_bytes, 16, ciphertext_bytes.length);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            Key key = new SecretKeySpec(key_bytes, "AES");
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, key, ivspec);
            byte[] value = c.doFinal(rest);
            plaintextPassword = new String(value, "UTF-8");
            utils.logMessage("ConnectToWifi","Decrypted WiFi router password");
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
        return plaintextPassword;
    }

    public static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     *
     */
    public void checkIfWiFiPasswordChanged(ArrayList<ScanResult> matching_networks, String currentMacAddress)
    {
        try
        {
            PowerManager pm = (PowerManager)
                    getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();

            // if we're currently connected to a network that's a community hotspot
            /*boolean connectedToCommunityHotspot = false;
            for(ScanResult network : matching_networks)
            {
                if(network.BSSID.equals(currentMacAddress))
                {
                    connectedToCommunityHotspot = true;
                    break;
                }
            }*/
            RealmQuery<AddedWiFiNetwork> query = realm.where(AddedWiFiNetwork.class);
            query.equalTo("mac_address", currentMacAddress.toUpperCase());
            RealmResults<AddedWiFiNetwork> results = query.findAll();
            AddedWiFiNetwork myNetwork;
            if(results.size()>0)
            {
                myNetwork = results.first();
                long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
                long last_check_date = myNetwork.getLast_password_check_date();
                if ((now - last_check_date > 86400) // it's been at least 24 hours since we verified its password
                    && isScreenOn==false // and the device is not in use (screen is off)
                    && sharedPrefs.getBoolean("connected_to_community_hotspot",false)==false)// and it's not a third-party community hotspot we're already connected to - so the password is correct
                {
                    // attempt to reconnect to it using our stored password (without turning on VPN)
                    wifi.disconnect();
                    attemptConnection(matching_networks, true, currentMacAddress);
                    utils.logMessage("Checking Password of WiFi Network","ConnectToWifi",new Properties().putValue("mac_address",currentMacAddress));
                }
            }
            else if(loggedMacNotInCustomerRouters==null || !loggedMacNotInCustomerRouters.equals(currentMacAddress))
            {
                utils.logMessage("MAC address in checkIfWiFiPasswordChanged not in Added networks database table","ConnectToWifi");
                loggedMacNotInCustomerRouters = currentMacAddress;
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                String action = intent.getAction();

                if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                {
                    Log.d("in WiFi", "scan finished");

                    //because scan results may be available more frequently than we want (every 30 seconds)
                    //we'll calculate how long its been since our last scan results were delivered
                    long now = System.currentTimeMillis()/1000;

                    long time_diff = now - previous_wifi_scan_received_time;
                    if(time_diff>=30 && wifi.isWifiEnabled()) //30 seconds
                    {
                        List<ScanResult> wifiScanResults = wifi.getScanResults();
                        ArrayList<ScanResult> scanResults = new ArrayList(wifiScanResults);//only an ArrayList is serializable, and thus passable to a service

                        if(wifiScanResults.size() == 0)
                            Log.d("in WiFi", "no wifi networks found in scan");
                        else
                        {
                            Log.d("in WiFi", wifiScanResults.size() + " networks found in scan");
                            Log.d("in WiFi", "mac bloom count:" + sharedPrefs.getLong("loaded_mac_bloom_filter_count",0));

                            Boolean found_match = false;
                            final ArrayList<ScanResult> matching_networks = new ArrayList();
                            //check the macList to see if the WiFi network is on UnifiApp
                            for(ScanResult result:scanResults)
                            {
                                String mac_address = result.BSSID;
                                Log.d("MainActivity","searching for match of mac:" + result.BSSID.toUpperCase());
                                if(mac_address!=null && macList.lookupNetwork(result.BSSID.toUpperCase())==true)
//                                        && ((!sharedPrefs.getString("last_failed_connect_mac_address","").equals(result.BSSID))
//                                        || (sharedPrefs.getString("last_failed_connect_mac_address","").equals(result.BSSID) && (now - sharedPrefs.getLong("last_failed_connect_time", 0)>86400))))
                                {
                                    if(loggedFoundMatchForMacAddress==null || !loggedFoundMatchForMacAddress.equals(result.BSSID))
                                    {
                                        utils.logMessage("Found match for scanned WiFi MAC address in bloom filter","ConnectToWifi",new Properties().putValue("mac_address",result.BSSID));
                                        loggedFoundMatchForMacAddress = result.BSSID;
                                    }
                                    found_match = true;
                                    //now, ensure this isn't a network that the customer themselves added
                                    //we could check their wifi configurations, but those would only match by SSID, which may not be conclusive
                                    //instead we compare to the wifi routers they've added
                                    RealmQuery<AddedWiFiNetwork> query = realm.where(AddedWiFiNetwork.class);
                                    // Add query conditions:
                                    query.equalTo("mac_address", result.BSSID.toUpperCase());
                                    // Execute the query:
                                    RealmResults<AddedWiFiNetwork> results = query.findAll();

                                    // TODO remove special case for Delhi
                                    float latitude = sharedPrefs.getFloat("coarse_latitude",0);
                                    float longitude = sharedPrefs.getFloat("coarse_longitude",0);
                                    if(results.size()==0) //wasn't a router the customer themselves added, so add it to our matching_networks
                                    {
                                        matching_networks.add(result);
                                    }
                                    else if((Math.abs(latitude-28.6100)<=0.5 && Math.abs(longitude-77.2300)<=0.5) || (Math.abs(latitude-37.7833)<=1.5 && Math.abs(-1*longitude-122.4167)<=1.5))
                                    {
                                        matching_networks.add(result);
                                        utils.logMessage("Delhi/Bay Area special case - Scanned network is router added by customer themselves - attempting to connect", "ConnectToWifi");
                                    }
                                    else if(loggedScannedNetworkAddedByCustomer==null || !loggedScannedNetworkAddedByCustomer.equals(result.BSSID))
                                    {
                                        utils.logMessage("Scanned network is router added by customer themselves - not attempting to connect","ConnectToWifi");
                                        loggedScannedNetworkAddedByCustomer = result.BSSID;
                                    }
                                }
                                else if(sharedPrefs.getString("last_failed_connect_mac_address","").equals(result.BSSID))
                                {
                                    //without this, it would continue to attempt to connect to a network 100 times in a row, even after the first attempt failed
                                    if(loggedNotConnectingFailedWarningMacAddress==null || !loggedNotConnectingFailedWarningMacAddress.equals(result.BSSID))
                                    {
                                        utils.logMessage("Not connecting to same MAC address of previously failed connection attempt for 24 hours", "MainActivity", new Properties().putValue("mac_address", result.BSSID.toUpperCase()));
                                        loggedNotConnectingFailedWarningMacAddress = result.BSSID;
                                        //if the MainActivity home screen is not active, will report no receiver registered for this event
                                        Events.OutOfRangeCommunityWiFiEvent outOfRangeCommunityWiFiEvent = new Events.OutOfRangeCommunityWiFiEvent();
                                        EventBus.getDefault().post(outOfRangeCommunityWiFiEvent);
                                    }
                                }
                            }
                            //if we're not already connected to a WiFi network, attempt to get the password for this network and connect to it
                            //if(found_match==true && wifiInfo.isConnectedOrConnecting()==false) - this is often true after you 'forget' a network, but you're not connected to wifi!
                            NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                            if(activeNetwork==null)
                            {
                                Log.d("ConnectToWiFi","active network is null");
                            }
                            else
                                Log.d("ConnectToWiFi","active network type:" + activeNetwork.getTypeName());
                            if((matching_networks.size()>0 && wifiInfo!=null && wifiInfo.isConnectedOrConnecting()==false)
                                    || (matching_networks.size()>0 && activeNetwork!=null && activeNetwork.getType()!=ConnectivityManager.TYPE_WIFI)
                                    || (matching_networks.size()>0 && activeNetwork==null))
                            {
                                //if the MainActivity home screen is not active, will report no receiver registered for this event
                                Events.InRangeCommunityWiFiEvent inRangeCommunityWiFiEvent = new Events.InRangeCommunityWiFiEvent();
                                EventBus.getDefault().post(inRangeCommunityWiFiEvent);
                                attemptConnection(matching_networks, false, null);
                            }
                            else if(matching_networks.size()>0)
                            {
                                if(wifi.getConnectionInfo()!=null)
                                {
                                    String currentMacAddress = wifi.getConnectionInfo().getBSSID();
                                    if(loggedAlreadyConnectedMacAddress==null || !loggedAlreadyConnectedMacAddress.equals(currentMacAddress))
                                    {
                                        utils.logMessage("Already connected to WiFi - not attempting to connect to community hotspot","ConnectToWifi");
                                        loggedAlreadyConnectedMacAddress = currentMacAddress;
                                    }
                                    if(currentMacAddress!=null)
                                    {
                                        checkIfWiFiPasswordChanged(matching_networks, currentMacAddress);
                                    }
                                }
                            }
                            else if(matching_networks.size()==0)
                            {
                                Events.OutOfRangeCommunityWiFiEvent outOfRangeCommunityWiFiEvent = new Events.OutOfRangeCommunityWiFiEvent();
                                EventBus.getDefault().post(outOfRangeCommunityWiFiEvent);
                            }
                        }
                        previous_wifi_scan_received_time = now;
                    }
                    else if(!wifi.isWifiEnabled())
                    {
                        if(loggedWifiNotOn==null)
                        {
                            utils.logMessage("WiFi is not on - not attempting to scan or connect to community hotspots","ConnectToWifi");
                            loggedWifiNotOn = "logged";
                        }
                    }
                }
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
    };

    Callback<APIClient.OfflineNetworkAccess> offline_callback = new Callback<APIClient.OfflineNetworkAccess>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "ConnectToWifi", "offline_callback");
        }
        @Override
        public void success(APIClient.OfflineNetworkAccess result, Response arg1)
        {
            try
            {
                Log.d("ConnectToWifi", "putting new data from server API call for offline data");
                realm.beginTransaction();
                List<WifiRouterAccess> list = result.getList();
                for(WifiRouterAccess router:list)
                {
                    if(router.bssid!=null)
                    {
                        RealmQuery<WiFiNetworkAccess> query = realm.where(WiFiNetworkAccess.class);
                        // Add query conditions:
                        query.equalTo("mac_address", router.bssid.toUpperCase());
                        // Execute the query:
                        RealmResults<WiFiNetworkAccess> results = query.findAll();
                        if(results.size()==0) //if it doesn't already exist, add it
                        {
                            if(router.ssid!=null && router.bssid!=null && router.password!=null && router.authentication_algorithm!=null)
                            {
                                WiFiNetworkAccess network = realm.createObject(WiFiNetworkAccess.class); // Create a new object
                                network.setSsid(router.ssid);
                                network.setMac_address(router.bssid.toUpperCase());
                                network.setEncrypted_password(router.password);
                                network.setAuthentication_algorithm(router.authentication_algorithm);
                            }
                        }
                        else //if it already exists, update its password & authentication mechanism
                        {
                            WiFiNetworkAccess network = results.first();
                            network.setAuthentication_algorithm(router.authentication_algorithm);
                            network.setEncrypted_password(router.password);
                        }
                    }
                    else
                        Log.d("ConnectToWifi", "null mac while putting new data from server API call for offline data");
                }
                realm.commitTransaction();
                editor.putLong("last_offline_network_access_sync_date",result.getSyncDate());
                editor.commit();
            }
            catch(Exception e)
            {
                utils.logException(e);
            }
        }
    };

    Callback<APIClient.WifiRouters> customerRoutersCallback = new Callback<APIClient.WifiRouters>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "ConnectToWifi", "customerRoutersCallback");
        }
        @Override
        public void success(APIClient.WifiRouters result, Response arg1)
        {
            try
            {
                Log.d("ConnectToWifi", "putting new data from server API call for customer routers");
                realm.beginTransaction();
                List<APIClient.WifiRouter> list = result.getRouters();
                for(WifiRouter router:list)
                {
                    if(router.bssid!=null)
                    {
                        RealmQuery<AddedWiFiNetwork> query = realm.where(AddedWiFiNetwork.class);
                        // Add query conditions:
                        query.equalTo("mac_address", router.bssid.toUpperCase());
                        // Execute the query:
                        RealmResults<AddedWiFiNetwork> results = query.findAll();
                        if(results.size()==0) //if it doesn't already exist, add it
                        {
                            if(router.ssid!=null && router.bssid!=null)
                            {
                                AddedWiFiNetwork network = realm.createObject(AddedWiFiNetwork.class); // Create a new object
                                network.setSsid(router.ssid);
                                network.setMac_address(router.bssid.toUpperCase());
                                network.setLatitude(router.latitude);
                                network.setLatitude(router.longitude);
                                network.setAltitude(router.altitude);
                            }
                        }
                        //if it already exists, do nothing
                    }
                    else
                        Log.d("ConnectToWifi", "null mac while putting new data from server API call for customer routers");
                }
                realm.commitTransaction();
                long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
                editor.putLong("last_customer_routers_added_sync_date",now);
                editor.commit();
            }
            catch(Exception e)
            {
                utils.logException(e);
            }
        }
    };

    Callback<APIClient.Response> passwordCheckCallback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "ConnectToWifi", "passwordCheckCallback");
        }

        @Override
        public void success(APIClient.Response result, Response arg1)
        {

        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        try
        {
            realm.close();
            EventBus.getDefault().unregister(this);
            if(wifiThreadPoolExecutor!=null)
            {
                wifiThreadPoolExecutor.shutdownNow();
            }
            if(wifi_scan_receiver_registered)
            {
                unregisterReceiver(wifiScanReceiver);
            }
            if(power_receiver_registered)
            {
                unregisterReceiver(powerReceiver);
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     * @param result
     * @param mac_address
     * @param ssid
     */
    public void logConnectionResult(Boolean result, String mac_address, String ssid)
    {
        Properties properties = new Properties()
                .putValue("mac_address", mac_address)
                .putValue("ssid", ssid)
                .putValue("latitude",sharedPrefs.getFloat("coarse_latitude",0.0f))
                .putValue("longitude",sharedPrefs.getFloat("coarse_longitude",0.0f))
                .putValue("altitude",sharedPrefs.getFloat("coarse_altitude",0.0f));
        if(connectionSuccessful!=null && connectionSuccessful==true)
        {
            utils.logMessage("connected to community hotspot","MainActivity",properties);
            //TODO: want to send more accurate location
            //start the VPN
            Events.StartVPNEvent startVPNEvent = new Events.StartVPNEvent();
            EventBus.getDefault().post(startVPNEvent);
        }
        else if(connectionSuccessful!=null && connectionSuccessful==false)
        {
            utils.logMessage("could not connect to community hotspot","MainActivity",properties);
            editor.putString("last_failed_connect_mac_address",mac_address);
            editor.putLong("last_failed_connect_time", System.currentTimeMillis()/1000); // seconds since epoch (UTC));
            editor.commit();
            //TODO: want to send more accurate location
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    /**
     *
     * @param event
     */
    public void onEvent(Events.SyncMacBloomFilterEvent event)
    {
        macList.syncWithServer();
    }

    /*

     */
    public void onEvent(Events.StartAPEvent event)
    {
        try
        {
            //save the current SSID of the WiFi access point
            //(for later restoration)
            WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

            editor.putString("original_wifi_ap_ssid", wifiConfig.SSID);
            editor.commit();

            wifiConfig.SSID = " Free WiFi Mumbai - unifiapp.com";

            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setConfigMethod.invoke(wifiManager, wifiConfig);
            //turn on the WiFi access point
            utils.logMessage("Turning on WiFi AP", "onEvent StartAPEvent");
            wifiAP.configAPState(true);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /*

     */
    public void onEvent(Events.StopAPEvent event)
    {
        try
        {
            //turn off the WiFi access point
            utils.logMessage("Turning off WiFi AP", "onEvent StopAPEvent");
            wifiAP.configAPState(false);
            //return the SSID to its original name
            WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

            String originalSSID = sharedPrefs.getString("original_wifi_ap_ssid", "AndroidAP");

            wifiConfig.SSID = originalSSID;

            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setConfigMethod.invoke(wifiManager, wifiConfig);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }
}