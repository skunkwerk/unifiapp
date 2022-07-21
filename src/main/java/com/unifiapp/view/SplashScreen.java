package com.unifiapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.provider.Settings.Secure;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.security.MessageDigest;
import android.content.pm.Signature;
import android.util.Base64;
import android.content.pm.PackageManager.NameNotFoundException;
import java.security.NoSuchAlgorithmException;

import com.crashlytics.android.Crashlytics;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.squareup.picasso.Picasso;
import com.unifiapp.R;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIClient.API;
import com.unifiapp.model.APIFactory;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.fabric.sdk.android.Fabric;
import retrofit.Callback;
import retrofit.RetrofitError;

public class SplashScreen extends FragmentActivity
{
    SharedPreferences sharedPref;
    private LoginButton loginBtn;
    private ProgressBar spinner;
    private UiLifecycleHelper uiHelper;
    private ImageView background;
    private static final List<String> PERMISSIONS = Arrays.asList("public_profile,email");
    public API api;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.splash_screen);

        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);

        loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
        loginBtn.setReadPermissions(PERMISSIONS);

        spinner = (ProgressBar) findViewById(R.id.waiting_spinner);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        background = (ImageView) findViewById(R.id.splash_screen_image);
        Picasso.with(getApplicationContext()).load(R.drawable.walkthrough_1).fit().into(background);//will resize the image to fit the ImageView

        api = new APIFactory().getAPI();

        Boolean seen_walkthrough = sharedPref.getBoolean(getString(R.string.seen_walkthrough), false);
        if (seen_walkthrough==false)
        {
            //start the walkthrough activity, as this is the first time the user has opened the app
            //show fb button and add callback onloggedin to finish
            //when they're done with the walkthrough activity, we set seen_walkthrough to true and start the MainActivity

            Analytics.with(getApplicationContext()).track("User downloaded app onto device");

            loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback()
            {
                @Override
                public void onUserInfoFetched(GraphUser user)
                {
                    if (user != null)
                    {
                        try
                        {
                            processUser(user);
                        }
                        catch (Exception e)
                        {
                            Crashlytics.log(Log.ERROR, "SplashScreen", "could not save new customer through API");
                            Crashlytics.logException(e);
                            Analytics.with(getApplicationContext()).track("on SplashScreen, could not save new customer through API");
                        }
                        //we can't set android:noHistory='true' in the manifest for this activity to ensure it will not be kept in the activity stack
                        //becuase facebook login starts a new activity, which would have no SplashScreen activity to return results to
                        //so we need to call finish on this activity after starting the MainActivity
                        //but because we may have an async REST call pending a response that we need to store (customer_id), we have to call finish from the callback
                    }
                    else
                    {
                        Log.d("fb","You are not logged");
                    }
                }
            });
        }
        else
        {
            //if not first time, hide the fb button and callback, just 1 second delay, then start the MainActivity
            loginBtn.setVisibility(View.INVISIBLE);
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    Intent intent = new Intent(getApplicationContext(),com.unifiapp.MainActivity.class);
                    startActivity(intent);
                    releaseMemory(background);
                    finish();
                }
            }, 1000);//about 1 second of black screen now
            //why even bother - just start the MainActivity already!  it won't have time to even flicker - have 2 second black screen now
            /*Intent i= new Intent(SplashScreen.this,com.unifiapp.MainActivity.class);
            startActivity(i);
            finish();*/
        }
    }

    /*@Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.main, container, false);
        LoginButton authButton = (LoginButton) view.findViewById(R.id.authButton);
        authButton.setFragment(this);

        return view;
    }*/

    public void processUser(GraphUser user)
    {
        try
        {
            loginBtn.setVisibility(View.INVISIBLE);
            spinner.setIndeterminate(true);
            spinner.setVisibility(View.VISIBLE);

            String email_address = "";
            if(user.getProperty("email")!=null)
            {
                //not every user has an email registered with facebook!
                email_address = user.getProperty("email").toString();
            }
            else
            {
                Log.d("SplashScreen","facebook returned null email for user");
                Crashlytics.log(Log.DEBUG, "SplashScreen", "facebook returned null email for user");
            }
            String first_name = (user.getFirstName()==null) ? "" : user.getFirstName();
            String last_name = (user.getLastName()==null) ? "" : user.getLastName();
            String user_id = (user.getId()==null) ? "" : user.getId();

            String android_id = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
            if(android_id!=null)
            {
                api.get_or_create_customer(first_name, last_name, email_address, "facebook", user_id, android_id, callback);
            }
            else
            {
                api.get_or_create_customer(first_name, last_name, email_address, "facebook", user_id, callback);
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.seen_walkthrough), true);
            editor.putString("first_name", first_name);
            editor.putString("last_name", last_name);
            editor.putString("email_address", email_address);
            editor.putString("facebook_user_id", user_id);
            editor.commit();

            Analytics.with(getApplicationContext()).track("User logged in via Facebook on SplashScreen");
        }
        catch (Exception e)
        {
            Log.d("SplashScreen", "error!");
            Crashlytics.log(Log.ERROR, "SplashScreen", "error in processUser");
            Crashlytics.logException(e);
            Analytics.with(getApplicationContext()).track("on SplashScreen, exception in processUser");
        }
    }

    Callback<APIClient.Customer> callback = new Callback<APIClient.Customer>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            spinner.setVisibility(View.INVISIBLE);
            Log.d("in REST", "failure!" + arg0);
            Crashlytics.log(Log.ERROR, "SplashScreen", "Couldn't register new user! - REST failure");
            Analytics.with(getApplicationContext()).track("Couldn't register new user! - REST failure", new Properties().putValue("RetroFitError",arg0.toString()));
            //start the main activity
            Intent intent = new Intent(getApplicationContext(),com.unifiapp.MainActivity.class);
            startActivity(intent);
            releaseMemory(background);
            finish();
        }

        @Override
        public void success(APIClient.Customer result, retrofit.client.Response arg1)
        {
            try
            {
                spinner.setVisibility(View.INVISIBLE);
                Log.d("in REST", "saved customer with id:" + result.id);
                if(result.id==-1)
                {
                    Crashlytics.log(Log.ERROR, "SplashScreen", "Couldn't register new user,  got -1 response from server!");
                    Analytics.with(getApplicationContext()).track("Couldn't register new user,  got -1 response from server!");
                }
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("customer_id", result.id);
                editor.putString("signup_date", result.signup_date);
                editor.commit();

                //start the main activity
                Intent intent = new Intent(SplashScreen.this,com.unifiapp.MainActivity.class);
                startActivity(intent);
                releaseMemory(background);
                finish();
            }
            catch (Exception e)
            {
                Crashlytics.log(Log.ERROR, "SplashScreen", "Couldn't register new user! - successful REST but Exception");
                Crashlytics.logException(e);
                Analytics.with(getApplicationContext()).track("Couldn't register new user! - successful REST but Exception", new Properties().putValue("traceback:",e.toString()));
            }
        }
    };

    @Override
    public void onStart()
    {
        super.onStart();

        Branch branch = Branch.getInstance(getApplicationContext(), "60830651875590146");
        branch.initSession(new BranchReferralInitListener()
        {
            @Override
            public void onInitFinished(JSONObject referringParams)
            {
                // params are the deep linked params associated with the link that the user clicked before showing up
                // may be null if so such referral
                // we do the actual API call in MainActivity, since we want to wait until we've got the customer registered
            }
        }, this.getIntent().getData());
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Branch.getInstance(getApplicationContext()).closeSession();
    }

    public void releaseMemory(ImageView image_view)
    {
        /*Drawable drawable = image_view.getDrawable();
        if (drawable instanceof BitmapDrawable)
        {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmap.recycle();
        }*/
        background = null;
    }

    private Session.StatusCallback statusCallback = new Session.StatusCallback()
    {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception)
        {
            if (state.isOpened())
            {
                Log.d("FacebookSampleActivity", "Facebook session opened");
            }
            else if (state.isClosed())
            {
                Log.d("FacebookSampleActivity", "Facebook session closed");
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();

        Analytics.with(getApplicationContext()).screen("Splash","");

        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null && (session.isOpened() || session.isClosed()))
        {
            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception)
    {
        /*if (session != null && session.isOpened())
        {
            //session.getAccessToken()
            Log.d("fb", "facebook session is open ");
            // make request to the /me API
            Request.executeMeRequestAsync(session, new Request.GraphUserCallback()
            {
                // callback after Graph API response with user object
                @Override
                public void onCompleted(GraphUser user, Response response)
                {
                    if (user != null)
                    {
                        processUser(user);
                    }
                }
            });
        }*/
    }

    @Override
    public void onPause()
    {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle savedState)
    {
        super.onSaveInstanceState(savedState);
        uiHelper.onSaveInstanceState(savedState);
    }

}