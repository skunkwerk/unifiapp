package com.unifiapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.segment.analytics.Analytics;
import com.unifiapp.R;
import com.unifiapp.events.Events;

import java.util.Arrays;
import java.util.List;

import de.greenrobot.event.EventBus;

public class LeaderboardFragment extends Fragment
{

    private FragmentTabHost mTabHost;
    private UiLifecycleHelper uiHelper;

    public UiLifecycleHelper getFbUiLifecycleHelper()
    {
        return uiHelper;
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        mTabHost = new FragmentTabHost(getActivity());

        mTabHost.setup(getActivity().getApplicationContext(), getChildFragmentManager(), R.id.realtabcontent);

        mTabHost.addTab(mTabHost.newTabSpec("community").setIndicator(getResources().getString(R.string.leaderboard_community_tab_title)), LeaderboardCommunityFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("friends").setIndicator(getResources().getString(R.string.leaderboard_friends_tab_title)), LeaderboardFriendsFragment.class, null);
        return mTabHost;
    }

    public void onEvent(Events.FacebookFriendsRequestEvent event)
    {
        Log.d("LeaderboardFragment", "received FacebookFriendsRequestEvent");
        try
        {
            Session session = Session.getActiveSession();

            if(session==null)
            {
                Log.d("LeaderboardFragment","session was null");
                session = new Session(getActivity());
            }
            List<String> newPermissions = Arrays.asList("user_friends");
            session.requestNewReadPermissions(new Session.NewPermissionsRequest(this, newPermissions));
            //private static final int AUTH_PUBLISH_ACTIONS_SCORES_ACTIVITY_CODE = 103;
            //.setRequestCode(AUTH_PUBLISH_ACTIONS_SCORES_ACTIVITY_CODE);
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "LeaderboardFragment", "error requesting Facebook friend permissions");
            Crashlytics.logException(e);
            Analytics.with(getActivity()).track("error requesting Facebook friend permissions");
        }
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception)
    {
        if (state.isOpened())
        {
            Log.d("LeaderboardFragment","sessionStateChange - isOpened");
        }
        else if (state.isClosed())
        {
            Log.d("LeaderboardFragment","sessionStateChange - isClosed");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
        Log.d("LeaderboardFragment","onActivityResult");
        Events.FacebookFriendsRequestReturnedEvent getFriendsReturnedEvent = new Events.FacebookFriendsRequestReturnedEvent();
        EventBus.getDefault().post(getFriendsReturnedEvent);
        //resultCode == Activity.RESULT_CANCELED
    }

    private Session.StatusCallback callback = new Session.StatusCallback()
    {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception)
        {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        uiHelper.onResume();

        Session session = Session.getActiveSession();
        if (session != null && (session.isOpened() || session.isClosed()))
        {
            Log.d("LeaderboardFragment","onResume, session is not null");
            onSessionStateChange(session, session.getState(), null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState)
    {
        super.onSaveInstanceState(savedState);
        uiHelper.onSaveInstanceState(savedState);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mTabHost = null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        uiHelper.onDestroy();
    }
}
