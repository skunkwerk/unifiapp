package com.unifiapp.controller.mainactivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.R;
import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient;
import com.unifiapp.utils.Utilities;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Callbacks
{
    Context context;
    Utilities utils;
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    public Callbacks(Context context, Utilities utils)
    {
        this.context = context;
        this.utils = utils;
    }

    Callback<APIClient.Response> arn_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "Callbacks", "arn_callback");
        }
        @Override
        public void success(APIClient.Response result, Response arg1)
        {
            try
            {
                if(result.status==true)
                    Log.d("arn callback","success");
                else
                {
                    Log.d("arn callback", "failure");
                    Crashlytics.log(Log.ERROR, "MainFragment", "ARN registration failure - status not true");
                    Analytics.with(context).track("ARN registration failure - status not true");
                }
            }
            catch(Exception e)
            {
                utils.logException(e);
            }
        }
    };

    public Callback<APIClient.Response> update_customer_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(APIClient.Response result, Response response)
        {
            Log.d("update_customer response",String.valueOf(result.status));
        }
    };

    public Callback<APIClient.Version> version_callback = new Callback<APIClient.Version>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(APIClient.Version result, Response response)
        {
            if (utils.getAppVersion(context)<result.version && sharedPrefs.getBoolean("seen_upgrade_dialog",false))
            {
                Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(context.getResources().getString(R.string.please_upgrade_title), context.getResources().getString(R.string.please_upgrade_text), false);
                EventBus.getDefault().post(dialogEvent);
                editor.putBoolean("seen_upgrade_dialog", true);
                editor.commit();
                Analytics.with(context).track("Prompted User to Upgrade App to Latest Version");
            }
        }
    };

    Callback<APIClient.Response> send_credit_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
            String title = context.getResources().getString(R.string.send_credit_problem_title);
            String msg = context.getResources().getString(R.string.send_credit_problem_text);
            Boolean show_help_button = true;

            Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(title, msg, show_help_button);
            EventBus.getDefault().post(dialogEvent);

            Analytics.with(context).track("Attempt to Send Prepaid Credit Failed");
        }

        @Override
        public void success(APIClient.Response result, Response arg1)
        {
            Log.d("in REST", "sent money with:" + result + "," + arg1);
            //check status - true or false
            //create an in-app dialog
            String title, msg;
            Boolean show_help_button;
            if(result.status==true)
            {
                show_help_button = false;
                title = context.getResources().getString(R.string.sent_credit_title);
                msg = context.getResources().getString(R.string.sent_credit_text);
                Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(title, msg, show_help_button);
                EventBus.getDefault().post(dialogEvent);
                Analytics.with(context).track("Attempt to Send Prepaid Credit Succeeded");
                final Handler rating_handler = new Handler();
                rating_handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Events.PromptToRateApp promptEvent = new Events.PromptToRateApp();
                        EventBus.getDefault().post(promptEvent);
                    }
                }, 5000);//delay by 5 seconds
            }
            else
            {
                show_help_button = true;
                title = context.getResources().getString(R.string.send_credit_failed_title);
                msg = context.getResources().getString(R.string.send_credit_failed_text);
                Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(title, msg, show_help_button);
                EventBus.getDefault().post(dialogEvent);
                Analytics.with(context).track("Attempt to Send Prepaid Credit Failed");
            }
        }
    };

    public Callback<APIClient.Customer> customer_registration_callback = new Callback<APIClient.Customer>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error,"Callbacks","customer_registration_callback");
        }

        @Override
        public void success(APIClient.Customer result, retrofit.client.Response arg1)
        {
            try
            {
                Log.d("in REST", "saved customer with id:" + result.id);
                if(result.id==-1)
                {
                    utils.logMessage("Couldn't register new user,  got -1 response from server!","Callbacks");
                }
                editor.putInt("customer_id", result.id);
                editor.putString("signup_date", result.signup_date);
                editor.commit();
                utils.configureCustomerIdentification(context, sharedPrefs);
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
    };

    public Callback<APIClient.Response> referral_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Analytics.with(context).track("Couldn't analyze install referral - REST error", new Properties().putValue("error",arg0.toString()));
        }
        @Override
        public void success(APIClient.Response result, retrofit.client.Response arg1)
        {}
    };
}
