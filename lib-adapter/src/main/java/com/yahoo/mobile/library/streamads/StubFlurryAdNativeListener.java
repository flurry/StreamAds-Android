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

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeListener;

/**
 * Convenience class for creating {@link FlurryAdNativeListener} objects. You can choose to
 * override only the methods you are interested in. All other methods are empty.
 *
 * Inconsistent with Java Event API naming conventions, but this isn't really Java and calling this
 * an "Adapter" is confusing.
 */
public abstract class StubFlurryAdNativeListener implements FlurryAdNativeListener {
    @Override
    public void onAppExit(FlurryAdNative flurryAdNative) { }

    @Override
    public void onClicked(FlurryAdNative flurryAdNative) { }

    @Override
    public void onCloseFullscreen(FlurryAdNative flurryAdNative) { }

    @Override
    public void onError(FlurryAdNative flurryAdNative, FlurryAdErrorType flurryAdErrorType,
                        int errorCode) { }

    @Override
    public void onFetched(FlurryAdNative flurryAdNative) { }

    @Override
    public void onImpressionLogged(FlurryAdNative flurryAdNative) { }

    @Override
    public void onShowFullscreen(FlurryAdNative flurryAdNative) { }

    @Override
    public void onCollapsed(FlurryAdNative flurryAdNative) { }

    @Override
    public void onExpanded(FlurryAdNative flurryAdNative) { }
}
