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

import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;

import java.util.HashSet;
import java.util.Set;

/**
 * Implements the methods that <b>should</b> be common between all {@link AdapterAdPositioner}.
 */
public abstract class BaseAdapterAdPositioner implements AdapterAdPositioner {

    /**
     * Set that holds ad positions that should be skipped when positioning ads.
     */
    protected Set<Integer> mSkippedAdPositions = new HashSet<>(10);
    /**
     * Used for memoization of getPrecedingSkippedCount(int) calls
     */
    protected SparseArray<Integer> mPreceedingSkipMemo = new SparseArray<>();

    @Override
    public int getOriginalPosition(int adjustedPosition, int noOfFetchedAds) {
        /*
        If we have a list with ad at startPosition 5 and repeatAfter 3, the ad positions will be
        at index {5, 9, 13, 17,...}.

        That means no. of ads preceding an adjusted position, for instance 2, should be the
        index of the next ad position (index 0 for ad position 5) and ads preceding adjusted
        position 6 should return the index of the next ad position (index 1 for ad position 9).

        So, in above example:
        size = 0, nextAdIndex = 0
        size = 1, nextAdIndex = 0
        size = 2, nextAdIndex = 0
        size = 3, nextAdIndex = 0
        size = 4, nextAdIndex = 0
        size = 5, nextAdIndex = 1
        size = 6, nextAdIndex = 1
        */
        int adIndex = getUnadjustedAdIndex(adjustedPosition);
        int noOfAdsPreceding = Math.min(adIndex + 1,
                noOfFetchedAds + getSkippedPositionCount()) -
                getPrecedingSkippedCount(adjustedPosition);
        return adjustedPosition - noOfAdsPreceding;
    }

    @Override
    public void addSkippedPosition(int positionToSkip) {
        mSkippedAdPositions.add(positionToSkip);
        // Remove previous memoization
        mPreceedingSkipMemo.remove(positionToSkip);
    }

    @Override
    public int getSkippedPositionCount() {
        return mSkippedAdPositions.size();
    }

    protected int getPrecedingSkippedCount(int currentPosition) {
        int precedingSkippedPositions = mPreceedingSkipMemo.get(currentPosition, 0);

        if (precedingSkippedPositions != 0) {
            return precedingSkippedPositions;
        }

        for (int skippedPosition : mSkippedAdPositions) {
            if (skippedPosition <= currentPosition) {
                ++precedingSkippedPositions;
            }
        }
        mPreceedingSkipMemo.put(currentPosition, precedingSkippedPositions);
        return precedingSkippedPositions;
    }

    /*
     Gets the ad index as if no ad positions were skipped.
     */
    @VisibleForTesting protected int getUnadjustedAdIndex(int adjustedPosition) {
        int precedingSkippedCount = getPrecedingSkippedCount(adjustedPosition);
        // Get adIndex as if no positions were skipped.
        int adIndex = getAdIndex(adjustedPosition + precedingSkippedCount);
        // Compensate for the adjustment that getAdIndex performs when precedingSkippedCount > 0
        return adIndex + getPrecedingSkippedCount(adjustedPosition + precedingSkippedCount);
        // TODO: there must be a better way
    }

    @VisibleForTesting protected void injectMockSkipMemo(SparseArray<Integer> mockSkipMemo) {
        mPreceedingSkipMemo = mockSkipMemo;
    }
}
