package com.unifiapp.model;

import java.util.Date;
import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

public class APIClient
{
    public class WifiRouters
    {
        private List<WifiRouter> resources;

        public List<WifiRouter> getRouters()
        {
            return resources;
        }
    }

    public static class WifiRouter
    {
        public String ssid;
        public String bssid;
        public String password;
        public String authentication_algorithm;
        public float latitude;
        public float longitude;
        public float altitude;
        public String public_ip_address;
    }

    public static class WifiRouterAccess
    {
        public String ssid;
        public String bssid;
        public String password;
        public String authentication_algorithm;

        public WifiRouterAccess(String ssid, String bssid, String password, String authentication_algorithm) {
            this.ssid = ssid;
            this.bssid = bssid;
            this.password = password;
            this.authentication_algorithm = authentication_algorithm;
        }
    }

    public static class OfflineNetworkAccess
    {
        private List<WifiRouterAccess> list;

        public List<WifiRouterAccess> getList()
        {
            return list;
        }

        private long sync_date;

        public long getSyncDate() { return sync_date; }
    }

    public class Coordinates
    {
        private List<Coordinate> coordinates;

        private long sync_date;

        public long getSyncDate() { return sync_date; }

        public List<Coordinate> getCoordinates()
        {
            return coordinates;
        }
    }

    public class Coordinate
    {
        public String ssid;
        public String mac_address;
        public float latitude;
        public float longitude;
    }

    public static class OauthUser
    {

        public String userId;

        public OauthUser(String userId) {
            this.userId = userId;
        }
    }

    public class Response
    {
        public Boolean status;
    }

    public class Version
    {
        public Integer version;
    }

    public class Count
    {
        public Integer count;
    }

    public class Operator
    {
        public Integer operator_id;
    }

    public class AccessType
    {
        public String accessType;
    }

    public class Customer
    {
        public int id;
        public String first_name;
        public String last_name;
        public String email_address;
        public String phone_number;
        public String oauth_provider;
        public String oauth_user_id;
        public String push_registration_id;
        public int app_version;
        public String signup_date;
    }

    public class CustomerPoints
    {
        private List<CustomerLeaderboard> leaderboard;
        public Points your_points;

        public List<CustomerLeaderboard> getCustomers()
        {
            return leaderboard;
        }
    }

    public class FriendsPoints
    {
        private List<CustomerLeaderboard> leaderboard;
        public List<CustomerLeaderboard> getCustomers()
        {
            return leaderboard;
        }
    }

    public class MacList
    {
        private List<String> mac_list;
        private long sync_date;
        public List<String> getMacList()
        {
            return mac_list;
        }
        public long getSyncDate() { return sync_date; }
    }

    public class Points
    {
        public int month_points;
        public int all_time_points;
    }

    public class ConnectionStats
    {
        public int thanks_received;
        public int connections_made;
        public int minutes_used;
    }

    public static class CustomerLeaderboard
    {
        public int customer_id;
        public String first_name;
        public String oauth_user_id;
        public int total_points;
        public int all_time_points;

        public CustomerLeaderboard(int customer_id, String first_name, String oauth_user_id, int total_points, int all_time_points)
        {
            this.customer_id = customer_id;
            this.first_name = first_name;
            this.oauth_user_id = oauth_user_id;
            this.total_points = total_points;
            this.all_time_points = all_time_points;
        }
    }

    public static class NearestWiFiRouter
    {
        public String ssid;
        public String mac_address;
        public float latitude;
        public float longitude;
        public float distance;
    }

    public static class WiFiNetwork
    {
        public String ssid;
        public String mac_address;
        public int frequency;
        public float latitude;
        public float longitude;
        public float altitude;

        public WiFiNetwork(String ssid, String mac_address, int frequency, float latitude, float longitude, float altitude)
        {
            this.ssid = ssid;
            this.mac_address = mac_address;
            this.frequency = frequency;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }
    }

    public static class DataUsage
    {
        public float data_uploaded_mobile;
        public float data_downloaded_mobile;
        public float data_uploaded_wifi;
        public float data_downloaded_wifi;
        public Date datetime;
        public float latitude;
        public float longitude;
        public float altitude;
        public String anonymous_customer_id_hash;

        public DataUsage(float data_uploaded_mobile, float data_downloaded_mobile, float data_uploaded_wifi, float data_downloaded_wifi, Date datetime, float latitude, float longitude, float altitude, String anonymous_customer_id_hash)
        {
            this.data_uploaded_mobile = data_uploaded_mobile;
            this.data_downloaded_mobile = data_downloaded_mobile;
            this.data_uploaded_wifi = data_uploaded_wifi;
            this.data_downloaded_wifi = data_downloaded_wifi;
            this.datetime = datetime;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.anonymous_customer_id_hash = anonymous_customer_id_hash;
        }
    }

    public static class DailyConnectivity
    {
        public float data_connection_active_average;
        public float wifi_on_average;
        public float wifi_connected_average;
        public float mobile_data_connected_average;
        public Date date;
        public String anonymous_customer_id_hash;

        public DailyConnectivity(float data_connection_active_average, float wifi_on_average, float wifi_connected_average, float mobile_data_connected_average, Date date, String anonymous_customer_id_hash)
        {
            this.data_connection_active_average = data_connection_active_average;
            this.wifi_on_average = wifi_on_average;
            this.wifi_connected_average = wifi_connected_average;
            this.mobile_data_connected_average = mobile_data_connected_average;
            this.date = date;
            this.anonymous_customer_id_hash = anonymous_customer_id_hash;
        }
    }

    public class Empty
    {

    }

    //A method with a return type will be executed synchronously
    public interface API
    {
        @GET("/duplicate_router_check")
        void duplicate_router_check(
                @Query("mac_address") String mac_address,
                @Query("public_ip_address") String public_ip_address,
                Callback<Response> cb
        );
        @Headers("Cache-Control: max-age=86400")//cache for 1 day
        @GET("/latest_version")
        void latest_version(
                Callback<Version> cb
        );
        @GET("/count_routers")
        void count_routers(
                Callback<Count> cb
        );
        @Headers("Cache-Control: max-age=3600")//cache for 1 hour
        @FormUrlEncoded
        @POST("/router_locations_sync")
        void router_locations_sync(
                @Field("sync_date") Long sync_date,
                Callback<Coordinates> cb
        );
        @Headers("Cache-Control: max-age=3600")//cache for 1 hour
        @GET("/customer_points/{city}/{customer_id}")
        void customer_points(
                @Path("city") String city,
                @Path("customer_id") int customer_id,
                Callback<CustomerPoints> cb
        );
        @Headers("Cache-Control: max-age=3600")//cache for 1 hour
        @POST("/friends_points")
        void friends_points(
                @Body List<OauthUser> friends,
                Callback<FriendsPoints> cb
        );
        @POST("/wifi_scan_results")
        void wifi_scan_results(
                @Body List<WiFiNetwork> scans,
                Callback<Response> cb
        );
        @FormUrlEncoded
        @POST("/customer_router_count")
        Count customer_router_count(
                @Field("customer_id") int customer_id
        );
        @FormUrlEncoded
        @POST("/customer_routers")
        void customer_routers(
                @Field("customer_id") int customer_id,
                Callback<WifiRouters> cb
        );
        @POST("/daily_connectivity_percentages")
        void daily_connectivity_percentages(
                @Body List<DailyConnectivity> percentages,
                Callback<Response> cb
        );
        @POST("/password_check_results")
        void password_check_results(
                @Field("mac_address") String mac_address,
                @Field("success") Boolean success,
                Callback<Response> cb
        );
        @POST("/data_usage_statistics")
        void data_usage_statistics(
                @Body List<DataUsage> statistics,
                Callback<Response> cb
        );
        @GET("/bootstrap_active/{city}")
        void city_bootstrap_active(
                @Path("city") String city,
                Callback<Response> cb
        );
        @Headers("Cache-Control: max-age=86400")//cache for 1 day
        @GET("/account_access/{customer_id}")
        void account_access(
                @Path("customer_id") int customer_id,
                Callback<AccessType> cb
        );
        @Headers("Cache-Control: max-age=86400")//cache for 1 day
        @GET("/connection_stats/{customer_id}")
        void connection_stats(
                @Path("customer_id") int customer_id,
                Callback<ConnectionStats> cb
        );
        @FormUrlEncoded
        @POST("/unable_to_connect_to_network")
        void unable_to_connect(
                @Field("mac_address") String mac_address,
                @Field("customer_id") int customer_id,
                Callback<Response> cb
        );
        @FormUrlEncoded
        @POST("/offline_network_access")
        void offline_network_access(
                @Field("city") String city,
                @Field("sync_date") Long sync_date,
                Callback<OfflineNetworkAccess> cb
        );
        @FormUrlEncoded
        @POST("/mac_list")
        void mac_list(
                @Field("city") String city,
                @Field("sync_date") Long sync_date,
                Callback<MacList> cb
        );
        @FormUrlEncoded
        @POST("/router_access")
        void router_access(
                @Field("mac_address") String mac_address,
                Callback<WifiRouterAccess> cb
        );
        @FormUrlEncoded
        @POST("/nearest_network")
        void nearest_network(
                @Field("latitude") double latitude,
                @Field("longitude") double longitude,
                Callback<NearestWiFiRouter> cb
        );
        @FormUrlEncoded
        @POST("/nearest_network")
        void nearest_network_with_sync_date(
                @Field("latitude") double latitude,
                @Field("longitude") double longitude,
                @Field("sync_date") String sync_date,
                Callback<NearestWiFiRouter> cb
        );
        @FormUrlEncoded
        @POST("/register_new_router")
        void new_router(
                @Field("ssid") String ssid,
                @Field("password") String password,
                @Field("mac_address") String mac_address,
                @Field("authentication_algorithm") String authentication_algorithm,
                @Field("customer_id") int customer_id,
                @Field("latitude") double latitude,
                @Field("longitude") double longitude,
                @Field("altitude") double altitude,
                @Field("date_added") String date_added,
                @Field("public_ip_address") String public_ip_address,
                @Field("link_speed") int link_speed,
                @Field("isp") String current_isp,
                Callback<WifiRouter> cb
        );
        @FormUrlEncoded
        @POST("/update_router_password")
        void update_router_password(
                @Field("ssid") String ssid,
                @Field("password") String password,
                @Field("mac_address") String mac_address,
                @Field("authentication_algorithm") String authentication_algorithm,
                @Field("latitude") double latitude,
                @Field("longitude") double longitude,
                @Field("altitude") double altitude,
                @Field("public_ip_address") String public_ip_address,
                @Field("link_speed") int link_speed,
                @Field("isp") String current_isp,
                Callback<Response> cb
        );
        @FormUrlEncoded
        @POST("/get_operator_for_number")
        void get_operator_for_number(
                @Field("phone_number") String phone_number,
                Callback<Operator> reply
        );
        @FormUrlEncoded
        @POST("/notify_customer_of_referral_install")
        void notify_customer_of_referral_install(
                @Field("referring_customer_id") int referring_customer_id,
                @Field("referred_customer_name") String referred_customer_name,
                @Field("referred_customer_id") int referred_customer_id,
                Callback<Response> cb
        );
        @FormUrlEncoded
        @POST("/send_prepaid_credit_with_operator")
        void send_credit_with_operator(
                @Field("phone_number") String phone_number,
                @Field("customer_id") int customer_id,
                @Field("operator_id") Integer operator_id,
                @Field("version") String version,
                @Field("debug") String debug,
                Callback<Response> reply
        );
        @FormUrlEncoded
        @POST("/get_or_create_customer")
        void get_or_create_customer(
                @Field("first_name") String first_name,
                @Field("last_name") String last_name,
                @Field("email_address") String email_address,
                @Field("oauth_provider") String oauth_provider,
                @Field("oauth_user_id") String oauth_user_id,
                @Field("android_id") String android_id,
                Callback<Customer> cb
        );
        @FormUrlEncoded
        @POST("/get_or_create_customer")
        void get_or_create_customer(
                @Field("first_name") String first_name,
                @Field("last_name") String last_name,
                @Field("email_address") String email_address,
                @Field("oauth_provider") String oauth_provider,
                @Field("oauth_user_id") String oauth_user_id,
                Callback<Customer> cb
        );
        //would have preferred a PATCH, but Square doesn't support out-of-the-box, see:
        //http://stackoverflow.com/questions/24118929/retrofit-http-patch
        @FormUrlEncoded
        @PUT("/update_customer/{id}")
        void update_customer(
                @Path("id") int id,
                @Field("id") int existing_id,
                @Field("first_name") String first_name,
                @Field("last_name") String last_name,
                @Field("email_address") String email_address,
                @Field("phone_number") String phone_number,
                @Field("signup_date") String signup_date,
                @Field("oauth_provider") String oauth_provider,
                @Field("oauth_user_id") String oauth_user_id,
                @Field("push_registration_id") String push_registration_id,
                @Field("app_version") int app_version,
                Callback<Response> cb
        );
        @FormUrlEncoded
        @POST("/register_new_arn")
        void register_new_arn(
                @Field("customer_id") int customer_id,
                @Field("push_registration_id") String push_registration_id,
                Callback<Response> cb
        );
    }
}