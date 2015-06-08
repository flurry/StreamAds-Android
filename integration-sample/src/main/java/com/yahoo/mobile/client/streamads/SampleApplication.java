package com.yahoo.mobile.client.streamads;

import android.app.Application;
import android.util.Log;

import com.flurry.android.FlurryAgent;

public class SampleApplication extends Application {

    private static final String FLURRY_API_KEY = "JQVT87W7TGN5W7SWY2FH";
    @Override
    public void onCreate() {
        super.onCreate();
        FlurryAgent.setLogLevel(Log.DEBUG);
        FlurryAgent.setLogEnabled(true);
        FlurryAgent.init(this, FLURRY_API_KEY);
    }
}
