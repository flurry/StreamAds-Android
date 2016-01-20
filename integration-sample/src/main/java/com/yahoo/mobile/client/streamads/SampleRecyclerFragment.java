package com.yahoo.mobile.client.streamads;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.flurry.android.ads.FlurryAdTargeting;

import com.yahoo.mobile.client.streamads.sample.R;
import com.yahoo.mobile.library.streamads.FlurryAdRecyclerAdapter;
import com.yahoo.mobile.library.streamads.NativeAdAdapter;
import com.yahoo.mobile.library.streamads.NativeAdViewBinder;
import com.yahoo.mobile.library.streamads.StubFlurryAdNativeListener;
import com.yahoo.mobile.library.streamads.positioning.LinearIntervalAdPositioner;

public class SampleRecyclerFragment extends Fragment {
    public static final String TAG = SampleRecyclerFragment.class.getSimpleName();
    private static final String AD_SPACE = "StaticVideoNativeTest";

    private RecyclerView mRecyclerView;

    public static final SampleRecyclerFragment newInstance() {
        return new SampleRecyclerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false);
        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.recycler_view);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int data[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        SimpleArrayAdapter adapter = new SimpleArrayAdapter(data);

        NativeAdViewBinder.ViewBinderBuilder viewBinderBuilder = new NativeAdViewBinder.ViewBinderBuilder();

        NativeAdViewBinder viewBinder = viewBinderBuilder.setAdLayoutId(R.layout.list_item_ad)
                .setHeadlineTextId(R.id.ad_headline)
                .setDescriptionTextId(R.id.ad_description)
                .setSourceTextId(R.id.ad_source)
                .setBrandingLogoImageId(R.id.sponsored_image)
                .setAppStarRatingImageId(R.id.app_rating_image)
                .setAdImageId(R.id.ad_image)
                .build();

        FlurryAdTargeting flurryAdTargeting = new FlurryAdTargeting();
        flurryAdTargeting.setEnableTestAds(true);

        FlurryAdNativeListener adStateListener = new StubFlurryAdNativeListener() {
            @Override
            public void onError(FlurryAdNative flurryAdNative,
                                FlurryAdErrorType flurryAdErrorType, int errorCode) {
                Log.e(TAG, "Ad could not load. Error code: " + errorCode);
            }
        };

        FlurryAdRecyclerAdapter adsAdapter = FlurryAdRecyclerAdapter.from(getActivity(), adapter,
                viewBinder, AD_SPACE)
                .setAdPositioner(new LinearIntervalAdPositioner(3, 4))
                .setTargeting(flurryAdTargeting)
                .setFlurryAdNativeListener(adStateListener)
                .setAutoDestroy(false)
                .build();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(adsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        ((FlurryAdRecyclerAdapter)mRecyclerView.getAdapter()).refreshAds();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (getActivity().isFinishing()) {
            ((FlurryAdRecyclerAdapter) mRecyclerView.getAdapter()).destroyAds();
        }
    }

    final class SimpleArrayAdapter extends RecyclerView.Adapter<SimpleViewHolder> {
        int array[];

        SimpleArrayAdapter(@NonNull int[] array) {
            this.array = array;
        }

        @Override
        public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.list_item_sample, parent, false);
            return new SimpleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SimpleViewHolder holder, int position) {
            holder.textView.setText(String.valueOf(array[position]));
        }

        @Override
        public int getItemCount() {
            return array.length;
        }
    }

    final class SimpleViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        SimpleViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.sample_data_text);
        }
    }
}
