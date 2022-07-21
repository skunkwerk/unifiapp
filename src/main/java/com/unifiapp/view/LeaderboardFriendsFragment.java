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
import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.unifiapp.R;
import com.unifiapp.adapter.ExpandableList;
import com.unifiapp.adapter.Group;
import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIClient.OauthUser;
import com.unifiapp.model.APIFactory;
import com.unifiapp.utils.Utilities;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;

public class LeaderboardFriendsFragment extends Fragment
{
    View rootView;
    LayoutInflater layoutInflater;
    SharedPreferences sharedPrefs;
    Context context;
    Utilities utils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.leaderboard_friends_fragment, container, false);
        layoutInflater = inflater;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        context = this.getActivity(); //has to be called after onAttach otherwise null
        utils = new Utilities(context);

        Session session = getOrCreateSession();

        /* make the API call */
        Boolean hasPermission = session.isPermissionGranted("user_friends");
        if (hasPermission==false)
        {
            Log.d("friends","requesting fb permissions");

            Events.FacebookFriendsRequestEvent getFriendsEvent = new Events.FacebookFriendsRequestEvent();
            EventBus.getDefault().post(getFriendsEvent);
        }
        else
        {
            Log.d("friends","about to request friends");
            requestFriends(session);
        }

        FontAwesomeText add_router_button = (FontAwesomeText) rootView.findViewById(R.id.add_router_button);

        FontAwesomeText invite_friends_button = (FontAwesomeText) rootView.findViewById(R.id.invite_friends_button);

        FontAwesomeText share_more_button = (FontAwesomeText) rootView.findViewById(R.id.share_more_button);

        add_router_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Events.DisplayFragmentEvent displayEvent = new Events.DisplayFragmentEvent(2);
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
                Events.DisplayFragmentEvent displayEvent = new Events.DisplayFragmentEvent(4);
                EventBus.getDefault().post(displayEvent);
            }
        });

        Toast.makeText(getActivity(), getResources().getString(R.string.loading), Toast.LENGTH_SHORT).show();

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    public void onEvent(Events.FacebookFriendsRequestReturnedEvent event)
    {
        Log.d("friends", "received FacebookFriendsRequestEvent");
        try
        {
            requestFriends(getOrCreateSession());
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "LeaderboardFriendsFragment", "error requesting Facebook friends");
            Crashlytics.logException(e);
            Analytics.with(getActivity()).track("error requesting Facebook friends");
        }
    }

    public void requestFriends(Session session)
    {
        try
        {
            Boolean hasPermission = session.isPermissionGranted("user_friends");
            if (hasPermission==true)
            {
                RequestAsyncTask friendsRequest = Request.newMyFriendsRequest(session, new Request.GraphUserListCallback()
                {
                    @Override
                    public void onCompleted(List<GraphUser> users, Response response)
                    {
                        Log.d("friends", "graphuserlist onCompleted");
                        FacebookRequestError error = response.getError();
                        if (error != null)
                        {
                            Log.e("friends", error.toString());
                            Analytics.with(getActivity()).track("error getting Facebook friends", new Properties().putValue("error", error.toString()));
                        }
                        else
                        {
                            populateList(users);
                        }
                    }
                }).executeAsync();
            }
            else
            {
                //we've already prompted them to allow us permissions, but they said no
                Analytics.with(getActivity()).track("user prompted for Facebook friends permission, but not granted");
            }
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "LeaderboardFriendsFragment", "error requesting Facebook friends");
            Crashlytics.logException(e);
            Analytics.with(getActivity()).track("error requesting Facebook friends");
        }
    }

    public Session getOrCreateSession()
    {
        Session session = Session.getActiveSession();

        if(session==null)
        {
            Log.d("friends","session was null");
            session = new Session(getActivity());
        }
        return session;
    }

    /**
     *
     * @param users
     */
    public void populateList(List<GraphUser> users)
    {
        try
        {
            APIClient.API api;
            api = new APIFactory().getAPI();
            Log.d("friends","in populateList");

            List<OauthUser> friends = new ArrayList<OauthUser>();
            for (GraphUser user : users)
            {
                Log.d("friend found:", user.getName());
                Log.d("friend found userid:", user.getId());
                friends.add(new OauthUser(user.getId()));
            }
            if(friends.size()>0)
                api.friends_points(friends, friends_callback);
            else
            {
                //TODO: make this an alternate dynamic view with just text
                Toast.makeText(getActivity(), getResources().getString(R.string.leaderboard_no_friends), Toast.LENGTH_LONG).show();
            }
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "Leaderboard", "couldn't display leaderboard data");
            Crashlytics.logException(e);
            Log.d("friends", Log.getStackTraceString(e));
        }
    }

    Callback<APIClient.FriendsPoints> friends_callback = new Callback<APIClient.FriendsPoints>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(APIClient.FriendsPoints result, retrofit.client.Response response)
        {
            List<APIClient.CustomerLeaderboard> leaderboard = result.getCustomers();
            displayFriends(leaderboard);
        }
    };

    public void displayFriends(List<APIClient.CustomerLeaderboard> leaderboard)
    {
        SparseArray<Group> groups = new SparseArray<Group>();
        int j = 0;
        Group group;

        if(isAdded())
        {
            for(APIClient.CustomerLeaderboard customer:leaderboard)
            {
                if(customer.customer_id!=0)
                {
                    Log.d("friends leaderboard:",String.valueOf(customer.all_time_points));
                    group = new Group(customer.first_name,false);
                    group.profile_picture_url = "https://graph.facebook.com/" + customer.oauth_user_id + "/picture?type=square";
                    group.points = "+ " + String.valueOf(customer.total_points);
                    group.children.add(getResources().getString(R.string.leaderboard_all_time_points) + String.valueOf(customer.all_time_points));
                }
                else
                {
                    group = new Group(customer.first_name,true);
                }
                groups.append(j, group);
                j += 1;
            }

            ExpandableListView listView = (ExpandableListView) rootView.findViewById(R.id.friends_listview);
            ExpandableList adapter = new ExpandableList(layoutInflater, groups);
            listView.setAdapter(adapter);
            listView.setGroupIndicator(null);//hide the carrot group indicator
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Analytics.with(getActivity()).screen("Leaderboard Friends","");
    }
}
