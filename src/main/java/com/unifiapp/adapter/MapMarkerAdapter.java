package com.unifiapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.unifiapp.R;
import com.unifiapp.view.CoverageMapFragment;

public class MapMarkerAdapter implements InfoWindowAdapter
{
    private View view;
    TextView title;
    TextView details;
    TextView detailsLineTwo;
    CoverageMapFragment mapFragment;
    Context context;

    public MapMarkerAdapter(Context context, LayoutInflater inflater, CoverageMapFragment mapFragment)
    {
        view = inflater.inflate(R.layout.map_marker, null);

        this.context = context;
        title = (TextView) view.findViewById(R.id.infoWindowTitle);
        details = (TextView) view.findViewById(R.id.infoWindowDetails);
        detailsLineTwo = (TextView) view.findViewById(R.id.infoWindowDetailsLine2);

        this.mapFragment = mapFragment;
    }

    //The first of these (getInfoWindow()) allows you to provide a view that will be used for the entire info window.
    // The second of these (getInfoContents()) allows you to just customize the contents of the window but still keep
    // the default info window frame and background
    @Override
    public View getInfoContents(Marker mark)
    {
        if (mapFragment.clickedClusterItem != null)
        {
            title.setText(context.getResources().getString(R.string.report_network_problem_title) + mapFragment.clickedClusterItem.ssid);

            if(mapFragment.directions!=null && mapFragment.directions==true)
            {
                if(mapFragment.clickedClusterItem.mac_address.equals(mapFragment.destination_mac_address))
                {
                    details.setText(context.getResources().getString(R.string.report_network_problem_text_1));
                    detailsLineTwo.setText(context.getResources().getString(R.string.report_network_problem_text_2));
                }
            }
        }
        return view;
    }

    //The API will first call getInfoWindow(Marker) and if null is returned, it will then call getInfoContents(Marker)
    @Override
    public View getInfoWindow(final Marker mark)
    {
        return null;
    }
}
