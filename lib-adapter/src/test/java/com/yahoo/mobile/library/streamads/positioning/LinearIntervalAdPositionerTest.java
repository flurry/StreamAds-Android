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

import android.util.SparseArray;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

public class LinearIntervalAdPositionerTest extends TestCase {
    List<Integer> dataList;

    @Mock
    SparseArray<Integer> mMockPrecedingSkippedMemo;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        dataList = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            dataList.add(i);
        }

        doReturn(0).when(mMockPrecedingSkippedMemo).get(anyInt());
        doReturn(0).when(mMockPrecedingSkippedMemo).get(anyInt(), eq(0));
    }

    public void testCanPlaceAd() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);

        for (int i = 0; i <= 45; i++) {
            if (i == 3) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 9) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 15) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 21) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 27) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 33) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 39) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 45) { assertTrue(positioner.canPlaceAd(i)); continue; }

            assertFalse(positioner.canPlaceAd(i));
        }

        positioner = new LinearIntervalAdPositioner(6, 4);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        for (int i = 0; i <= 21; i++) {
            if (i == 6) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 11) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 16) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 21) { assertTrue(positioner.canPlaceAd(i)); continue; }

            assertFalse(positioner.canPlaceAd(i));
        }
    }

    public void testCanPlaceAd_skipped() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);

        positioner.addSkippedPosition(3);
        positioner.addSkippedPosition(14);
        positioner.addSkippedPosition(25);
        positioner.addSkippedPosition(36);

        for (int i = 0; i <= 45; i++) {
            if (i == 8) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 19) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 30) { assertTrue(positioner.canPlaceAd(i)); continue; }
            if (i == 41) { assertTrue(positioner.canPlaceAd(i)); continue; }

            assertFalse(positioner.canPlaceAd(i));
        }
    }

    public void testGetMaxFittableAds() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(0, 1);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        int result = positioner.getMaxFittableAds(dataList.size());
        assertEquals(1, result); // Min repeat interval is 2, so repeat interval will be unset

        positioner = new LinearIntervalAdPositioner(0, 2);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        result = positioner.getMaxFittableAds(dataList.size());
        assertEquals(26, result);

        positioner = new LinearIntervalAdPositioner(1, 3);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        result = positioner.getMaxFittableAds(dataList.size());
        assertEquals(17, result);

        positioner = new LinearIntervalAdPositioner(2, 4);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        result = positioner.getMaxFittableAds(dataList.size());
        assertEquals(13, result);

        positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        result = positioner.getMaxFittableAds(dataList.size());
        assertEquals(10, result);

        positioner = new LinearIntervalAdPositioner(4, 100);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        result = positioner.getMaxFittableAds(dataList.size());
        assertEquals(1, result);

        positioner = new LinearIntervalAdPositioner(5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        result = positioner.getMaxFittableAds(dataList.size());
        assertEquals(1, result);
    }

    public void testGetAdIndex() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 3);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        assertEquals(0, positioner.getAdIndex(3));
        assertEquals(1, positioner.getAdIndex(7));
        assertEquals(2, positioner.getAdIndex(11));
        assertEquals(3, positioner.getAdIndex(15));

        positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        assertEquals(0, positioner.getAdIndex(3));
        assertEquals(1, positioner.getAdIndex(9));
        assertEquals(2, positioner.getAdIndex(15));
        assertEquals(3, positioner.getAdIndex(21));
        assertEquals(4, positioner.getAdIndex(27));
        assertEquals(5, positioner.getAdIndex(33));
        assertEquals(6, positioner.getAdIndex(39));
        assertEquals(7, positioner.getAdIndex(45));

        positioner = new LinearIntervalAdPositioner(6, 4);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        assertEquals(0, positioner.getAdIndex(6));
        assertEquals(1, positioner.getAdIndex(11));
        assertEquals(2, positioner.getAdIndex(16));
        assertEquals(3, positioner.getAdIndex(21));
        assertEquals(4, positioner.getAdIndex(26));
        assertEquals(5, positioner.getAdIndex(31));
        assertEquals(6, positioner.getAdIndex(36));
        assertEquals(7, positioner.getAdIndex(41));
        assertEquals(8, positioner.getAdIndex(46));
        assertEquals(9, positioner.getAdIndex(51));
        assertEquals(10, positioner.getAdIndex(56));
        assertEquals(11, positioner.getAdIndex(61));
    }

    public void testGetAdIndex_skipped() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        positioner.addSkippedPosition(3);
        assertEquals(0, positioner.getAdIndex(9));
        assertEquals(1, positioner.getAdIndex(15));
        assertEquals(2, positioner.getAdIndex(21));
        assertEquals(3, positioner.getAdIndex(27));
        assertEquals(4, positioner.getAdIndex(33));
        assertEquals(5, positioner.getAdIndex(39));
        assertEquals(6, positioner.getAdIndex(45));

        positioner.addSkippedPosition(9);
        assertEquals(0, positioner.getAdIndex(15));
        assertEquals(1, positioner.getAdIndex(21));
        assertEquals(2, positioner.getAdIndex(27));
        assertEquals(3, positioner.getAdIndex(33));
        assertEquals(4, positioner.getAdIndex(39));
        assertEquals(5, positioner.getAdIndex(45));

        positioner.addSkippedPosition(27);
        assertEquals(0, positioner.getAdIndex(15));
        assertEquals(1, positioner.getAdIndex(21));
        assertEquals(2, positioner.getAdIndex(33));
        assertEquals(3, positioner.getAdIndex(39));
        assertEquals(4, positioner.getAdIndex(45));
    }

    public void testGetOriginalPosition() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        assertEquals(4, positioner.getOriginalPosition(5, 100));
        assertEquals(14, positioner.getOriginalPosition(17, 100));
        assertEquals(24, positioner.getOriginalPosition(29, 100));
        assertEquals(34, positioner.getOriginalPosition(41, 100));
        assertEquals(44, positioner.getOriginalPosition(53, 100));

        assertEquals(15, positioner.getOriginalPosition(17, 2));
        assertEquals(14, positioner.getOriginalPosition(17, 3));
        assertEquals(27, positioner.getOriginalPosition(29, 2));
        assertEquals(26, positioner.getOriginalPosition(29, 3));
    }

    public void testGetOriginalPosition_skipped() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        positioner.addSkippedPosition(3);

        assertEquals(5, positioner.getOriginalPosition(5, 100));
        assertEquals(15, positioner.getOriginalPosition(17, 100));
        assertEquals(25, positioner.getOriginalPosition(29, 100));
        assertEquals(35, positioner.getOriginalPosition(41, 100));
        assertEquals(45, positioner.getOriginalPosition(53, 100));

        positioner.addSkippedPosition(9);

        assertEquals(5, positioner.getOriginalPosition(5, 100));
        assertEquals(16, positioner.getOriginalPosition(17, 100));
        assertEquals(26, positioner.getOriginalPosition(29, 100));
        assertEquals(36, positioner.getOriginalPosition(41, 100));
        assertEquals(46, positioner.getOriginalPosition(53, 100));

        positioner.addSkippedPosition(27);

        assertEquals(5, positioner.getOriginalPosition(5, 100));
        assertEquals(16, positioner.getOriginalPosition(17, 100));
        assertEquals(27, positioner.getOriginalPosition(29, 100));
        assertEquals(37, positioner.getOriginalPosition(41, 100));
        assertEquals(47, positioner.getOriginalPosition(53, 100));
    }

    public void testAddSkippedPosition() throws Exception {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);
        positioner.addSkippedPosition(9);
        positioner.addSkippedPosition(21);
        positioner.addSkippedPosition(33);

        assertEquals(3, positioner.getSkippedPositionCount());

        positioner.addSkippedPosition(3);
        positioner.addSkippedPosition(15);
        positioner.addSkippedPosition(33); // Duplicate

        assertEquals(5, positioner.getSkippedPositionCount());
    }

    public void testGetPrecedingSkippedCount() {
        LinearIntervalAdPositioner positioner = new LinearIntervalAdPositioner(3, 5);
        positioner.injectMockSkipMemo(mMockPrecedingSkippedMemo);

        for (int i = 0; i < 50; i++) {
            assertEquals(0, positioner.getPrecedingSkippedCount(i));
        }

        positioner.addSkippedPosition(3);
        for (int i = 4; i < 50; i++) {
            assertEquals(1, positioner.getPrecedingSkippedCount(i));
        }

        positioner.addSkippedPosition(9);
        for (int i = 10; i < 50; i++) {
            assertEquals(2, positioner.getPrecedingSkippedCount(i));
        }

        positioner.addSkippedPosition(15);
        for (int i = 16; i < 50; i++) {
            assertEquals(3, positioner.getPrecedingSkippedCount(i));
        }

        for (int i = 4; i < 9; i++) {
            assertEquals(1, positioner.getPrecedingSkippedCount(i));
        }
    }
}