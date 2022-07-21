package com.unifiapp.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.unifiapp.R;
import com.unifiapp.events.Events.HelpRequestedEvent;

import de.greenrobot.event.EventBus;

public class OKOrHelpDialogFragment extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        Boolean display_help_button = getArguments().getBoolean("display_help_button");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        if(display_help_button==true)
        {
            builder.setMessage(message)
                    .setPositiveButton(getActivity().getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id) {
                            //dismiss
                        }
                    })
                    .setNegativeButton(getActivity().getResources().getString(R.string.dialog_contact_support), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            // User needs help!
                            // events are much easier than using NoticeDialogListener
                            HelpRequestedEvent helpEvent = new HelpRequestedEvent();
                            EventBus.getDefault().post(helpEvent);
                        }
                    });
        }
        else
        {
            builder.setMessage(message)
                    .setPositiveButton(getActivity().getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id) {
                            //dismiss
                        }
                    });
        }
        // Create the AlertDialog object and return it
        return builder.create();
    }
}