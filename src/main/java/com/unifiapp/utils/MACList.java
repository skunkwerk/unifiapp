package com.unifiapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.PowerManager;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import com.segment.analytics.Analytics;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIFactory;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MACList
{
    BloomFilter<String> networks;
    Context context;
    Utilities utils;
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    APIClient.API api;
    long loaded_mac_bloom_filter_count = 0;

    public MACList(Context context, SharedPreferences sharedPrefs)
    {
        try
        {
            this.context = context;
            this.sharedPrefs = sharedPrefs;
            this.editor = sharedPrefs.edit();
            utils = new Utilities(context);
            api = new APIFactory().getAPI();
            Date lastBloomFilterSyncDate = new Date(sharedPrefs.getLong("last_mac_bloom_filter_sync_date",0));
            if(networks==null && lastBloomFilterSyncDate.getTime()!=0)
            {
                Log.d("MAC bloom filter", "loading from file");
                load();
            }
            else if(networks==null)
            {
                createBloomFilter();
            }
        }
        catch(Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     */
    public void createBloomFilter()
    {
        Log.d("MAC bloom filter", "creating empty bloom filter");
        networks = BloomFilter.create(macAddressFunnel, 500000, 0.01);
        editor.putLong("last_mac_bloom_filter_sync_date", 0);//reset this, just in case we're re-creating it because networks was null
        editor.commit();
    }

    /**
     *
     */
    Funnel<String> macAddressFunnel = new Funnel<String>()
    {
        @Override
        public void funnel(String mac_address, PrimitiveSink primitiveSink)
        {
            if(mac_address==null)
                Log.e("maclist","mac is null!");
            else
                primitiveSink.putUnencodedChars(mac_address.toUpperCase());
        }
    };

    /**
     *
     */
    public void syncWithServer()
    {
        try
        {
            Log.d("MAC bloom filter", "syncWithServer");
            //if it's been more than 6 hours, do the sync
            //otherwise, don't sync
            //only do the sync if the phone's screen is off (ie in background)
            long now = System.currentTimeMillis()/1000;
            long last_sync_time = sharedPrefs.getLong("last_mac_bloom_filter_sync_date",0);
            long difference = now - last_sync_time;
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();
            if(difference >= 600)// && isScreenOn==false)
            {
                if(networks==null)
                {
                    Analytics.with(context).track("Tried to sync with server, but MAC bloom filter null - recreating");
                    createBloomFilter();
                }
                Log.d("MAC bloom filter", "syncWithServer running");
                api.mac_list("mumbai", sharedPrefs.getLong("last_mac_bloom_filter_sync_date",0), callback);//TODO: mumbai
            }
            else
            {
                Log.d("MAC bloom filter", "syncWithServer delayed - 10 minutes since last sync haven't elapsed");
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     */
    public void save()
    {
        try
        {
            //we don't need to update the mac_bloom_filter_count, as it was already updated when we added to it
            Log.d("MAC bloom filter", "saving");
            if(networks==null)
            {
                Analytics.with(context).track("Tried to save MAC bloom filter, but is null - recreating");
                createBloomFilter();
            }
            FileOutputStream fileOutputStream = context.openFileOutput("networks.bloomfilter",Context.MODE_PRIVATE);//stores into context.getFilesDir()
            networks.writeTo(fileOutputStream);
            fileOutputStream.close();
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    /**
     *
     */
    public void load()
    {
        try
        {
            Log.d("MAC bloom filter", "loading");
            FileInputStream fileInputStream = context.openFileInput("networks.bloomfilter");//from context.getFilesDir()
            BloomFilter<String> bloomFilter = BloomFilter.readFrom(fileInputStream, macAddressFunnel);
            fileInputStream.close();
            networks = bloomFilter;
            loaded_mac_bloom_filter_count = sharedPrefs.getLong("mac_bloom_filter_count",0);
            if(networks==null)
                utils.logMessage("MACList", "loaded MAC bloom filter is null!");
            if(networks.mightContain("94:44:52:3E:F1:C5") && networks.mightContain("00:15:E9:E2:67:AB"))
            {
                //utils.logMessage("Passed MAC bloom filter smoke test","MACList");
            }
            else
            {
                utils.logMessage("Failed MAC bloom filter smoke test","MACList");
            }
        }
        catch (FileNotFoundException | EOFException | ArrayIndexOutOfBoundsException e)
        {
            utils.logMessage("MACList","couldn't load MAC list - resetting & syncing with server");
            createBloomFilter();
            syncWithServer();
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
    }

    public Boolean lookupNetwork(String mac_address)
    {
        Boolean result = false;
        try
        {
            Log.d("MAC bloom filter", "looking up network");
            if(networks==null)
                Log.e("mac","networks is null in lookupNetwork!");
            if(mac_address==null)
                Log.e("mac","mac_address is null in lookupNetwork!");
            if (networks!=null && mac_address!=null && networks.mightContain(mac_address))
                result = true;
            else
                result = false;
            if(sharedPrefs.getLong("mac_bloom_filter_count",0)<=1)
            {
                Log.d("mac","<=1 macs in bloom filter - syncing");
                syncWithServer();
            }
        }
        catch (Exception e)
        {
            utils.logException(e);
        }
        return result;
    }

    Callback<APIClient.MacList> callback = new Callback<APIClient.MacList>()
    {
        @Override
        public void failure(RetrofitError error)
        {
            utils.logRetrofitError(error, "MACList", "callback");
        }
        @Override
        public void success(APIClient.MacList result, Response arg1)
        {
            try
            {
                Log.d("MAC bloom filter", "putting new data from server API call");
                List<String> mac_list = result.getMacList();
                int counter=0;
                for(String mac:mac_list)
                {
                    networks.put(mac);
                    counter+=1;
                }
                Log.d("MAC bloom filter", "loaded:" + String.valueOf(counter) + "networks into bloom filter");
                loaded_mac_bloom_filter_count += counter;
                editor.putLong("mac_bloom_filter_count",loaded_mac_bloom_filter_count);
                editor.putLong("last_mac_bloom_filter_sync_date",result.getSyncDate());
                editor.commit();
                save();//save so that we're always in a consistent state
            }
            catch(Exception e)
            {
                utils.logException(e);
            }
        }
    };
}
