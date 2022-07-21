package com.unifiapp.controller.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.unifiapp.model.APIClient;
import com.unifiapp.utils.Utilities;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Callbacks
{
    PostVerifyWiFiCheckCallback postVerifyCallback;
    PreVerifyWiFiCheckCallback preVerifyWiFiCheckCallback;
    Context context;
    SharedPreferences sharedPrefs;
    Utilities utils;

    public Callbacks(PreVerifyWiFiCheckCallback preVerifyWiFiCheckCallback, PostVerifyWiFiCheckCallback postVerifyCallback, Context context, SharedPreferences sharedPrefs)
    {
        this.preVerifyWiFiCheckCallback = preVerifyWiFiCheckCallback;
        this.postVerifyCallback = postVerifyCallback;
        this.context = context;
        this.sharedPrefs = sharedPrefs;
        utils = new Utilities(context);
    }

    public Callback<APIClient.Response> duplicate_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "Callbacks", "duplicate_callback");
            postVerifyCallback.onPostVerifyWiFiCheckComplete(Utilities.NO_INTERNET);
        }

        @Override
        public void success(APIClient.Response result, Response response)
        {
            Log.d("duplicate callback:",response.getStatus() + " " + response.getBody().toString());

            if (result.status==false)
            {
                Log.d("in duplicate_callback", "no match for router");
                postVerifyCallback.onPostVerifyWiFiCheckComplete(Utilities.SUCCESS);
            }
            else
            {
                Log.d("in duplicate_callback", "match for router");
                postVerifyCallback.onPostVerifyWiFiCheckComplete(Utilities.DUPLICATE_FOUND);
            }
        }
    };

    public Callback<APIClient.Operator> operator_callback = new Callback<APIClient.Operator>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "Callbacks", "operator_callback");
        }

        @Override
        public void success(APIClient.Operator result, Response response)
        {
            Log.d("operator callback:",response.getStatus() + " " + response.getBody().toString());

            if (result.operator_id==0)
            {
                Log.d("in operator callback", "no operator for number");
            }
            else
            {
                Log.d("in operator callback", "found operator for number");
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt("operator_id",result.operator_id);
                editor.commit();
            }
        }
    };

    public Callback<APIClient.WifiRouter> callback = new Callback<APIClient.WifiRouter>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "Callbacks", "callback");
        }

        @Override
        public void success(APIClient.WifiRouter result, Response arg1)
        {
            Log.d("in REST callback for router", "saved with:" + result.ssid + "," + result.password);
        }
    };

    public Callback<APIClient.Response> update_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "Callbacks", "update_callback");
        }

        @Override
        public void success(APIClient.Response result, Response arg1)
        {
            Log.d("in REST callback for update password", String.valueOf(result.status));
        }
    };

    public interface PreVerifyWiFiCheckCallback
    {
        public void onPreVerifyWiFiCheckComplete(Bundle result);
    }

    public interface PostVerifyWiFiCheckCallback
    {
        public void onPostVerifyWiFiCheckComplete(int result);
    }
}
