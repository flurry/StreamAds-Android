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
import android.view.View;

import com.flurry.android.ads.FlurryAdNative;

/**
 * View holder that also holds a mapping of a {@link FlurryAdNative} to a View so that when
 * recycling that View, the ad object can be disassociated with that View and be associated with
 * another.
 */
class FlurryAdViewHolder extends NativeAdViewHolder {
    @Nullable FlurryAdNative flurryAdNative;

    private FlurryAdViewHolder(@NonNull View parentView,
                               @NonNull NativeAdViewBinder viewBinder) {
        super(parentView, viewBinder);
    }

    static FlurryAdViewHolder newInstance(@NonNull View parentView,
                                          @NonNull NativeAdViewBinder viewBinder,
                                          @Nullable FlurryAdNative flurryAdNative) {
        FlurryAdViewHolder viewHolder = new FlurryAdViewHolder(parentView, viewBinder);
        viewHolder.flurryAdNative = flurryAdNative;

        return viewHolder;
    }
}
