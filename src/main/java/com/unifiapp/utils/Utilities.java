package com.unifiapp.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.helpshift.Helpshift;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.unifiapp.BuildConfig;
import com.unifiapp.R;
import com.unifiapp.events.Events;
import com.unifiapp.vpn.logic.TrustedCertificateManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.realm.Realm;
import retrofit.RetrofitError;

public class Utilities
{
    public static final int LATEST_SCHEMA_VERSION = 3;
    public static final long UPDATE_INTERVAL = 1000;
    public static final long FASTEST_INTERVAL = 1000;
    public static final int WAITING = -1;
    public static final int FAILURE = 0;
    public static final int SUCCESS = 1;
    public static final int OUTSIDE = 2;
    public static final int NO_INTERNET = 3;
    public static final int TIMEOUT = 4;
    public static final int DUPLICATE_FOUND = 5;
    public static final int TETHERED = 6;
    public static final int EXCEEDED_ROUTER_LIMIT = 7;
    final String TAG = "GCM";
    final String PROPERTY_APP_VERSION = "app_version";
    final String PROPERTY_REG_ID = "registration_id";
    public HashMap helpshift_config;
    public Context context;
    public SharedPreferences sharedPrefs;
    public SharedPreferences.Editor editor;

    public Utilities(Context context)
    {
        this.context = context;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPrefs.edit();
    }

    public void setupDatabase(String className)
    {
        //check sharedPrefs for our database schema version (which we increment any time we need to do a migration)
        //instead of actually doing a migration, though (which would be tedious) we just delete the database
        //and start over again, as we can get most of the information from the server again, and the rest a loss of only a day or so of
        //connectivity stats, etc.
        //ANYTIME WE UPDATE THE SCHEMA, WE NEED TO UPDATE THE LATEST_SCHEMA_VERSION constant in this class
        try
        {
            int databaseSchemaVersion = sharedPrefs.getInt("database_schema_version",0);
            if(databaseSchemaVersion==0 || databaseSchemaVersion != LATEST_SCHEMA_VERSION)
            {
                logMessage("Deleting database to re-create for new schema version",className);
                Realm.deleteRealmFile(context);
                editor.putInt("database_schema_version",LATEST_SCHEMA_VERSION);
                //to force re-sync of mac list & coverage map data, we set the sync date to 0
                editor.putLong("last_mac_bloom_filter_sync_date",0);
                editor.putLong("last_coverage_map_sync_date",0);
                editor.putLong("last_offline_network_access_sync_date",0);
                editor.putLong("last_customer_routers_added_sync_date",0);
                editor.commit();
            }
        }
        catch(Exception e)
        {
            logException(e);
        }
    }

    /**
     *
     * @param message
     */
    public void logMessage(String message, String className)
    {
        if(BuildConfig.DEBUG)
        {
            Log.d(className, message);
        }
        Analytics.with(context).track(message);
    }

    /**
     *
     * @param message
     */
    public void logMessage(String message, String className, Properties properties)
    {
        if(BuildConfig.DEBUG)
        {
            Log.d(className, message);
            for(Map.Entry<String, Object> entry : properties.entrySet())
            {
                Log.d(entry.getKey(),String.valueOf(entry.getValue()));
            }
        }
        Analytics.with(context).track(message,properties);
    }

    /**
     *
     */
    public void logException(Exception e)
    {
        //send to Crashlytics, Analytics, and debug log (if debugging)
        if(BuildConfig.DEBUG)
        {
            Log.e(e.getStackTrace().getClass().toString(), e.getStackTrace()[0].getMethodName());
            e.printStackTrace();
        }
        Crashlytics.logException(e);
        StackTraceElement[] stackTraces = e.getStackTrace();
        if(stackTraces.length>0)
            Analytics.with(context).track("Exception in " + stackTraces[0].getClassName(), new Properties().putValue("lineNumber",stackTraces[0].getLineNumber()).putValue("methodName", stackTraces[0].getMethodName()).putValue("type of error",e.getClass().toString()).putValue("error", e.toString()));
        else
            Analytics.with(context).track("Exception", new Properties().putValue("type of error", e.getClass().toString()).putValue("error", e.toString()));
    }

    public void logRetrofitError(RetrofitError error, String className, String methodName)
    {
        //send to Crashlytics, Analytics, and debug log (if debugging)
        if(BuildConfig.DEBUG)
        {
            Log.e(className, "Retrofit error:" + error.toString());
        }
        Crashlytics.log(Log.ERROR, className, "Retrofit error");
        Analytics.with(context).track("Retrofit error in " + className + ":" + methodName, new Properties().putValue("error",error.toString()));
    }

    public boolean isMyServiceRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Class that loads the cached CA certificates.
     */
    public class LoadCertificatesTask extends AsyncTask<Void, Void, TrustedCertificateManager>
    {
        @Override
        protected void onPreExecute()
        {}
        @Override
        protected TrustedCertificateManager doInBackground(Void... params)
        {
            return TrustedCertificateManager.getInstance().load();
        }
        @Override
        protected void onPostExecute(TrustedCertificateManager result)
        {}
    }

    /**
     *
     */
    public Boolean deleteWiFiConfiguration(int netId)
    {
        //we try/catch this as the network IDs may have changed over time (even though removeNetwork itself has try/catch)
        Boolean result = false;
        try
        {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            result = wifiManager.removeNetwork(netId);
        }
        catch (Exception e)
        {
            logException(e);
        }
        if(result)
        {
            Analytics.with(context).track("Deleted community WiFiConfiguration after disconnection");
        }
        else
        {
            Analytics.with(context).track("Could not delete community WiFiConfiguration after disconnection");
        }
        return result;
    }

    public Boolean isDeviceRooted()
    {
        {
            if ((!isTestKeyBuild()) && (!hasSuperuserApk()) && (!canExecuteSuCommand()))
                return false;
            else
            {
                Properties props = new Properties();
                props.putValue("test_key_build",isTestKeyBuild());
                props.putValue("superuser_apk",hasSuperuserApk());
                props.putValue("execute_su",canExecuteSuCommand());
                Analytics.with(context).track("Device is Rooted");
                //return true;
                return false;//temporarily, while we debug connectivity issues
            }
        }
    }

    private boolean canExecuteSuCommand()
    {
        try
        {
            Runtime.getRuntime().exec("su");
            return true;
        }
        catch (IOException localIOException)
        {
            return false;
        }
    }

    private boolean hasSuperuserApk()
    {
        return new File("/system/app/Superuser.apk").exists();
    }

    private boolean isTestKeyBuild()
    {
        String str = Build.TAGS;
        if ((str != null) && (str.contains("test-keys")))
            return true;
        return false;
    }

    /**
     *
     */
    public static class GetCoarseLocation implements
            GooglePlayServicesClient.ConnectionCallbacks,
            GooglePlayServicesClient.OnConnectionFailedListener,
            LocationListener
    {
        public LocationClient locationClient;
        LocationManager locationManager;
        LocationRequest locationRequest;
        SharedPreferences sharedPrefs;

        public GetCoarseLocation(SharedPreferences sharedPrefs, Context context)
        {
            this.sharedPrefs = sharedPrefs;
            locationClient = new LocationClient(context,this,this);
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationClient.connect();
            locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
            locationRequest.setInterval(3600000);//1 hour updates
            locationRequest.setFastestInterval(60000);//1 minute
        }

        @Override
        public void onLocationChanged(Location location)
        {
            saveLocationToPrefs(location);
        }

        @Override
        public void onConnected(Bundle bundle)
        {
            Location location = locationClient.getLastLocation();
            if (location == null)
            {
                locationClient.requestLocationUpdates(locationRequest, this);
            }
            else
            {
                saveLocationToPrefs(location);
            }
        }

        public void saveLocationToPrefs(Location location)
        {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putFloat("coarse_latitude",(float) location.getLatitude());
            editor.putFloat("coarse_longitude",(float) location.getLongitude());
            editor.putFloat("coarse_altitude",(float) location.getAltitude());
            editor.commit();
        }

        @Override
        public void onDisconnected() {}

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {}
    }

    /**
     *
     */
    public static class GetAccurateLocation implements
            GooglePlayServicesClient.ConnectionCallbacks,
            GooglePlayServicesClient.OnConnectionFailedListener,
            LocationListener
    {
        public LocationClient locationClient;
        LocationManager locationManager;
        LocationRequest locationRequest;
        SharedPreferences sharedPrefs;
        Boolean locationUpdateReceived = false;

        public GetAccurateLocation(SharedPreferences sharedPrefs, Context context)
        {
            this.sharedPrefs = sharedPrefs;
            locationClient = new LocationClient(context,this,this);
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationClient.connect();
            locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            locationRequest.setInterval(60000);//1 minute updates
            locationRequest.setFastestInterval(60000);//1 minute
            locationRequest.setExpirationDuration(30000);//expires after 30 seconds
        }

        @Override
        public void onLocationChanged(Location location)
        {
            sendLocation(location);
        }

        @Override
        public void onConnected(Bundle bundle)
        {
            Location location = locationClient.getLastLocation();
            if (location == null)
            {
                locationClient.requestLocationUpdates(locationRequest, this);
                ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);//1 thread
                threadPoolExecutor.schedule(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (locationUpdateReceived==false)
                        {
                            Log.d("in location onConnected", "been 30 seconds - still no location update received");
                            sendTimeoutEvent();
                        }
                    }
                }, 30, TimeUnit.SECONDS);//run it once, after 5 seconds
            }
            else
            {
                sendLocation(location);
            }
        }

        public void sendLocation(Location location)
        {
            locationUpdateReceived = true;
            //fire off an event that's processed in the HomeFragment
            Events.LocationUpdateEvent locationUpdateEvent = new Events.LocationUpdateEvent(location);
            EventBus.getDefault().post(locationUpdateEvent);
        }

        @Override
        public void onDisconnected() {}

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult)
        {
            sendTimeoutEvent();
        }

        public void sendTimeoutEvent()
        {
            Events.LocationTimedOutEvent locationTimedOutEvent = new Events.LocationTimedOutEvent();
            EventBus.getDefault().post(locationTimedOutEvent);
        }
    }


    /**
     *
     * @param input
     * @return
     */
    public String hashString(String input)
    {
        String result = "unknown";
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++)
            {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            System.out.println("Hex format : " + sb.toString());

            //convert the byte to hex format method 2
            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<byteData.length;i++)
            {
                String hex=Integer.toHexString(0xff & byteData[i]);
                if(hex.length()==1) hexString.append('0');
                hexString.append(hex);
            }
            result = hexString.toString();
        }
        catch (Exception e)
        {
            logException(e);
        }
        return result;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public boolean checkPlayServices(Activity activity)
    {
        Log.d("gcm","checkPlayServices");
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        return checkPlayServicesResponseCode(resultCode, activity);
    }

    public boolean checkPlayServicesResponseCode(int resultCode, Activity activity)
    {
        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else
            {
                Log.i(TAG, "This device is not supported.");
                Analytics.with(context).track("Device not Supported by Google Play");
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public String getRegistrationId(Context context)
    {
        Log.d("gcm","getRegistrationId");

        String registrationId = sharedPrefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty())
        {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = sharedPrefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion)
        {
            Log.i(TAG, "App version changed.");
            return "";
        }
        Log.d("gcm","reg id is: " + registrationId);
        return registrationId;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context)
    {
        Log.d("gcm","getAppVersion");
        try
        {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    public void storeRegistrationId(Context context, String regId)
    {
        int appVersion = getAppVersion(context);
        final String PROPERTY_REG_ID = "registration_id";
        final String PROPERTY_APP_VERSION = "app_version";
        Log.d("gcm", "Saving regId on app version " + appVersion);
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     *
     * @param sharedPref
     * @param context
     */
    public void getPublicIPAddress(final SharedPreferences sharedPref, final Context context)
    {
        Runnable ipThread = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String ip_address = HttpRequest.get("http://whatismyip.akamai.com/").body();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("current_public_ip_address", ip_address);
                    editor.commit();
                }
                catch (Exception e)
                {
                    Crashlytics.log(Log.ERROR, "PreShareFragment", "could not get public IP address");
                    Analytics.with(context).track("could not get public IP address", new Properties().putValue("error",e.toString()));
                    Crashlytics.logException(e);
                }
            }
        };
        new Thread(ipThread).start();
    }

    /**
     *
     * @param source
     * @param sharedPrefs
     * @param context
     */
    public void inviteFriends(String source, SharedPreferences sharedPrefs, final Context context)
    {
        Analytics.with(context).track("User Pressed Invite Friends Button");

        // associate data with a link
        // you can access this data from any instance that installs or opens the app from this link (amazing...)

        JSONObject dataToInclude = new JSONObject();
        try
        {
            dataToInclude.put("referring_customer_id", sharedPrefs.getInt("customer_id", 0));
            dataToInclude.put("referring_customer_name", sharedPrefs.getString("first_name", "") + " " + sharedPrefs.getString("last_name", ""));
            dataToInclude.put("destination", "preshare");
            dataToInclude.put("points", 0);//only giving points when actually add a router
        }
        catch (JSONException ex) {}

        Branch branch = Branch.getInstance(context);
        branch.getShortUrl(new ArrayList<String>(), "app", Branch.FEATURE_TAG_REFERRAL, source, dataToInclude, new BranchLinkCreateListener()
        {
            @Override
            public void onLinkCreate(String url)
            {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                //HTML limits options to Gmail, Telegram, etc. - no SMS/whatsapp/viber.  so we have to use plaintext
                //sendIntent.setType("text/html");
                //sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Get free WiFi throughout Mumbai - download <a href='" + url + "'>UnifiApp</a>"));
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.invite_friends_message) + url);
                context.startActivity(sendIntent);
            }
        });
    }

    /**
     *
     * @param ip
     * @param port
     * @param timeout
     * @return
     */
    public static boolean isPortOpen(final String ip, final int port, final int timeout)
    {
        try
        {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        }
        catch (ConnectException ce)
        {
            Log.d("share", "connect exception");
            return false;
        }
        catch (Exception e)
        {
            Log.d("share","connect exception");
            return false;
        }
    }

    /**
     *
     * @param config
     * @param context
     */
    public void LogWiFiConfiguration(WifiConfiguration config, Context context)
    {
        Analytics.with(context).track("connectToWiFi matching config", new Properties()
                .putValue("ssid", config.SSID.replace("\"", ""))
                .putValue("wepKeys[0] null?", String.valueOf(config.wepKeys[0] == null))
                .putValue("preSharedKey",String.valueOf(config.preSharedKey))
                .putValue("status",String.valueOf(config.status))
                .putValue("BSSID",String.valueOf(config.BSSID))
                .putValue("allowedProtocols",String.valueOf(config.allowedProtocols))
                .putValue("allowedPairwiseCiphers",String.valueOf(config.allowedPairwiseCiphers))
                .putValue("allowedKeyManagement",String.valueOf(config.allowedKeyManagement))
                .putValue("allowedGroupCiphers",String.valueOf(config.allowedGroupCiphers))
                .putValue("allowedAuthAlgorithms",String.valueOf(config.allowedAuthAlgorithms)));
    }

    /**
     *
     * @param addr
     * @return
     */
    public String intToIp(int addr)
    {
        return  ((addr & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF));
    }

    /**
     *
     * @param config
     * @return
     */
    public static String getSecurity(WifiConfiguration config)
    {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK))
        {
            return "WPA";
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X))
        {
            return "EAP";
        }
        return (config.wepKeys[0] != null) ? "WEP" : "NONE";
    }

    public static String quoteNonHex(String value, int... allowedLengths)
    {
        return isHexOfLength(value, allowedLengths) ? value : convertToQuotedString(value);
    }

    /**
     * Encloses the incoming string inside double quotes, if it isn't already quoted.
     * @param string the input string
     * @return a quoted string, of the form "input".  If the input string is null, it returns null
     * as well.
     */
    private static String convertToQuotedString(String string)
    {
        if (string == null || string.length() == 0) {
            return null;
        }
        // If already quoted, return as-is
        if (string.charAt(0) == '"' && string.charAt(string.length() - 1) == '"') {
            return string;
        }
        return '\"' + string + '\"';
    }

    /**
     * @param value input to check
     * @param allowedLengths allowed lengths, if any
     * @return true if value is a non-null, non-empty string of hex digits, and if allowed lengths are given, has
     *  an allowed length
     */
    private static boolean isHexOfLength(CharSequence value, int... allowedLengths)
    {
        final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");
        if (value == null || !HEX_DIGITS.matcher(value).matches()) {
            return false;
        }
        if (allowedLengths.length == 0) {
            return true;
        }
        for (int length : allowedLengths) {
            if (value.length() == length) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param ssid
     * @return
     */
    public static WifiConfiguration changeNetworkCommon(String ssid)
    {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        // Android API insists that an ascii SSID must be quoted to be correctly handled.
        config.SSID = Utilities.quoteNonHex(ssid);
        return config;
    }

    /**
     *
     * @param latitude
     * @param longitude
     * @return
     */
    public static Boolean verifyInsideCity(Double latitude, Double longitude)
    {
        //MUMBAI is: 18.9750, 72.8258 +/- 0.5 degree
        //SF BAY AREA is: 37.7833, -122.4167 +/- 0.5 degree
        //DELHI is: 28.6100, 77.2300 +/- 0.5 degree
        if (Math.abs(latitude-18.975)<=0.5 && Math.abs(longitude-72.8258)<=0.5)
            return true;
        else if (Math.abs(latitude-37.7833)<=1.5 && Math.abs(-1*longitude-122.4167)<=1.5)
            return true;
        else if (Math.abs(latitude-28.6100)<=0.5 && Math.abs(longitude-77.2300)<=0.5)
            return true;
        else
            return false;
    }

    /**
     *
     * @param ssid
     * @return
     */
    public static String stripQuotesSSID(String ssid)
    {
        if (ssid.startsWith("\"") && ssid.endsWith("\""))
        {
            ssid = ssid.substring(1, ssid.length()-1);
        }
        return ssid;
    }

    /**
     *
     * @param wifi
     * @param context
     * @param logMessage
     * @return
     */
    public int verifyInternetConnectivity(WifiManager wifi, Context context, String logMessage, Boolean printDebugMessage)
    {
        try
        {
            /*InetAddress google = InetAddress.getByName("8.8.8.8");
            Boolean reachable = google.isReachable(1000);
            not enough to check DNS, as some captive portals allow DNS but not HTTP*/
            URL url = new URL("http://whatismyip.akamai.com/");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(2000); //set timeout to 2 seconds
            int responseCode = urlConnection.getResponseCode();
            //if (!url.getHost().equals(urlConnection.getURL().getHost()))
            if(printDebugMessage)
            {
                Log.d("in UrlConnection", "got response code:" + String.valueOf(responseCode));
                Log.d("in WiFi","done with HttpURLConnection");
                Log.d("wifi detailed wait:", wifi.getConnectionInfo().getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState()).toString());
                Analytics.with(context).track(logMessage, new Properties()
                        .putValue("UrlConnection response code", String.valueOf(responseCode))
                        .putValue("detailed state", wifi.getConnectionInfo().getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState()).toString()));
            }
            urlConnection.disconnect();
            return responseCode;
        }
        catch (Exception e)
        {
            //IOExceptions happen when can't connect
            logException(e);
            return -1;
        }
    }

    /**
     *
     * @param context
     * @param sharedPrefs
     */
    public void configureCustomerIdentification(Context context, SharedPreferences sharedPrefs)
    {
        Analytics.with(context).identify(String.valueOf(sharedPrefs.getInt("customer_id", 0)), new Traits()
                .putEmail(sharedPrefs.getString("email_address", ""))
                .putLastName(sharedPrefs.getString("last_name", ""))
                .putFirstName(sharedPrefs.getString("first_name", ""))
                .putValue("user_oauth_provider","facebook")
                .putValue("oauth_user_id", sharedPrefs.getString("facebook_user_id", ""))
                .putValue("app_version",sharedPrefs.getInt("app_version",0)),null);

        Crashlytics.setUserIdentifier(String.valueOf(sharedPrefs.getInt("customer_id", 0)));
        Crashlytics.setUserEmail(sharedPrefs.getString("email_address", ""));
        configureHelpshift(context.getResources().getString(R.string.helpshift_default_message), sharedPrefs);
    }

    /**
     *
     * @param prefill_text
     */
    public void configureHelpshift(String prefill_text, SharedPreferences sharedPrefs)
    {
        helpshift_config = new HashMap();
        HashMap customMetadata = new HashMap();
        customMetadata.put("phone_number",sharedPrefs.getString("phone_number", "0"));
        helpshift_config.put(Helpshift.HSCustomMetadataKey, customMetadata);
        helpshift_config.put("enableContactUs", Helpshift.ENABLE_CONTACT_US.ALWAYS);
        helpshift_config.put("gotoConversationAfterContactUs", true);
        Helpshift.setNameAndEmail(sharedPrefs.getString("first_name", "") + " " + sharedPrefs.getString("last_name", ""), sharedPrefs.getString("email_address", ""));
        Helpshift.setUserIdentifier(String.valueOf(sharedPrefs.getInt("customer_id", 0)));
        helpshift_config.put("hideNameAndEmail", true);
        helpshift_config.put("conversationPrefillText", prefill_text);
    }

    /**
     *
     * @return
     */
    public HashMap getHelpshiftConfig()
    {
        return helpshift_config;
    }
}
