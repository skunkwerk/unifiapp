package com.unifiapp.model;

import com.maxmind.geoip2.WebServiceClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;

public class APIFactory
{
    RestAdapter restAdapter;
    APIClient.API api;
    WebServiceClient ip_api;

    public APIFactory()
    {
        RequestInterceptor requestInterceptor = new RequestInterceptor()
        {
            @Override
            public void intercept(RequestFacade request)
            {
                request.addHeader("Authorization", "Basic YWRtaW46OTN3Y0Jqc3A=");
                //request.addHeader("Accept-Encoding", "gzip"); okhttp handles itself
            }
        };
        restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://www.unifiapp.com")
                .setRequestInterceptor(requestInterceptor)
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog("in REST"))
                .build();
        api = restAdapter.create(APIClient.API.class);
        ip_api = new WebServiceClient.Builder(93122, "6Ke3igKf9ZQr").build();
    }

    public APIClient.API getAPI()
    {
        return api;
    }
    public WebServiceClient getIPAPI()
    {
        return ip_api;
    }
}
