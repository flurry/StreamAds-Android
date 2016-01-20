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

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>Interface for an adapter that renders ads interspersed between other application data.</p>
 *
 * <p>Ideally meant for children of {@link android.support.v7.widget.RecyclerView.Adapter} or
 * {@link android.widget.ListAdapter} to implement, but any adapter that renders a collection of
 * views could implement this interface.</p>
 */
public interface NativeAdAdapter {

    /**
     * Ad views do not support toggling between expanded and collapsed.
     */
    public static final int EXPANDABLE_AD_MODE_OFF = 0;
    /**
     * Ad views in the adapter will start off as collapsed and support expansion toggling.
     */
    public static final int EXPANDABLE_AD_MODE_COLLAPSED = 1;
    /**
     * Ad views in the adapter will start off as expanded and support expansion toggling.
     */
    public static final int EXPANDABLE_AD_MODE_EXPANDED = 2;

    @IntDef({EXPANDABLE_AD_MODE_OFF, EXPANDABLE_AD_MODE_COLLAPSED, EXPANDABLE_AD_MODE_EXPANDED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExpandableAdMode {}

    /**
     * Refreshes the ads in the adapter
     */
    void refreshAds();

    /**
     * Destroys all ads in the adapter
     */
    void destroyAds();

    /**
     * Adds a listener that will be notified during ad rendering events.
     *
     * @param adRenderListener the listener to be notified
     */
    void addAdRenderListener(NativeAdRenderListener adRenderListener);

    /**
     * Gets the original position of data from the wrapped adapter
     * @param position the adjusted position of data after ads may have been loaded into the adapter
     * @return the original position of data in the adapter as if no ads are loaded
     */
    int getOriginalPosition(int position);

    /**
     * Gets the number of ads currently showing in the adapter
     * @return the number of rendered ads
     */
    int getNumberOfAds();

    /**
     * <p>Sets if the adapter should try and render an ad at a position if it previously failed to
     * render at that position.</p>
     *
     * <p>This behavior works better with a list that is scrolled from zero-index to higher
     * indexes (e.g. from top to bottom, like a newsfeed). It could function unpredictably if used
     * in a list that is scrolled from higher indexes to lower indexes (e.g. bottom to top scrolling
     * in a chat application).</p>
     *
     * @param retryFailedAdPositions <code>true</code> if the adapter can try again to render the
     *                               failed ad position the next time it comes up
     */
    void setRetryFailedAdPositions(boolean retryFailedAdPositions);

    interface NativeAdRenderListener {
        /**
         * Callback method triggered when the adapter successfully renders an ad unto a View
         * in position.
         *
         * @param position the position that the ad was loaded into
         */
        void onAdRendered(int position);

        /**
         * Callback method triggered when the adapter tries to render an ad unto a View in position,
         * but the ad is not ready or available.
         *
         * @param position the position that the ad failed to load into
         */
        void onAdRenderFailed(int position);
    }
}
