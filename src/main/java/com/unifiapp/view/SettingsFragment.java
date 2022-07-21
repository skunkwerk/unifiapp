package com.unifiapp.view;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.segment.analytics.Analytics;
import com.unifiapp.R;
import com.unifiapp.events.Events;

import de.greenrobot.event.EventBus;

public class SettingsFragment extends Fragment
{

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
	public SettingsFragment(){}

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        editor = sharedPref.edit();
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        SeekBar percentShare = (SeekBar) rootView.findViewById(R.id.percentShare);

        final TextView percentText = (TextView) rootView.findViewById(R.id.percentText);

        percentShare.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            int percent = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser)
            {
                percent = progressValue + 5;//5 is the minimum
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                // Display the value in textview
                percentText.setText(String.valueOf(percent));
                editor.putInt("wifi_percent_sharing", percent);
                editor.commit();
            }
        });

        NumberPicker numberPicker = (NumberPicker) rootView.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(50);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i2)
            {
                editor.putInt("cap_monthly_usage_at", i2);
                editor.commit();
            }
        });

        CheckBox cap_monthly_usage = (CheckBox) rootView.findViewById(R.id.checkbox_cap_monthly_usage);
        CheckBox auto_connect = (CheckBox) rootView.findViewById(R.id.checkbox_auto_connect);

        auto_connect.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (((CheckBox) v).isChecked())
                {
                    editor.putBoolean("auto_connect",true);
                    editor.commit();
                }
                else
                {
                    editor.putBoolean("auto_connect",false);
                    editor.commit();
                }
            }
        });

        cap_monthly_usage.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (((CheckBox) v).isChecked())
                {
                    editor.putBoolean("cap_monthly_usage",true);
                    editor.commit();
                }
                else
                {
                    editor.putBoolean("cap_monthly_usage",false);
                    editor.commit();
                }
            }
        });

        //read the saved values, if any, from sharedPrefs
        int percent_sharing = sharedPref.getInt("wifi_percent_sharing",5);
        percentText.setText(String.valueOf(percent_sharing));
        percentShare.setProgress(percent_sharing);
        if (sharedPref.getBoolean("auto_connect",true)==true)
            auto_connect.setChecked(true);
        else
            auto_connect.setChecked(false);
        if (sharedPref.getBoolean("cap_monthly_usage",false)==true)
            cap_monthly_usage.setChecked(true);
        else
            cap_monthly_usage.setChecked(false);
        numberPicker.setValue(sharedPref.getInt("cap_monthly_usage_at",5));

        BootstrapButton updatePasswordButton;
        updatePasswordButton = (BootstrapButton) rootView.findViewById(R.id.update_password);
        updatePasswordButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //take them to the Share fragment for verification again
                Bundle arguments = new Bundle();
                arguments.putBoolean("update",true);
                Events.DisplayFragmentEvent displayFragmentEvent = new Events.DisplayFragmentEvent(7, arguments);
                EventBus.getDefault().post(displayFragmentEvent);
            }
        });
         
        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Analytics.with(getActivity()).screen("Settings","");
    }
}
