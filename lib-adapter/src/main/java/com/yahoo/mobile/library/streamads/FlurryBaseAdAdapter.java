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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;

import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.flurry.android.ads.FlurryAdTargeting;
import com.yahoo.mobile.library.streamads.positioning.AdapterAdPositioner;
import com.yahoo.mobile.library.streamads.positioning.LinearIntervalAdPositioner;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation ozf {@link NativeAdAdapter}. Designed to be used by composition and does not
 * directly implement <code>NativeAdAdapter</code>.
 */
class FlurryBaseAdAdapter {

    /*
    Potentially memory-dangerous attempt to outlive Activity rotation by using static fields.
    This is circumvented by destroying ad objects when no longer in use.
     */
    private static SparseArray<FlurryAdNative> sAdPositionMapping;

    private FlurryNativeAdFetcher mNativeAdFetcher;
    private AdapterAdPositioner mPositioner;
    private ListAdapterDataListener mAdapterDataListener;

    private List<NativeAdAdapter.NativeAdRenderListener> mAdRenderListeners;

    private String mAdSpaceName;
    private boolean mRetryFailedAdPositions;
    private boolean mAutoDestroyAds;

    FlurryBaseAdAdapter(ListAdapterDataListener adapterDataListener) {
        if (!(adapterDataListener instanceof NativeAdAdapter)) {
            throw new IllegalArgumentException(
                    "The ListAdapterDataListener should also be a NativeAdAdapter"
            );
        }
        mAdapterDataListener = adapterDataListener;
        mAdRenderListeners = new ArrayList<>();
        // Set a default AdapterAdPositioner with startPosition and interval of 3
        mPositioner = new LinearIntervalAdPositioner(3, 3);
        initAdPositionMap(5);
    }

    void initAdFetcher(Context context) {
        mNativeAdFetcher = new FlurryNativeAdFetcher(context);

        if (mAutoDestroyAds) {
            registerActivityLifecycleListener(context);
        }
    }

    void refreshAds(String adSpaceName) {
        // Destroy ads from previous ad space
        destroyAds();
        setAdSpaceName(adSpaceName);
        refreshAds();
    }

    //region methods from NativeAdAdapter

    /**
     * A base implementation of {@link NativeAdAdapter#refreshAds()}.
     */
    void refreshAds() {
        mNativeAdFetcher.prefetchAds(mAdSpaceName);
    }

    /**
     * A base implementation of {@link NativeAdAdapter#destroyAds()}.
     */
    void destroyAds() {
        /*
         A bit slower than looping through HashMap, but any good ad integration
         should not contain > 50 ads, so perf. diff. is negligible and memory savings are greater
         */
        for (int i = 0; i < sAdPositionMapping.size(); i++) {
            sAdPositionMapping.valueAt(i).destroy();
        }
        sAdPositionMapping.clear();
        sAdPositionMapping = null;
        mNativeAdFetcher.setFetchListener(null);
        mNativeAdFetcher.clearFlurryAdNativeListeners();
        mNativeAdFetcher.destroyAds();
    }

    /**
     * A base implementation of
     * {@link NativeAdAdapter#addAdRenderListener(NativeAdAdapter.NativeAdRenderListener)}.
     *
     * @param adRenderListener the {@link NativeAdAdapter.NativeAdRenderListener} to add for render
     *                         events.
     */
    void addAdRenderListener(NativeAdAdapter.NativeAdRenderListener adRenderListener) {
        mAdRenderListeners.add(adRenderListener);
    }

    /**
     * A base implementation of {@link NativeAdAdapter#getOriginalPosition(int)}.
     *
     * @param adjustedPosition the adjusted position after ads have been loaded into the adapter
     * @param internalAdapterSize an extra parameter indicating the size of the adapter without ads
     * @return the original position of data in the adapter as if no ads are loaded
     */
    int getOriginalPosition(int adjustedPosition, int internalAdapterSize) {
        if (getNumberOfAds(internalAdapterSize) > 0) {
            return mPositioner.getOriginalPosition(adjustedPosition,
                    getNumberOfAds(internalAdapterSize));
        } else {
            return adjustedPosition;
        }
    }

    /**
     * A base implementation of {@link NativeAdAdapter#getNumberOfAds()}.
     *
     * @param internalAdapterSize an extra parameter indicating the size of the adapter without ads
     * @return the number of ads in the adapter
     */
    int getNumberOfAds(int internalAdapterSize) {
        int numberOfAds = Math.min(sAdPositionMapping.size() + mNativeAdFetcher.getQueuedAdsCount(),
                mPositioner.getMaxFittableAds(internalAdapterSize));
        int numberOfAdsToShow = 0;
        int skippedAdPositionCount = mPositioner.getSkippedPositionCount();
        if (numberOfAds > skippedAdPositionCount) {
            numberOfAdsToShow = numberOfAds - skippedAdPositionCount;
        }
        return numberOfAdsToShow;
    }

    /**
     * A base implementation of {@link NativeAdAdapter#setRetryFailedAdPositions(boolean)}
     *
     * @param retryFailedAdPositions <code>true</code> if the adapter can try again to render the
     *                               failed ad position the next time it comes up
     */
    void setRetryFailedAdPositions(boolean retryFailedAdPositions) {
        mRetryFailedAdPositions = retryFailedAdPositions;
    }

    //endregion

    /**
     * Notifies the {@link NativeAdAdapter.NativeAdRenderListener}s that an ad has been rendered at
     * the given position.
     *
     * @param position the position at which the ad was rendered.
     */
    void notifyAdRendered(int position) {
        for (NativeAdAdapter.NativeAdRenderListener listener : mAdRenderListeners) {
            listener.onAdRendered(position);
        }
    }

    /**
     * Checks if an ad should be shown in the given position. The check is facilitated by
     * {@link FlurryBaseAdAdapter#canShowAd(int, int)} and
     * {@link FlurryBaseAdAdapter#isAdAvailable(int, int)}.
     *
     * This method checks if the positioning logic allows an ad to be placed here <em>and</em> if
     * there is an available ad to be placed here.
     *
     * @param position the adapter position to check
     * @param internalAdapterSize the size of the adapter without ads
     * @return <code>true</code> if an ad should be placed in the given position, <code>false</code>
     * otherwise
     *
     * @see FlurryBaseAdAdapter#canShowAd(int, int)
     * @see FlurryBaseAdAdapter#isAdAvailable(int, int)
     */
    boolean shouldShowAd(int position, int internalAdapterSize) {
        if (mPositioner.canPlaceAd(position)) {
            if (isAdAvailable(position, internalAdapterSize)) {
                return true;
            } else if (!mRetryFailedAdPositions) { // Do not retry position
                // Can place ad, but ad not ready
                for (NativeAdAdapter.NativeAdRenderListener listener : mAdRenderListeners) {
                    listener.onAdRenderFailed(position);
                }
                mPositioner.addSkippedPosition(position);
                if (mAdapterDataListener instanceof RecyclerAdapterDataListener) {
                    ((RecyclerAdapterDataListener)mAdapterDataListener).notifyItemRemoved(position);
                } else {
                    mAdapterDataListener.notifyDataSetChanged();
                }
            }
        }

        return false;
    }

    /**
     * Gets an Flurry native ad object for a given position.
     *
     * @param position the position to return an ad object for
     * @return the Flurry native ad object
     */
    @Nullable
    FlurryAdNative getAdForPosition(int position) {
        if (sAdPositionMapping.get(position) != null) {
            return sAdPositionMapping.get(position);
        } else {
            FlurryAdNative flurryAdNative = mNativeAdFetcher.popLoadedAd();
            if (flurryAdNative != null) {
                sAdPositionMapping.put(position, flurryAdNative);
                return flurryAdNative;
            }
        }
        return null;
    }

    /**
     * Sets the ad space name to be used for the {@link FlurryNativeAdFetcher} when fetching ads.
     *
     * @param adSpaceName the name of the ad space, obtained from the Flurry dev. portal
     */
    void setAdSpaceName(String adSpaceName) {
        mAdSpaceName = adSpaceName;
    }

    /**
     * Adds a new {@link FlurryAdNativeListener} to be notified of native ad state events.
     *
     * @param flurryAdNativeListener the listener to add
     */
    void addFlurryAdNativeListener(FlurryAdNativeListener flurryAdNativeListener) {
        mNativeAdFetcher.addFlurryAdNativeListener(flurryAdNativeListener);
    }

    /**
     * Sets the {@link FlurryNativeAdFetcher.FetchListener} to be notified for fetch events.
     * @param fetchListener the listener to set
     */
    void setFetchListener(FlurryNativeAdFetcher.FetchListener fetchListener) {
        mNativeAdFetcher.setFetchListener(fetchListener);
    }

    /**
     * Sets the ad targeting settings to use for all fetched ads in this adapter.
     *
     * @param targeting the ad targeting settings
     */
    void setAdTargeting(FlurryAdTargeting targeting) {
        if (mNativeAdFetcher != null) {
            mNativeAdFetcher.setTargeting(targeting);
        }
    }

    /**
     * Sets the ad positioner to use to populate and position ads in the adapter.
     *
     * @param positioner the adapter ad positioner to use
     * @param internalAdapterSize the size of the adapter without ads
     */
    void setPositioner(@NonNull AdapterAdPositioner positioner, int internalAdapterSize) {
        mPositioner = positioner;

        int maxFittableAds = mPositioner.getMaxFittableAds(
                internalAdapterSize
        );

        initAdPositionMap(maxFittableAds);
    }

    /**
     * Sets whether ads in the adapter should be automatically destroyed when the Activity housing
     * the adapter is destroyed.
     * @param autoDestroy <code>true</code> if ads should be automatically destroyed,
     *                    <code>false</code> otherwise
     */
    void setAutoDestroy(boolean autoDestroy) {
        mAutoDestroyAds = autoDestroy;
    }

    /**
     * Checks if this is a position in which the positioning logic allows an ad to be placed.
     * @param position the adapter position to check
     * @param internalAdapterSize the size of the adapter without ads
     * @return <code>true</code> if an ad can be placed in the given position, <code>false</code>
     * otherwise
     */
    boolean canShowAd(int position, int internalAdapterSize) {
        return mPositioner.canPlaceAd(position) && isAdAvailable(position, internalAdapterSize);
    }

    /**
     * Replaces the internal {@link FlurryNativeAdFetcher} object with a new one.
     *
     * For testing purposes only.
     *
     * TODO: Extract an interface native ad fetcher.
     *
     * @param nativeAdFetcher the mock native ad fetcher
     */
    void injectMockAdFetcher(FlurryNativeAdFetcher nativeAdFetcher) {
        mNativeAdFetcher = nativeAdFetcher;
    }

    /**
     * Checks if an ad is available and read for a given position.
     * @param position the adapter position to check
     * @param internalAdapterSize the size of the adapter without ads
     * @return <code>true</code> if an ad is available for the given position, <code>false</code>
     * otherwise
     */
    @VisibleForTesting
    protected boolean isAdAvailable(int position, int internalAdapterSize) {
        int adIndex = mPositioner.getAdIndex(position);
        if (adIndex < getNumberOfAds(internalAdapterSize)) {
            FlurryAdNative flurryAdNative = getAdForPosition(position);
            if (flurryAdNative != null) {
                if (!flurryAdNative.isExpired()) {
                    return true;
                } else {
                    // Remove expired ad. TODO: Check how this affects the layout
                    sAdPositionMapping.remove(position);
                    mAdapterDataListener.notifyDataSetChanged();
                }
            }
        }
        return false;
    }

    /**
     * Initializes the structure that maps an ad to an index position in the Adapter
     * @param maxFittableAds the maximum number of ads that can fit in this adapter, as
     *                       determined by the positioning logic
     */
    private void initAdPositionMap(int maxFittableAds) {
        sAdPositionMapping = new SparseArray<>(maxFittableAds);
    }

    /**
     * Register ActivityLifecycleCallbacks to notify the adapter of Activity lifecycle changes.
     * This enables the Adapter destroy ads when the Activity has been destroyed.
     *
     * @param context the Activity context
     */
    private void registerActivityLifecycleListener(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                context instanceof Activity) {
            Application.ActivityLifecycleCallbacks lifecycleCallbacks =
                    new Application.ActivityLifecycleCallbacks() {

                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

                @Override
                public void onActivityStarted(Activity activity) {}

                @Override
                public void onActivityResumed(Activity activity) {}

                @Override
                public void onActivityPaused(Activity activity) {}

                @Override
                public void onActivityStopped(Activity activity) {}

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

                @Override
                @SuppressLint("NewApi")
                public void onActivityDestroyed(Activity activity) {
                    if (activity.isFinishing() &&
                            activity == context) {
                        destroyAds();

                        ((Activity) context).getApplication()
                                .unregisterActivityLifecycleCallbacks(this);
                    }
                }
            };
            /*
            Monitor for onDestroy callbacks in the Activity and destroy ads when the Activity is
            also destroyed. Will only work for API 14+.
            */
            ((Activity) context).getApplication().registerActivityLifecycleCallbacks(
                    lifecycleCallbacks);
        }
    }

    /**
     * Callback interface to notify listeners that support
     * {@link android.database.DataSetObservable} of when the dataset should have changed.
     */
    interface ListAdapterDataListener {
        void notifyDataSetChanged();
    }

    /**
     * Callback interface to notify listeners that support
     * {@link android.support.v7.widget.RecyclerView.AdapterDataObserver} of when the dataset
     * should have changed.
     */
    interface RecyclerAdapterDataListener extends ListAdapterDataListener {
        void notifyItemRemoved(int position);
    }
}
