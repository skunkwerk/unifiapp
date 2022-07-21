package com.unifiapp.view;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.view.ViewPager.PageTransformer;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.unifiapp.events.Events.DisplayFragmentEvent;
import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.squareup.picasso.Picasso;
import com.unifiapp.R;
import com.unifiapp.adapter.LayoutAdapter;
import com.unifiapp.events.Events;
import com.unifiapp.model.APIClient;
import com.unifiapp.model.APIFactory;
import com.unifiapp.utils.Utilities;
import com.viewpagerindicator.CirclePageIndicator;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PreShareFragment extends Fragment
{
    private BootstrapEditText phone_number_field;
    private ViewPager view_pager;
    LayoutAdapter adapter;
    CirclePageIndicator indicator;
    private Handler handler;
    private BootstrapButton next_button;
    private View placeholder;
    Boolean content_visible = false;
    SharedPreferences sharedPref;
    int current_page;
    int router_count;
    LayoutInflater root_inflater;
    ViewGroup root_container;
    APIClient.API api;
    Context context;
    Utilities utils;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        context = getActivity().getApplicationContext();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        utils = new Utilities(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.preshare_fragment, container, false);
        root_inflater = inflater;
        root_container = container;
        phone_number_field = (BootstrapEditText) rootView.findViewById(R.id.phone_number);
        next_button = (BootstrapButton) rootView.findViewById(R.id.preshare_next);
        placeholder = (View) rootView.findViewById(R.id.placeholder);

        view_pager = (ViewPager) rootView.findViewById(R.id.pager);
        adapter = new LayoutAdapter();
        view_pager.setAdapter(adapter); // Binds the Adapter to the ViewPager
        view_pager.setOffscreenPageLimit(1);//we can't set it to 0, so we can't re-create the pages every time to add the images later after the transition is done

        //cross-fade between the pages background colors
        view_pager.setPageTransformer(false, new PageTransformer()
        {
            @Override
            public void transformPage(View view, float position)
            {
                int pageWidth = view.getWidth();
                View backgroundView = view.findViewById(R.id.walkthrough_background);
                View contentView = view.findViewById(R.id.content_area);
                if (position < -1)
                { // [-Infinity,-1)
                    // This page is way off-screen to the left
                }
                else if (position <= 0)
                { // [-1,0]
                    // This page is moving out to the left

                    // Counteract the default swipe
                    view.setTranslationX(pageWidth * -position);
                    if (contentView != null) {
                        // But swipe the contentView
                        contentView.setTranslationX(pageWidth * position);
                    }
                    if (backgroundView != null) {
                        // Fade the image in
                        backgroundView.setAlpha(1 + position);
                    }
                }
                else if (position <= 1)
                { // (0,1]
                    // This page is moving in from the right

                    // Counteract the default swipe
                    view.setTranslationX(pageWidth * -position);
                    if (contentView != null) {
                        // But swipe the contentView
                        contentView.setTranslationX(pageWidth * position);
                    }
                    if (backgroundView != null) {
                        // Fade the image out
                        backgroundView.setAlpha(1 - position);
                    }
                }
                else
                { // (1,+Infinity]
                    // This page is way off-screen to the right
                }
            }
        });

        // ViewPager Indicator
        indicator = (CirclePageIndicator) rootView.findViewById(R.id.indicator);
        indicator.setViewPager(view_pager);

        api = new APIFactory().getAPI();
        handler = new Handler();

        indicator.setOnPageChangeListener(new OnPageChangeListener()
        {
            @Override
            public void onPageSelected(final int selected_page)//get called after completely moved to this page
            {
                Log.d("splash on page (0-indexed):", String.valueOf(selected_page));
                Analytics.with(getActivity()).track("User swiped through preshare page");
                current_page = selected_page;
                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d("preshare","requesting show_view from new page selected");
                        show_view(selected_page);
                    }
                }, 400);//delay by .4 seconds

            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                Log.d("moving page position:", String.valueOf(position) + " offset:" + String.valueOf(positionOffset));
                if(position == current_page)
                {
                    // We are moving to next screen on right side
                    if (positionOffset > 0.5)
                    {
                        if(current_page == 0)
                        {
                            //view_pager.setCurrentItem(1, true);//force it to go to the next page, with smooth scroll - still 'JUMPS' though
                            placeholder.setVisibility(View.GONE);//this allows the mouse event to continue happening, but for the view to be hidden
                            content_visible = false;
                        }
                        else
                        {
                            ((ViewGroup) placeholder).removeAllViews();
                            Log.d("preshare","removing views");
                            content_visible = false;
                        }
                        // Closer to next screen than to current
                        //or what if you don't remove it, but just set visible to false - so that continues to issue scroll events?

                    }
                    else
                    {
                        // Closer to current screen than to next
                        if(content_visible==false)
                        {
                            //we scrolled past the 0.5 level (removing the content), then came back
                            //so we need to re-display the content now
                            Log.d("preshare","requesting show_view" + String.valueOf(positionOffset));
                            show_view(current_page);
                        }
                    }
                }
                else
                {
                    // We are moving to next screen left side
                    if (positionOffset < 0.5)
                    {
                        // Closer to next screen than to current
                        ((ViewGroup) placeholder).removeAllViews();
                        Log.d("preshare","removing views");
                        content_visible = false;
                    }
                    else
                    {
                        // Closer to current screen than to next
                        if(content_visible==false)
                        {
                            //we scrolled past the 0.5 level (removing the content), then came back
                            //so we need to re-display the content now
                            /*Log.d("preshare","requesting show_view" + String.valueOf(positionOffset));
                            show_view(current_page);*/
                        }
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int arg0) { }
        });

        View.OnClickListener nextButtonListener = new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //verify that their phone number is 10 digits long, and only contains digits
                String temp_number = phone_number_field.getText().toString();
                if (temp_number.length() != 10)
                {
                    Events.DisplayOKOrHelpDialogEvent dialogEvent = new Events.DisplayOKOrHelpDialogEvent(getResources().getString(R.string.incorrect_number_dialog_title), getResources().getString(R.string.incorrect_number_dialog_text), false);
                    EventBus.getDefault().post(dialogEvent);
                }
                else
                {
                    //add the country code prefix to the phone number
                    //TODO: mumbai-only
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    SharedPreferences.Editor editor = sharedPref.edit();
                    temp_number = "+91" + temp_number;
                    editor.putString("phone_number", temp_number);
                    editor.commit();

                    Analytics.with(getActivity()).track("User typed in their phone #", new Properties().putValue("phone_number", temp_number));

                    //update the database with the customer's phone number
                    if(sharedPref.getInt("customer_id",0)!=0 && sharedPref.getString("first_name","").equals("")==false && sharedPref.getString("last_name","").equals("")==false)
                    {
                        api.update_customer(sharedPref.getInt("customer_id",0),sharedPref.getInt("customer_id",0),sharedPref.getString("first_name",""),sharedPref.getString("last_name",""),sharedPref.getString("email_address",""),temp_number,sharedPref.getString("signup_date",""),"facebook",sharedPref.getString("facebook_user_id",""),sharedPref.getString("registration_id",""),sharedPref.getInt("app_version",0),update_customer_callback);
                    }

                    Events.DisplayFragmentEvent displayFragmentEvent = new DisplayFragmentEvent(7);
                    EventBus.getDefault().post(displayFragmentEvent);
                }
            }
        };

        next_button.setOnClickListener(nextButtonListener);
        router_count = 7761;//default, until we get a response from the server on the latest count
        api.count_routers(progress_callback);
        show_view(0);//for Android 4.0, otherwise won't display the webview initially
        api.city_bootstrap_active("mumbai",city_bootstrap_callback);//TODO: mumbai

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("seen_share_wifi", true);
        editor.commit();

        return rootView;
    }

    public void show_view(int selected_page)
    {
        if(content_visible==false)
        {
            //find the existing views & resources, and remove them all or set them to null so they can be garbage collected
            //otherwise, we'll get an OutOfMemory exception on some devices
            ((ViewGroup) placeholder).removeAllViews();
            placeholder.setVisibility(View.VISIBLE);
            if(selected_page==0)
            {
                View walkthrough_1_view = root_inflater.inflate(R.layout.walkthrough_1_content, root_container, false);
                ((ViewGroup) placeholder).addView(walkthrough_1_view);
                setup_webview(placeholder);
            }
            else if(selected_page==1)
            {
                View walkthrough_2_view = root_inflater.inflate(R.layout.walkthrough_2_content, root_container, false);
                //because the WiFi icons are added on-the-fly to the view which will get removed (but not deleted) when clearing,
                // the next time we swipe back to this screen we need to make sure they're deleted
                //from the parent view, so they can be added again with animation
                //this code deletes, but for some reason it gets added again without delay
                FrameLayout walkthrough_2_layout = (FrameLayout) walkthrough_2_view.findViewById(R.id.skyline_layout);
                final ImageView skyline = (ImageView) walkthrough_2_layout.findViewById(R.id.skyline);

                View wifi_icon_1 = walkthrough_2_layout.findViewById(R.id.wifi_icon_1);
                if(wifi_icon_1!=null)
                {
                    ((ViewManager) walkthrough_2_layout).removeView(wifi_icon_1);
                }
                View wifi_icon_2 = walkthrough_2_layout.findViewById(R.id.wifi_icon_2);
                if(wifi_icon_1!=null)
                {
                    ((ViewManager) walkthrough_2_layout).removeView(wifi_icon_2);
                }
                View wifi_icon_3 = walkthrough_2_layout.findViewById(R.id.wifi_icon_3);
                if(wifi_icon_1!=null)
                {
                    ((ViewManager) walkthrough_2_layout).removeView(wifi_icon_3);
                }
                ((ViewGroup) placeholder).addView(walkthrough_2_view);
                final FrameLayout walkthrough_layout = (FrameLayout) placeholder.findViewById(R.id.skyline_layout);
                final PathView path_view = (PathView) placeholder.findViewById(R.id.path);
                Boolean temp_landscape = false;
                try
                {
                    //to avoid null pointer exception in rare cases
                    Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    int rotation = display.getRotation();
                    if(rotation==90 || rotation==270)
                    {
                        temp_landscape = true;
                    }
                    else
                    {
                        temp_landscape = false;
                    }
                }
                catch(Exception e)
                {
                    utils.logException(e);
                }
                final Boolean landscape = temp_landscape;

                //we need to get the dimensions of the image dynamically at run-time
                Picasso.with(getActivity()).load(R.drawable.skyline_small_compressed).fit().into(skyline, new com.squareup.picasso.Callback()
                {
                    @Override
                    public void onSuccess()
                    {
                        Log.d("preShare","skyline loaded!");
                        Path path = generatePath(skyline);
                        if(path_view!=null)
                            path_view.init(path);
                    }

                    @Override
                    public void onError()
                    {
                        Log.d("preShare","skyline loading error");
                    }
                });//will resize the image to fit the ImageView

                //TODO: replace these with a single call to runwithfixeddelay, that stops after the last one.  use a global var to keep state
                //start the animations
                skyline.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d("preshare","requesting showing of first wifi_icon");
                        try
                        {
                            addWiFiIconToView(0.18f,0.66f,skyline,R.id.wifi_icon_1,walkthrough_layout,landscape);
                        }
                        catch (Exception e)
                        {
                            Log.d("preshare","wifi icon no longer visible to show");
                        }
                    }
                }, 1000);//delay by 1 seconds
                skyline.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d("preshare","requesting showing of second wifi_icon");
                        try
                        {
                            addWiFiIconToView(0.62f,0.60f,skyline,R.id.wifi_icon_2,walkthrough_layout,landscape);
                        }
                        catch (Exception e)
                        {
                            Log.d("preshare","wifi icon no longer visible to show");
                        }
                    }
                }, 2000);//delay by 2 seconds
                skyline.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d("preshare","requesting showing of third wifi_icon");
                        try
                        {
                            addWiFiIconToView(0.85f,0.38f,skyline,R.id.wifi_icon_3,walkthrough_layout,landscape);
                        }
                        catch (Exception e)
                        {
                            Log.d("preshare","wifi icon no longer visible to show");
                        }
                    }
                }, 3000);//delay by 3 seconds
            }
            else if(selected_page==2)
            {
                View walkthrough_3_view = root_inflater.inflate(R.layout.walkthrough_3_content, root_container, false);
                ImageView secured_image = (ImageView) walkthrough_3_view.findViewById(R.id.norton_secured);
                Picasso.with(getActivity()).load(R.drawable.norton_secured_compressed).fit().into(secured_image);//will resize the image to fit the ImageView
                TextView stop_image = (TextView) walkthrough_3_view.findViewById(R.id.stop_sign);
                Typeface fontFamily = Typeface.createFromAsset(getActivity().getAssets(), "fontawesome-webfont.ttf");
                stop_image.setTypeface(fontFamily);
                stop_image.setText("\uf05e");
                ((ViewGroup) placeholder).addView(walkthrough_3_view);
            }
        }
        else
        {
            Log.d("preshare", "request to show view when already visible! - nothing done");
        }
        content_visible = true;
    }

    public void setup_webview(View view)
    {
        WebView web_view = (WebView) view.findViewById(R.id.walkthrough_webview);
        web_view.getSettings().setJavaScriptEnabled(true);
        web_view.addJavascriptInterface(new WebAppInterface(this.getActivity()), "Android");
        web_view.loadUrl("file:///android_asset/intro_screens.html");
        web_view.setHorizontalScrollBarEnabled(false);
        web_view.setBackgroundColor(0x00000000);//transparent background
        web_view.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);//turn off GPU acceleration to get transparent background on Android 4.0
        web_view.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                Log.d("webview", "scroll");
                try
                {
                    view_pager.dispatchTouchEvent(event);//THIS IS THE KEY!  otherwise, the webview keeps the scroll event, and the view pager doesn't swipe
                    return (event.getAction() == MotionEvent.ACTION_MOVE);
                }
                catch (Exception e)
                {
                    Log.d("preshare", "error dispatching touch event to view pager!");//rarely happens due to pointerIndex out of range, even though tested with 2 fingers as well
                    Crashlytics.log(Log.ERROR, "PreShare", "error dispatching touch event to view pager!");
                    Crashlytics.logException(e);
                    return false;
                }
            }
        });//to disable scrolling
    }

    @Override
    public void onConfigurationChanged(Configuration myConfig)
    {
        //any time the orientation changes, start the viewpager at the first item, and clear the screen
        super.onConfigurationChanged(myConfig);
        ((ViewGroup) placeholder).removeAllViews();
        view_pager.setCurrentItem(0);
    }

    public Path generatePath(ImageView skyline)
    {
        Path path = new Path();
        try
        {
            int[] offset = getBitmapOffset(skyline,true);

            float image_height = skyline.getMeasuredHeight();//height of imageView
            float image_width = skyline.getMeasuredWidth();//width of imageView
            float original_height = skyline.getDrawable().getIntrinsicHeight();//original height of underlying image
            float original_width = skyline.getDrawable().getIntrinsicWidth();//original width of underlying image

            if (image_height/original_height <= image_width/original_width) image_width = original_width*image_height/original_height;//rescaled width of image within ImageView
            else image_height = original_height*image_width/original_width;//rescaled height of image within ImageView

            float correct_width = image_width;
            float correct_height = image_height;

            float[][] points = { {0.38411458f,0.57569296f}, {0.25260417f,0.63752665f}, {0.44791666f,0.680170575f}, {0.65234375f,0.552238806f}, {0.87890625f,0.5628997868f}, {0.87890625f,0.4104477612f} };
            int counter = 0;
            for (float [] point: points)
            {
                if(counter==0)
                    path.moveTo(point[0] * correct_width + offset[1], point[1] * correct_height + offset[0]);
                else
                    path.lineTo(point[0] * correct_width + offset[1], point[1] * correct_height + offset[0]);
                counter++;
            }
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "PreShare", "could not generate path");
            Crashlytics.logException(e);
        }
        return path;
    }

    public void addWiFiIconToView(float x_coord, float y_coord, ImageView skyline, int id, FrameLayout walkthrough_layout, Boolean landscape)
    {
        try
        {
            if(landscape==false)
            {
                int[] offset = getBitmapOffset(skyline,true);

                float image_height = skyline.getMeasuredHeight();//height of imageView
                float image_width = skyline.getMeasuredWidth();//width of imageView
                float original_height = skyline.getDrawable().getIntrinsicHeight();//original height of underlying image
                float original_width = skyline.getDrawable().getIntrinsicWidth();//original width of underlying image

                if (image_height/original_height <= image_width/original_width) image_width = original_width*image_height/original_height;//rescaled width of image within ImageView
                else image_height = original_height*image_width/original_width;//rescaled height of image within ImageView

                float correct_width = image_width;
                float correct_height = image_height;

                ImageView imgView = new ImageView(getActivity());
                Picasso.with(getActivity()).load(R.drawable.wifi_icon).fit().into(imgView);//will resize the image to fit the ImageView
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                float density = getActivity().getResources().getDisplayMetrics().density;
                params.height = Math.round(32 * density);
                params.width = Math.round(32 * density);
                imgView.setId(id);
                params.setMargins(Math.round(x_coord * correct_width), Math.round(y_coord * correct_height + offset[0]), 0, 0);
                imgView.setVisibility(View.VISIBLE);
                walkthrough_layout.addView(imgView, params);
            }
            else
                Log.d("preShare","not adding WiFi icons - landscape mode");
        }
        catch (Exception e)
        {
            Crashlytics.log(Log.ERROR, "PreShare", "could not add WiFi icon");
            Crashlytics.logException(e);
        }
    }

    public static int[] getBitmapOffset(ImageView img,  Boolean includeLayout)
    {
        int[] offset = new int[2];
        float[] values = new float[9];

        Matrix m = img.getImageMatrix();
        m.getValues(values);

        offset[0] = (int) values[5];
        offset[1] = (int) values[2];

        if (includeLayout)
        {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) img.getLayoutParams();
            int paddingTop = (int) (img.getPaddingTop() );
            int paddingLeft = (int) (img.getPaddingLeft() );

            offset[0] += paddingTop + lp.topMargin;
            offset[1] += paddingLeft + lp.leftMargin;
        }
        return offset;
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Analytics.with(getActivity()).screen("PreShare","");
        Utilities utils = new Utilities(context);
        utils.getPublicIPAddress(sharedPref, getActivity());
    }

    Callback<APIClient.Count> progress_callback = new Callback<APIClient.Count>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(APIClient.Count result, Response response)
        {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("router_count", result.count);
            editor.commit();
        }
    };

    Callback<APIClient.Response> city_bootstrap_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(APIClient.Response result, Response response)
        {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("city_bootstrap_active", result.status);
            editor.commit();
            if(isAdded())
            {
                if(result.status==true)
                {
                    phone_number_field.setHint(getResources().getString(R.string.prepaid_number_field_for_credit));
                }
                else
                {
                    phone_number_field.setHint(getResources().getString(R.string.prepaid_number_field_for_no_credit));
                }
            }
        }
    };

    Callback<APIClient.Response> update_customer_callback = new Callback<APIClient.Response>()
    {
        @Override
        public void failure(RetrofitError arg0)
        {
            Log.d("in REST", "failure!" + arg0);
        }

        @Override
        public void success(APIClient.Response result, Response response)
        {
            Log.d("update_customer response",String.valueOf(result.status));
        }
    };

    public class WebAppInterface
    {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c)
        {
            mContext = c;
        }

        @JavascriptInterface
        public int getCount()
        {
            try
            {
                return sharedPref.getInt("router_count",router_count);
            }
            catch (Exception e)
            {
                Crashlytics.log(Log.ERROR, "PreShare", "failed to send JavaScript the router count");
                Crashlytics.logException(e);
                return 7761;
            }
        }
    }
}