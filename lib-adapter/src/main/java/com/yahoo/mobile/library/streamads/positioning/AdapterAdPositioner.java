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

package com.yahoo.mobile.library.streamads.positioning;

import android.support.annotation.IntRange;

/**
 * <p>Interface that defines methods for the calculation of positions and indexes in an adapter that
 * has interspersed ads within other content.</p>
 *
 * @see LinearIntervalAdPositioner
 */
public interface AdapterAdPositioner {
    boolean canPlaceAd(@IntRange(from=0) int position);

    /**
     * Gets the maximum number of ads that can fit into a given data size with this positioning
     * data.
     *
     * @param count the data size to check
     * @return the maximum number of ads that fits
     */
    int getMaxFittableAds(@IntRange(from=0) int count);

    /**
     * Gets the zero-based index of an ad in relation to other ads in the adapter using the overall
     * position of the ad in the adapter.
     *
     * @param adPosition the adjusted position in the adapter
     * @return the zero-based index of the ad in relation to other ads in the adapter
     */
    int getAdIndex(@IntRange(from=0) int adPosition);

    /**
     * Gets the position of a data item translated from its current position with ads in
     * the adapter to the original position it would have been in without ads in the adapter.
     *
     * @param adjustedPosition the adjusted position with ads in the adapter
     * @param noOfFetchedAds the number of ads contained in the adapter
     * @return the original position
     */
    int getOriginalPosition(@IntRange(from=0) int adjustedPosition, int noOfFetchedAds);

    /**
     * Adds an ad position to skip. This position should correspond to the adjusted index that would
     * normally contain an ad if all ads were present in the adapter.
     *
     * @param positionToSkip the ad position to skip
     */
    void addSkippedPosition(@IntRange(from=0) int positionToSkip);

    /**
     * Gets the number of skipped positions.
     *
     * @return the number of skipped positions set in this positioner.
     *
     * @see AdapterAdPositioner#addSkippedPosition(int)
     */
    int getSkippedPositionCount();
}
