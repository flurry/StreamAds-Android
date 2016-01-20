package com.yahoo.mobile.client.streamads;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeListener;

import com.yahoo.mobile.client.streamads.sample.R;
import com.yahoo.mobile.library.streamads.NativeAdAdapter;
import com.yahoo.mobile.library.streamads.StubFlurryAdNativeListener;
import com.yahoo.mobile.library.streamads.positioning.LinearIntervalAdPositioner;
import com.yahoo.mobile.library.streamads.FlurryAdListAdapter;
import com.yahoo.mobile.library.streamads.NativeAdViewBinder;

public class SampleListFragment extends ListFragment implements NativeAdAdapter.NativeAdRenderListener {
    public static final String TAG = SampleListFragment.class.getSimpleName();
    private static final String AD_SPACE = "StaticVideoNativeTest";

    public static final SampleListFragment newInstance() {
        return new SampleListFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Integer data[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        BaseAdapter dataAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_sample,
                R.id.sample_data_text, data);
        NativeAdViewBinder.ViewBinderBuilder viewBinderBuilder = new NativeAdViewBinder.ViewBinderBuilder();

        NativeAdViewBinder viewBinder = viewBinderBuilder.setAdLayoutId(R.layout.list_item_ad)
                .setHeadlineTextId(R.id.ad_headline)
                .setDescriptionTextId(R.id.ad_description)
                .setSourceTextId(R.id.ad_source)
                .setBrandingLogoImageId(R.id.sponsored_image)
                .setAppStarRatingImageId(R.id.app_rating_image)
                .setAdImageId(R.id.ad_image)
                .setCallToActionViewId(R.id.ad_cta_btn)
                .setAdCollapseViewId(R.id.ad_collapse_btn)
                .build();

        FlurryAdNativeListener adStateListener = new StubFlurryAdNativeListener() {
            @Override
            public void onError(FlurryAdNative flurryAdNative,
                                FlurryAdErrorType flurryAdErrorType, int errorCode) {
                Log.e(TAG, "Ad could not load. Error code: " + errorCode);
            }
        };

        FlurryAdListAdapter adListAdapter = FlurryAdListAdapter
                .from(getActivity(), dataAdapter,  viewBinder, AD_SPACE)
                .setAdPositioner(new LinearIntervalAdPositioner(3, 4))
                .setFlurryAdNativeListener(adStateListener)
                .setAutoDestroy(false)
                .setExpandableAdMode(NativeAdAdapter.EXPANDABLE_AD_MODE_COLLAPSED)
                .build();

        adListAdapter.addAdRenderListener(this);
        adListAdapter.setRetryFailedAdPositions(false);

        setListAdapter(adListAdapter);

        ((FlurryAdListAdapter)getListAdapter()).refreshAds();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (getActivity().isFinishing()) {
            ((FlurryAdListAdapter) getListAdapter()).destroyAds();
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Object listItem = listView.getItemAtPosition(position);

        Toast.makeText(getActivity(), "Item clicked: " + listItem, Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback method triggered when the adapter successfully renders an ad unto a View
     * in position.
     *
     * @param position the position that the ad was loaded into
     */
    @Override
    public void onAdRendered(int position) {
        Log.i(TAG, "Ad rendered at position " + position);
    }

    /**
     * Callback method triggered when the adapter tries to render an ad unto a View in position,
     * but the ad is not ready or available.
     *
     * @param position the position that the ad failed to load into
     */
    @Override
    public void onAdRenderFailed(int position) {
        Log.w(TAG, "Ad render failed at position " + position);
    }
}
