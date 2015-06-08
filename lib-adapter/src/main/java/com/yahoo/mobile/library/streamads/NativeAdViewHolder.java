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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class NativeAdViewHolder {
    private final static String TAG = NativeAdViewHolder.class.getSimpleName();
    @Nullable
    TextView headlineTextView;
    @Nullable
    TextView descriptionTextView;
    @Nullable
    TextView sourceTextView;
    @Nullable
    ImageView brandingLogoImageView;
    @Nullable
    ImageView appStarRatingImageView;
    @Nullable
    ImageView adImageView;

    protected NativeAdViewHolder(@NonNull View parentView,
                                 @NonNull NativeAdViewBinder viewBinder) {
        headlineTextView = initView(parentView, viewBinder.getHeadlineTextId(), TextView.class);
        descriptionTextView = initView(parentView, viewBinder.getDescriptionTextId(), TextView.class);
        sourceTextView = initView(parentView, viewBinder.getSourceTextId(), TextView.class);
        brandingLogoImageView = initView(parentView, viewBinder.getBrandingLogoImageId(), ImageView.class);
        appStarRatingImageView = initView(parentView, viewBinder.getAppStarRatingImageId(), ImageView.class);
        adImageView = initView(parentView, viewBinder.getAdImageId(), ImageView.class);
    }

    /**
     * Unnecessarily complicated method to initialize a View from a parent View and avoid failing
     * if just one view resource ID is incorrect or not given.
     *
     * @param parentView the parent View containing the View to init
     * @param resourceId the resource ID to search for in parent View
     * @param type the class type of the expected View
     * @param <T> the type of expected View
     * @return the expected View or null if the View was not found
     */
    private static <T extends View> T initView(View parentView, int resourceId, Class<T> type) {
        try {
            return type.cast(parentView.findViewById(resourceId));
        } catch (NullPointerException | ClassCastException ex) {
            Log.w(TAG, "Cannot find invalid resource ID " + resourceId);
            return null;
        }
    }
}
