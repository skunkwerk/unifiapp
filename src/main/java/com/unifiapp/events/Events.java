package com.unifiapp.events;

import android.location.Location;
import android.os.Bundle;

import com.unifiapp.model.APIClient;

import java.util.List;

public class Events
{

    /**
     * used because the leaderboard has nested fragments
     * and Android doesn't propagate onActivityResult messages to child fragments
     * and this workaround doesn't work, as we don't control the startActivity,
     * which needs to be done like:
     * getParentFragment().startActivityForResult.
     * and we can't put the button in the LeaderboardFragment, as
     */
    public static class FacebookFriendsRequestEvent
    {
        public FacebookFriendsRequestEvent() {}
    }

    public static class FacebookFriendsRequestReturnedEvent
    {
        public FacebookFriendsRequestReturnedEvent() {}
    }

    public static class ConnectivityChangedEvent
    {
        public ConnectivityChangedEvent() {}
    }

    public static class LocationUpdateEvent
    {
        Location location = null;

        public LocationUpdateEvent(Location location) { this.location = location; }

        public Location getLocation()
        {
            return this.location;
        }
    }

    public static class LocationTimedOutEvent
    {
        public LocationTimedOutEvent() { }
    }

    public static class StartVPNEvent
    {
        public StartVPNEvent() {}
    }

    public static class VPNStatusUpdateEvent
    {
        String status = null;

        public VPNStatusUpdateEvent(String status)
        {
            this.status = status;
        }

        public String getStatus()
        {
            return this.status;
        }
    }

    public static class DisconnectFromVPNEvent
    {
        public DisconnectFromVPNEvent() {}
    }

    public static class VPNDisconnectedEvent
    {
        public VPNDisconnectedEvent() {}
    }

    public static class VPNNotSupportedEvent
    {
        public VPNNotSupportedEvent() {}
    }

    public static class DisconnectFromWiFiEvent
    {
        public DisconnectFromWiFiEvent() {}
    }

    public static class WiFiDisconnectedEvent
    {
        public WiFiDisconnectedEvent() {}
    }

    public static class SyncMacBloomFilterEvent
    {
        public SyncMacBloomFilterEvent() {}
    }

    public static class LoadCoordinatesEvent
    {
        public LoadCoordinatesEvent() {}
    }

    public static class InRangeCommunityWiFiEvent
    {
        public InRangeCommunityWiFiEvent() {}
    }

    public static class OutOfRangeCommunityWiFiEvent
    {
        public OutOfRangeCommunityWiFiEvent() {}
    }

    public static class AddCircleEvent
    {
        int type;
        List<APIClient.Coordinate> coords;

        public AddCircleEvent(int type, List<APIClient.Coordinate> coords)
        {
            this.type = type;
            this.coords = coords;
        }

        public int getType() { return type; }
        public List<APIClient.Coordinate> getCoords() { return coords; }
    }

    public static class SyncCoverageMapEvent
    {
        public SyncCoverageMapEvent() {}
    }

    public static class MoveActivityToBackEvent
    {
        public MoveActivityToBackEvent() {}
    }

    public static class StartAPEvent
    {
        public StartAPEvent() {}
    }

    public static class StopAPEvent
    {
        public StopAPEvent() {}
    }

    public static class DisplayFragmentEvent
    {
        int fragment = 0;
        Bundle arguments = null;

        public DisplayFragmentEvent(int fragment)
        {
            this.fragment = fragment;
        }

        public DisplayFragmentEvent(int fragment, Bundle arguments)
        {
            this.fragment = fragment;
            this.arguments = arguments;
        }

        public int getFragment()
        {
            return this.fragment;
        }
        public Bundle getArguments()
        {
            return this.arguments;
        }
    }

    public static class PromptToRateApp
    {
        public PromptToRateApp() {}
    }

    public static class SendCreditWithOperatorEvent
    {
        Integer operator_id;

        public SendCreditWithOperatorEvent(Integer operator_id)
        {
            this.operator_id = operator_id;
        }

        public Integer getOperator()
        {
            return this.operator_id;
        }
    }

    public static class DisplayOperatorDialogEvent
    {
        String title;
        String msg;
        Boolean display_help_button;

        public DisplayOperatorDialogEvent(String title, String msg, Boolean display_help_button)
        {
            this.title = title;
            this.msg = msg;
            this.display_help_button = display_help_button;
        }

        public String getTitle()
        {
            return this.title;
        }
        public String getMsg() { return this.msg; }
        public Boolean getShowHelpButton() { return this.display_help_button; }
    }

    public static class DisplayOKOrHelpDialogEvent
    {
        String title;
        String msg;
        Boolean display_help_button;

        public DisplayOKOrHelpDialogEvent(String title, String msg, Boolean display_help_button)
        {
            this.title = title;
            this.msg = msg;
            this.display_help_button = display_help_button;
        }

        public String getTitle()
        {
            return this.title;
        }
        public String getMsg() { return this.msg; }
        public Boolean getShowHelpButton() { return this.display_help_button; }
    }

    public static class SendRegistrationIdEvent
    {
        public SendRegistrationIdEvent() {}
    }

    public static class ShowFAQEvent
    {
        String id;

        public ShowFAQEvent(String id) { id = id; }

        public String getId()
        {
              return id;
        }
    }

    public static class HelpRequestedEvent
    {
        public HelpRequestedEvent() {}
    }
}
