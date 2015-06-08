package com.yahoo.mobile.client.streamads;

import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.yahoo.mobile.client.streamads.sample.R;

import java.io.File;
import java.io.IOException;

public class SampleListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_pane);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            FragmentManager.enableDebugLogging(true);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, SampleListFragment.newInstance(), SampleListFragment.TAG)
                    .commit();
        }

        // http response cache
        File httpCacheDir = new File(getCacheDir(), "http");
        long httpCacheSize = 100 * 1024 * 1024; // 100 MiB

        try {
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i(SampleListActivity.class.getSimpleName(), "HTTP response cache installation failed:" + e);
        }
    }
}
