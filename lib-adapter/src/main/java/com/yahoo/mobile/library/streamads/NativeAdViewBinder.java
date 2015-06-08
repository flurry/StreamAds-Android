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

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;

public class NativeAdViewBinder {
    private int mAdLayoutId;
    private int mHeadlineTextId;
    private int mDescriptionTextId;
    private int mSourceTextId;
    private int mBrandingLogoImageId;
    private int mAppStarRatingImageId;
    private int mAdImageId;

    NativeAdViewBinder() {
        // Prevent instantiation
    }

    int getAdLayoutId() {
        return mAdLayoutId;
    }

    int getHeadlineTextId() {
        return mHeadlineTextId;
    }

    int getDescriptionTextId() {
        return mDescriptionTextId;
    }

    int getSourceTextId() {
        return mSourceTextId;
    }

    int getBrandingLogoImageId() {
        return mBrandingLogoImageId;
    }

    int getAppStarRatingImageId() {
        return mAppStarRatingImageId;
    }

    int getAdImageId() {
        return mAdImageId;
    }

    public final static class ViewBinderBuilder {
        NativeAdViewBinder mNativeAdViewBinder;

        public ViewBinderBuilder() {
            mNativeAdViewBinder = new NativeAdViewBinder();
        }

        public ViewBinderBuilder setAdLayoutId(@LayoutRes int adLayoutId) {
            mNativeAdViewBinder.mAdLayoutId = adLayoutId;
            return this;
        }

        public ViewBinderBuilder setHeadlineTextId(@IdRes int headlineTextId) {
            mNativeAdViewBinder.mHeadlineTextId = headlineTextId;
            return this;
        }

        public ViewBinderBuilder setDescriptionTextId(@IdRes int summaryTextId) {
            mNativeAdViewBinder.mDescriptionTextId = summaryTextId;
            return this;
        }

        public ViewBinderBuilder setSourceTextId(@IdRes int sourceTextId) {
            mNativeAdViewBinder.mSourceTextId = sourceTextId;
            return this;
        }

        public ViewBinderBuilder setBrandingLogoImageId(@IdRes int brandingLogoImageId) {
            mNativeAdViewBinder.mBrandingLogoImageId = brandingLogoImageId;
            return this;
        }

        public ViewBinderBuilder setAppStarRatingImageId(@IdRes int appStarRatingImageId) {
            mNativeAdViewBinder.mAppStarRatingImageId = appStarRatingImageId;
            return this;
        }

        public ViewBinderBuilder setAdImageId(@IdRes int adImageId) {
            mNativeAdViewBinder.mAdImageId = adImageId;
            return this;
        }

        public NativeAdViewBinder build() {
            return mNativeAdViewBinder;
        }
    }
}
