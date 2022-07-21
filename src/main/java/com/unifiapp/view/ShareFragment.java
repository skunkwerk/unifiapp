package com.unifiapp.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.maxmind.geoip2.WebServiceClient;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.BuildConfig;
import com.unifiapp.R;
import com.unifiapp.controller.WiFiCheck;
import com.unifiapp.controller.share.Callbacks;
import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient.API;
import com.unifiapp.model.APIFactory;
import com.unifiapp.utils.Utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import io.branch.referral.Branch;

public class ShareFragment extends Fragment implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, Callbacks.PostVerifyWiFiCheckCallback,
        Callbacks.PreVerifyWiFiCheckCallback
{
    public Context context;
    public WifiManager wifi;
    public ConnectivityManager connManager;
    public NetworkInfo net;
    public TextView text_description;
    public BootstrapButton verify_button;
    public BootstrapButton invite_button;
    public BootstrapEditText password_field;
    public ImageButton security_button;
    public ProgressBar spinner;
    public Boolean location_done = false;
    public Location current_location;
    public String current_password;
    public String current_ssid;
    public int current_network_id;
    public String current_auth;
    public String current_mac;
    public String current_isp;
    public ConnectivityChangeReceiver change_receiver;
    SharedPreferences sharedPref;
    public ShareFragment(){}
    WebServiceClient ip_lookup;

    LocationClient locationClient;
    LocationManager locationManager;
    LocationRequest locationRequest;
    private Boolean location_update_received = false;
    private long wifi_verified_time = 0;
    Utilities utils;
    Callbacks callbacks;
    WiFiCheck wifiCheck;
    Boolean updatePassword = false;

    private API api;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        context = this.getActivity(); //has to be called after onAttach otherwise null
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        net = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        utils = new Utilities(context);
        callbacks = new Callbacks(this, this, context, sharedPref);
        wifiCheck = new WiFiCheck(wifi, connManager, context);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            if (change_receiver!=null)
            {
                context.unregisterReceiver(change_receiver);
                change_receiver = null;
            }
        }
        catch (Exception e)
        {
            Log.d("onPause","error!");
            Crashlytics.log(Log.ERROR, "ShareFragment", "error onPause unregisterReceiver");
            Crashlytics.logException(e);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Analytics.with(getActivity()).screen("Share", "");
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.new_password, container, false);
        text_description = (TextView) rootView.findViewById(R.id.share_password_text_prompt);
        security_button = (ImageButton) rootView.findViewById(R.id.share_password_security);
        spinner = (ProgressBar) rootView.findViewById(R.id.spinner);
        password_field = (BootstrapEditText) rootView.findViewById(R.id.share_password);
        verify_button = (BootstrapButton) rootView.findViewById(R.id.verify);
        invite_button = (BootstrapButton) rootView.findViewById(R.id.share_wifi);
        invite_button.setVisibility(View.INVISIBLE);
        verify_button.setVisibility(View.INVISIBLE);
        password_field.setVisibility(View.INVISIBLE);
        security_button.setVisibility(View.INVISIBLE);

        security_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Events.ShowFAQEvent faqEvent = new Events.ShowFAQEvent("4");
                EventBus.getDefault().post(faqEvent);
            }
        });

        APIFactory factory = new APIFactory();
        api = factory.getAPI();
        ip_lookup = factory.getIPAPI();

        locationClient = new LocationClient(context,this,this);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationClient.connect();
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //PRIORITY_HIGH_ACCURACY - with this, the GPS may turn on, in which case we won't get a location fix within 5 seconds (especially if they're indoors)
        // Set the update interval to 1 second
        locationRequest.setInterval(utils.UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        locationRequest.setFastestInterval(utils.FASTEST_INTERVAL);
        locationRequest.setNumUpdates(1);// stop after the first update, even though people may come back later when on different wifi network
        locationRequest.setExpirationDuration(60000);//expires in 60 seconds, in case the location can't be computed.  otherwise would be a battery drain
        change_receiver = new ConnectivityChangeReceiver();

        if(getArguments()!=null && getArguments().getBoolean("update", false)==true)
        {
            updatePassword = true;
        }

        return rootView;
    }

    public void prompt_for_wifi()
    {
        //first, query for the operator of the phone number they entered
        api.get_operator_for_number(sharedPref.getString("phone_number","0"), callbacks.operator_callback);
        //we get their public IP address (we don't cache this or attempt to get it on the preshare page, as users may keep that page open, but change wifi connections
        // which would leave us with an out-of-date IP address)
        utils.getPublicIPAddress(sharedPref, getActivity());
        wifiCheck.getConnectedWiFiDetails(this, sharedPref.getString("current_public_ip_address", ""));
    }

    @Override
    public void onPreVerifyWiFiCheckComplete(Bundle data)
    {
        Log.d("share", "preverify wifi check complete");
        String message = data.getString("message", "");
        Log.d("in", "connected_wifi_handler");
        if(isAdded())
        {
            if (data.getBoolean("success", false) == true)
            {
                String ssid = data.getString("ssid", "");
                String auth = data.getString("auth", "");
                Boolean good_connection = data.getBoolean("good_http_connection", false);
                Log.d("goodConnection", String.valueOf(data.getBoolean("good_http_connection", false)));
                current_ssid = ssid;
                current_network_id = data.getInt("network_id", 0);
                current_auth = auth;
                Set<String> networks_submitted = sharedPref.getStringSet("wifi_networks_submitted", new HashSet<String>());
                if (networks_submitted.contains(ssid) == true)
                {
                    text_description.setText(getResources().getString(R.string.share_already_added));
                    spinner.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    security_button.setVisibility(View.INVISIBLE);
                }
                else if (good_connection == false)
                {
                    text_description.setText(getResources().getString(R.string.share_could_not_connect_internet));
                    spinner.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    security_button.setVisibility(View.INVISIBLE);
                    Analytics.with(context).track("Customer connected to network without Internet connection & blocked from sharing");
                }
                else if (auth == null || auth == "NONE")// && wifi_name.ssid.equals("0x")==false)
                {
                    //automatically add the network, but don't send credit (not doing this anymore)
                    text_description.setText(getResources().getString(R.string.share_open_network));
                    spinner.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    security_button.setVisibility(View.INVISIBLE);
                    Analytics.with(context).track("Customer connected to open WiFi network & blocked from sharing");
                    /*api.new_router(wifi_name.ssid, "", wifi_name.mac, "NONE", current_location.getLatitude(), current_location.getLongitude(), callback);*/
                }
                else if (data.getBoolean("tethered", false) == true)
                {
                    text_description.setText(getResources().getString(R.string.share_tethered));
                    spinner.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    security_button.setVisibility(View.INVISIBLE);
                    Analytics.with(context).track("Customer connected to mobile WiFi network & blocked from sharing");
                }
                else if(data.getBoolean("exceeded_router_limit",false) == true)
                {
                    text_description.setText(getResources().getString(R.string.share_exceeded_router_limit));
                    spinner.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    security_button.setVisibility(View.INVISIBLE);
                    Analytics.with(context).track("Customer reached WiFi network sharing limit & blocked from sharing");
                }
                else if (ssid.equals("0x") == false)
                {
                    if(updatePassword==true)
                    {
                        //TODO: verify that this network is actually a network they previously added (networks_submitted.contains(ssid))
                        text_description.setText(getResources().getString(R.string.share_please_enter_new_password_prefix) + ssid + getResources().getString(R.string.share_please_enter_new_password_suffix));
                    }
                    else
                    {
                        text_description.setText(getResources().getString(R.string.share_please_enter_password_prefix) + ssid + getResources().getString(R.string.share_please_enter_password_suffix));
                    }
                    verify_button.setVisibility(View.VISIBLE);
                    password_field.setVisibility(View.VISIBLE);
                    security_button.setVisibility(View.VISIBLE);
                }
                else
                {
                    Log.d("in prompt_for_wifi", "got 0x SSID");
                    Analytics.with(context).track("Got 0x SSID");
                }
            }
            else
            {
                text_description.setText(message);
                spinner.setVisibility(View.INVISIBLE);
                password_field.setVisibility(View.INVISIBLE);
                verify_button.setVisibility(View.INVISIBLE);
                security_button.setVisibility(View.INVISIBLE);
            }
            invite_button.setVisibility(View.INVISIBLE);
            spinner.setVisibility(View.INVISIBLE);
            password_field.setOnEditorActionListener(new TextView.OnEditorActionListener()
            {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
                {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_DONE)
                    {
                        run_verification(current_ssid, current_network_id);
                        handled = true;
                    }
                    return handled;
                }
            });
            verify_button.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    run_verification(current_ssid, current_network_id);
                }
            });
            invite_button.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    utils.inviteFriends("share", sharedPref, context);
                }
            });
        }
    }

    public void run_verification(String ssid, int network_id)
    {
        //hide the keyboard
        Log.d("ShareFragment","in run verification");
        InputMethodManager inputManager = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(password_field.getWindowToken(), 0);
        current_password = password_field.getText().toString();
        Analytics.with(context).track("User typed in WiFi password", new Properties().putValue("password", current_password));
        int password_length = 8;
        //WPA password minimum length is 8, WEP password minimum length is 5 (for 64-bit key)
        if (current_auth==null || (current_auth.equals("WEP") && current_password.length() < 5) || (current_auth.equals("WPA") && current_password.length() < 8))
        {
            if(current_auth!=null && current_auth.equals("WEP"))
                password_length = 5;
            Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(getResources().getString(R.string.share_password_length_title), getResources().getString(R.string.share_password_length_text_prefix) + String.valueOf(password_length) + getResources().getString(R.string.share_password_length_text_suffix), false);
            EventBus.getDefault().post(dialogEvent);
        }
        else
        {
            spinner.setIndeterminate(true);
            spinner.setVisibility(View.VISIBLE);
            text_description.setText(getResources().getString(R.string.share_verifying));
            security_button.setVisibility(View.INVISIBLE);
            password_field.setVisibility(View.INVISIBLE);
            verify_button.setVisibility(View.INVISIBLE);
            try
            {
                if (change_receiver!=null)
                {
                    context.unregisterReceiver(change_receiver);
                    change_receiver = null;
                }
            }
            catch (Exception e)
            {
                Crashlytics.log(Log.ERROR, "ShareFragment", "could not unregisterReceiver");
                Crashlytics.logException(e);
            }
            //try to connect to the WiFi using this password
            //ssid, current_password, network_id
            wifiCheck.verifyPassword(current_password, network_id, ssid, callbacks.duplicate_callback, net, sharedPref.getBoolean("good_http_connection",false), this);
        }
    }

    public class ConnectivityChangeReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //check to see that this isn't called immediately after we just verified a password (as that changes the connectivity)
            long now = System.currentTimeMillis();
            long diff = now - wifi_verified_time;
            //need a diff of at least 60 seconds, otherwise we ignore the event
            if (diff>60000)
            {
                Log.d("ShareFragment","connectivity_change prompt_for_wifi");
                prompt_for_wifi();
            }
        }

        private void debugIntent(Intent intent, String tag) {}
    }

    @Override
    public void onPostVerifyWiFiCheckComplete(int result)
    {
        //duplicate, no duplicate, no internet, failure, waiting, success
        Log.d("share","wifi check complete, got result:" + String.valueOf(result));
        if(isAdded())
        {
            switch(result)
            {
                case Utilities.SUCCESS:
                    //check to see if we're still sending Rs. 50 credit for this city to bootstrap the network or not
                    TimeZone tz = TimeZone.getTimeZone("UTC");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                    df.setTimeZone(tz);
                    final String nowAsISO = df.format(new Date());
                    final int customer_id = sharedPref.getInt("customer_id",0);
                    final String public_ip = sharedPref.getString("current_public_ip_address", "");
                    current_isp = sharedPref.getString("current_isp","");
                    api.new_router(sharedPref.getString("current_ssid",""), current_password, sharedPref.getString("current_mac",""), sharedPref.getString("current_auth_type",""), customer_id, current_location.getLatitude(), current_location.getLongitude(), current_location.getAltitude(), nowAsISO, public_ip, sharedPref.getInt("current_link_speed",0), sharedPref.getString("current_isp",""), callbacks.callback);

                    if(updatePassword==true)
                    {
                        //don't send credit, just tell the user that their password has been updated
                        text_description.setText(getResources().getString(R.string.share_thanks_password_updated));
                        api.update_router_password(sharedPref.getString("current_ssid",""), current_password, sharedPref.getString("current_mac",""), sharedPref.getString("current_auth_type",""), current_location.getLatitude(), current_location.getLongitude(), current_location.getAltitude(), public_ip, sharedPref.getInt("current_link_speed",0), sharedPref.getString("current_isp",""), callbacks.update_callback);
                        Analytics.with(context).track("Updated WiFi password",  new Properties()
                                .putValue("ssid", sharedPref.getString("current_ssid",""))
                                .putValue("mac_address", sharedPref.getString("current_mac","")));
                    }
                    else
                    {
                        if(sharedPref.getBoolean("city_bootstrap_active", true))
                        {
                            text_description.setText(getResources().getString(R.string.thanks_boostrap_active));
                            //send money to their number
                            String phone_number = sharedPref.getString("phone_number", "0");
                            if(phone_number.equals("0")==false)
                            {
                                Boolean show_help_button = true;
                                String title = getResources().getString(R.string.share_select_operator);
                                String msg = getResources().getString(R.string.share_select_operator_message);
                                Events.DisplayOperatorDialogEvent dialogEvent = new Events.DisplayOperatorDialogEvent(title, msg, show_help_button);
                                EventBus.getDefault().post(dialogEvent);
                                Analytics.with(context).track("Prompted for Operator to Send Prepaid Credit", new Properties().putValue("phone_number", phone_number));
                            }
                            else
                                Analytics.with(context).track("trying to send credit, but phone number is 0!");
                        }
                        else
                        {
                            text_description.setText(getResources().getString(R.string.thanks_bootstrap_inactive));
                            Analytics.with(context).track("Not Requesting Prepaid Credit Transfer to Customer - bootstrap ended");
                        }
                        //if they were referred to the app, award points to the referrer
                        // only for the first router added, though - Branch will ensure this and issue a callback to our server webhook
                        Branch branch = Branch.getInstance(context);
                        branch.userCompletedAction("added_router");

                        //you have to make a copy of the StringSet for it to save properly when updating
                        Set<String> networks_submitted = new HashSet<String>(sharedPref.getStringSet("wifi_networks_submitted", new HashSet<String>()));
                        networks_submitted.add(current_ssid);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putStringSet("wifi_networks_submitted",networks_submitted);
                        editor.commit();
                    }
                    verify_button.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    security_button.setVisibility(View.INVISIBLE);
                    spinner.setVisibility(View.INVISIBLE);
                    invite_button.setVisibility(View.VISIBLE);
                    wifi_verified_time = System.currentTimeMillis();
                    //for refreshing when they later connect to a different wifi network
                    //TODO: put this back
                    //if(change_receiver==null)
                    //    context.registerReceiver(change_receiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                    break;
                case Utilities.FAILURE:
                    text_description.setText(getResources().getString(R.string.share_tethered));
                    spinner.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.VISIBLE);
                    password_field.setVisibility(View.VISIBLE);
                    //change_receiver = new ConnectivityChangeReceiver(); may be '0x' SSID if do this and not connected
                    if(change_receiver==null)
                        context.registerReceiver(change_receiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                    break;
                case Utilities.TETHERED:
                    text_description.setText(getResources().getString(R.string.share_tethered));
                    spinner.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    security_button.setVisibility(View.INVISIBLE);
                    Analytics.with(context).track("Customer connected to mobile WiFi network & blocked from sharing");
                case Utilities.DUPLICATE_FOUND:
                    text_description.setText(getResources().getString(R.string.share_duplicate_found));
                    spinner.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);

                    Analytics.with(context).track("User attempted to share a WiFi router already in the database", new Properties()
                            .putValue("ssid", current_ssid)
                            .putValue("mac_address", current_mac)
                            .putValue("ip_address", sharedPref.getString("current_public_ip_address",""))
                            .putValue("isp", sharedPref.getString("current_isp","")));
                    break;
                case Utilities.NO_INTERNET:
                    text_description.setText(getResources().getString(R.string.share_could_not_connect_internet));
                    spinner.setVisibility(View.INVISIBLE);
                    verify_button.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    Analytics.with(context).track("After password verification, could not access Internet through user's WiFi network", new Properties()
                            .putValue("ssid", current_ssid)
                            .putValue("mac_address", current_mac));
                    break;
                case Utilities.WAITING:
                    text_description.setText("Verifying...");
                    verify_button.setVisibility(View.INVISIBLE);
                    password_field.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }

    protected Handler location_handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(isAdded())
            {
                switch(msg.what)
                {
                    case Utilities.SUCCESS:
                        location_done = true;
                        Log.d("ShareFragment","location handler prompt_for_wifi");
                        prompt_for_wifi();
                        break;
                    case Utilities.FAILURE:
                        if(location_done==false)
                        {
                            text_description.setText(getResources().getString(R.string.share_location_failure));
                            spinner.setVisibility(View.INVISIBLE);
                            verify_button.setVisibility(View.INVISIBLE);
                            password_field.setVisibility(View.INVISIBLE);

                            Analytics.with(context).track("Could not detect user's location due to no Google Play installed");
                        }
                        break;
                    case Utilities.WAITING:
                        text_description.setText(getResources().getString(R.string.verifying_location_connection));
                        verify_button.setVisibility(View.INVISIBLE);
                        password_field.setVisibility(View.INVISIBLE);
                        security_button.setVisibility(View.INVISIBLE);
                        break;
                    case Utilities.OUTSIDE:
                        text_description.setText(getResources().getString(R.string.share_located_outside));
                        location_done = true;
                        spinner.setVisibility(View.INVISIBLE);
                        verify_button.setVisibility(View.INVISIBLE);
                        password_field.setVisibility(View.INVISIBLE);
                        security_button.setVisibility(View.INVISIBLE);

                        if(current_location!=null)
                        {
                            Analytics.with(context).track("User is located outside Mumbai", new Properties()
                                    .putValue("latitude", current_location.getLatitude())
                                    .putValue("longitude", current_location.getLongitude()));
                        }
                        else
                            Analytics.with(context).track("User is located outside Mumbai");

                        break;
                    case Utilities.TIMEOUT:
                        text_description.setText(getResources().getString(R.string.share_could_not_detect_location));
                        location_done = true;
                        spinner.setVisibility(View.INVISIBLE);
                        verify_button.setVisibility(View.INVISIBLE);
                        password_field.setVisibility(View.INVISIBLE);
                        security_button.setVisibility(View.INVISIBLE);

                        Boolean gps_on = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        Boolean network_on = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                        Analytics.with(context).track("Could not detect user's location due to timeout", new Properties()
                                .putValue("GPS_PROVIDER_ON", gps_on)
                                .putValue("NETWORK_PROVIDER_ON", network_on));
                        break;
                    case Utilities.NO_INTERNET:
                        //superficially a failure in detecting location, but the root cause was lack of Internet connection to look up their location
                        text_description.setText(getResources().getString(R.string.share_could_not_connect_internet));
                        location_done = true;
                        spinner.setVisibility(View.INVISIBLE);
                        verify_button.setVisibility(View.INVISIBLE);
                        password_field.setVisibility(View.INVISIBLE);
                        security_button.setVisibility(View.INVISIBLE);

                        Analytics.with(context).track("Could not detect user's location due to no Internet");
                        break;
                }
            }
        }
    };

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        if (BuildConfig.DEBUG)
        {
            Log.d("location", "connection failed!");
            location_handler.sendEmptyMessage(Utilities.FAILURE);
        }
        if (connectionResult.hasResolution())
        {
            Log.d("location", "connection failed!");
            location_handler.sendEmptyMessage(Utilities.FAILURE);
        }
        else
        {
            //If no resolution is available, display a message to the user with the error
            Log.d("location", "Connection failed to Google Play Services: " + connectionResult.getErrorCode());
            location_handler.sendEmptyMessage(Utilities.FAILURE);
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Location location = locationClient.getLastLocation();
        Log.d("location", "Connected to Google Play Services.");
        if (location == null)
        {
            locationClient.requestLocationUpdates(locationRequest, this);

            ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);//1 thread
            threadPoolExecutor.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    if (location_update_received==false)
                    {
                        try
                        {
                            utils.logMessage("been 10 seconds - still no location update received","ShareFragment");
                            //we can use an IP-to-location database to lookup which city we're in, but that's not accurate enough for the coverage map
                            //when giving directions
                            //we check to see if at least the network provider is enabled
                            //if so, and we're connected to the Internet, we should be able to get the location
                            //if not, we timeout so that they're prompted to allow location in their Android settings
                            Boolean network_on = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                            if(network_on==true)
                            {
                                int response_code = utils.verifyInternetConnectivity(wifi, context, "Verifying Internet connectivity",false);
                                if(response_code!=200)
                                {
                                    //they don't have an Internet connection
                                    location_handler.sendEmptyMessage(Utilities.NO_INTERNET);
                                }
                                else
                                {
                                    //this shouldn't happen
                                    location_handler.sendEmptyMessage(Utilities.TIMEOUT);
                                    utils.logMessage("could not get location by IP - NETWORK_PROVIDER IS enabled, and have Internet connection","ShareFragment");
                                }
                            }
                            else if(network_on==false)
                            {
                                location_handler.sendEmptyMessage(Utilities.TIMEOUT);
                                utils.logMessage("could not get location by IP - NETWORK_PROVIDER not enabled","ShareFragment");
                            }
                        }
                        catch (Exception e)
                        {
                            location_handler.sendEmptyMessage(Utilities.TIMEOUT);
                            utils.logException(e);
                        }
                    }
                }
            }, 10, TimeUnit.SECONDS);//run it once, after 10 seconds
        }
        else
        {
            Log.d("location", "Location:" + location.getLatitude() + "," + location.getLongitude());
            current_location = location;
            if (utils.verifyInsideCity(location.getLatitude(), location.getLongitude())==true)
                location_handler.sendEmptyMessage(Utilities.SUCCESS);
            else
                location_handler.sendEmptyMessage(Utilities.OUTSIDE);
        }
    }

    @Override
    public void onDisconnected()
    {
        Log.d("location", "Disconnected from Google Play Services.");
        location_handler.sendEmptyMessage(Utilities.FAILURE);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        Log.d("location", "location changed");
        Log.d("location", "Location:" + location.getLatitude() + "," + location.getLongitude());
        current_location = location;
        location_handler.sendEmptyMessage(Utilities.SUCCESS);
        location_update_received = true;
    }
}
