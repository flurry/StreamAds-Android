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

import android.support.annotation.NonNull;

import com.flurry.android.ads.FlurryAdNative;

/**
 * Utility class that handles rendering Flurry native ads.
 */
final class FlurryNativeAdViewBuilder {

    // Assets documented here: https://developer.yahoo.com/flurry/docs/publisher/code/android/
    private static final String AD_ASSET_SUMMARY = "summary";
    private static final String AD_ASSET_HEADLINE = "headline";
    private static final String AD_ASSET_SOURCE = "source";
    private static final String AD_ASSET_SEC_HQ_BRANDING_LOGO = "secHqBrandingLogo";
    private static final String AD_ASSET_SEC_BRANDING_LOGO = "secBrandingLogo";
    private static final String AD_ASSET_SEC_HQ_RATING_IMAGE = "secHqRatingImg";
    private static final String AD_ASSET_SEC_RATING_IMAGE = "secRatingImg";
    private static final String AD_ASSET_SHOW_RATING = "showRating";
    private static final String AD_ASSET_CALL_TO_ACTION = "callToAction";
    private static final String AD_ASSET_SEC_HQ_IMAGE = "secHqImage";
    private static final String AD_ASSET_SEC_IMAGE = "secImage";

    private static final int SEC_BRANDING_LOGO_WIDTH = 20;
    private static final int SEC_RATING_IMAGE_WIDTH = 77;
    private static final int SEC_IMAGE_WIDTH = 82;
    private static final int SEC_IMAGE_HEIGHT = 82;
    private static final int SEC_HQ_IMAGE_HEIGHT = 627;

    /**
     * Renders a Flurry native ad unto given views.
     * @param flurryAdNative the {@link FlurryAdNative} object
     * @param viewHolder a view holder containing the views
     */
    static void buildAdIntoViews(@NonNull FlurryAdNative flurryAdNative,
                                 @NonNull FlurryAdViewHolder viewHolder) {
        viewHolder.flurryAdNative = flurryAdNative;

        // Clear previous values
        clearAdHolder(viewHolder);

        // Null views will be handled by the SDK
        flurryAdNative.getAsset(AD_ASSET_SUMMARY).loadAssetIntoView(viewHolder.descriptionTextView);
        flurryAdNative.getAsset(AD_ASSET_HEADLINE).loadAssetIntoView(viewHolder.headlineTextView);
        flurryAdNative.getAsset(AD_ASSET_SOURCE).loadAssetIntoView(viewHolder.sourceTextView);
        flurryAdNative.getAsset(AD_ASSET_CALL_TO_ACTION).loadAssetIntoView(viewHolder.callToActionView);

        // If ImageView is too large for smallest branding logo, use larger branding logo
        if (viewHolder.brandingLogoImageView != null &&
                viewHolder.brandingLogoImageView.getWidth() > 0) {
            String brandingAssetToLoad;
            if (viewHolder.brandingLogoImageView.getWidth() > SEC_BRANDING_LOGO_WIDTH) {
                brandingAssetToLoad = AD_ASSET_SEC_HQ_BRANDING_LOGO;
            } else {
                brandingAssetToLoad = AD_ASSET_SEC_BRANDING_LOGO;
            }
            flurryAdNative.getAsset(brandingAssetToLoad).loadAssetIntoView(
                    viewHolder.brandingLogoImageView);
        }


        boolean shouldShowRating = flurryAdNative.getAsset(AD_ASSET_SHOW_RATING) != null ?
                Boolean.valueOf(flurryAdNative.getAsset(AD_ASSET_SHOW_RATING).getValue()) : false;

        // If ImageView is too large for smallest app rating image, use larger app rating image
        if (viewHolder.appStarRatingImageView != null &&
                viewHolder.appStarRatingImageView.getWidth() > 0 && shouldShowRating) {
            String starRatingAssetToLoad;
            if (viewHolder.appStarRatingImageView.getWidth() > SEC_RATING_IMAGE_WIDTH) {
                starRatingAssetToLoad = AD_ASSET_SEC_HQ_RATING_IMAGE;
            } else {
                starRatingAssetToLoad = AD_ASSET_SEC_RATING_IMAGE;
            }

            if (flurryAdNative.getAsset(starRatingAssetToLoad) != null) {
                flurryAdNative.getAsset(starRatingAssetToLoad).loadAssetIntoView(
                        viewHolder.appStarRatingImageView);
            }
        }

        // If ImageView is too large for smallest ad image, use larger ad image
        if (viewHolder.adImageView != null && viewHolder.adImageView.getWidth() > 0) {
            String adImageAssetToLoad;
            int imageHeight;
            if (viewHolder.adImageView.getWidth() > SEC_IMAGE_WIDTH) {
                adImageAssetToLoad = AD_ASSET_SEC_HQ_IMAGE;
                imageHeight = SEC_HQ_IMAGE_HEIGHT;
            } else {
                adImageAssetToLoad = AD_ASSET_SEC_IMAGE;
                imageHeight = SEC_IMAGE_HEIGHT;
            }

            if (flurryAdNative.getAsset(adImageAssetToLoad) != null) {
                flurryAdNative.getAsset(adImageAssetToLoad).loadAssetIntoView(
                        viewHolder.adImageView);
                // Prevent flickering row height from dynamic image
                viewHolder.adImageView.getLayoutParams().height = imageHeight;
            } else {
                /*
                Because we're adjusting height above, we have to set height to 0 in case of
                recycled views.
                */
                viewHolder.adImageView.getLayoutParams().height = 0;
            }
        }
    }

    private static void clearAdHolder(@NonNull FlurryAdViewHolder viewHolder) {
        if (viewHolder.descriptionTextView != null) {
            viewHolder.descriptionTextView.setText(null);
        }

        if (viewHolder.headlineTextView != null) {
            viewHolder.headlineTextView.setText(null);
        }

        if (viewHolder.sourceTextView != null) {
            viewHolder.sourceTextView.setText(null);
        }

        if (viewHolder.brandingLogoImageView != null) {
            viewHolder.brandingLogoImageView.setImageDrawable(null);
        }

        if (viewHolder.appStarRatingImageView != null) {
            viewHolder.appStarRatingImageView.setImageDrawable(null);
        }

        if (viewHolder.adImageView != null) {
            viewHolder.adImageView.setImageDrawable(null);
        }
    }
}
