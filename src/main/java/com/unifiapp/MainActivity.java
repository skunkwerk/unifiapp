package com.unifiapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.helpshift.Helpshift;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.adapter.NavDrawerListAdapter;
import com.unifiapp.controller.mainactivity.Callbacks;
import com.unifiapp.controller.mainactivity.EventHandlers;
import com.unifiapp.events.Events;
import com.unifiapp.events.Events.SendRegistrationIdEvent;
import com.unifiapp.model.APIClient.API;
import com.unifiapp.model.APIFactory;
import com.unifiapp.model.NavDrawerItem;
import com.unifiapp.service.ConnectToWifi;
import com.unifiapp.service.Statistics;
import com.unifiapp.utils.Utilities;
import com.unifiapp.utils.Utilities.GetCoarseLocation;
import com.unifiapp.view.CoverageMapFragment;
import com.unifiapp.view.HomeFragment;
import com.unifiapp.view.LeaderboardFragment;
import com.unifiapp.view.PreShareFragment;
import com.unifiapp.view.SettingsFragment;
import com.unifiapp.view.ShareFragment;
import com.unifiapp.vpn.logic.CharonVpnService;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import io.branch.referral.Branch;
import io.fabric.sdk.android.Fabric;
import io.realm.Realm;

public class MainActivity extends ActionBarActivity
{
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	// nav drawer title
	private CharSequence mDrawerTitle;

	// used to store app title
	private CharSequence mTitle;

	// slide menu items
	private String[] navMenuTitles;
	private TypedArray navMenuIcons;

	private ArrayList<NavDrawerItem> navDrawerItems;
	private NavDrawerListAdapter adapter;
	
	private WifiManager wifi;
	private ScheduledThreadPoolExecutor threadPoolExecutor;
    private Utilities utils;
    private EventHandlers eventHandlers;
    private Callbacks callbacks;
    public Realm realm;
    private GetCoarseLocation coarseLocation;
    public Bundle mProfileInfo;
    private ConnectivityManager connManager;

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    String SENDER_ID = "885155265907";

    GoogleCloudMessaging gcm;
    String regid;

    private API api;
    public static boolean active = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        Log.d("MainActivity","starting");
        Helpshift.install(getApplication(),
                "63be1736c28cb3d977c8837519879fed", // API Key
                "unifiapp.helpshift.com", // Domain Name
                "unifiapp_platform_20140829173459813-cc62e8c3c6c0d0c"); // App ID
		setContentView(com.unifiapp.R.layout.activity_main);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPrefs.edit();

        /*editor.putBoolean("mainactivity_active",true); //less issues than using a static variable.  used in ConnectToWiFi
        editor.commit();*/

        // Check status of Google Play Services
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        final int RQS_GooglePlayServices = 1;
        // Check Google Play Service Available
        try
        {
            if (status != ConnectionResult.SUCCESS)
            {
                GooglePlayServicesUtil.getErrorDialog(status, this, RQS_GooglePlayServices).show();
            }
        }
        catch (Exception e)
        {
            Log.e("Error: GooglePlayServiceUtil: ", "");
            Crashlytics.log(Log.ERROR, "MainActivity", "user doesn't have Google Play Services installed");
            Crashlytics.logException(e);
            Analytics.with(getApplicationContext()).track("user doesn't have Google Play Services installed");
        }

        api = new APIFactory().getAPI();//be sure to do this before the call to SendRegistrationIdEvent, as it uses the API
        utils = new Utilities(getApplicationContext());
        eventHandlers = new EventHandlers(sharedPrefs, getApplicationContext(), api, utils, this, getSupportFragmentManager(), this);
        callbacks = new Callbacks(getApplicationContext(), utils);
        coarseLocation = new GetCoarseLocation(sharedPrefs, getApplicationContext());
        active = true;

        try
        {
            utils.setupDatabase("MainActivity");
            realm = Realm.getInstance(getApplicationContext());//may crash if a database migration is needed
        }
        catch (Exception e)
        {
            utils.logException(e);
        }

        if (utils.checkPlayServices(this))
        {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = utils.getRegistrationId(this.getApplicationContext());

            if (regid==null || regid.isEmpty())
            {
                registerInBackground();
            }
            else
            {
                SendRegistrationIdEvent sendRegistrationIdEvent = new SendRegistrationIdEvent();
                EventBus.getDefault().post(sendRegistrationIdEvent);
            }
        }
        else
        {
            Log.i("gcm", "No valid Google Play Services APK found.");
        }

        utils.new LoadCertificatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        com.facebook.AppEventsLogger.activateApp(getApplicationContext(), "830154490350849");

        try
        {
            Branch branch = Branch.getInstance(getApplicationContext());
            JSONObject sessionParams = branch.getFirstReferringParams();
            if (sharedPrefs.getBoolean("processed_referral_install",false)==false && sessionParams!=null && sessionParams.has("referring_customer_id"))
            {
                Analytics.with(getApplicationContext()).track("Customer installed app from referral", new Properties().putValue("deep_link", sessionParams.toString()));
                api.notify_customer_of_referral_install(sessionParams.getInt("referring_customer_id"), sharedPrefs.getString("first_name", ""), sharedPrefs.getInt("customer_id", 0), callbacks.referral_callback);
                editor.putBoolean("processed_referral_install", true);
                editor.commit();
            }
            branch.setIdentity(String.valueOf(sharedPrefs.getInt("customer_id", 0)));
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "MainActivity", "process install referral");
            Analytics.with(getApplicationContext()).track("Couldn't process install referral");
            Crashlytics.logException(e);
        }

        mTitle = mDrawerTitle = getTitle();
        getSupportActionBar().setBackgroundDrawable(null);//hide the thin colored line below the actionbar (as our walkthrough uses full-page solid colors that clash)

		// load slide menu items
		navMenuTitles = getResources().getStringArray(com.unifiapp.R.array.nav_drawer_items);

		// nav drawer icons from resources
		navMenuIcons = getResources().obtainTypedArray(com.unifiapp.R.array.nav_drawer_icons);

		mDrawerLayout = (DrawerLayout) findViewById(com.unifiapp.R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(com.unifiapp.R.id.list_slidermenu);

		navDrawerItems = new ArrayList<NavDrawerItem>();

		// adding nav drawer items to array
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));//, true, "22"));
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[4], navMenuIcons.getResourceId(4, -1)));
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[5], navMenuIcons.getResourceId(5, -1)));//, true, "50+"));
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[6], navMenuIcons.getResourceId(6, -1)));//, true, "50+"));

		// Recycle the typed array
		navMenuIcons.recycle();

		mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

		// setting the nav drawer list adapter
		adapter = new NavDrawerListAdapter(getApplicationContext(),navDrawerItems);
		mDrawerList.setAdapter(adapter);

		// enabling action bar app icon and behaving it as toggle button
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				com.unifiapp.R.drawable.ic_drawer, //nav menu toggle icon
				com.unifiapp.R.string.app_name, // nav drawer open - description for accessibility
				com.unifiapp.R.string.app_name // nav drawer close - description for accessibility
		) 
		{
			public void onDrawerClosed(View view)
			{
				getSupportActionBar().setTitle(mTitle);
				// calling onPrepareOptionsMenu() to show action bar icons
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView)
			{
				getSupportActionBar().setTitle(mDrawerTitle);
				// calling onPrepareOptionsMenu() to hide action bar icons
				invalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_HOME_AS_UP); // what's mainly important here is DISPLAY_SHOW_CUSTOM. the rest is optional*/
        actionBar.setDisplayShowTitleEnabled(true);
        //Drawable d= getResources().getDrawable(R.drawable.actionbarbackground);
        //actionBar.setBackgroundDrawable(d);

        Boolean seen_share_wifi = sharedPrefs.getBoolean("seen_share_wifi", false);
        if (savedInstanceState == null && seen_share_wifi==true)
		{
			// on first time display view for first nav item
			displayView(0, null);
		}
        else if(seen_share_wifi==false)
        {
            displayView(2, null);
        }

        if(sharedPrefs.getString("anonymous_customer_id_hash","").equals(""))
        {
            String hashed = utils.hashString(String.valueOf(sharedPrefs.getInt("customer_id",0)));
            editor.putString("anonymous_customer_id_hash", hashed);
            editor.commit();
        }
		
		wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        //for getting changes in WiFi connectivity (ie when we're out of range of the WiFi network we connected to)
        this.registerReceiver(connectivityChangeReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        //could also do WifiManager.SUPPLICANT_STATE_CHANGED_ACTION or ConnectivityManager.CONNECTIVITY_ACTION

        //the receiver can be called even if we're already connected to WiFi, and we never call startScan(),
        //as the system or other apps may scan

        threadPoolExecutor = new ScheduledThreadPoolExecutor(1);//1 thread
        threadPoolExecutor.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                Log.d("in WiFi", "starting WiFi scan");
                wifi.startScan();
                //we don't care if the previous call to startScan finished or not
            }
        }, 0, 1, TimeUnit.MINUTES);//run it every minute, after the end of the previous call to run()

        //start the service to calculate statistics & connect to WiFi networks
        //this is necessary if this is our first time running the app
        //otherwise, it's setup to start on boot
        if(utils.isMyServiceRunning(Statistics.class)==false)
        {
            try
            {
                Intent statsIntent = new Intent(getApplicationContext(), Statistics.class);
                startService(statsIntent);
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
        if(utils.isMyServiceRunning(ConnectToWifi.class)==false)
        {
            try
            {
                Intent connectIntent = new Intent(getApplicationContext(), ConnectToWifi.class);
                startService(connectIntent);
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
	}
	
	@Override
	protected void onResume()
	{
	    super.onResume();
        try
        {
            eventHandlers.setVisible(true);
            // Check device for Play Services APK
            utils.checkPlayServices(this);

            if(sharedPrefs.getInt("customer_id", 0)==0 || sharedPrefs.getInt("customer_id", 0)==-1)
            {
                //the customer was not registered properly with the servers, likely due to an Internet connectivity issue, so try again
                Crashlytics.log(Log.DEBUG, "MainActivity", "customer_id is 0 or -1, attempting to re-register");
                API api = new APIFactory().getAPI();
                api.get_or_create_customer(sharedPrefs.getString("first_name",""), sharedPrefs.getString("last_name",""), sharedPrefs.getString("email_address",""), "facebook", sharedPrefs.getString("facebook_user_id",""), callbacks.customer_registration_callback);
            }
            else
            {
                utils.configureCustomerIdentification(getApplicationContext(), sharedPrefs);
            }
        }
        catch (Exception e)
        {
            Log.d("onResume","error!");
            Crashlytics.log(Log.ERROR, "MainActivity", "error onResume");
            Crashlytics.logException(e);
        }
	}

    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        Log.d("MainActivity","in onPostResume");
        Intent receivedIntent = getIntent();
        String receivedAction = receivedIntent.getAction();//null if started from the menu
        /*List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        for( WifiConfiguration i : list )
        {
            //on pre-4.2 devices i.SSID will be in quotes and info.getSSID will NOT be in quotes!
            String i_ssid = i.SSID;

            if (i.status == WifiConfiguration.Status.CURRENT)
            {
                utils.LogWiFiConfiguration(i, this);
            }
        }*/

        try
        {
            String receivedText;
            if(receivedAction!=null && receivedAction.equals("display_screen"))
            {
                Analytics.with(getApplicationContext()).track("Clicked on New Notification");
                Log.d("receivedAction","got here");
                //app has NOT been launched directly
                receivedText = receivedIntent.getStringExtra("screen");
                if(receivedText!=null && receivedText.equals("chat"))
                {
                    utils.configureHelpshift(getResources().getString(R.string.helpshift_default_message), sharedPrefs);
                    Analytics.with(getApplicationContext()).screen("Chat with Support","");
                        /*
                        TODO: for some reason, this leads to 3 requests to helpRequestedEvent
                        HelpRequestedEvent helpRequestedEvent = new HelpRequestedEvent();
                        EventBus.getDefault().post(helpRequestedEvent);*/
                    Helpshift.showConversation(MainActivity.this, utils.getHelpshiftConfig());
                }
                else if(receivedText!=null && receivedText.equals("coverage"))
                {
                    displayView(1, null);
                }
                else if(receivedText!=null && receivedText.equals("preshare"))
                {
                    displayView(2, null);
                }
                else if(receivedText!=null && receivedText.equals("update_password"))
                {
                    Bundle args = new Bundle();
                    args.putBoolean("update",true);
                    displayView(7, args);
                }
            }
            else if(receivedAction!=null && receivedAction.equals("display_dialog"))
            {
                receivedText = receivedIntent.getStringExtra("dialog");
                if(receivedText!=null && receivedText.equals("operator"))
                {
                    Boolean show_help_button = true;
                    String title = getResources().getString(R.string.dialog_select_operator_label);
                    String msg = getResources().getString(R.string.dialog_select_operator_text);
                    /*Causes "Can not perform this action after onSaveInstanceState" TODO: until figure out how to use Eventbus for this - onEventMainThread?
                    DisplayOperatorDialogEvent dialogEvent = new DisplayOperatorDialogEvent(title, msg, show_help_button);
                    EventBus.getDefault().post(dialogEvent);*/

                    eventHandlers.DisplayOperatorDialog();
                }
            }
            //else, just open the main screen (ie for read-only notifications)
            //clear the intent data to prevent this happening in an infinite loop
            receivedIntent.setAction("");
            receivedIntent.removeExtra("screen");
        }
        catch(Exception e)
        {
            Log.e("Error: parse the notification screen intent: ", "" + e);
            Crashlytics.log(Log.ERROR, "MainActivity", "Couldn't parse the notification screen intent");
            Crashlytics.logException(e);
        }
        //check to see if there's a newer version that the user hasn't upgraded to yet
        api.latest_version(callbacks.version_callback);
    }
	
	@Override
	protected void onPause()
	{
	    super.onPause();
        eventHandlers.setVisible(false);
        Log.d("onPause", "");
	}

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d("MainActivity","onDestroy");
        editor.putBoolean("seen_upgrade_dialog", false);//set to false, so the next time they start the app, they'll be prompted to update
        //editor.putBoolean("mainactivity_active",false); //less issues than using a static variable.  used in ConnectToWiFi
        active = false;
        editor.commit();
        eventHandlers.unregister();

        try
        {
            threadPoolExecutor.shutdownNow();

            if (coarseLocation.locationClient.isConnected())
            {
                coarseLocation.locationClient.removeLocationUpdates(coarseLocation);
            }
            coarseLocation.locationClient.disconnect();

            if (connectivityChangeReceiver!=null)
            {
                unregisterReceiver(connectivityChangeReceiver);
                connectivityChangeReceiver = null;
            }
        }
        catch (Exception e)
        {
            Log.d("onPause", "error!");
            Crashlytics.log(Log.ERROR, "MainActivity", "error onPause");
            Crashlytics.logException(e);
        }
        finally
        {
            if(realm!=null)
            {
                realm.close();
            }
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground()
    {
        Log.d("gcm","registerInBackground");
        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                String msg = "";
                try
                {
                    if (gcm == null)
                    {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    //sendRegistrationIdToBackend();
                    //can't do Helpshift.registerDeviceToken(getApplicationContext(), regid) here otherwise get the error:
                    //Can't create handler inside thread that has not called Looper.prepare()
                    //ie registerDeviceToken creates a handler, which can't be run from inside an AsyncTask - has to be in main UI thread

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    utils.storeRegistrationId(getApplicationContext(), regid);

                    SendRegistrationIdEvent sendRegistrationIdEvent = new SendRegistrationIdEvent();
                    EventBus.getDefault().post(sendRegistrationIdEvent);
                }
                catch (IOException ex)
                {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                catch (Exception e)
                {
                    utils.logException(e);
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg)
            {
                Log.d("gcm",msg + "\n");
            }
        }.execute(null, null, null);
    }

    /**
	 * Slide menu item click listener
	 * */
	private class SlideMenuClickListener implements
			ListView.OnItemClickListener 
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id)
        {
			// display view for selected nav drawer item
			displayView(position, null);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		//getMenuInflater().inflate(com.unifiapp.R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        return true;
	}

	/**
	 * Called when invalidateOptionsMenu() is triggered
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// if nav drawer is opened, hide the action items
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		//TODO:menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Diplaying fragment view for selected nav drawer list item
	 * */
	public void displayView(int position, Bundle arguments)
	{
		// update the main content by replacing fragments
		Fragment fragment = null;
        String tag = "";
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);//the default
		switch (position)
        {
		case 0:
			fragment = new HomeFragment();
            tag = "Home";
			break;
		case 1:
			fragment = new CoverageMapFragment();
            tag = "CoverageMap";
			break;
		case 2:
			fragment = new PreShareFragment();
            tag = "PreShare";
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);//disable the drawer swipe (as it interferes with the viewpager swipe)
			break;
        case 3:
            fragment = new LeaderboardFragment();
            tag = "Leaderboard";
            break;
		case 4:
            fragment = new SettingsFragment();
            tag = "Settings";
            break;
		case 5:
            Analytics.with(getApplicationContext()).screen("F.A.Q.","");
            Helpshift.showFAQs(MainActivity.this, utils.getHelpshiftConfig());
            break;
		case 6:
            Analytics.with(getApplicationContext()).screen("Chat with Support","");
            Helpshift.showConversation(MainActivity.this, utils.getHelpshiftConfig());
            break;
        case 7:
            fragment = new ShareFragment();
            tag = "Share";
            break;

		default:
			break;
		}

		if (fragment != null)
		{
            if(arguments!=null)
            {
                fragment.setArguments(arguments);
            }
            FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.replace(com.unifiapp.R.id.frame_container, fragment, tag).commit();

            if(position<7)
            {
                // update selected item and title, then close the drawer
                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);
                if(position==3)
                    setTitle(" > Mumbai " + navMenuTitles[position]);//TODO: mumbai
                else
                    setTitle(" > " + navMenuTitles[position]);
                mDrawerLayout.closeDrawer(mDrawerList);
            }
		} 
		else
		{
			// error in creating fragment
			Log.e("MainActivity", "Error in creating fragment");
		}
	}

	@Override
	public void setTitle(CharSequence title)
	{
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //TODO: adjust this to call super for facebook handling with different request number
        Log.d("MainActivity","onActivityResult, request is:" + String.valueOf(requestCode));
        final int PREPARE_VPN_SERVICE = 0;
        switch (requestCode)
        {
            case PREPARE_VPN_SERVICE:
                if (resultCode == -1 && mProfileInfo != null)
                {
                    try
                    {
                        Analytics.with(this).track("Granted permission to start VPN");
                        Intent intent = new Intent(this, CharonVpnService.class);
                        intent.putExtras(mProfileInfo);
                        startService(intent);
                        //to update the status on the home screen
                        Events.ConnectivityChangedEvent connectivityChangedEvent = new Events.ConnectivityChangedEvent();
                        EventBus.getDefault().post(connectivityChangedEvent);
                    }
                    catch (Exception e)
                    {
                        utils.logException(e);
                    }
                }
                else
                {
                    //they didn't grant us access to start the VPN, so disconnect them from WiFi
                    Analytics.with(this).track("Not granted permission to start VPN");
                    Events.VPNDisconnectedEvent vpnDisconnectedEvent = new Events.VPNDisconnectedEvent();
                    EventBus.getDefault().post(vpnDisconnectedEvent);
                    Log.d("Home","didn't grant vpn access");
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);//will propagate to child fragments
        }
    }

    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d("MainActivity","ConnectivityChangeReceiver onReceive");
            //if we're (no longer connected to WiFi or connected to a different WiFi network), and we were connected to a community hotspot,
            //turn off the VPN and delete the WiFiConfiguration from the phone
            try
            {
                if(connManager!=null)
                {
                    NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                    Boolean connectedToHotspot = sharedPrefs.getBoolean("connected_to_community_hotspot",false);
                    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String community_hotspot_mac_address = sharedPrefs.getString("connected_to_community_hotspot_mac_address","");
                    String community_hotspot_ssid = sharedPrefs.getString("connected_to_community_hotspot_ssid","");
                    //intent.getParcelableExtra(wifi.EXTRA_NEW_STATE).toString().equals(SupplicantState.SCANNING))
                    if ((activeNetwork!=null && activeNetwork.getType() != ConnectivityManager.TYPE_WIFI || (wifiInfo!=null && wifiInfo.getBSSID()!=null && wifiInfo.getBSSID().equals(community_hotspot_mac_address)==false)) && connectedToHotspot)
                    {
                        Events.WiFiDisconnectedEvent wiFiDisconnectedEvent = new Events.WiFiDisconnectedEvent();
                        EventBus.getDefault().post(wiFiDisconnectedEvent);

                        long now = System.currentTimeMillis()/1000; // seconds since epoch (UTC)
                        long minutes_of_usage = now - sharedPrefs.getLong("currently_connected_community_hotspot_start_time",0)/60;

                        float all_received_bytes = TrafficStats.getTotalRxBytes();
                        float all_transmitted_bytes = TrafficStats.getTotalTxBytes();
                        float data_downloaded = 0;
                        float data_uploaded = 0;
                        if(all_received_bytes!=TrafficStats.UNSUPPORTED && all_transmitted_bytes!=TrafficStats.UNSUPPORTED)
                        {
                            float all_received_bytes_delta = all_received_bytes - sharedPrefs.getFloat("currently_connected_community_hotspot_received_bytes",0.0f);
                            float all_transmitted_bytes_delta = all_transmitted_bytes - sharedPrefs.getFloat("currently_connected_community_hotspot_transmitted_bytes",0.0f);
                            //hopefully we get these before the stats get reset due to the change in connectivity
                            if(all_received_bytes_delta > 0)
                                data_downloaded = all_received_bytes_delta;
                            if(all_transmitted_bytes_delta >0)
                                data_uploaded = all_transmitted_bytes_delta;
                        }

                        Analytics.with(getApplicationContext()).track("Disconnected from community hotspot", new Properties()
                                .putValue("mac_address", community_hotspot_mac_address)
                                .putValue("ssid", community_hotspot_ssid)
                                .putValue("minutes_of_usage",minutes_of_usage)
                                .putValue("data_downloaded",data_downloaded)
                                .putValue("data_uploaded",data_uploaded));
                        editor.putLong("currently_connected_community_hotspot_start_time",0);
                        editor.putLong("currently_connected_community_hotspot_received_bytes",0);
                        editor.putLong("currently_connected_community_hotspot_transmitted_bytes",0);
                        editor.commit();
                    }
                }
                Events.ConnectivityChangedEvent connectivityChangedEvent = new Events.ConnectivityChangedEvent();
                EventBus.getDefault().post(connectivityChangedEvent);
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
    };
}