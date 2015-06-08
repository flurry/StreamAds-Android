package com.yahoo.mobile.library.streamads;

import android.app.Activity;
import android.content.Context;

import com.yahoo.mobile.library.streamads.positioning.LinearIntervalAdPositioner;

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

// TODO: Cannot mock FlurryNativeAdFetcher#popLoadedAd(), so testing is VERY limited
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class FlurryBaseAdAdapterTest extends TestCase {

    private final static int MOCK_AD_COUNT = 5;
    private final static int WRAPPED_ADAPTER_SIZE = 20;

    private FlurryBaseAdAdapter mFlurryBaseAdAdapter;

    @Mock
    FlurryNativeAdFetcher mMockNativeAdFetcher;
    MockAdapterImpl mMockAdapter;
    /*
     Cannot be mocked by Mockito because 'final' and cannot be mocked externally because of
     Java 7 [0] and an issue in Robolectric [1].

     See FlurryAdListAdapterTest#initMocks(Context).


     [0] https://www.java.net/print/885714
     [1] https://github.com/robolectric/robolectric-gradle-plugin/issues/144
     */
    // FlurryAdNative mMockFlurryAdNative;

    @Before
    public void setUp() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        initMocks(activity);

        // Using the default configuration for the base ad adapter (positioner(3,3))
        mFlurryBaseAdAdapter = new FlurryBaseAdAdapter(mMockAdapter);
        mFlurryBaseAdAdapter.initAdFetcher(activity);
        mFlurryBaseAdAdapter.injectMockAdFetcher(mMockNativeAdFetcher);

        // Mock default FlurryNativeAdFetcher behaviour
        doNothing().when(mMockNativeAdFetcher).prefetchAds(anyString());
        // doReturn(mMockFlurryAdNative).when(mMockNativeAdFetcher).popLoadedAd();
    }

    @SuppressWarnings("unused")
    private void initMocks(Context context) {
        MockitoAnnotations.initMocks(this);
        mMockAdapter = new MockAdapterImpl();
        /*
         TODO: Fails with java.lang.VerifyError when compiling. Option is to use
         -XX:-UseSplitVerifier. See http://stackoverflow.com/q/15253173/779983
         */
        // mMockFlurryAdNative = new FlurryAdNative(context, "FAKE_AD_SPACE");
    }

    @Test
    public void testGetOriginalPosition() throws Exception {
        assertEquals(5, mFlurryBaseAdAdapter.getOriginalPosition(5, WRAPPED_ADAPTER_SIZE));
        assertEquals(15, mFlurryBaseAdAdapter.getOriginalPosition(15, WRAPPED_ADAPTER_SIZE));

        doReturn(MOCK_AD_COUNT).when(mMockNativeAdFetcher).getQueuedAdsCount();

        assertEquals(4, mFlurryBaseAdAdapter.getOriginalPosition(5, WRAPPED_ADAPTER_SIZE));
        assertEquals(11, mFlurryBaseAdAdapter.getOriginalPosition(15, WRAPPED_ADAPTER_SIZE));
    }

    @Test
    public void testGetNumberOfAds() throws Exception {
        assertEquals(0, mFlurryBaseAdAdapter.getNumberOfAds(WRAPPED_ADAPTER_SIZE));

        doReturn(MOCK_AD_COUNT).when(mMockNativeAdFetcher).getQueuedAdsCount();

        assertEquals(MOCK_AD_COUNT, mFlurryBaseAdAdapter.getNumberOfAds(WRAPPED_ADAPTER_SIZE));

        assertEquals(3, mFlurryBaseAdAdapter.getNumberOfAds(WRAPPED_ADAPTER_SIZE));
    }

    @Test
    public void testSetPositioner() throws Exception {
        mFlurryBaseAdAdapter.setPositioner(new LinearIntervalAdPositioner(3, 20),
                WRAPPED_ADAPTER_SIZE);

        doReturn(MOCK_AD_COUNT).when(mMockNativeAdFetcher).getQueuedAdsCount();

        assertEquals(1, mFlurryBaseAdAdapter.getNumberOfAds(WRAPPED_ADAPTER_SIZE));

        mFlurryBaseAdAdapter.setPositioner(new LinearIntervalAdPositioner(3, 3),
                WRAPPED_ADAPTER_SIZE);

        assertEquals(MOCK_AD_COUNT, mFlurryBaseAdAdapter.getNumberOfAds(WRAPPED_ADAPTER_SIZE));
    }

    private final static class MockAdapterImpl implements NativeAdAdapter,
            FlurryBaseAdAdapter.ListAdapterDataListener {

        @Override
        public void notifyDataSetChanged() { }

        @Override
        public void refreshAds() { }

        @Override
        public void destroyAds() { }

        @Override
        public void addAdRenderListener(NativeAdRenderListener adRenderListener) { }

        @Override
        public int getOriginalPosition(int position) { return 0; }

        @Override
        public int getNumberOfAds() { return 0; }

        @Override
        public void setRetryFailedAdPositions(boolean retryFailedAdPositions) { }
    }
}