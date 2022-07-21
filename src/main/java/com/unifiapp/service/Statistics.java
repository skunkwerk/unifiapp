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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.unifiapp.MainActivity;
import com.unifiapp.R;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIFactory;
import com.unifiapp.model.Connectivity;
import com.unifiapp.model.DailyConnectivity;
import com.unifiapp.model.DataUsage;
import com.unifiapp.model.DataUsageState;
import com.unifiapp.model.WiFiNetwork;
import com.unifiapp.utils.Utilities;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Statistics extends Service
{
    ConnectivityManager connManager;
    WifiManager wifi;
    Context context;
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    APIClient.API api;
    public Realm realm;
    Utilities utils;
    ScheduledThreadPoolExecutor threadPoolExecutor;
    ScheduledThreadPoolExecutor wifiThreadPoolExecutor;
    private long previous_wifi_scan_received_time;
    Boolean wifi_scan_receiver_registered;
    long lastQueriedDuplicateNetworkTime = 0;
    String passwordPromptedSsid;

    /**
     *
     */
    public Statistics() { }

    public void startWiFiScan()
    {
        this.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //for getting changes in WiFi connectivity (ie when we're out of range of the WiFi network we connected to)
        this.registerReceiver(connectivityChangeReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        wifi_scan_receiver_registered = true;
        previous_wifi_scan_received_time = 0;

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

    public void setupTimer()
    {
        threadPoolExecutor = new ScheduledThreadPoolExecutor(1);//1 thread
        threadPoolExecutor.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                evaluateStats();
            }
        }, 0, 10, TimeUnit.MINUTES);//run it every 10 minute, after the end of the previous call to run()
    }

    public void evaluateStats()
    {
        collectDataUsageStatistics();
        collectConnectivityStatistics();
        calculateDailySummaryConnectivityStatistics();
        attemptUpload();
    }

    /**
     *
     */
    public void collectConnectivityStatistics()
    {
        try
        {
            NetworkInfo active_network = connManager.getActiveNetworkInfo();
            realm.beginTransaction();
            Connectivity connectivity = realm.createObject(Connectivity.class); // Create a new object
            connectivity.setDatetime(new Date());
            connectivity.setData_connection_active((active_network == null) ? false : true);
            connectivity.setWifi_on(wifi.isWifiEnabled());
            connectivity.setConnection_type((active_network == null) ? "null" : active_network.getTypeName());
            Log.d("Statistics", "committing connectivitystats to database");
            realm.commitTransaction();
        }
        catch (Exception e)
        {
            utils.logException(e);
        }

    }

    /**
     * if we're in a new day, calculate the summary of the previous day
     * and insert it into a row
     * we can't assume that the app is going to be run continuously in background
     * so the rows of data may be from 5 days ago, or discontinuous, etc.
     * so wait until they're at least 144 rows (24 hours) of data in there - this prevents upload as soon as app is installed with 1 row
     * then group them by date.  calculate summaries for any that are not for today, and send those if at least 6 rows in each.
     * The problem is that Realm doesn't yet support GROUP BY/SELECT DISTINCT so it's hard to do that -
     * to simplify matters, we just make a best effort - we search for entries from the previous day, and if they're at least 6 we average them
     * and delete everything else
     */
    public void calculateDailySummaryConnectivityStatistics()
    {
        try
        {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
            int today = calendar.get(Calendar.DATE);
            int last_run_day = sharedPrefs.getInt("last_daily_summary_statistics_calculation_day",0);
            if(today != last_run_day)//if it's a newer/different day
            {
                Log.d("statistics","in a new day - calculating daily summary of connectivity, and deleting processed data");
                int data_connection_active_count = 0;
                int wifi_on_count = 0;
                int wifi_connected_count = 0;
                int mobile_data_connected_count = 0;
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                Date yesterday = cal.getTime();
                RealmResults<Connectivity> all_rows = realm.where(Connectivity.class).findAll();
                RealmResults<Connectivity> yesterdays_rows = realm.where(Connectivity.class).between("datetime", yesterday, new Date()).findAll();

                int total_rows = yesterdays_rows.size();
                if(total_rows>5)
                {
                    for(Connectivity row: yesterdays_rows)
                    {
                        if(row.isData_connection_active())
                            data_connection_active_count += 1;
                        if(row.isWifi_on())
                            wifi_on_count += 1;
                        if(row.getConnection_type().equals("MOBILE"))
                            mobile_data_connected_count += 1;
                        if(row.getConnection_type().equals("WIFI"))
                            wifi_connected_count += 1;
                    }

                    float data_connection_active_average = (data_connection_active_count/total_rows) * 100;
                    float wifi_on_average = (wifi_on_count/total_rows) * 100;
                    float wifi_connected_average = (wifi_connected_count/total_rows) * 100;
                    float mobile_data_connected_average = (mobile_data_connected_count/total_rows) * 100;
                    realm.beginTransaction();
                    DailyConnectivity dailyConnectivity = realm.createObject(DailyConnectivity.class); // Create a new object
                    dailyConnectivity.setData_connection_active_average(data_connection_active_average);
                    dailyConnectivity.setDate(new Date());
                    dailyConnectivity.setMobile_data_connected_average(mobile_data_connected_average);
                    dailyConnectivity.setWifi_connected_average(wifi_connected_average);
                    dailyConnectivity.setWifi_on_average(wifi_on_average);
                    dailyConnectivity.setAnonymous_customer_id_hash(sharedPrefs.getString("anonymous_customer_id_hash",""));
                    realm.commitTransaction();

                    editor.putInt("last_daily_summary_statistics_calculation_day",today);
                    editor.commit();
                }
                else
                {
                    Log.d("statistics","in new day, but <6 rows of connectivity data - not calculating daily summary of connectivity");

                }
                //now, delete all the rows (if we calculate the summary, we don't need them anymore.  and if there weren't enough rows - that's never going to change)
                realm.beginTransaction();
                all_rows.clear();
                realm.commitTransaction();
            }
            else
            {
                Log.d("statistics","not in a new day - not calculating daily summary of connectivity");
            }
        }
        catch (IllegalStateException e)
        {
            //this happens when we have an existing version of the table that was created from the MainActivity thread, not the service thread
            //in such cases, delete any existing data so that we don't get the same error next time
            realm.clear(Connectivity.class);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     */
    public void collectDataUsageStatistics()
    {
        // NOTE the TrafficStats counters will reset after a reboot.
        // Going into airplane mode or changing between mobile and Wi-Fi will not reset these counters.
        // just saying have we recorded before is not enough - as app could start, stop, then start again after 10 hours - the diff isn't correct
        try
        {
            float all_received_bytes = TrafficStats.getTotalRxBytes();
            float all_transmitted_bytes = TrafficStats.getTotalTxBytes();
            float mobile_received_bytes = TrafficStats.getMobileRxBytes();
            float mobile_transmitted_bytes = TrafficStats.getMobileTxBytes();

            if(all_received_bytes!=TrafficStats.UNSUPPORTED && all_transmitted_bytes!=TrafficStats.UNSUPPORTED && mobile_received_bytes!=TrafficStats.UNSUPPORTED && mobile_transmitted_bytes!=TrafficStats.UNSUPPORTED)
            {
                RealmQuery<DataUsageState> query = realm.where(DataUsageState.class);
                RealmResults<DataUsageState> results = query.findAll();
                DataUsageState state;
                if(results.size()>0)
                {
                    state = results.first();
                    long elapsed_time = ((new Date()).getTime() - state.getDatetime().getTime())/1000;//in seconds

                    //check if between 9-11 minutes have elapsed since the last record
                    //if so, we can do a valid comparison to figure out the difference
                    if(elapsed_time > 540 && elapsed_time < 660)
                    {
                        //calculate the differences
                        float all_received_bytes_delta = all_received_bytes - state.getLast_recorded_all_received_bytes_value();
                        float all_transmitted_bytes_delta = all_transmitted_bytes - state.getLast_recorded_all_transmitted_bytes_value();
                        float mobile_received_bytes_delta = mobile_received_bytes - state.getLast_recorded_mobile_received_bytes_value();
                        float mobile_transmitted_bytes_delta = mobile_transmitted_bytes - state.getLast_recorded_mobile_transmitted_bytes_value();
                        //if the difference is negative, the counters have been reset - so use the current values as absolute
                        //on the off chance the counter is reset, but enough time elapses before the app is called again, this may not be negative
                        //but in that case, we'll know time has elapsed and we won't record the first values
                        if(all_received_bytes_delta<0)
                            all_received_bytes_delta = all_received_bytes;
                        if(all_transmitted_bytes_delta<0)
                            all_transmitted_bytes_delta = all_transmitted_bytes;
                        if(mobile_received_bytes_delta<0)
                            mobile_received_bytes_delta = mobile_received_bytes;
                        if(mobile_transmitted_bytes_delta<0)
                            mobile_transmitted_bytes_delta = mobile_transmitted_bytes;
                        Log.d("Statistics","committing datausage to database");
                        //calculate wifi stats by subtracting mobile from all
                        //this assumes that there are only 2 Internet connections on a device - 3G/4G and WiFi
                        float wifi_received_bytes_delta = all_received_bytes_delta - mobile_received_bytes_delta;
                        float wifi_transmitted_bytes_delta = all_transmitted_bytes_delta - mobile_transmitted_bytes_delta;
                        realm.beginTransaction();
                        DataUsage usage = realm.createObject(DataUsage.class); // Create a new object
                        usage.setDatetime(new Date());
                        usage.setData_downloaded_mobile(mobile_received_bytes_delta);
                        usage.setData_uploaded_mobile(mobile_transmitted_bytes_delta);
                        usage.setData_downloaded_wifi(wifi_received_bytes_delta);
                        usage.setData_uploaded_wifi(wifi_transmitted_bytes_delta);
                        usage.setAnonymous_customer_id_hash(sharedPrefs.getString("anonymous_customer_id_hash", ""));
                        usage.setDatetime(new Date());
                        float coarse_latitude = sharedPrefs.getFloat("coarse_latitude",0.0f);
                        float coarse_longitude = sharedPrefs.getFloat("coarse_longitude",0.0f);
                        if(coarse_latitude!=0 && coarse_longitude!=0)
                        {
                            usage.setLatitude(coarse_latitude);
                            usage.setLongitude(coarse_longitude);
                            usage.setAltitude(sharedPrefs.getFloat("coarse_altitude",0.0f));
                        }
                        realm.commitTransaction();
                        //now, update the state
                        realm.beginTransaction();
                        state.setLast_recorded_all_received_bytes_value(all_received_bytes);
                        state.setLast_recorded_all_transmitted_bytes_value(all_transmitted_bytes);
                        state.setLast_recorded_mobile_received_bytes_value(mobile_received_bytes);
                        state.setLast_recorded_mobile_transmitted_bytes_value(mobile_transmitted_bytes);
                        state.setDatetime(new Date());
                        realm.commitTransaction();
                    }
                    else
                    {
                        //otherwise, too much time has elapsed, and we overwrite the value in DataUsageState
                        storeCurrentTrafficStatsInState(all_received_bytes,all_transmitted_bytes,mobile_received_bytes,mobile_transmitted_bytes);
                    }
                }
                else
                {
                    //this is our first run of this, so we store the current values in the state
                    storeCurrentTrafficStatsInState(all_received_bytes,all_transmitted_bytes,mobile_received_bytes,mobile_transmitted_bytes);
                }
            }
            else
            {
                Analytics.with(context).track("TrafficStats are undefined - not calculating or uploading statistics");
            }
        }
        catch (IllegalStateException e)
        {
            //this happens when we have an existing version of the table that was created from the MainActivity thread, not the service thread
            //in such cases, delete any existing data so that we don't get the same error next time
            realm.clear(DataUsageState.class);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     * @param all_received_bytes
     * @param all_transmitted_bytes
     * @param mobile_received_bytes
     * @param mobile_transmitted_bytes
     */
    public void storeCurrentTrafficStatsInState(float all_received_bytes, float all_transmitted_bytes, float mobile_received_bytes, float mobile_transmitted_bytes)
    {
        try
        {
            realm.beginTransaction();
            DataUsageState state = realm.createObject(DataUsageState.class); // Create a new object
            state.setDatetime(new Date());
            state.setLast_recorded_all_received_bytes_value(all_received_bytes);
            state.setLast_recorded_all_transmitted_bytes_value(all_transmitted_bytes);
            state.setLast_recorded_mobile_received_bytes_value(mobile_received_bytes);
            state.setLast_recorded_mobile_transmitted_bytes_value(mobile_transmitted_bytes);
            realm.commitTransaction();
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     * @param scanResults
     */
    public void recordWiFiScanResults(ArrayList<ScanResult> scanResults)
    {
        try
        {
            realm.beginTransaction();
            for(ScanResult result:scanResults)
            {
                //if it's not in our database already, add it
                RealmQuery<WiFiNetwork> query = realm.where(WiFiNetwork.class);
                // Add query conditions:
                query.equalTo("mac_address", result.BSSID);
                // Execute the query:
                RealmResults<WiFiNetwork> results = query.findAll();
                if(results.size()==0)
                {
                    WiFiNetwork network = realm.createObject(WiFiNetwork.class); // Create a new object
                    network.setMac_address(result.BSSID);
                    network.setSsid(result.SSID);
                    network.setFrequency(result.frequency);
                    float coarse_latitude = sharedPrefs.getFloat("coarse_latitude",0.0f);
                    float coarse_longitude = sharedPrefs.getFloat("coarse_longitude",0.0f);
                    if(coarse_latitude!=0 && coarse_longitude!=0)
                    {
                        network.setLatitude(coarse_latitude);
                        network.setLongitude(coarse_longitude);
                        network.setAltitude(sharedPrefs.getFloat("coarse_altitude",0.0f));
                    }
                    Log.d("Statistics","committing wifinetwork to database");
                }
            }
            realm.commitTransaction();
        }
        catch (IllegalStateException e)
        {
            //this happens when we have an existing version of the table that was created from the MainActivity thread, not the service thread
            //in such cases, delete any existing data so that we don't get the same error next time
            realm.clear(WiFiNetwork.class);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }

    }

    /**
     * we try to upload data here, instead of a SyncAdapter as the SyncAdapter has some overhead
     * and doesn't really do anything besides allow you to upload at the same time as other apps, saving battery
     * TODO: try a syncadapter later
     */
    public void attemptUpload()
    {
        //first, check to see when the last time we uploaded data was
        //only upload as often as every 3 hours to preserve battery life
        //and attempt to do so only when on WiFi
        //but if 24 hours have elapsed with no upload - send it over their mobile connection
        try
        {
            Log.d("Statistics","in attemptUpload");
            long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
            long last_run_time = sharedPrefs.getLong("last_statistics_upload_time",0);
            if(now - last_run_time > 10800) //3 hours in seconds
            {
                if(wifi.isWifiEnabled() && connManager.getActiveNetworkInfo()!=null && connManager.getActiveNetworkInfo().isConnected() && connManager.getActiveNetworkInfo().getTypeName().equals("WIFI"))
                {
                    Log.d("Statistics","wifi on & connected, been more than 3 hours since upload - sending data");
                    uploadData();
                }
                else if(now - last_run_time > 86400)//24 hours in seconds
                {
                    Log.d("Statistics","wifi not on/connected, been more than 24 hours since upload - sending data anyway");
                    uploadData();
                }
            }
            else
            {
                //uploadData();//TODO remove
                Log.d("Statistics","not uploading - diff is: " + String.valueOf(now - last_run_time));
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     */
    public void uploadData()
    {
        //iterate through all the rows in each table, send them to the server, and then delete them
        //convert from realm objects to retrofit objects, otherwise we get errors like:
        //Realm access from incorrect thread. Realm objects can only be accessed on the thread they where created.
        //TODO: should be able to send realm objects directly as JSON, so don't have this conversion overhead
        try
        {
            Log.d("Statistics","uploading data");
            RealmResults<WiFiNetwork> wifi_rows = realm.where(WiFiNetwork.class).findAll();
            RealmResults<DataUsage> data_rows = realm.where(DataUsage.class).findAll();
            RealmResults<DailyConnectivity> connectivity_rows = realm.where(DailyConnectivity.class).findAll();
            Log.d("Statistics","done with queries");

            //convert from Realm to Retrofit objects
            List<APIClient.WiFiNetwork> networks = new ArrayList<APIClient.WiFiNetwork>();
            for(WiFiNetwork wifi_row: wifi_rows)
            {
                APIClient.WiFiNetwork new_network = new APIClient.WiFiNetwork(wifi_row.getSsid(),wifi_row.getMac_address(),wifi_row.getFrequency(),wifi_row.getLatitude(),wifi_row.getLongitude(),wifi_row.getAltitude());
                networks.add(new_network);
            }
            List<APIClient.DataUsage> usages = new ArrayList<APIClient.DataUsage>();
            for(DataUsage usage: data_rows)
            {
                APIClient.DataUsage new_usage = new APIClient.DataUsage(usage.getData_uploaded_mobile(), usage.getData_downloaded_mobile(), usage.getData_uploaded_wifi(), usage.getData_downloaded_wifi(), usage.getDatetime(), usage.getLatitude(), usage.getLongitude(), usage.getAltitude(), usage.getAnonymous_customer_id_hash());
                usages.add(new_usage);
            }
            List<APIClient.DailyConnectivity> connectivities = new ArrayList<APIClient.DailyConnectivity>();
            for(DailyConnectivity connectivity: connectivity_rows)
            {
                APIClient.DailyConnectivity new_connectivity = new APIClient.DailyConnectivity(connectivity.getData_connection_active_average(), connectivity.getWifi_on_average(), connectivity.getWifi_connected_average(), connectivity.getMobile_data_connected_average(), connectivity.getDate(), connectivity.getAnonymous_customer_id_hash());
                connectivities.add(new_connectivity);
            }
            Log.d("Statistics","done with conversion");

            api.wifi_scan_results(networks, callback);
            api.data_usage_statistics(usages, callback);
            api.daily_connectivity_percentages(connectivities, callback);

            //now, delete all the rows we've processed
            realm.beginTransaction();
            wifi_rows.clear();
            data_rows.clear();
            connectivity_rows.clear();
            realm.commitTransaction();

            long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
            editor.putLong("last_statistics_upload_time",now);
            editor.commit();
        }
        catch (IllegalStateException e)
        {
            //this happens when we have an existing version of the table that was created from the MainActivity thread, not the service thread
            //in such cases, delete any existing data so that we don't get the same error next time
            realm.clear(WiFiNetwork.class);
            realm.clear(DataUsage.class);
            realm.clear(DailyConnectivity.class);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public void promptForPassword()
    {
        try
        {
            NotificationManager mNotificationManager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);

            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction("display_screen");
            intent.putExtra("screen","preshare");
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);

            String incentive = "";
            if(sharedPrefs.getBoolean("city_bootstrap_active", true)==true)
            {
                incentive = getResources().getString(R.string.rs_50_credit);
            }
            else
            {
                incentive = getResources().getString(R.string.five_karma_points);
            }

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.logo_rings)
                            .setContentTitle(getResources().getString(R.string.add_wifi_network_title))
                            .setAutoCancel(true)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(getResources().getString(R.string.add_wifi_network_text_prefix) + passwordPromptedSsid + getResources().getString(R.string.add_wifi_network_text_suffix) + incentive))
                            .setContentText("");
            mBuilder.setContentIntent(contentIntent);
            mNotificationManager.notify(-1, mBuilder.build());
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    Callback<APIClient.Response> callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(APIClient.Response result, Response arg1)
        {
            Log.d("in REST", "uploaded statistics to server with:" + result + "," + arg1);
        }
    };

    Callback<APIClient.Response> duplicate_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "Statistics", "duplicate_callback");
        }

        @Override
        public void success(APIClient.Response result, Response response)
        {
            Log.d("duplicate callback:",response.getStatus() + " " + response.getBody().toString());

            if (result.status==false)
            {
                Log.d("in duplicate_callback", "no match for router");
                //second check is now to see if they've already reached the limit of 3 routers added
                customerRouterCountCheck();
            }
            else
            {
                Log.d("in duplicate_callback", "match for router");
            }
        }
    };

    /**
     *
     */
    public void customerRouterCountCheck()
    {
        try
        {
            //to avoid networking on main thread exception
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    APIClient.Count routerCount = api.customer_router_count(sharedPrefs.getInt("customer_id",0));
                    if (routerCount.count>=3)
                    {
                        Log.d("customer_router_count", ">3 routers for user");
                    }
                    else
                    {
                        Log.d("customer_router_count", "<3 routers for user");
                        promptForPassword();
                    }
                }
            }).start();
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
                    Log.d("in Statistics", "wifi scan finished");

                    //because scan results may be available more frequently than we want (every 30 seconds)
                    //we'll calculate how long its been since our last scan results were delivered
                    long now = System.currentTimeMillis()/1000;

                    long time_diff = now - previous_wifi_scan_received_time;
                    if(time_diff>=60)//60 seconds
                    {
                        List<ScanResult> wifiScanResults = wifi.getScanResults();
                        ArrayList<ScanResult> scanResults = new ArrayList(wifiScanResults);//only an ArrayList is serializable, and thus passable to a service
                        recordWiFiScanResults(scanResults);
                        previous_wifi_scan_received_time = now;
                    }
                }
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
    };

    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                final long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
                //put this time delay in, as this is often called rapidly in quick successsion when there's a connectivity change
                if(connManager!=null && (now - lastQueriedDuplicateNetworkTime > 30))
                {
                    lastQueriedDuplicateNetworkTime = now;
                    NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                    Boolean connectedToHotspot = sharedPrefs.getBoolean("connected_to_community_hotspot",false);
                    WifiInfo wifiInfo = wifi.getConnectionInfo();
                    final String ssid = (wifiInfo != null) ? wifiInfo.getSSID() : null;
                    final String mac_address = (wifiInfo != null) ? wifiInfo.getBSSID() : null;

                    Long lastNotificationToAddWiFiPassword = sharedPrefs.getLong("last_notification_to_add_wifi_password",0);

                    Set<String> networks_submitted = sharedPrefs.getStringSet("wifi_networks_submitted", new HashSet<String>());
                    final Set<String> networks_password_prompted = sharedPrefs.getStringSet("wifi_networks_prompted_for_password", new HashSet<String>());

                    if (activeNetwork!=null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && ssid!=null &&
                            mac_address!=null && connectedToHotspot==false && networks_submitted.contains(ssid) == false &&
                            networks_password_prompted.contains(ssid) == false &&
                            (now - lastNotificationToAddWiFiPassword > 432000) &&
                            ssid.equals("0x")==false && ssid.equals("<unknown ssid>")==false)
                    {
                        //5 days is 432,000 seconds.  don't want to bother users every single day
                        //check for ActiveNetwork is there, otherwise we don't have a data connection and the API lookups fail, stays stuck in 'waiting for location' forever
                        //but even with this check it fails, so we just use a handler to issue the call after 15 seconds
                        Handler nearestNetworkHandler = new Handler();
                        nearestNetworkHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Log.d("stats","duplicate api lookup for mac:" + mac_address);
                                api.duplicate_router_check(mac_address, sharedPrefs.getString("current_public_ip_address", ""), duplicate_callback);

                                passwordPromptedSsid = ssid;

                                editor.putLong("last_notification_to_add_wifi_password",now);

                                //you have to make a copy of the StringSet for it to save properly when updating
                                Set<String> networks_password_prompted = new HashSet<String>(sharedPrefs.getStringSet("networks_password_prompted", new HashSet<String>()));
                                networks_password_prompted.add(ssid);
                                editor.putStringSet("networks_password_prompted",networks_password_prompted);
                                editor.commit();
                            }
                        }, 10000);//10 second delay
                    }
                }
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        context = this;
        try
        {
            Fabric.with(this, new Crashlytics());
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            editor = sharedPrefs.edit();
            api = new APIFactory().getAPI();
            utils = new Utilities(context);
            utils.setupDatabase("Statistics");
            realm = Realm.getInstance(context, false);
            setupTimer();
            startWiFiScan();
            //we set auto-refresh off, otherwise we get the erorr: java.lang.IllegalStateException: Cannot set auto-refresh in a Thread without a Looper
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        try
        {
            realm.close();
            threadPoolExecutor.shutdownNow();
            wifiThreadPoolExecutor.shutdownNow();
            if(wifi_scan_receiver_registered)
            {
                unregisterReceiver(wifiScanReceiver);
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
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
}
