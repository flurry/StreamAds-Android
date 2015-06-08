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
 * {@link AdapterAdPositioner} that uses a linear function to space ads in an adapter.
 */
public class LinearIntervalAdPositioner extends BaseAdapterAdPositioner {
    private int mStartingPosition = 2;
    private int mRepeatAfterInterval = Integer.MAX_VALUE;
    private static final int MIN_REPEAT_INTERVAL = 2;

    /**
     * Initializes the positioner.
     *
     * @param startingPosition the first position that an ad should appear in the adapter
     * @param repeatInterval the number of positions to skip between ads
     */
    public LinearIntervalAdPositioner(@IntRange(from = 0) int startingPosition,
                                      @IntRange(from = MIN_REPEAT_INTERVAL) int repeatInterval) {
        this(startingPosition);
        setRepeatAfterInterval(repeatInterval);
    }

    /**
     * Initializes the positioner without a repeat interval.
     *
     * @param startingPosition the first position that an ad should appear in the adapter
     */
    public LinearIntervalAdPositioner(@IntRange(from = 0) int startingPosition) {
        this.mStartingPosition = startingPosition;
    }

    public void setRepeatAfterInterval(@IntRange(from = MIN_REPEAT_INTERVAL)
                                       int repeatAfterInterval) {
        if (repeatAfterInterval >= MIN_REPEAT_INTERVAL) {
            this.mRepeatAfterInterval = repeatAfterInterval;
        }
    }

    @Override
    public boolean canPlaceAd(int position) {
        if (mSkippedAdPositions.contains(position) || position < mStartingPosition) {
            return false;
        }
        int preceedingSkippedPos = getPrecedingSkippedCount(position);

        return ((position + preceedingSkippedPos  - mStartingPosition)
                % (mRepeatAfterInterval + 1) == 0);
        // If mRepeatAfterInterval not set, it overflows "safely"
    }

    /**
     * @inheritDoc
     *
     * <p>Accounts for the first ad placed at the starting position and all other ads placed at
     * recurring intervals.</p>
     */
    @Override
    public int getMaxFittableAds(int size) {
        return (int)Math.ceil((size + 1.0 - mStartingPosition) / mRepeatAfterInterval);
    }

    /**
     * @inheritDoc
     *
     * Uses an equation of the form:
     * adIndex = ((adPosition + repeatAfter - startPosition + 1) / (repeatAfter + 1)) - 1
     *
     * E.g. list with ad at startPosition 5 and repeatAfter 3 will have adPositions at:
     * {5, 9, 13, 17} which is of the closed form: adIndex = (adPosition - 1 / 4) - 1
     */
    @Override
    public int getAdIndex(int adPosition) {
        int unadjustedAdIndex = (((adPosition + mRepeatAfterInterval - mStartingPosition + 1)
                / (mRepeatAfterInterval + 1)) - 1);
        /*
        Adjust the ad index for the preceding skipped ads. So if we have ad positions at
        {5, 9, 13, 17} and adPosition is 9, the unadjustedAdIndex is 1, but if position 5
        failed, the adjusted ad index is 0.
        */
        return unadjustedAdIndex - getPrecedingSkippedCount(adPosition);
    }
}
