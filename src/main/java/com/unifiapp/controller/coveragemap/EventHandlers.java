package com.unifiapp.controller.coveragemap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.NetworkCluster;
import com.unifiapp.model.NetworkCoordinate;
import com.unifiapp.utils.Utilities;
import com.unifiapp.view.CoverageMapFragment;

import java.util.List;

import de.greenrobot.event.EventBus;
import io.realm.Realm;
import io.realm.RealmResults;

public class EventHandlers
{
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    Context context;
    APIClient.API api;
    Utilities utils;
    Callbacks callbacks;
    CoverageMapFragment main;
    List<APIClient.Coordinate> coords;

    Integer DB_DATA_TYPE = 0;
    Integer API_DATA_TYPE = 1;

    public EventHandlers(SharedPreferences sharedPrefs, Context context, APIClient.API api, Utilities utils, CoverageMapFragment main)
    {
        this.sharedPrefs = sharedPrefs;
        this.editor = sharedPrefs.edit();
        this.context = context;
        this.api = api;
        this.utils = utils;
        this.callbacks = new Callbacks(context, utils);
        this.main = main;
        EventBus.getDefault().register(this);
    }

    public void unregister()
    {
        //eventbus will continue to run in background, so we do this here & not in onPause
        EventBus.getDefault().unregister(this);
    }

    /**
     *
     * @param event
     */
    public void onEvent(Events.SyncCoverageMapEvent event)
    {
        Long sync_date = sharedPrefs.getLong("last_coverage_map_sync_date",0);
        api.router_locations_sync(sync_date, callbacks.callback);//get any new coordinates added as well
    }

    public void onEvent(Events.AddCircleEvent event)
    {
        coords = event.getCoords();
        new AddCircleTask().execute(event.getType());
    }

    public void onEvent(Events.LoadCoordinatesEvent event)
    {
        new AddCircleTask().execute(DB_DATA_TYPE);
    }

    class AddCircleTask extends AsyncTask<Integer, Void, Void>
    {
        @Override
        protected Void doInBackground(Integer... dataType)
        {
            try
            {
                if(dataType[0]==DB_DATA_TYPE)
                {
                    //we re-query for the database results, as otherwise Realm will crash because
                    //we're accessing results from a different thread
                    try
                    {
                        Realm realm = Realm.getInstance(context, false);
                        RealmResults<NetworkCoordinate> db_coords = realm.where(NetworkCoordinate.class).findAll();
                        for (NetworkCoordinate coord : db_coords)
                        {
                            NetworkCluster marker = new NetworkCluster(coord.getLatitude(),coord.getLongitude(),coord.getMac_address(),coord.getSsid());
                            main.mClusterManager.addItem(marker);
                            if(main.directions==true && coord.getMac_address().equals(main.destination_mac_address))
                            {
                                main.destinationItem = marker;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        utils.logException(e);
                    }
                }
                else if(dataType[0]==API_DATA_TYPE)
                {
                    for (APIClient.Coordinate coord : coords)
                    {
                        NetworkCluster marker = new NetworkCluster(coord.latitude,coord.longitude,coord.mac_address,coord.ssid);
                        main.mClusterManager.addItem(marker);
                        if(main.directions!=null && main.directions==true && main.destination_mac_address!=null && coord.mac_address!=null && coord.mac_address.equals(main.destination_mac_address))
                        {
                            main.destinationItem = marker;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
            return(null);
        }

        @Override
        protected void onPostExecute(Void unused)
        {
            try
            {
                main.mClusterManager.cluster();//done asynchronously, so destination marker may not be displayed immediately
            }
            catch (Exception e)
            {
                utils.logException(e);
            }
        }
    }
}
