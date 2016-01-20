/*
 * Copyright 2015 Yahoo Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yahoo.mobile.library.streamads;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.flurry.android.ads.FlurryAdTargeting;

import java.lang.ref.WeakReference;

import com.yahoo.mobile.library.streamads.positioning.AdapterAdPositioner;
import com.yahoo.mobile.library.streamads.positioning.LinearIntervalAdPositioner;

/**
 * A {@link BaseAdapter} that wraps another BaseAdapter and shows ads interspersed in data from the
 * wrapped adapter.
 */
public final class FlurryAdListAdapter extends BaseAdapter implements NativeAdAdapter,
        FlurryBaseAdAdapter.ListAdapterDataListener {

    public static final String TAG = FlurryAdListAdapter.class.getSimpleName();
    private static DataSetObserver sAdapterObserver;

    private FlurryBaseAdAdapter mBaseAdAdapter;
    private WeakReference<Context> mContextReference;
    private Adapter mWrappedAdapter;
    private NativeAdViewBinder mViewBinder;
    private SparseArray<Boolean> mPositionExpandedMap;
    private ExpandedAdListener mExpandedAdListener;
    private boolean mAdListenerAttached;

    // Private to prevent external instantiation
    private FlurryAdListAdapter() {
        mBaseAdAdapter = new FlurryBaseAdAdapter(this);
        mPositionExpandedMap = new SparseArray<>();
        mExpandedAdListener = new ExpandedAdListener();
    }

    /**
     * Initializes the {@link FlurryAdListAdapter.Builder} for
     * the {@link FlurryAdListAdapter} with all the mandatory values.
     *
     * @param context the {@link Context} to use. You could use either an {@link Activity} or
     *                {@link android.app.Application} context. Keep in mind that if using
     *                Application context, the theme styling applied to your Activities will NOT
     *                be applied to your ad layout.
     * @param adapter the {@link android.widget.ListAdapter} with data that should be wrapped
     * @param viewBinder the {@link NativeAdViewBinder} to use and build ad views
     * @param adSpaceName an Flurry ad space name to use and generate Flurry ads.
     */
    public static Builder from(@NonNull Context context,
                               @NonNull Adapter adapter,
                               @NonNull NativeAdViewBinder viewBinder,
                               @NonNull String adSpaceName) {
        return new Builder(context, adapter, viewBinder, adSpaceName);
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getCount() {
        return mWrappedAdapter.getCount() > 0 ? mWrappedAdapter.getCount() + getNumberOfAds() : 0;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Object getItem(int position) {
        if (mBaseAdAdapter.canShowAd(position, mWrappedAdapter.getCount())) {
            return mBaseAdAdapter.getAdForPosition(position);
        }
        return mWrappedAdapter.getItem(getOriginalPosition(position));
    }

    /**
     * @inheritDoc
     */
    @Override
    public long getItemId(int position) {
        if (mBaseAdAdapter.canShowAd(position, mWrappedAdapter.getCount())) {
            return System.identityHashCode(mBaseAdAdapter.getAdForPosition(position));
        }
        return mWrappedAdapter.getItemId(getOriginalPosition(position));
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getViewTypeCount() {
        return mWrappedAdapter.getViewTypeCount() + 1;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getItemViewType(int position) {
        if (mBaseAdAdapter.shouldShowAd(position, mWrappedAdapter.getCount())) {
            // View type should be next index in 0-indexed
            return mWrappedAdapter.getViewTypeCount();
        } else {
            return mWrappedAdapter.getItemViewType(getOriginalPosition(position));
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View returnedView;
        final int adViewType = mWrappedAdapter.getViewTypeCount();

        if (getItemViewType(position) == adViewType) {
            int expandableAdMode = mBaseAdAdapter.getExpandableAdMode();

            FlurryAdNative flurryAdNative = mBaseAdAdapter.getAdForPosition(position);
            FlurryAdViewHolder adViewHolder;

            if (flurryAdNative == null) {
                Log.e(TAG, "Should not happen. Flurry ad should not be null at this point");
                return convertView;
            }

            if (convertView == null) {
                convertView = LayoutInflater.from(mContextReference.get()).inflate(
                        mViewBinder.getAdLayoutId(), parent, false
                );

                adViewHolder = FlurryAdViewHolder.newInstance(convertView, mViewBinder,
                        flurryAdNative);

                if (expandableAdMode != EXPANDABLE_AD_MODE_OFF && !mAdListenerAttached) {
                    mBaseAdAdapter.addFlurryAdNativeListener(mExpandedAdListener);
                    mAdListenerAttached = true;
                }
                convertView.setTag(adViewHolder);
            } else {
                adViewHolder = (FlurryAdViewHolder) convertView.getTag();
                // Remove tracking from previous view since view will be recycled
                if (adViewHolder.flurryAdNative != null) {
                    adViewHolder.flurryAdNative.removeTrackingView();
                }
            }

            mExpandedAdListener.setAdViewHolder(adViewHolder);
            mExpandedAdListener.setPosition(position);

            FlurryNativeAdViewBuilder.buildAdIntoViews(flurryAdNative, adViewHolder);

            switch (expandableAdMode) {
                case EXPANDABLE_AD_MODE_OFF:
                    flurryAdNative.setTrackingView(convertView);
                    break;
                case EXPANDABLE_AD_MODE_EXPANDED:
                    if (mPositionExpandedMap.get(position) == null ||
                            mPositionExpandedMap.get(position)) {
                        expandAdView(adViewHolder);
                    } else {
                        collapseAdView(adViewHolder);
                    }
                    break;
                case EXPANDABLE_AD_MODE_COLLAPSED:
                    if (mPositionExpandedMap.get(position) == null ||
                            !mPositionExpandedMap.get(position)) {
                        collapseAdView(adViewHolder);
                    } else {
                        expandAdView(adViewHolder);
                    }
                    break;
            }

            mBaseAdAdapter.notifyAdRendered(position);

            returnedView = convertView;
        } else {
            returnedView = mWrappedAdapter.getView(getOriginalPosition(position),
                    convertView, parent);
        }

        return returnedView;
    }

    /**
     * Refreshes ads with a new Flurry ad space.
     *
     * @param adSpaceName the new Flurry ad space to use
     * @see FlurryAdListAdapter#refreshAds()
     */
    public void refreshAds(String adSpaceName) {
        mBaseAdAdapter.refreshAds(adSpaceName);
    }

    /**
     * @inheritDoc
     *
     * <p>Refreshes ads using the already set Flurry ad space.</p>
     */
    @Override
    public void refreshAds() {
        mBaseAdAdapter.refreshAds();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void destroyAds() {
        mBaseAdAdapter.destroyAds();
        notifyDataSetChanged();
        mWrappedAdapter.unregisterDataSetObserver(sAdapterObserver);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addAdRenderListener(NativeAdRenderListener adRenderListener) {
        mBaseAdAdapter.addAdRenderListener(adRenderListener);
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getOriginalPosition(int adjustedPosition) {
        return mBaseAdAdapter.getOriginalPosition(adjustedPosition, mWrappedAdapter.getCount());
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getNumberOfAds() {
        return mBaseAdAdapter.getNumberOfAds(mWrappedAdapter.getCount());
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setRetryFailedAdPositions(boolean retryFailedAdPositions) {
        mBaseAdAdapter.setRetryFailedAdPositions(retryFailedAdPositions);
    }

    private void expandAdView(FlurryAdViewHolder adViewHolder) {
        if (adViewHolder.callToActionView != null) {
            adViewHolder.callToActionView.setVisibility(View.GONE);
        }
        if (adViewHolder.adCollapseView != null) {
            adViewHolder.adCollapseView.setVisibility(View.VISIBLE);
        }
        if (adViewHolder.adImageView != null) {
            adViewHolder.adImageView.setVisibility(View.VISIBLE);
        }

        if (adViewHolder.flurryAdNative != null) {
            adViewHolder.flurryAdNative.setCollapsableTrackingView(adViewHolder.parentView,
                    adViewHolder.adCollapseView);
        }
    }

    private void collapseAdView(FlurryAdViewHolder adViewHolder) {
        if (adViewHolder.callToActionView != null) {
            adViewHolder.callToActionView.setVisibility(View.VISIBLE);
        }
        if (adViewHolder.adCollapseView != null) {
            adViewHolder.adCollapseView.setVisibility(View.GONE);
        }
        if (adViewHolder.adImageView != null) {
            adViewHolder.adImageView.setVisibility(View.GONE);
        }

        if (adViewHolder.flurryAdNative != null) {
            adViewHolder.flurryAdNative.setExpandableTrackingView(adViewHolder.parentView,
                    adViewHolder.callToActionView);
        }
    }

    public static class Builder {
        FlurryAdListAdapter mFlurryAdapter;

        /**
         * Initializes the builder for the {@link FlurryAdListAdapter} with all the mandatory values.
         *
         * @param context the {@link Context} to use
         * @param adapter the {@link android.widget.ListAdapter} with data that should be wrapped
         * @param viewBinder the {@link NativeAdViewBinder} to use and build ad views
         * @param adSpaceName an Flurry ad space name to use and generate Flurry ads.
         */
        Builder(@NonNull Context context, @NonNull Adapter adapter,
                @NonNull NativeAdViewBinder viewBinder, @NonNull String adSpaceName) {
            mFlurryAdapter = new FlurryAdListAdapter();
            mFlurryAdapter.mContextReference = new WeakReference<>(context);
            mFlurryAdapter.mWrappedAdapter = adapter;
            mFlurryAdapter.mViewBinder = viewBinder;
            mFlurryAdapter.mBaseAdAdapter.initAdFetcher(context);
            mFlurryAdapter.mBaseAdAdapter.setAdSpaceName(adSpaceName);
        }

        /**
         * Sets the ad positioner to use to populate and position ads in the adapter.
         *
         * @param adapterAdPositioner the adapter ad positioner to use. If not set, uses a default
         *                            {@link LinearIntervalAdPositioner} with starting position and
         *                            repeat-after-interval of 3.
         * @return a {@link FlurryAdListAdapter.Builder} instance
         *
         * @see LinearIntervalAdPositioner
         */
        public Builder setAdPositioner(@Nullable AdapterAdPositioner adapterAdPositioner) {
            mFlurryAdapter.mBaseAdAdapter.setPositioner(adapterAdPositioner,
                    mFlurryAdapter.mWrappedAdapter.getCount());
            return this;
        }

        /**
         * Sets the ad targeting settings to use for all fetched ads in this adapter.
         *
         * @param targeting the ad targeting settings
         * @return a {@link FlurryAdListAdapter.Builder} instance
         */
        public Builder setTargeting(@NonNull FlurryAdTargeting targeting) {
            mFlurryAdapter.mBaseAdAdapter.setAdTargeting(targeting);
            return this;
        }

        /**
         * <p>Sets an optional {@link FlurryAdNativeListener} if you also want to listen for ad events
         * for analytics or tracking purposes. Note that setting this listener will <b>not</b>
         * notify you about changes in the ads shown in {@link FlurryAdListAdapter}.</p>
         *
         * <p>If you are just interested in getting notifications for changes in ads shown in
         * {@link FlurryAdListAdapter}, use {@link FlurryAdListAdapter#addAdRenderListener(
         *NativeAdRenderListener)} instead.</p>
         *
         * @param listener the listener to set
         * @return a {@link FlurryAdListAdapter.Builder} instance
         *
         * @see StubFlurryAdNativeListener
         * @see FlurryAdListAdapter#addAdRenderListener(NativeAdRenderListener)
         */
        public Builder setFlurryAdNativeListener(@NonNull FlurryAdNativeListener listener) {
            mFlurryAdapter.mBaseAdAdapter.addFlurryAdNativeListener(listener);
            return this;
        }

        /**
         * <p>Sets a flag on whether or not to automatically destroy ads when the current
         * <code>Activity</code> is destroyed.</p>
         *
         * <p>This will only work if the {@link Context} used to build this app is an Activity
         * context.</p>
         *
         * @param autoDestroy <code>true</code> if the ads in this adapter should be automatically
         *                    destroyed, <code>false</code> if you prefer to destroy the ads
         *                    yourself
         * @return a {@link FlurryAdListAdapter.Builder} instance
         */
        public Builder setAutoDestroy(boolean autoDestroy) {
            mFlurryAdapter.mBaseAdAdapter.setAutoDestroy(autoDestroy);
            return this;
        }

        /**
         * <p>Sets the expanded mode that ads from this adapter should start in.</p>
         *
         * <p>Mode value should be one of:</p>
         * <ul>
         *     <li>{@link #EXPANDABLE_AD_MODE_OFF} -
         *          Default mode. Ad expansion toggling is not supported.</li>
         *     <li>{@link #EXPANDABLE_AD_MODE_EXPANDED} -
         *          Ads start off in expanded mode and can be toggled.</li>
         *     <li>{@link #EXPANDABLE_AD_MODE_COLLAPSED} -
         *          Ads start off in collapsed mode and can be toggled.</li>
         * </ul>
         *
         * <p>If you set any mode other than <code>EXPANDABLE_AD_MODE_OFF</code>, you must pass
         * in a call-to-action view
         * ({@link NativeAdViewBinder.ViewBinderBuilder#setCallToActionViewId(int)}) and a
         * "collapse" button ({@link NativeAdViewBinder.ViewBinderBuilder#setAdCollapseViewId(int)}).
         * </p>
         *
         * @param mode the expanded mode to set for all ads in the adapter
         * @return a {@link FlurryAdListAdapter.Builder} instance
         */
        public Builder setExpandableAdMode(@ExpandableAdMode int mode) {
            mFlurryAdapter.mBaseAdAdapter.setExpandableAdMode(mode);
            return this;
        }

        /**
         * Builds the {@link FlurryAdListAdapter} with the current settings
         * @return the ready to use {@link FlurryAdListAdapter}
         */
        public FlurryAdListAdapter build() {
            if (mFlurryAdapter.mBaseAdAdapter.getExpandableAdMode() != EXPANDABLE_AD_MODE_OFF &&
                    (mFlurryAdapter.mViewBinder.getCallToActionViewId() <= 0 ||
                    mFlurryAdapter.mViewBinder.getAdCollapseViewId() <= 0)) {
                throw new IllegalStateException("If you are not passing EXPANDABLE_AD_MODE_OFF to " +
                        "FlurryAdListAdapter.Builder#setExpandableAdMode(int), you should set a " +
                        "call-to-action and collapse-ad View in your NativeAdViewBinder.");
            }

            mFlurryAdapter.mBaseAdAdapter.setFetchListener(
                    new FlurryNativeAdFetcher.FetchListener() {
                        @Override
                        public void onAdFetched() {
                            Log.i(TAG, "Ad fetched");
                            mFlurryAdapter.notifyDataSetChanged();
                        }
                    }
            );

            mFlurryAdapter.setRetryFailedAdPositions(true);

            sAdapterObserver = new DataSetObserver() {
                @Override
                public void onChanged() {
                    mFlurryAdapter.notifyDataSetChanged();
                }

                @Override
                public void onInvalidated() {
                    mFlurryAdapter.notifyDataSetInvalidated();
                }
            };
            mFlurryAdapter.mWrappedAdapter.registerDataSetObserver(sAdapterObserver);

            return mFlurryAdapter;
        }

        @VisibleForTesting
        FlurryAdListAdapter buildWithMockAdFetcher(FlurryNativeAdFetcher mockFetcher) {
            mFlurryAdapter.mBaseAdAdapter.injectMockAdFetcher(mockFetcher);
            return build();
        }
    }

    private class ExpandedAdListener extends StubFlurryAdNativeListener {
        private FlurryAdViewHolder mAdViewHolder;
        private int mPosition;


        @Override
        public void onExpanded(FlurryAdNative flurryAdParam) {
            mPositionExpandedMap.put(mPosition, true);
            expandAdView(mAdViewHolder);
        }

        @Override
        public void onCollapsed(FlurryAdNative flurryAdParam) {
            mPositionExpandedMap.put(mPosition, false);
            collapseAdView(mAdViewHolder);
        }

        public void setAdViewHolder(FlurryAdViewHolder adViewHolder) {
            mAdViewHolder = adViewHolder;
        }

        public void setPosition(int position) {
            mPosition = position;
        }
    }
}
