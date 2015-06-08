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
import android.widget.ArrayAdapter;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import com.yahoo.mobile.library.streamads.positioning.AdapterAdPositioner;
import com.yahoo.mobile.library.streamads.positioning.LinearIntervalAdPositioner;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class FlurryAdListAdapterTest extends TestCase {
    private FlurryAdListAdapter mAdListAdapter;
    private ArrayAdapter<Integer> mWrappedAdapter;
    private AdapterAdPositioner mAdPositioner;

    private final static int WRAPPED_ADAPTER_SIZE = 50;
    private final static int MOCK_AD_COUNT = 10;

    @Mock
    FlurryNativeAdFetcher mMockNativeAdFetcher;

    @Before
    public void setUp() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        MockitoAnnotations.initMocks(this);

        mWrappedAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_list_item_1);

        for (int i = 0; i < WRAPPED_ADAPTER_SIZE; i++) {
            mWrappedAdapter.add(i);
        }

        NativeAdViewBinder viewBinder = new NativeAdViewBinder.ViewBinderBuilder()
                .setAdLayoutId(android.R.layout.simple_list_item_2)
                .setHeadlineTextId(android.R.id.text1)
                .setDescriptionTextId(android.R.id.text2)
                .build();

        mAdPositioner = new LinearIntervalAdPositioner(3, 5);

        mAdListAdapter = FlurryAdListAdapter
                .from(activity, mWrappedAdapter, viewBinder, "FAKE_AD_SPACE")
                .setAdPositioner(mAdPositioner)
                .buildWithMockAdFetcher(mMockNativeAdFetcher);

        // Mock default FlurryNativeAdFetcher behaviour
        doNothing().when(mMockNativeAdFetcher).prefetchAds(anyString());
    }

    @Test
    public void testDataObserver() throws Exception {
        assertEquals(mWrappedAdapter.getCount(), mAdListAdapter.getCount());

        mWrappedAdapter.add(51);
        mWrappedAdapter.add(52);
        mWrappedAdapter.add(53);

        assertEquals(mWrappedAdapter.getCount(), mAdListAdapter.getCount());

        mWrappedAdapter.remove(1);
        mWrappedAdapter.remove(2);
        mWrappedAdapter.remove(3);
        mWrappedAdapter.remove(4);
        mWrappedAdapter.remove(5);

        assertEquals(mWrappedAdapter.getCount(), mAdListAdapter.getCount());
    }

    @Test
    public void testGetCount() throws Exception {
        assertEquals(mWrappedAdapter.getCount(), mAdListAdapter.getCount());

        doReturn(MOCK_AD_COUNT).when(mMockNativeAdFetcher).getQueuedAdsCount();

        assertEquals(WRAPPED_ADAPTER_SIZE + MOCK_AD_COUNT, mAdListAdapter.getCount());
    }

    @Test
    public void testGetItem() throws Exception {
        for (int i = 0; i < mAdListAdapter.getCount(); i++) {
            assertEquals(mWrappedAdapter.getItem(i), mAdListAdapter.getItem(i));
        }
    }

    @Test
    public void testGetItemId() throws Exception {
        for (int i = 0; i < mAdListAdapter.getCount(); i++) {
            assertEquals(mWrappedAdapter.getItemId(i), mAdListAdapter.getItemId(i));
        }
    }

    @Test
    public void testGetViewTypeCount() throws Exception {
        assertEquals(2, mAdListAdapter.getViewTypeCount());
    }

    @Test
    public void testGetOriginalPosition() throws Exception {
        for (int i = 0; i < mWrappedAdapter.getCount(); i++) {
            assertEquals(i, mAdListAdapter.getOriginalPosition(i));
        }

        doReturn(MOCK_AD_COUNT).when(mMockNativeAdFetcher).getQueuedAdsCount();

        assertEquals(4, mAdListAdapter.getOriginalPosition(5));
        assertEquals(14, mAdListAdapter.getOriginalPosition(17));
        assertEquals(24, mAdListAdapter.getOriginalPosition(29));
        assertEquals(34, mAdListAdapter.getOriginalPosition(41));
        assertEquals(44, mAdListAdapter.getOriginalPosition(53));
    }

    @Test
    public void testGetNumberOfAds() throws Exception {
        assertEquals(0, mAdListAdapter.getNumberOfAds());

        doReturn(MOCK_AD_COUNT).when(mMockNativeAdFetcher).getQueuedAdsCount();

        assertEquals(MOCK_AD_COUNT, mAdListAdapter.getNumberOfAds());

        doReturn(MOCK_AD_COUNT * 20).when(mMockNativeAdFetcher).getQueuedAdsCount();

        int maxFittableAds = mAdPositioner.getMaxFittableAds(mWrappedAdapter.getCount());
        assertEquals(maxFittableAds, mAdListAdapter.getNumberOfAds());
    }

    public void testSetRetryFailedAdPositions() throws Exception {

    }
}