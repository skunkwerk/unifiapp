package com.unifiapp.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.maxmind.geoip2.WebServiceClient;
import com.maxmind.geoip2.model.CityResponse;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.R;
import com.unifiapp.controller.share.Callbacks.PostVerifyWiFiCheckCallback;
import com.unifiapp.controller.share.Callbacks.PreVerifyWiFiCheckCallback;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIClient.API;
import com.unifiapp.model.APIFactory;
import com.unifiapp.utils.BlackListWhiteList;
import com.unifiapp.utils.ConnectedWifi;
import com.unifiapp.utils.Utilities;

import java.net.InetAddress;
import java.util.List;

import retrofit.Callback;

public class WiFiCheck
{
    Utilities utils;
    WifiManager wifi;
    Context context;
    BlackListWhiteList mac_list;
    ConnectivityManager connManager;
    APIFactory factory;
    API api;
    WebServiceClient ip_api;
    SharedPreferences sharedPref;

    public WiFiCheck(WifiManager wifi, ConnectivityManager connManager, Context context)
    {
        utils = new Utilities(context);
        this.wifi = wifi;
        this.context = context;
        this.connManager = connManager;
        mac_list = new BlackListWhiteList(context);
        factory = new APIFactory();
        api = factory.getAPI();
        ip_api = factory.getIPAPI();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public void getConnectedWiFiDetails(PreVerifyWiFiCheckCallback listener, String current_public_ip_address)
    {
        Bundle data = new Bundle();
        data.putString("ssid",null);
        data.putInt("networkId",0);
        data.putString("auth","");
        data.putString("ip_address",null);
        data.putString("mac",null);

        try
        {
            //wifi.setWifiEnabled(true);
            if (wifi.isWifiEnabled())
            {
                Log.d("wifi is", "enabled");
                String auth=null;
                String backup_auth=null;
                int backup_auth_priority = -1;
                WifiInfo info = wifi.getConnectionInfo();//current WiFi connection, if any is active

                if (info!=null && info.getSSID()!=null && info.getSSID()!="<unknown ssid>" && !TextUtils.isEmpty(info.getSSID()))
                {
                    Log.d("wifiinfo is","connected");
                    List<WifiConfiguration> list = wifi.getConfiguredNetworks();
                    Boolean found_active_config = false;
                    for( WifiConfiguration i : list )
                    {
                        //on pre-4.2 devices i.SSID will be in quotes and info.getSSID will NOT be in quotes!
                        String i_ssid = i.SSID;
                        i_ssid = utils.stripQuotesSSID(i_ssid);
                        String o_ssid = info.getSSID();
                        o_ssid = utils.stripQuotesSSID(o_ssid);
                        Log.d("comparing:", i_ssid + " & " + o_ssid);

                        //we need to check the status as well, as some phones may have multiple configurations for the same SSID
                        //so we can't just pick the first one - you have to pick the one currently connected to

                        //BUT on Android 4.0 there's a bug - none of the WiFiConfigurations have a status of CURRENT, even though one is connected
                        //on newer versions of Android, this is fixed.
                        if(i.status==WifiConfiguration.Status.CURRENT)
                        {
                            Log.d("found","active wifi config");
                            found_active_config = true;
                        }
                        if(i_ssid.equals(o_ssid) && i.status==WifiConfiguration.Status.CURRENT)
                        {
                            auth = utils.getSecurity(i);
                            Analytics.with(context).track("Auth is:" + String.valueOf(auth));
                            utils.LogWiFiConfiguration(i,context);
                            break;
                        }
                        else if(i_ssid.equals(o_ssid) && i.status!=WifiConfiguration.Status.CURRENT)
                        {
                            //edge case is Android 4.0, with multiple configs with the same SSID - which one do you go with?
                            //choose the one with the higher priority, and that is ENABLED
                            if(i.status==WifiConfiguration.Status.ENABLED && i.priority>backup_auth_priority)
                            {
                                backup_auth = utils.getSecurity(i);
                                backup_auth_priority = i.priority;
                            }
                            else
                            {
                                Analytics.with(context).track("ignoring old WiFiConfiguration with lower priority or disabled");
                                Log.d("ignoring","old WiFiConfiguration with lower priority or disabled");
                            }
                        }
                    }
                    if(auth==null && backup_auth==null)
                    {
                        Log.d("auth", "couldn't find matching wifi config - auth & backup_auth are null");
                        Analytics.with(context).track("could not determine auth type of router through matching SSIDs", new Properties().putValue("current_ssid", info.getSSID()));
                    }
                    if(found_active_config==false && backup_auth!=null)
                    {
                        //use the auth from the config, even though it didn't have a status of CURRENT
                        auth = backup_auth;
                    }
                    int local_ip = info.getIpAddress();
                    String local_ip_address = String.format(
                            "%d.%d.%d.%d",
                            (local_ip & 0xff),
                            (local_ip >> 8 & 0xff),
                            (local_ip >> 16 & 0xff),
                            (local_ip >> 24 & 0xff));
                    AsyncTask task = new PreConnectCheck(listener,current_public_ip_address).execute(new ConnectedWifi(info.getSSID(),info.getNetworkId(),"",false, auth, false, info.getBSSID(),"",local_ip_address));
                }
                else
                {
                    //if no connection active, ask them to select one from scan results, or to connect first in android and try again
                    Log.d("wifiinfo is", "NOT connected");
                    Analytics.with(context).track("User Trying to Share WiFi, but WiFi not connected");
                    data.putString("message",context.getResources().getString(R.string.wifi_check_not_connected_wifi));
                    data.putBoolean("success",false);
                    listener.onPreVerifyWiFiCheckComplete(data);
                }
            }
            else
            {
                Log.d("in NewShare","WiFi still not enabled");
                Analytics.with(context).track("User Trying to Share WiFi, but WiFi not turned on");
                data.putString("message",context.getResources().getString(R.string.wifi_check_wifi_not_enabled));
                data.putBoolean("success",false);
                listener.onPreVerifyWiFiCheckComplete(data);
            }
        }
        catch (Exception e)
        {
            Log.d("test", "could not determine WiFi connection settings");
            Analytics.with(context).track("User Trying to Share WiFi, but could not determine WiFi connection settings");
            data.putString("message", context.getResources().getString(R.string.wifi_check_exception));
            data.putBoolean("success",false);
            listener.onPreVerifyWiFiCheckComplete(data);
        }
    }

    /*
    checks for 4 properties of the WiFi connection
    1 - is WiFi turned on?
    2 - are we connected to a WiFi network?
    3 - is the WiFi network password-protected?
    4 - are we located in a city we've launched in?
    if any of these checks is false, we return false
     */
    public class PreConnectCheck extends AsyncTask<ConnectedWifi, Void, Bundle>
    {
        private PreVerifyWiFiCheckCallback listener;
        String current_public_ip_address;

        public PreConnectCheck(PreVerifyWiFiCheckCallback listener, String current_public_ip_address)
        {
            super();
            this.listener=listener;
            this.current_public_ip_address = current_public_ip_address;
        }

        protected Bundle doInBackground(ConnectedWifi... info)
        {
            Boolean good_http_connection = false;
            Bundle data = new Bundle();

            String ssid = info[0].ssid;
            String mac_address = info[0].mac;
            String local_ip_address = info[0].local_ip_address;
            data.putString("ssid",ssid);
            data.putInt("network_id",info[0].network_id);
            data.putString("auth",info[0].auth);
            data.putString("ip_address",current_public_ip_address);
            data.putString("mac",mac_address);
            data.putString("message","");
            data.putBoolean("success",true);
            SharedPreferences.Editor editor = sharedPref.edit();

            //verify we can connect to the Internet first - no guarantee this will be through WiFi, though unless you check the active network!
            try
            {
                NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                if (ssid==null || activeNetwork==null || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI)//even though we checked for this earlier, some time has elapsed and this may have changed
                {
                    //if no connection active, ask them to select one from scan results, or to connect first in android and try again
                    Log.d("wifiinfo is", "NOT connected");
                    Analytics.with(context).track("User Trying to Share WiFi, but WiFi not connected");
                    data.putString("message",context.getResources().getString(R.string.wifi_check_not_connected_wifi));
                    data.putBoolean("success",false);
                    return data;
                }
                int response_code = utils.verifyInternetConnectivity(wifi, context, "Verifying Internet connectivity",true);

                if(response_code!=200)//302 for example, would be redirect for captive portal
                {
                    data.putBoolean("good_http_connection", false);
                    editor.putBoolean("good_http_connection",false);
                    editor.commit();
                    return data;
                }
                else
                {
                    good_http_connection = true;
                    data.putBoolean("good_http_connection",true);
                    editor.putBoolean("good_http_connection",true);
                    editor.commit();
                }
            }
            catch (Exception e)
            {
                Log.d("HTTPUrlConnection", "error");
                Crashlytics.log(Log.ERROR, "ShareFragment", "error in isTethered");
                Crashlytics.logException(e);
                Analytics.with(context).track("Exception Verifying Internet connectivity", new Properties().putValue("exception", e.toString()));
                data.putBoolean("good_http_connection", false);
                editor.putBoolean("good_http_connection",false);
                editor.commit();
                return data;
            }
            try
            {
                if (good_http_connection == true)
                {
                    //we haven't looked up the ISP yet for this IP address, so do it now
                    CityResponse response = ip_api.city(InetAddress.getByName(current_public_ip_address));
                    editor.putString("current_isp", response.getTraits().getIsp());
                    editor.commit();
                }
            }
            catch (Exception e)
            {
                utils.logException(e);
                //this try/catch around the ISP lookup is to catch people who don't have an internet connection on the preshare page,
                //so we don't have a valid current_public_ip_address, so the call to get their ISP will fail,
                //causing it to skip all the mobile hotspot checks!
                //by keeping these in separate try/catches, even if this ISP lookup fails, we will check for their mobile hotspot
            }
            try
            {
                if(good_http_connection)
                {
                    data.putBoolean("tethered",isTethered(ssid, mac_address, local_ip_address));
                    //if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) { urlConnect.setRequestProperty("Connection", "close"); }
                    //System.setProperty("http.keepAlive", "false");
                    /*APIClient.Count routerCount = api.customer_router_count(sharedPref.getInt("customer_id",0));
                    if (routerCount.count>=3)
                    {
                        Log.d("customer_router_count", ">3 routers for user");
                        data.putBoolean("exceeded_router_limit",true);
                    }
                    else
                    {
                        Log.d("customer_router_count", "<3 routers for user");
                        data.putBoolean("exceeded_router_limit",false);
                    }*/
                    data.putBoolean("exceeded_router_limit",false);//we have now removed the limitation of 3 routers, as the prepaid credit no longer active
                    return data;
                }
                else
                {
                    Analytics.with(context).track("WiFiCheck isTethered check - good_http_connection is false, skipped check");
                    data.putBoolean("good_http_connection", false);
                    editor.putBoolean("good_http_connection",false);
                    editor.commit();
                    return data;
                }
            }
            catch (Exception e)
            {
                utils.logException(e);
                data.putBoolean("good_http_connection", true);
                return data;
            }
        }

        protected void onPostExecute(Bundle result)
        {
            //do any UI updates here
            listener.onPreVerifyWiFiCheckComplete(result);
        }
    }

    public void verifyPassword(String password, int network_id, String ssid, Callback<APIClient.Response> duplicate_check_callback, NetworkInfo net, Boolean good_http_connection, PostVerifyWiFiCheckCallback listener)
    {
        AsyncTask task = new PostConnectCheck(password, network_id, ssid, duplicate_check_callback, net, good_http_connection, listener).execute();
    }

    public class PostConnectCheck extends AsyncTask<Void, Void, Integer>
    {
        private PostVerifyWiFiCheckCallback listener;
        private String password;
        private int network_id;
        private String ssid;
        private String current_auth_type;
        private String current_ssid;
        private String current_mac;
        private Callback<APIClient.Response> duplicate_check_callback;
        private NetworkInfo net;
        private Boolean good_http_connection;

        public PostConnectCheck(String password, int network_id, String ssid, Callback<APIClient.Response> duplicate_check_callback, NetworkInfo net, Boolean good_http_connection, PostVerifyWiFiCheckCallback listener)
        {
            this.listener=listener;
            this.password = password;
            this.network_id = network_id;
            this.ssid = ssid;
            this.net = net;
            this.good_http_connection = good_http_connection;
            this.duplicate_check_callback = duplicate_check_callback;
        }

        protected Integer doInBackground(Void... params)
        {
            int result;

            //String ssid, String password, int network_id
            //updateNetwork is not working, so just create a new configuration, disable every other config, and after verified enable everyone
            Log.d("in WiFi", "in PostConnectCheck");
            Log.d("password is:", password);
            Log.d("network_id is:", String.valueOf(network_id));
            WifiConfiguration conf=null;
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();//Upon failure to fetch or when when Wi-Fi is turned off, it can be null.
            if (list!=null)
            {
                for( WifiConfiguration i : list )
                {
                    if(i.networkId==network_id)
                    {
                        conf = i;//and delete the configuration we added
                        utils.LogWiFiConfiguration(i,context);
                        break;
                    }
                }
            }
            final WifiConfiguration original_conf = conf;
            int update_result = -2;
            if(conf!=null)
            {
                //add result will be -1 if <8 characters for WPA or incorrect password?
                current_auth_type = utils.getSecurity(conf);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("current_auth_type", current_auth_type);
                editor.commit();
                if (current_auth_type.equals("WEP"))
                {
                    WifiConfiguration config = utils.changeNetworkCommon(ssid);
                    config.networkId = network_id;
                    config.wepKeys[0] = utils.quoteNonHex(password, 10, 26, 58);
                    update_result = wifi.updateNetwork(config);
                    Log.d("in WiFi configuring with WEP password, update result is:", String.valueOf(update_result));
                }
                else if(current_auth_type.equals("WPA"))
                {
                    WifiConfiguration config = utils.changeNetworkCommon(ssid);
                    config.networkId = network_id;
                    // Hex passwords that are 64 bits long are not to be quoted.
                    config.preSharedKey = utils.quoteNonHex(password, 64);
                    update_result = wifi.updateNetwork(config);
                    Log.d("in WiFi configuring with WPA password, update result is:", String.valueOf(update_result));
                }
                else if(current_auth_type.equals("NONE"))
                {
                    Log.d("in WiFi configuring with NO password","");
                }
                if(update_result==-1)
                {
                    Analytics.with(context).track("wifi.updateNetwork failed!");
                }
                wifi.saveConfiguration();
                wifi.disconnect();
                wifi.enableNetwork(network_id, true);
                wifi.reconnect();
            }

            //wait for up to 10 seconds, and check every half a second to see if we're connected
            //in a separate thread
            if(ssid==null)
            {
                result = Utilities.FAILURE;
                Log.d("in run_verification", "ssid is null despite previous checks");
                Crashlytics.log(Log.ERROR,"in run_verification","ssid is null despite previous checks");
            }
            ssid = utils.stripQuotesSSID(ssid);
            final String router_ssid = ssid; // the final is important
            try
            {
                int elapsed_time = 0;
                //SSID can sometimes be null, so we have to be careful
                Boolean net_is_connected = net.isConnected();
                String detected_ssid = wifi.getConnectionInfo().getSSID();
                SupplicantState supplicant_state = wifi.getConnectionInfo().getSupplicantState();
                NetworkInfo.DetailedState detailed_state = wifi.getConnectionInfo().getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState());
                Log.d("wifi connected:",String.valueOf(net_is_connected));
                Log.d("wifi supplicant:",String.valueOf(supplicant_state));
                Log.d("wifi detailed:", String.valueOf(detailed_state));
                Log.d("ssid detailed:", String.valueOf(detected_ssid));
                while(elapsed_time<10 && (net.isConnected()==false || supplicant_state!= SupplicantState.COMPLETED || detected_ssid==null || detected_ssid.equals("<unknown ssid>") || (detailed_state!= NetworkInfo.DetailedState.CONNECTED)))// && detailed_state!= NetworkInfo.DetailedState.OBTAINING_IPADDR)))
                {
                    //known issue: Android stays in state OBTAINING_IPADDR even after it's got an IP address
                    //so to prevent having to wait all 10 seconds, we
                    android.os.SystemClock.sleep(1000);//wait for 1 second
                    elapsed_time += 1;
                    Log.d("in thread","waiting");
                    Log.d("wifi detailed wait:", wifi.getConnectionInfo().getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState()).toString());
                    Log.d("wifi ssid wait:", String.valueOf(wifi.getConnectionInfo().getSSID()));
                    Log.d("net is connected?", String.valueOf(net.isConnected()));
                }
                //we only send the logs at the end of the loop to avoid overloading the analytics tools with identical logs
                Analytics.with(context).track("WiFi verification while loop 1", new Properties()
                        .putValue("detailed state", wifi.getConnectionInfo().getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState()).toString())
                        .putValue("SSID", String.valueOf(wifi.getConnectionInfo().getSSID()))
                        .putValue("net connected?", String.valueOf(net.isConnected())));
                //we now do this 'good_http_connection' check before we disconnect from WiFi, and use the global variable
                elapsed_time=0;
                int connectvityResult = -1;
                while(elapsed_time<10)
                {
                    //use an HTTP GET request to ensure we're connected
                    //Android's HTTPUrlConnection does not respect the connectionTimeout, so we have to wrap it in a thread instead
                    //example: you're connected to a WiFi router, but that router has no access to the public Internet
                    //requests will hang for minutes
                    //we could set it to be a single TimeOut of 10 seconds, but may be the case that if we get Internet mid-way it still fails
                    //for now, if they don't have an internet connection it'll just stay verifying forever
                    Log.d("2nd while loop","waiting");
                    connectvityResult = utils.verifyInternetConnectivity(wifi, context, "WiFi verification while loop 2",false);
                    if(connectvityResult!=-1)
                        break;
                    else
                    {
                        android.os.SystemClock.sleep(1000);//wait for 1 second
                        elapsed_time+=1;
                    }
                }
                Log.d("while loop","outside");
                Analytics.with(context).track("WiFi verification while loop 2", new Properties()
                        .putValue("UrlConnection response code", String.valueOf(connectvityResult))
                        .putValue("detailed state", wifi.getConnectionInfo().getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState()).toString()));
                //now, verify that we're connected to the correct SSID/network ID
                WifiInfo wifiInfo = wifi.getConnectionInfo();//current WiFi connection, if any is active
                if (net.isConnected() && wifiInfo!=null && good_http_connection==true)
                {
                    //on pre-4.2 devices, the SSID will be returned without quotes, but on later devices it will be returned with quotes
                    //we normalize this by stripping quotes of all the SSIDs, and then comparing & storing those
                    current_ssid = wifiInfo.getSSID();
                    if (current_ssid!=null)
                    {
                        current_ssid = utils.stripQuotesSSID(current_ssid);
                    }
                    current_mac = wifiInfo.getBSSID();//info.getMacAddress() IS THE PHONE'S MAC ADDRESS, NOT THE ROUTER'S!  the router's is bssid
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("current_ssid", current_ssid);
                    editor.putString("current_mac", current_mac);
                    editor.putInt("current_link_speed", wifiInfo.getLinkSpeed());
                    editor.commit();
                    int network_id = wifiInfo.getNetworkId();
                    int local_ip = wifiInfo.getIpAddress();
                    String local_ip_address = String.format(
                            "%d.%d.%d.%d",
                            (local_ip & 0xff),
                            (local_ip >> 8 & 0xff),
                            (local_ip >> 16 & 0xff),
                            (local_ip >> 24 & 0xff));

                    //first, we do ANOTHER check for fraudulent mobile hotspots, in case they somehow bypassed the first check
                    if(isTethered(current_ssid,current_mac,local_ip_address))
                    {
                        result = Utilities.TETHERED;
                    }

                    //due to a bug in Android (even 4.4) with WEP networks, with incorrect passwords the SSID & net.isConnected is stale
                    //so we have to use this to verify we are in fact connected to WiFi
                    NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

                    if (current_ssid!=null && current_ssid.equals(router_ssid) && activeNetwork!=null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                    {
                        //password is correct!
                        Log.d("in WiFi", "password is correct");
                        Analytics.with(context).track("User Entered Correct WiFi Password", new Properties()
                            .putValue("SSID", current_ssid)
                            .putValue("mac_address", current_mac));

                        //first, check to see if this router's password already exists in the database
                        Log.d("unifi","about to call duplicate router check");
                        api.duplicate_router_check(current_mac, sharedPref.getString("current_public_ip_address", ""), duplicate_check_callback);
                        result = Utilities.WAITING;
                        Log.d("unifi","returning waiting");
                    }
                    else
                    {
                        //password was not correct
                        //ask the user to try again
                        //if you jump the gun, may be connecting with SSID reported as <unknown ssid>
                        result = Utilities.FAILURE;
                        Log.d("in check password", "failure SSID is:" + current_ssid);
                        Analytics.with(context).track("User Entered Incorrect WiFi Password", new Properties()
                            .putValue("current SSID", current_ssid)
                            .putValue("expected SSID", String.valueOf(router_ssid))
                            .putValue("MAC", String.valueOf(current_mac))
                        );

                        //restore the original saved conf so they can connect again
                        if(original_conf!=null)
                        {
                            wifi.updateNetwork(original_conf);
                            wifi.saveConfiguration();
                            wifi.disconnect();
                            wifi.enableNetwork(network_id, false);
                            wifi.reconnect();
                        }
                    }
                }
                else if(net.isConnected() && wifiInfo!=null && good_http_connection==false)
                {
                    //if (current_ssid.equals(router_ssid)) //current_ssid may sometimes be null, so this won't work
                    //the WiFi connected, but we couldn't make an HTTP connection
                    result = Utilities.NO_INTERNET;
                    Log.d("in check password", "connected to WiFi, but couldn't make a good HTTP connection in time");
                    Analytics.with(context).track("WiFi Password correct, but couldn't make a good HTTP connection in time", new Properties()
                        .putValue("ssid", current_ssid)
                        .putValue("detailed_supplicant_state", wifi.getConnectionInfo().getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState()).toString())
                        .putValue("net_is_connected", String.valueOf(net.isConnected())));
                }
                else
                {
                    result = Utilities.FAILURE;
                    Log.d("in check password", "out of time failure");
                    Analytics.with(context).track("WiFi Password out of time failure", new Properties()
                        .putValue("ssid", current_ssid));
                }
                //whatever the result, we re-enable all the saved WiFi configurations
                List<WifiConfiguration> networkList = wifi.getConfiguredNetworks();
                for( WifiConfiguration i : networkList )
                {
                    wifi.enableNetwork(i.networkId, false);
                }
            }
            catch (Exception e)
            {
                Crashlytics.log(Log.ERROR, "ShareFragment", "Exception in wifi_thread of connect_to_wifi");
                Crashlytics.logException(e);
                result = Utilities.FAILURE;
            }
            Log.d("postverification result",String.valueOf(result));
            Log.d("unifi","returning result finally");
            return result;
        }

        protected void onPostExecute(Integer result)
        {
            //do any UI updates here
            Log.d("unifi","calling onpostverifywificheckcomplete");
            listener.onPostVerifyWiFiCheckComplete(result);
        }
    }

    public Boolean isTethered(String ssid, String mac_address, String local_ip_address)
    {
        // Apple, ZTE, & Huawei also make WiFi routers so they're not in the blacklisted_macs list
        // ssid.toLowerCase().contains("iball") - removed as iBall also makes WiFi routers (baton)
        Boolean tethered = false;

        if(ssid.toLowerCase().contains("android") || ssid.toLowerCase().contains("iphone") || ssid.toLowerCase().contains("a2adp") || ssid.toLowerCase().contains("micromax") || ssid.toLowerCase().contains("xolo") || ssid.toLowerCase().contains("photon"))
        {
            Log.d("ssid blacklist", "true");
            Analytics.with(context).track("SSID blacklist", new Properties().putValue("ssid", ssid));
            tethered = true;
            return tethered;
        }
        else if(mac_list.mac_blacklisted(mac_address))
        {
            Log.d("mac blacklist", "true");
            Analytics.with(context).track("MAC blacklist", new Properties().putValue("mac", mac_address).putValue("ssid", ssid));
            tethered = true;
            return tethered;
        }
        else if(mac_list.mac_whitelisted(mac_address))
        {
            //we do this, because on rare occassions some WiFi routers (typically enterprise ones - won't have port 80 open)
            tethered = false;
            Analytics.with(context).track("MAC whitelist", new Properties().putValue("mac", mac_address).putValue("ssid", ssid));
            return tethered;
        }
        else if(local_ip_address!=null && !local_ip_address.isEmpty())
        {
            //regular routers all run an HTTP server on port 80 to allow remote administration
            //tethered mobile hotspots DO NOT run HTTP servers on port 80 when you turn on WiFi - they're administered locally
            //we use this to differentiate the two
            //you need root permissions to open port 80 on an Android phone - so unlikely
            final DhcpInfo dhcp = wifi.getDhcpInfo();
            if(dhcp!=null)
            {
                final String gateway_address = utils.intToIp(dhcp.gateway); //Formatter.formatIpAddress(dhcp.gateway); is deprecated
                if ((utils.isPortOpen(gateway_address, 80, 1000))==false)//1 second timeout
                {
                    Log.d("share", "router does not have port 80 opened - tethered");
                    Analytics.with(context).track("router does not have port 80 opened - tethered", new Properties().putValue("mac", mac_address));
                    tethered = true;
                    return tethered;
                }
                else if(gateway_address.equals("192.168.43.1") && local_ip_address!=null && (local_ip_address.contains("192.168.42.") || local_ip_address.contains("192.168.43.")))
                {
                    //android devices used as mobile hotspots have the gateway address as 192.168.43.1,
                    //and give out local IPs in the range 192.168.42.1-254 or 192.168.43.1-254
                    Analytics.with(context).track("Android tethering IPs - tethered", new Properties().putValue("mac", mac_address));
                    tethered = true;
                    return tethered;
                }
                else
                {
                    tethered = false;
                    Analytics.with(context).track("WiFiCheck isTethered check - not tethered", new Properties().putValue("mac", mac_address).putValue("ssid", ssid));
                    return tethered;
                }
            }
            else
            {
                Analytics.with(context).track("Android tethering IPs - dhcp is null, skipped check");
            }
        }
        else
        {
            Analytics.with(context).track("WiFiCheck isTethered check - IP is null or empty");
            tethered = false;
            return tethered;
        }
        return tethered;
    }
}
