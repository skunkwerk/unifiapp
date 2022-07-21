package com.unifiapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.R;
import com.unifiapp.adapter.MapMarkerAdapter;
import com.unifiapp.controller.coveragemap.Callbacks;
import com.unifiapp.controller.coveragemap.EventHandlers;
import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient.API;
import com.unifiapp.model.APIFactory;
import com.unifiapp.model.NetworkCluster;
import com.unifiapp.utils.Utilities;

import de.greenrobot.event.EventBus;

public class CoverageMapFragment extends Fragment implements RoutingListener
{
    MapView m;
    private GoogleMap map;
    API api;
    Context context;
    Utilities utils;
    public ClusterManager<NetworkCluster> mClusterManager;
    public NetworkCluster clickedClusterItem;
    public Boolean directions = false;
    public String destination_mac_address;
    public DefaultClusterRenderer renderer;
    public NetworkCluster destinationItem;
    Callbacks callbacks;
    SharedPreferences sharedPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        context = container.getContext();
        utils = new Utilities(context);
        callbacks = new Callbacks(context, utils);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        api = new APIFactory().getAPI();
        EventHandlers eventHandlers = new EventHandlers(sharedPrefs, context, api, utils, this);

        // inflate and return the layout
        View v = inflater.inflate(R.layout.coverage_map_fragment, container, false);

        try
        {
            MapsInitializer.initialize(context);
        }
        catch (Exception e)
        {
            utils.logException(e);
            int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
            final int RQS_GooglePlayServices = 1;
            GooglePlayServicesUtil.getErrorDialog(status, getActivity(), RQS_GooglePlayServices).show();
            return v;
        }

        m = (MapView) v.findViewById(R.id.map);
        m.onCreate(savedInstanceState);
        map = m.getMap();
        if (map!=null)
        {
            setupCoverageMap(inflater);
        }
        else
        {
            Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(getResources().getString(R.string.coverage_map_error_title), getResources().getString(R.string.coverage_map_error_text), true);
            EventBus.getDefault().post(dialogEvent);
        }

        return v;
    }

    public void setupCoverageMap(LayoutInflater inflater)
    {
        try
        {
            LatLng LOCATION = new LatLng(18.9750, 72.8258);//Mumbai city center
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LOCATION, 12));
            map.setMyLocationEnabled(true);//shows the button which you can use to find your location
            mClusterManager = new ClusterManager<NetworkCluster>(context, map);
            map.setOnCameraChangeListener(mClusterManager);
            map.setOnMarkerClickListener(mClusterManager);
            map.setInfoWindowAdapter(mClusterManager.getMarkerManager());

            mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new MapMarkerAdapter(context, inflater, this));

            //would be faster to do getMarker after all the markers have been added, but because cluster is an async call, won't know when it's done
            //com.google.android.gms.maps.model.Marker marker = main.renderer.getMarker(main.destinationItem);
            class NetworkClusterRenderer extends DefaultClusterRenderer<NetworkCluster>
            {

                public NetworkClusterRenderer(Context context, GoogleMap map,
                                              ClusterManager<NetworkCluster> clusterManager)
                {
                    super(context, map, clusterManager);
                }

                @Override
                protected void onClusterItemRendered(NetworkCluster clusterItem, Marker marker)
                {
                    super.onClusterItemRendered(clusterItem, marker);
                    if(directions==true && destinationItem!=null)
                    {
                        if(clusterItem==destinationItem && marker!=null)
                        {
                            clickedClusterItem = destinationItem;
                            marker.showInfoWindow();
                        }
                    }
                }
            }

            renderer = new NetworkClusterRenderer(context, map, mClusterManager);
            mClusterManager.setRenderer(renderer);

            mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<NetworkCluster>()
            {
                @Override
                public boolean onClusterItemClick(NetworkCluster item)
                {
                    clickedClusterItem = item;
                    return false;
                }
            });

            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener()
            {
                @Override
                public void onInfoWindowClick(Marker marker)
                {
                    Log.d("CoverageMap","clicked on info map");
                    if(clickedClusterItem!=null && clickedClusterItem.mac_address.equals(destination_mac_address))
                    {
                        api.unable_to_connect(getArguments().getString("mac_address"), sharedPrefs.getInt("customer_id",0), callbacks.could_not_connect_callback);
                        Analytics.with(context).track("Reported unable to connect to network", new Properties().putValue("mac_address",getArguments().getString("mac_address")));
                    }
                    marker.hideInfoWindow();
                }
            });

            Long sync_date = sharedPrefs.getLong("last_coverage_map_sync_date",0);

            Toast.makeText(getActivity(), getResources().getString(R.string.loading), Toast.LENGTH_LONG).show();

            //if we requested directions to the nearest hotspot, get the route
            if(getArguments()!=null && getArguments().getBoolean("get_directions", false))
            {
                double[] currentLocationCoordinates = getArguments().getDoubleArray("currentLocationCoordinates");
                float[] routerCoordinates = getArguments().getFloatArray("routerCoordinates");
                destination_mac_address = getArguments().getString("mac_address");
                if(currentLocationCoordinates!=null && routerCoordinates!=null)
                {
                    Routing routing = new Routing(Routing.TravelMode.WALKING);
                    routing.registerListener(this);
                    routing.execute(new LatLng(currentLocationCoordinates[0], currentLocationCoordinates[1]), new LatLng(routerCoordinates[0], routerCoordinates[1]));//start, end points
                }
                //zoom in on the start point (ie your current location)
                LatLng startPoint = new LatLng(currentLocationCoordinates[0], currentLocationCoordinates[1]);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 14));
                directions = true;
            }

            //load any saved data from the database cache
            Events.LoadCoordinatesEvent coordinatesEvent = new Events.LoadCoordinatesEvent();
            EventBus.getDefault().post(coordinatesEvent);

            api.router_locations_sync(sync_date, callbacks.callback);//get any new coordinates added as well
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    @Override
    public void onRoutingSuccess(PolylineOptions mPolyOptions, Route route)
    {
        try
        {
            PolylineOptions polyoptions = new PolylineOptions();
            polyoptions.color(Color.BLUE);
            polyoptions.width(10);
            polyoptions.addAll(mPolyOptions.getPoints());
            map.addPolyline(polyoptions);
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    @Override
    public void onRoutingFailure()
    {
        // The Routing request failed
        utils.logMessage("routing request failed","CoverageMapFragment");
    }

    @Override
    public void onRoutingStart() {}

    @Override
    public void onResume()
    {
        super.onResume();
        m.onResume();
        Analytics.with(getActivity()).screen("Coverage Map", "");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        m.onPause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        m.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        m.onLowMemory();
    }
}