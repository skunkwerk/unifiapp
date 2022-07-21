package com.unifiapp.controller.coveragemap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.NetworkCoordinate;
import com.unifiapp.utils.Utilities;

import java.util.List;

import de.greenrobot.event.EventBus;
import io.realm.Realm;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Callbacks
{
    Context context;
    Utilities utils;
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    Realm realm;
    Integer API_DATA_TYPE = 1;

    public Callbacks(Context context, Utilities utils)
    {
        this.context = context;
        this.utils = utils;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPrefs.edit();
        try
        {
            realm = Realm.getInstance(context, false);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     * @param result
     */
    public void storeCoordinates(APIClient.Coordinates result)
    {
        try
        {
            realm.beginTransaction();
            List<APIClient.Coordinate> coords = result.getCoordinates();
            for (APIClient.Coordinate coord : coords)
            {
                NetworkCoordinate coordinate = realm.createObject(NetworkCoordinate.class); // Create a new object
                coordinate.setLatitude(coord.latitude);
                coordinate.setLongitude(coord.longitude);
                if(coord.mac_address!=null)
                    coordinate.setMac_address(coord.mac_address);
                if(coord.ssid!=null)
                    coordinate.setSsid(coord.ssid);
            }
            realm.commitTransaction();

            editor.putLong("last_coverage_map_sync_date",result.getSyncDate());
            editor.commit();
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public Callback<APIClient.Coordinates> callback = new Callback<APIClient.Coordinates>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "CoverageMapFragment", "callback");
        }

        @Override
        public void success(APIClient.Coordinates result, Response response)
        {
            List<APIClient.Coordinate> coords = result.getCoordinates();
            if (coords.isEmpty())
            {
                Log.d("in WiFi", "no coordinates");
            }
            else
            {
                Log.d("in WiFi", "GOT coordinates");
                Events.AddCircleEvent addCircleEvent = new Events.AddCircleEvent(API_DATA_TYPE, coords);
                EventBus.getDefault().post(addCircleEvent);
                storeCoordinates(result);
            }
        }
    };

    public Callback<APIClient.Response> could_not_connect_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "CoverageMapFragment", "callback");
        }

        @Override
        public void success(APIClient.Response result, Response response)
        {
            Log.d("result:",String.valueOf(result.status));
        }
    };
}
