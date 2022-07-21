package com.unifiapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.FontAwesomeText;
import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.unifiapp.R;
import com.unifiapp.adapter.ExpandableList;
import com.unifiapp.adapter.Group;
import com.unifiapp.events.Events;
import com.unifiapp.events.Events.DisplayFragmentEvent;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIClient.CustomerLeaderboard;
import com.unifiapp.model.APIClient.CustomerPoints;
import com.unifiapp.model.APIFactory;
import com.unifiapp.utils.Utilities;

import java.util.List;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LeaderboardCommunityFragment extends Fragment
{
    // more efficient than HashMap for mapping integers to objects
    SparseArray<Group> groups = new SparseArray<Group>();
    View rootView;
    LayoutInflater layoutInflater;
    SharedPreferences sharedPrefs;
    Context context;
    Utilities utils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.leaderboard_community_fragment, container, false);
        layoutInflater = inflater;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        context = this.getActivity(); //has to be called after onAttach otherwise null
        utils = new Utilities(context);
        int customer_id = sharedPrefs.getInt("customer_id", 0);

        FontAwesomeText add_router_button = (FontAwesomeText) rootView.findViewById(R.id.add_router_button);

        FontAwesomeText invite_friends_button = (FontAwesomeText) rootView.findViewById(R.id.invite_friends_button);

        FontAwesomeText share_more_button = (FontAwesomeText) rootView.findViewById(R.id.share_more_button);

        add_router_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                DisplayFragmentEvent displayEvent = new Events.DisplayFragmentEvent(2);
                EventBus.getDefault().post(displayEvent);
            }
        });

        invite_friends_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getActivity(), getResources().getString(R.string.friend_referral_points),Toast.LENGTH_SHORT).show();
                utils.inviteFriends("leaderboard", sharedPrefs, context);
            }
        });

        share_more_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                    DisplayFragmentEvent displayEvent = new Events.DisplayFragmentEvent(4);
                    EventBus.getDefault().post(displayEvent);
            }
        });

        Toast.makeText(getActivity(), getResources().getString(R.string.loading),Toast.LENGTH_SHORT).show();

        APIClient.API api;
        api = new APIFactory().getAPI();//be sure to do this before the call to SendRegistrationIdEvent, as it uses the API
        api.customer_points("mumbai", customer_id, leaderboard_callback);//TODO: mumbai

        displayCompetitionDialog();

        return rootView;
    }

    public void displayCompetitionDialog()
    {
        if (sharedPrefs.getBoolean("seen_leaderboard_competition_dialog",false)==false && isAdded())
        {
            Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(getResources().getString(R.string.leaderboard_competition_dialog_title), getResources().getString(R.string.leaderboard_competition_dialog_text), false);
            EventBus.getDefault().post(dialogEvent);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean("seen_leaderboard_competition_dialog", true);
            editor.commit();
            Analytics.with(getActivity()).track("Showed Leaderboard Competition Dialog to Customer");
        }
    }

    Callback<CustomerPoints> leaderboard_callback = new Callback<CustomerPoints>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(CustomerPoints result, Response response)
        {
            try
            {
                if(isAdded())
                {
                    List<CustomerLeaderboard> leaders = result.getCustomers();
                    int my_customer_id = sharedPrefs.getInt("customer_id", 0);
                    Boolean you_are_in_leaders = false;
                    int j = 0;
                    Group group;
                    for (CustomerLeaderboard leader:leaders)
                    {
                        if (leader.customer_id == my_customer_id)
                        {
                            you_are_in_leaders = true;
                        }
                    }

                    List<CustomerLeaderboard> leaders_subset;
                    //if you're in the leaders, display 6 rows of results
                    //if you're not in the leaders, display 3 rows of results, plus two rows of dots, and then yourself
                    if(you_are_in_leaders==true)
                        leaders_subset = leaders.subList(0,6);
                    else
                        leaders_subset = leaders.subList(0,3);

                    for (CustomerLeaderboard leader:leaders_subset)
                    {
                        Log.d("leader:",String.valueOf(leader.first_name));
                        if (leader.customer_id==my_customer_id)
                        {
                            group = new Group(getResources().getString(R.string.leaderboard_you),false);
                        }
                        else
                        {
                            group = new Group(leader.first_name,false);
                        }
                        group.profile_picture_url = "https://graph.facebook.com/" + leader.oauth_user_id + "/picture?type=square";
                        group.points = "+ " + String.valueOf(leader.total_points);
                        group.children.add(getResources().getString(R.string.leaderboard_all_time_points) + String.valueOf(leader.all_time_points));
                        groups.append(j, group);
                        j += 1;
                    }
                    //add yourself at the end if necessary
                    if (you_are_in_leaders==false)
                    {
                        //add 2 rows of dots to separate
                        //. text centered, hide borders
                        group = new Group(".",true);
                        groups.append(j,group);
                        groups.append(j+1,group);

                        group = new Group(getResources().getString(R.string.leaderboard_you),false);
                        group.points = "+ " + String.valueOf(result.your_points.month_points);
                        group.profile_picture_url = "https://graph.facebook.com/" + sharedPrefs.getString("facebook_user_id", "") + "/picture?type=square";
                        group.children.add(getResources().getString(R.string.leaderboard_all_time_points) + String.valueOf(result.your_points.all_time_points));
                        groups.append(j+2, group);
                    }

                    ExpandableListView listView = (ExpandableListView) rootView.findViewById(R.id.community_listview);
                    ExpandableList adapter = new ExpandableList(layoutInflater, groups);
                    listView.setAdapter(adapter);
                    listView.setGroupIndicator(null);//hide the carrot group indicator
                }
            }
            catch (Exception e)
            {
                Crashlytics.log(Log.ERROR, "Leaderboard", "couldn't display leaderboard data");
                Crashlytics.logException(e);
            }

        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        Analytics.with(getActivity()).screen("Leaderboard Community","");
    }
}
