package com.unifiapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;
import android.util.Log;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.pascalwelsch.holocircularprogressbar.HoloCircularProgressBar;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.R;
import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIFactory;
import com.unifiapp.utils.Utilities;
import com.unifiapp.utils.Utilities.GetAccurateLocation;

import java.util.Date;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class HomeFragment extends Fragment implements ViewFactory
{
    private Context context;
    private APIClient.API api;
    private SharedPreferences sharedPrefs;
    TextView vpnStatus;
    TextView accessText;
    TextView thanksReceivedText;
    TextView connectionsMadeText;
    TextSwitcher titleText;
    Utilities utils;
    Boolean isVisible = false;
    GetAccurateLocation accurateLocation;
    TextView nearestNetworkText;
    BootstrapButton nearestNetworkDirectionsButton;
    Location currentLocation;
    Location previousLocation;
    WifiManager wifi;
    ConnectivityManager connManager;
    HoloCircularProgressBar circle;
    View circleBackground;
    long lastQueriedNearesetNetworkTime = 0;
    String[] titles;
    private int counter=0;

	public HomeFragment(){}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
	{
        context = getActivity();
        View rootView = inflater.inflate(R.layout.home_fragment, container, false);

        vpnStatus = (TextView) rootView.findViewById(R.id.vpn_status);
        titleText = (TextSwitcher) rootView.findViewById(R.id.home_title_text);
        accessText = (TextView) rootView.findViewById(R.id.access_type);
        thanksReceivedText = (TextView) rootView.findViewById(R.id.thanks_received_text);
        connectionsMadeText = (TextView) rootView.findViewById(R.id.connections_made_text);
        circle =  (HoloCircularProgressBar) rootView.findViewById(R.id.circle);
        circleBackground = rootView.findViewById(R.id.circle_background);
        nearestNetworkText = (TextView) rootView.findViewById(R.id.nearest_network_text);
        nearestNetworkDirectionsButton = (BootstrapButton) rootView.findViewById(R.id.nearest_network_directions_button);
        nearestNetworkDirectionsButton.setVisibility(View.INVISIBLE);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        LayoutParams params = circle.getLayoutParams();
        double limit = (size.x < 0.6*size.y) ? size.x : 0.6*size.y;
        params.width = (int) (limit * 0.8);
        params.height = (int) (limit * 0.8);
        circle.setLayoutParams(params);
        LayoutParams backgroundParams = circleBackground.getLayoutParams();
        backgroundParams.width = (int) (limit * 0.8);
        backgroundParams.height = (int) (limit * 0.8);
        circleBackground.setLayoutParams(params);
        circle.setMarkerEnabled(false);
        circle.setProgress(360);

        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //spinner = (ProgressBar) rootView.findViewById(R.id.spinner);

        /*statusCircle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Events.StartVPNEvent vpnEvent = new Events.StartVPNEvent();
                EventBus.getDefault().post(vpnEvent);
            }
        });*/

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        titles = new String[]{getResources().getString(R.string.home_rotating_title_1),getResources().getString(R.string.home_rotating_title_2)};
        titles[1] = String.valueOf(sharedPrefs.getInt("router_count",5176)) + titles[1];
        titleText.setFactory(this);
        Animation in = AnimationUtils.loadAnimation(context,android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(context,android.R.anim.fade_out);
        titleText.setInAnimation(in);
        titleText.setOutAnimation(out);

        api = new APIFactory().getAPI();
        utils = new Utilities(this.context);
        EventBus.getDefault().register(this);
        api.account_access(sharedPrefs.getInt("customer_id", 0), accountCallback);
        api.connection_stats(sharedPrefs.getInt("customer_id", 0),statsCallback);
        getNearestNetwork();

        Handler animationHandler = new Handler();
        animationHandler.postDelayed(animator, 3000);//3 second delay
        animationHandler.postDelayed(animator, 5000);//5 second delay

        return rootView;
    }

    public void onEvent(Events.ConnectivityChangedEvent event)
    {
        setStatusText();
        getNearestNetwork();
    }

    public void onEvent(Events.InRangeCommunityWiFiEvent event)
    {
        if(isAdded())
        {
            vpnStatus.setText(getResources().getString(R.string.home_in_range_attempting_connect));
            loadStatusImage(R.color.yellow);
        }
    }

    public void onEvent(Events.OutOfRangeCommunityWiFiEvent event)
    {
        setStatusText();
        getNearestNetwork();
    }

    public void onEvent(Events.LocationUpdateEvent event)
    {
        updateLocation(event.getLocation(), false);
    }

    public void onEvent(Events.LocationTimedOutEvent event)
    {
        updateLocation(null, true);
    }

    public void updateLocation(Location location, Boolean timedOut)
    {
        if(timedOut==true)
        {
            if(isVisible)
            {
                Events.DisplayOKOrHelpDialogEvent displayLocationTimedOutEvent = new Events.DisplayOKOrHelpDialogEvent(getResources().getString(R.string.home_location_timeout_title),
                        getResources().getString(R.string.home_location_timeout_text), true);
                EventBus.getDefault().post(displayLocationTimedOutEvent);
            }
            //if not visible, don't bother - as will try again the next time this fragment is resumed
        }
        else
        {
            //the status may have changed since we issued this request, so check again
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            Boolean wifiOn = wifi.isWifiEnabled();
            if(wifiOn && wifiInfo!=null && wifiInfo.isConnectedOrConnecting()==false)
            {
                currentLocation = location;
                float distance = 11;//high enough to always call nearest_network in case we can't compute a distance
                if(currentLocation!=null && previousLocation!=null)
                {
                    distance = currentLocation.distanceTo(previousLocation);
                    previousLocation = currentLocation;//store for next time
                }

                //location may update every minute or so, but it's no use to keep calling for the nearest network if they
                //haven't moved more than 10 meters
                if(distance >= 10)
                {
                    long seconds = sharedPrefs.getLong("last_mac_bloom_filter_sync_date",0);
                    //we only want the nearest router that we have in our bloom filter
                    if(seconds==0)
                    {
                        api.nearest_network(location.getLatitude(), location.getLongitude(), nearestNetworkCallback);
                    }
                    else
                    {
                        Date date = new Date(seconds * 1000);
                        java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                        String sync_date = simpleDateFormat.format(date);
                        api.nearest_network_with_sync_date(location.getLatitude(), location.getLongitude(), sync_date, nearestNetworkCallback);
                    }
                }
                else
                {
                    Log.d("HomeFragment","distance hasn't changed more than 10 meters, so not bothering to get nearest network in updateLocation again");
                }
                //TODO: remove duplicated code
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putFloat("coarse_latitude",(float) location.getLatitude());
                editor.putFloat("coarse_longitude",(float) location.getLongitude());
                editor.putFloat("coarse_altitude",(float) location.getAltitude());
                editor.commit();
            }
            else
            {
                nearestNetworkText.setText("");
                //else, we don't display directions
            }
        }
    }

    public void getNearestNetwork()
    {
        //coarse location from MainActivity is not going to be up-to-date, as it's on a 1 hour update cycle
        //nor will it be accurate enough, as only city-level accuracy

        //this may be called 8 times in quick succession while we're changing from 3G to WiFi
        //so throttle it
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
        if(wifiInfo.isConnectedOrConnecting()==false && (now - lastQueriedNearesetNetworkTime > 30) && connManager.getActiveNetworkInfo()!=null)
        {
            //check for ActiveNetwork is there, otherwise we don't have a data connection and the API lookups fail, stays stuck in 'waiting for location' forever
            //but even with this check it fails, so we just use a handler to issue the call after 15 seconds
            Handler nearestNetworkHandler = new Handler();
            nearestNetworkHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    setStatusText();
                    accurateLocation = new Utilities.GetAccurateLocation(sharedPrefs, context);
                    if(isAdded())
                    {
                        nearestNetworkText.setText(getResources().getString(R.string.home_waiting_for_location));
                    }
                    lastQueriedNearesetNetworkTime = now;
                }
            }, 10000);//10 second delay
        }
    }

    public void displayNearestNetwork(APIClient.NearestWiFiRouter router)
    {
        if(isAdded())
        {
            nearestNetworkText.setText(getResources().getString(R.string.nearest_wifi_prefix) + String.valueOf((int) router.distance) + getResources().getString(R.string.nearest_wifi_suffix));
        }
        final String ssid = router.ssid;
        final String mac_address = router.mac_address;
        final int distance = (int) router.distance;

        final Bundle bundle = new Bundle();
        bundle.putBoolean("get_directions", true);
        double[] currentLocationCoordinates = { currentLocation.getLatitude(),currentLocation.getLongitude() };
        float[] routerCoordinates = { router.latitude, router.longitude };

        bundle.putDoubleArray("currentLocationCoordinates",currentLocationCoordinates);
        bundle.putFloatArray("routerCoordinates",routerCoordinates);
        bundle.putString("mac_address", router.mac_address);
        bundle.putString("ssid", router.ssid);

        //display the directions button
        nearestNetworkDirectionsButton.setVisibility(View.VISIBLE);
        nearestNetworkDirectionsButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //take them to the coverage map page, and pass along their current location & the router's location
                Events.DisplayFragmentEvent displayFragmentEvent = new Events.DisplayFragmentEvent(1, bundle);
                EventBus.getDefault().post(displayFragmentEvent);
                long mac_seconds = sharedPrefs.getLong("last_mac_bloom_filter_sync_date",0);
                //we don't need to check the last coverage map sync date, as when the map is loaded it will check itself and reload if necessary with fresh data
                //unless we are in offline mode
                if(mac_seconds==0)
                {
                    //request sync of the MAC bloom filter
                    utils.logMessage("Requested Directions, but MAC bloom filter wasn't synced, requesting again","HomeFragment");
                    Events.SyncMacBloomFilterEvent syncMacBloomFilterEvent = new Events.SyncMacBloomFilterEvent();
                    EventBus.getDefault().post(syncMacBloomFilterEvent);
                }
                Date date = new Date(mac_seconds * 1000);
                java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String sync_date = simpleDateFormat.format(date);
                //send the data to analytics, so we know how often this is used, and what % of the time the user connects to a network they got directions for
                Analytics.with(context).track("Requested Directions to Nearest Hotspot",
                        new Properties()
                                .putValue("mac_address", mac_address)
                                .putValue("ssid", ssid)
                                .putValue("distance", distance)
                                .putValue("latitude", currentLocation.getLatitude())
                                .putValue("longitude", currentLocation.getLongitude())
                                .putValue("mac_list_sync_date", sync_date));
            }
        });
    }

    public void setStatusText()
    {
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Boolean wifiOn = wifi.isWifiEnabled();
        if(isAdded())
        {
            if(wifiOn && wifiInfo!=null && wifiInfo.isConnectedOrConnecting()==false)
            {
                vpnStatus.setText(getResources().getString(R.string.home_scanning_nearby_wifi));
                loadStatusImage(R.color.yellow);
            }
            else if(wifiOn && wifiInfo!=null && wifiInfo.isConnectedOrConnecting()==true)
            {
                Boolean connected_to_community_hotspot = sharedPrefs.getBoolean("connected_to_community_hotspot",false);
                if(connected_to_community_hotspot)
                {
                    vpnStatus.setText(getResources().getString(R.string.home_connected_community_hotspot));
                    loadStatusImage(R.color.emerald);
                    nearestNetworkText.setText("");
                    nearestNetworkDirectionsButton.setVisibility(View.INVISIBLE);
                }
                else
                {
                    vpnStatus.setText(getResources().getString(R.string.home_connected_own_hotspot));
                    loadStatusImage(R.color.emerald);
                    nearestNetworkText.setText("");
                    nearestNetworkDirectionsButton.setVisibility(View.INVISIBLE);
                }
            }
            else if(!wifiOn)
            {
                vpnStatus.setText(getResources().getString(R.string.home_turn_on_wifi));
                loadStatusImage(R.color.counter_text_bg);
            }
            else if(sharedPrefs.getLong("last_offline_network_access_sync_date",0)>86400 && connManager.getActiveNetworkInfo()==null)
            {
                //offline mode is now supported
                //if it's been more than 1 day since we synced, and you're currently not connected to 2G/3G
                vpnStatus.setText(getResources().getString(R.string.home_no_data_connection));
                loadStatusImage(R.color.counter_text_bg);
            }
            else if(utils.isDeviceRooted())
            {
                vpnStatus.setText(getResources().getString(R.string.home_device_rooted));
                loadStatusImage(R.color.counter_text_bg);
            }
        }
    }

    /**
     *
     * @param color
     */
    private void loadStatusImage(int color)
    {
        try
        {
            /*Fragment homeFragment = (Fragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentByTag("Home");
            if (homeFragment!= null && homeFragment.isVisible())*/ //otherwise we can get a 'IllegalStateException - fragment not attached to Activity' error
            circle.setProgressColor(color);
            circle.setProgressBackgroundColor(color);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    private Runnable animator = new Runnable()
    {
        @Override
        public void run()
        {
            updateText();
        }
    };

    public void updateText()
    {
        titleText.setText(String.valueOf(titles[counter]));
        counter += 1;
    }

    public View makeView()
    {
        TextView t = new TextView(getActivity());
        if(isAdded())
        {
            t.setTextColor(getResources().getColor(R.color.black));
        }
        t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        t.setTextSize(18);
        return t;
    }

    public Callback<APIClient.AccessType> accountCallback = new Callback<APIClient.AccessType>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "HomeFragment", "accountCallback");
        }

        @Override
        public void success(APIClient.AccessType result, Response response)
        {
            if(accessText!=null)
            {
                if(isAdded())
                {
                    accessText.setText(getResources().getString(R.string.home_access) + result.accessType);
                }
            }
        }
    };

    public Callback<APIClient.ConnectionStats> statsCallback = new Callback<APIClient.ConnectionStats>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "HomeFragment", "statusCallback");
        }

        @Override
        public void success(APIClient.ConnectionStats result, Response response)
        {
            if(thanksReceivedText!=null)
            {
                thanksReceivedText.setText(String.valueOf(result.thanks_received));
            }
            if(connectionsMadeText!=null)
            {
                connectionsMadeText.setText(String.valueOf(result.connections_made));
            }
            //minutes_used; - not currently displayed
        }
    };
	
	@Override
	public void onPause()
	{
		super.onPause();
        isVisible = false;
        if(accurateLocation!=null)
        {
            if (accurateLocation.locationClient.isConnected())
            {
                accurateLocation.locationClient.removeLocationUpdates(accurateLocation);
            }
            accurateLocation.locationClient.disconnect();
        }
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
        isVisible = true;
        setStatusText();
        Analytics.with(getActivity()).screen("Home","");
	}

    public Callback<APIClient.NearestWiFiRouter> nearestNetworkCallback = new Callback<APIClient.NearestWiFiRouter>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "HomeFragment", "nearestNetworkCallback");
        }

        @Override
        public void success(APIClient.NearestWiFiRouter result, Response response)
        {
            displayNearestNetwork(result);
        }
    };
}
