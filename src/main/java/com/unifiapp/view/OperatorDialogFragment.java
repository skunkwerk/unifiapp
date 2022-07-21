package com.unifiapp.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.unifiapp.R;
import com.unifiapp.events.Events.HelpRequestedEvent;
import com.unifiapp.events.Events.SendCreditWithOperatorEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.greenrobot.event.EventBus;

public class OperatorDialogFragment extends DialogFragment
{
    private Integer operator_id;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        Boolean display_help_button = getArguments().getBoolean("display_help_button");
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message);
        builder.setTitle(title);
        View view = inflater.inflate(R.layout.phone_dialog, null);
        Spinner operator_dropdown_menu = (Spinner) view.findViewById(R.id.operator_dropdown_menu);
        //operator_dropdown_menu.setMinimumHeight(50);
        //operator_dropdown_menu.setBackground(getResources().getDrawable(android.R.drawable.btn_dropdown));
        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.dialog_send), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                //send the credit request again through an event
                if(operator_id!=null)
                {
                    SendCreditWithOperatorEvent sendEvent = new SendCreditWithOperatorEvent(operator_id);
                    EventBus.getDefault().post(sendEvent);
                }
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.dialog_contact_support), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User needs help!
                // events are much easier than using NoticeDialogListener
                HelpRequestedEvent helpEvent = new HelpRequestedEvent();
                EventBus.getDefault().post(helpEvent);
            }
        });

        operator_dropdown_menu.setPrompt(getResources().getString(R.string.dialog_select_operator_label));

        final HashMap<String, Integer> operators = new HashMap<String, Integer>();
        operators.put(getResources().getString(R.string.operator_aircel_ap), 1437);
        operators.put(getResources().getString(R.string.operator_aircel_assam), 1428);
        operators.put(getResources().getString(R.string.operator_aircel_bihar), 1425);
        operators.put(getResources().getString(R.string.operator_aircel_chennai), 1376);
        operators.put(getResources().getString(R.string.operator_aircel_delhi), 1432);
        operators.put(getResources().getString(R.string.operator_aircel_hp), 1430);
        operators.put(getResources().getString(R.string.operator_aircel_jk), 1431);
        operators.put(getResources().getString(R.string.operator_aircel_karnataka), 1438);
        operators.put(getResources().getString(R.string.operator_aircel_kolkata), 1378);
        operators.put(getResources().getString(R.string.operator_aircel_maharashtra), 1450);
        operators.put(getResources().getString(R.string.operator_aircel_mumbai), 1433);
        operators.put(getResources().getString(R.string.operator_aircel_north_east), 1429);
        operators.put(getResources().getString(R.string.operator_aircel_orissa), 1427);
        operators.put(getResources().getString(R.string.operator_aircel_punjab), 1910);
        operators.put(getResources().getString(R.string.operator_aircel_rajasthan), 1912);
        operators.put(getResources().getString(R.string.operator_aircel_tamil_nadu), 1377);
        operators.put(getResources().getString(R.string.operator_aircel_upe), 1434);
        operators.put(getResources().getString(R.string.operator_aircel_west_bengal), 1426);
        operators.put(getResources().getString(R.string.operator_airtel_india), 1371);
        operators.put(getResources().getString(R.string.operator_bsnl), 1420);
        operators.put(getResources().getString(R.string.operator_idea), 1401);
        operators.put(getResources().getString(R.string.operator_loop_mobile), 55);
        operators.put(getResources().getString(R.string.operator_mtnl_delhi), -2);
        operators.put(getResources().getString(R.string.operator_mtnl_mumbai), -1);
        operators.put(getResources().getString(R.string.operator_mts), 1503);
        operators.put(getResources().getString(R.string.operator_reliance_cdma), 519);
        operators.put(getResources().getString(R.string.operator_reliance_gsm), 518);
        operators.put(getResources().getString(R.string.operator_tata_docomo), 1457);
        operators.put(getResources().getString(R.string.operator_tata_indicom), 622);
        operators.put(getResources().getString(R.string.operator_unicor), 1504);
        operators.put(getResources().getString(R.string.operator_videocon_gsm), 1497);
        operators.put(getResources().getString(R.string.operator_vodafone_ap), 1381);
        operators.put(getResources().getString(R.string.operator_vodafone_bangalore), 1379);
        operators.put(getResources().getString(R.string.operator_vodafone_bengal), 1391);
        operators.put(getResources().getString(R.string.operator_vodafone_chennai), 1380);
        operators.put(getResources().getString(R.string.operator_vodafone_delhi), 1382);
        operators.put(getResources().getString(R.string.operator_vodafone_gujarat), 1386);
        operators.put(getResources().getString(R.string.operator_vodafone_haryana), 1392);
        operators.put(getResources().getString(R.string.operator_vodafone_kerala), 1393);
        operators.put(getResources().getString(R.string.operator_vodafone_kolkata), 1385);
        operators.put(getResources().getString(R.string.operator_vodafone_maharashtra), 1396);
        operators.put(getResources().getString(R.string.operator_vodafone_mumbai), 1384);
        operators.put(getResources().getString(R.string.operator_vodafone_punjab), 1387);
        operators.put(getResources().getString(R.string.operator_vodafone_rajasthan), 1390);
        operators.put(getResources().getString(R.string.operator_vodafone_tamil_nadu), 1395);
        operators.put(getResources().getString(R.string.operator_vodafone_upe), 1388);
        operators.put(getResources().getString(R.string.operator_vodafone_upw), 1389);

        List<String> list = new ArrayList<String>(operators.keySet());
        java.util.Collections.sort(list);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operator_dropdown_menu.setAdapter(dataAdapter);

        //first, search the HashMap for the corresponding string for our operator_id
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int default_operator_id = sharedPrefs.getInt("operator_id",0);
        if (default_operator_id!=0)
        {
            try
            {
                String operator_name = (String) getKeyByValue(operators, default_operator_id);//TODO use google commons hashbimap instead, with proguard
                int spinnerPosition = dataAdapter.getPosition(operator_name);
                operator_dropdown_menu.setSelection(spinnerPosition); //set the default according to value
            }
            catch (Exception e)
            {
                Analytics.with(getActivity()).track("Couldn't set default operator_name in OperatorDialog");
                Crashlytics.log(Log.ERROR, "OperatorDialogFragment", "Couldn't set default operator_name in OperatorDialog");
                Crashlytics.logException(e);
            }
        }

        operator_dropdown_menu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                // An spinnerItem was selected. You can retrieve the selected item using
                // parent.getItemAtPosition(pos)
                Log.d("operator selected: ", String.valueOf(operators.get(parent.getItemAtPosition(pos))));
                operator_id = operators.get(parent.getItemAtPosition(pos));
            }

            public void onNothingSelected(AdapterView<?> parent)
            {
                // Do nothing, just another required interface callback
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value)
    {
        for (Entry<T, E> entry : map.entrySet())
        {
            if (value.equals(entry.getValue()))
            {
                return entry.getKey();
            }
        }
        return null;
    }
}