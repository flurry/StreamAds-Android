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
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.flurry.android.ads.FlurryAdTargeting;
import com.yahoo.mobile.library.streamads.positioning.AdapterAdPositioner;
import com.yahoo.mobile.library.streamads.positioning.LinearIntervalAdPositioner;

import java.lang.ref.WeakReference;

/**
 *
 */
public class FlurryAdRecyclerAdapter extends RecyclerView.Adapter implements NativeAdAdapter,
        FlurryBaseAdAdapter.RecyclerAdapterDataListener {
    public static final String TAG = FlurryAdRecyclerAdapter.class.getSimpleName();
    private static RecyclerView.AdapterDataObserver sAdapterObserver;

    // Attempt to ensure unique view type different from wrapped adapter's view types
    private final int VIEW_TYPE_AD = -42;

    private FlurryBaseAdAdapter mBaseAdAdapter;
    private WeakReference<Context> mContextReference;
    private RecyclerView.Adapter mWrappedAdapter;
    private NativeAdViewBinder mViewBinder;

    private FlurryAdRecyclerAdapter() {
        mBaseAdAdapter = new FlurryBaseAdAdapter(this);
    }

    /**
     * Initializes the {@link FlurryAdListAdapter.Builder} for
     * the {@link FlurryAdListAdapter} with all the mandatory values.
     *
     * @param context the {@link Context} to use. You could use either an {@link Activity} or
     *                {@link Application} context. Keep in mind that if using Application context,
     *                the theme styling applied to your Activities will NOT be applied to your
     *                ad layout.
     * @param adapter the {@link android.widget.ListAdapter} with data that should be wrapped
     * @param viewBinder the {@link NativeAdViewBinder} to use and build ad views
     * @param adSpaceName an Flurry ad space name to use and generate Flurry ads.
     */
    public static Builder from(@NonNull Context context, @NonNull RecyclerView.Adapter adapter,
                               @NonNull NativeAdViewBinder viewBinder,
                               @NonNull String adSpaceName) {
        return new Builder(context, adapter, viewBinder, adSpaceName);
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getItemCount() {
        return mWrappedAdapter.getItemCount() > 0 ?
                mWrappedAdapter.getItemCount() + getNumberOfAds() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (mBaseAdAdapter.shouldShowAd(position, mWrappedAdapter.getItemCount())) {
            return VIEW_TYPE_AD;
        } else {
            return mWrappedAdapter.getItemViewType(getOriginalPosition(position));
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContextReference.get());
        RecyclerView.ViewHolder viewHolder;
        switch (viewType) {
            case VIEW_TYPE_AD:
                View view = inflater.inflate(mViewBinder.getAdLayoutId(), parent, false);
                viewHolder = new FlurryRecyclerAdViewHolder(view, mViewBinder);
                break;
            default:
                viewHolder = mWrappedAdapter.onCreateViewHolder(parent, viewType);
                break;
        }
        return viewHolder;
    }

    /**
     * @inheritDoc
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_AD:
                FlurryAdNative flurryAdNative = mBaseAdAdapter.getAdForPosition(position);

                ((FlurryRecyclerAdViewHolder) holder).getNativeAdViewHolder().flurryAdNative =
                        flurryAdNative;

                if (flurryAdNative == null) {
                    Log.w(TAG, "Should not happen. Flurry ad should not be null at this point");
                    return;
                }

                flurryAdNative.setTrackingView(holder.itemView);

                FlurryNativeAdViewBuilder.buildAdIntoViews(
                        flurryAdNative,
                        ((FlurryRecyclerAdViewHolder)holder).getNativeAdViewHolder()
                );

                mBaseAdAdapter.notifyAdRendered(position);
                break;
            default:
                mWrappedAdapter.onBindViewHolder(holder, getOriginalPosition(position));
                break;
        }
    }

    /**
     * Refreshes ads with a new Flurry ad space.
     *
     * @param adSpaceName the new Flurry ad space to use
     */
    public void refreshAds(String adSpaceName) {
        mBaseAdAdapter.refreshAds(adSpaceName);
    }

    /**
     * @inheritDoc
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
        mWrappedAdapter.unregisterAdapterDataObserver(sAdapterObserver);
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
        return mBaseAdAdapter.getOriginalPosition(adjustedPosition, mWrappedAdapter.getItemCount());
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getNumberOfAds() {
        return mBaseAdAdapter.getNumberOfAds(mWrappedAdapter.getItemCount());
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setRetryFailedAdPositions(boolean retryFailedAdPositions) {
        mBaseAdAdapter.setRetryFailedAdPositions(retryFailedAdPositions);
    }

    public static class Builder {
        FlurryAdRecyclerAdapter mFlurryRecyclerAdapter;

        /**
         * Initializes the builder for the {@link FlurryAdRecyclerAdapter} with all the mandatory
         * values.
         *
         * @param context the {@link Context} to use
         * @param adapter the {@link android.widget.ListAdapter} with data that should be wrapped
         * @param viewBinder the {@link NativeAdViewBinder} to use and build ad views
         * @param adSpaceName an Flurry ad space name to use and generate Flurry ads.
         */
        Builder(@NonNull Context context, @NonNull RecyclerView.Adapter adapter,
                @NonNull NativeAdViewBinder viewBinder, @NonNull String adSpaceName) {
            mFlurryRecyclerAdapter = new FlurryAdRecyclerAdapter();
            mFlurryRecyclerAdapter.mContextReference = new WeakReference<>(context);
            mFlurryRecyclerAdapter.mWrappedAdapter = adapter;
            mFlurryRecyclerAdapter.mViewBinder = viewBinder;
            mFlurryRecyclerAdapter.mBaseAdAdapter.initAdFetcher(context);
            mFlurryRecyclerAdapter.mBaseAdAdapter.setAdSpaceName(adSpaceName);
        }

        /**
         * Sets the ad positioner to use to populate and position ads in the adapter.
         *
         * @param adapterAdPositioner the adapter ad positioner to use. If not set, uses a default
         *                            {@link LinearIntervalAdPositioner} with starting position and
         *                            repeat-after-interval of 3.
         * @return a {@link FlurryAdRecyclerAdapter.Builder} instance
         *
         * @see LinearIntervalAdPositioner
         */
        public Builder setAdPositioner(@Nullable AdapterAdPositioner adapterAdPositioner) {
            mFlurryRecyclerAdapter.mBaseAdAdapter.setPositioner(adapterAdPositioner,
                    mFlurryRecyclerAdapter.mWrappedAdapter.getItemCount());
            return this;
        }

        /**
         * Sets the ad targeting settings to use for all fetched ads in this adapter.
         *
         * @param targeting the ad targeting settings
         * @return a {@link FlurryAdRecyclerAdapter.Builder} instance
         */
        public Builder setTargeting(@NonNull FlurryAdTargeting targeting) {
            mFlurryRecyclerAdapter.mBaseAdAdapter.setAdTargeting(targeting);
            return this;
        }

        /**
         * <p>Sets an optional {@link FlurryAdNativeListener} if you also want to listen for ad events
         * for analytics or tracking purposes. Note that setting this listener will <b>not</b>
         * notify you about changes in the ads shown in {@link FlurryAdRecyclerAdapter}.</p>
         *
         * <p>If you are just interested in getting notifications for changes in ads shown in
         * {@link FlurryAdRecyclerAdapter}, use {@link FlurryAdRecyclerAdapter#addAdRenderListener(
         *NativeAdRenderListener)} instead.</p>
         *
         * @param listener the listener to set
         * @return a {@link FlurryAdRecyclerAdapter.Builder} instance
         *
         * @see StubFlurryAdNativeListener
         * @see FlurryAdRecyclerAdapter#addAdRenderListener(NativeAdRenderListener)
         */
        public Builder setFlurryAdNativeListener(@NonNull FlurryAdNativeListener listener) {
            mFlurryRecyclerAdapter.mBaseAdAdapter.addFlurryAdNativeListener(listener);
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
            mFlurryRecyclerAdapter.mBaseAdAdapter.setAutoDestroy(autoDestroy);
            return this;
        }

        /**
         * Builds the {@link FlurryAdRecyclerAdapter} with the current settings
         * @return the ready to use {@link FlurryAdRecyclerAdapter}
         */
        public FlurryAdRecyclerAdapter build() {
            mFlurryRecyclerAdapter.mBaseAdAdapter.setFetchListener(
                    new FlurryNativeAdFetcher.FetchListener() {
                        @Override
                        public void onAdFetched() {
                            Log.i(TAG, "Ad fetched");
                            mFlurryRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
            );

            mFlurryRecyclerAdapter.setRetryFailedAdPositions(true);

            sAdapterObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    mFlurryRecyclerAdapter.notifyDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    mFlurryRecyclerAdapter.notifyItemRangeChanged(positionStart, itemCount);
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    mFlurryRecyclerAdapter.notifyItemRangeInserted(positionStart, itemCount);
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    mFlurryRecyclerAdapter.notifyItemRangeRemoved(positionStart, itemCount);
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    mFlurryRecyclerAdapter.notifyDataSetChanged();
                }
            };
            mFlurryRecyclerAdapter.mWrappedAdapter.registerAdapterDataObserver(sAdapterObserver);
            return mFlurryRecyclerAdapter;
        }

        @VisibleForTesting
        FlurryAdRecyclerAdapter buildWithMockAdFetcher(FlurryNativeAdFetcher mockFetcher) {
            mFlurryRecyclerAdapter.mBaseAdAdapter.injectMockAdFetcher(mockFetcher);
            return build();
        }
    }
}
