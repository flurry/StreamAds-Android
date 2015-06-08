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

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.flurry.android.ads.FlurryAdTargeting;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches ads sequentially and fills a memory cache queue. Ensures that there is always ads
 * available in cache.
 */
class FlurryNativeAdFetcher {
    private final static String TAG = FlurryNativeAdFetcher.class.getName();
    /**
     * Maximum number of ads to queue.
     */
    private final static int PREFETCHED_ADS_SIZE = 5;
    /**
     * Maximum number of times to try fetch an ad after failed attempts.
     */
    private final static int MAX_FETCH_ATTEMPT = 5;
    /**
     * Maximum number of ads to successfully fetch throughout this fetcher's lifetime.
     */
    private final static int MAX_ADS_TO_FETCH = 30;
    /**
     * Retry time
     */
    private final static int RETRY_TIME_MILLI = 2000;
    private final Handler RETRY_FETCH_HANDLER;
    private final Runnable RETRY_FETCH_RUNNABLE;
    private final ArrayDeque<FlurryAdNative> AD_QUEUE;

    private String mAdSpaceName;
    private int mFetchFailCount;
    private int mFetchSucceedCount;
    private boolean mIsCurrentlyFetching;

    private WeakReference<Context> mContextReference;
    private List<FlurryAdNativeListener> mExternalListeners;
    // Holding on to this object to prevent garbage collection before request is complete.
    private FlurryAdNative mCurrentFlurryAdNative;
    private FlurryAdTargeting mFlurryAdTargeting;
    private FetchListener mFetchListener;

    FlurryNativeAdFetcher(@NonNull Context context) {
        RETRY_FETCH_HANDLER = new Handler();
        RETRY_FETCH_RUNNABLE = new Runnable() {
            @Override
            public void run() {
                replenishAdQueue();
            }
        };
        AD_QUEUE = new ArrayDeque<>(PREFETCHED_ADS_SIZE);
        mExternalListeners = new ArrayList<>();
        mContextReference = new WeakReference<>(context);
    }

    /**
     * <p>Adds a FlurryAdNativeListener that can be notified during the entire life-cycle of a
     * {@link FlurryAdNative}.</p>
     *
     * <p>If you care about just the fact that a new ad has been fetched, use
     * {@link FlurryNativeAdFetcher#setFetchListener(FetchListener)}, in collaboration with
     * {@link FlurryNativeAdFetcher#popLoadedAd()} instead.</p>
     * @param adNativeListener the listener to be notified during all life-cycle events of a
     *                         {@link FlurryAdNative}
     */
    void addFlurryAdNativeListener(@NonNull FlurryAdNativeListener adNativeListener) {
        mExternalListeners.add(adNativeListener);
    }

    /**
     * Clears all FlurryAdNativeListener objects.
     */
    void clearFlurryAdNativeListeners() {
        mExternalListeners.clear();
    }

    /**
     * Sets the listener to be notified when new ads are available.
     *
     * @param fetchListener the listener to be notified when an ad fetch request is successfull
     * @see FlurryNativeAdFetcher#addFlurryAdNativeListener(FlurryAdNativeListener)
     */
    void setFetchListener(FetchListener fetchListener) {
        this.mFetchListener = fetchListener;
    }

    /**
     * Sets the {@link FlurryAdTargeting} to be used for all subsequent ads fetched. This can be
     * changed at any time and will not affect the targeting of previous fetched ads in queue.
     *
     * @param targeting the {@link FlurryAdTargeting} to use
     * @see FlurryAdNative#setTargeting(FlurryAdTargeting)
     */
    void setTargeting(FlurryAdTargeting targeting) {
        this.mFlurryAdTargeting = targeting;
    }

    /**
     * Starts prefetching ads using a given Flurry ad space name. Noop if the Flurry session is not
     * started. Ad queuing will not start until this method has been called.
     *
     * @param adSpaceName the Flurry ad space name
     */
    void prefetchAds(@NonNull String adSpaceName) {
        mAdSpaceName = adSpaceName;

        if (!FlurryAgent.isSessionActive()) {
            Log.w(TAG, "Cannot fetch ads. Session is not yet active");

            RETRY_FETCH_HANDLER.postDelayed(RETRY_FETCH_RUNNABLE, RETRY_TIME_MILLI);

            Log.w(TAG, "Will retry fetch ads in " + RETRY_TIME_MILLI);
            return;
        }

        if (!mIsCurrentlyFetching) {
            replenishAdQueue();
        }
    }

    /**
     * Gets and removes the next ad from the queue. This will also replenish the queue.
     *
     * @return a valid, useable {@link FlurryAdNative} object or null if non is available
     */
    @Nullable
    FlurryAdNative popLoadedAd() {
        FlurryAdNative adNative = AD_QUEUE.pollFirst();
        replenishAdQueue();

        if (isAdUsable(adNative)) {
            return adNative;
        } else {
            return null;
        }
    }

    /**
     * Gets the number of currently queued ads (not the total number of fetched ads).
     * @return the number of queued ads
     */
    int getQueuedAdsCount() {
        return AD_QUEUE.size();
    }

    /**
     * Destroys all queued ads and removes them from memory
     */
    void destroyAds() {
        mFetchFailCount = 0;
        mIsCurrentlyFetching = false;

        if (mCurrentFlurryAdNative != null) {
            mCurrentFlurryAdNative.destroy();
        }

        for (FlurryAdNative adNative : AD_QUEUE) {
            adNative.destroy();
        }

        AD_QUEUE.clear();
        RETRY_FETCH_HANDLER.removeMessages(0);
    }

    private void replenishAdQueue() {
        if (AD_QUEUE.size() < PREFETCHED_ADS_SIZE &&
                mFetchFailCount < MAX_FETCH_ATTEMPT &&
                mFetchSucceedCount < MAX_ADS_TO_FETCH) {
            mIsCurrentlyFetching = true;

            Context context = mContextReference.get();
            mCurrentFlurryAdNative = new FlurryAdNative(context, mAdSpaceName);
            if (mFlurryAdTargeting != null) {
                mCurrentFlurryAdNative.setTargeting(mFlurryAdTargeting);
            }
            mCurrentFlurryAdNative.setListener(internalListener);
            mCurrentFlurryAdNative.fetchAd();
        } else {
            mIsCurrentlyFetching = false;
        }
    }

    private boolean isAdUsable(FlurryAdNative nativeAd) {
        return nativeAd != null && nativeAd.isReady() && !nativeAd.isExpired();
    }

    FlurryAdNativeListener internalListener = new FlurryAdNativeListener() {
        @Override
        public void onFetched(FlurryAdNative flurryAdNative) {
            Log.w(TAG, "onFetched");

            if (isAdUsable(flurryAdNative)) {
                AD_QUEUE.addLast(flurryAdNative);
                mFetchFailCount = 0;
                mFetchSucceedCount += 1;

                /*
                 Inform the fetch listener that an ad has been fetched so it can get the
                 ad from FlurryNativeAdFetcher#popLoadedAd() whenever it wants.
                 */
                if (mFetchListener != null) {
                    mFetchListener.onAdFetched();
                }

                for (FlurryAdNativeListener listener : mExternalListeners) {
                    listener.onFetched(flurryAdNative);
                }
            } else {
                flurryAdNative.destroy();
            }

            // Replenish immediately
            replenishAdQueue();
        }

        @Override
        public void onShowFullscreen(FlurryAdNative flurryAdNative) {
            for (FlurryAdNativeListener listener : mExternalListeners) {
                listener.onShowFullscreen(flurryAdNative);
            }
        }

        @Override
        public void onCloseFullscreen(FlurryAdNative flurryAdNative) {
            for (FlurryAdNativeListener listener : mExternalListeners) {
                listener.onCloseFullscreen(flurryAdNative);
            }
        }

        @Override
        public void onAppExit(FlurryAdNative flurryAdNative) {
            for (FlurryAdNativeListener listener : mExternalListeners) {
                listener.onAppExit(flurryAdNative);
            }
        }

        @Override
        public void onClicked(FlurryAdNative flurryAdNative) {
            for (FlurryAdNativeListener listener : mExternalListeners) {
                listener.onClicked(flurryAdNative);
            }
        }

        @Override
        public void onImpressionLogged(FlurryAdNative flurryAdNative) {
            for (FlurryAdNativeListener listener : mExternalListeners) {
                listener.onImpressionLogged(flurryAdNative);
            }
        }

        @Override
        public void onError(FlurryAdNative flurryAdNative, FlurryAdErrorType flurryAdErrorType,
                            int errorCode) {
            if (flurryAdErrorType == FlurryAdErrorType.FETCH) {
                mFetchFailCount++;
                flurryAdNative.destroy();
            }
            // Retry after some delay
            RETRY_FETCH_HANDLER.postDelayed(RETRY_FETCH_RUNNABLE, RETRY_TIME_MILLI);
            for (FlurryAdNativeListener listener : mExternalListeners) {
                listener.onError(flurryAdNative, flurryAdErrorType, errorCode);
            }

            Log.w(TAG, "onError. Error code: " + errorCode);
        }
    };

    /**
     * Simple callback interface for listeners who don't care about failed fetch requests or the
     * particular ad that was fetched, but just care that an ad was fetched.
     *
     * @see FlurryNativeAdFetcher#setFetchListener(FetchListener)
     * @see FlurryNativeAdFetcher#addFlurryAdNativeListener(FlurryAdNativeListener)
     */
    interface FetchListener {
        void onAdFetched();
    }
}
