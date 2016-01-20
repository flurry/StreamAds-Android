# Flurry Stream Ads Helper Library

Android library to help ad publishers easily integrate ads into their app streams.

## What this provides

- Handles ad fetching, caching and the entire ad lifecycle
- Handles ad positioning logic in stream
- Extensibility and ability to add new positioning logic

## How to use

After creating your data adapter, you can create an ad adapter wrapper using:

```java
FlurryAdListAdapter adAdapter = FlurryAdListAdapter.from(context, myAdapter, viewBinder, adSpace)
        .setAdPositioner(new LinearLayoutPositioner(3, 4))
        .build();
```

This will create an adapter with an ad at position 3 and with another ad appearing after every other
4 positions.

That's about all it takes to integrate ads into your data adapter. See the
[Samples](integration-sample/src/main/java/com/yahoo/mobile/client/streamads/SampleListFragment.java)
or read the sections below to learn more.

### Setting up a NativeAdViewBinder

Before setting up the ad adapter, you would need to set up a view binder that knows which view
resources to use for each native ad asset when building the native ad.

```java
NativeAdViewBinder viewBinder = viewBinderBuilder.setAdLayoutId(R.layout.list_item_ad)
                .setHeadlineTextId(R.id.ad_headline)
                .setDescriptionTextId(R.id.ad_description)
                .setSourceTextId(R.id.ad_source)
                .setBrandingLogoImageId(R.id.sponsored_image)
                .setAppStarRatingImageId(R.id.app_rating_image)
                .setAdImageId(R.id.ad_image)
                .build();
```

It is not necessary to specify a view for each ad asset. Any asset that is not bound to a view will
not be used when building the native ad.

### Setting ad positioning logic

The logic for positioning ads within the stream is handled by implementations of the
[`AdapterAdPositioner`](lib-adapter/src/main/java/com/yahoo/mobile/library/streamads/positioning/AdapterAdPositioner.java)
interface.

The library comes with one implementation, `LinearIntervalAdPositioner`, which uses a linear
function to determine ad placement intervals. To instantiate this positioner, you need to pass in an
ad start position and an ad repeat interval.

```java
AdapterAdPositioner positioner = new LinearLayoutPositioner(startPosition, repeatInterval);
```

Setting a `startPosition` of 3, for example, will make an ad take up the third position in the
adapter.

Setting a `repeatInterval` of x will make an ad show up after every x positions _after_ the start
position. To disable repeat ads in stream, you can set `repeatInterval` to 0.

You can also provide your own implementations of `AdapterAdPositioner` if you choose to use a
different ad placement logic.

### Other options

When building the adapter, you can specify the `FlurryAdTargeting` to be used for each ad in the
adapter.

You could also choose to register your own `FlurryAdNativeListener` if you choose to monitor native
ad callbacks, e.g. for reporting purposes.
A [stub](/lib-adapter/src/main/java/com/yahoo/mobile/library/streamads/StubFlurryAdNativeListener.java)
is provided for you to selectively implement callback methods.

If the `FlurryAdNativeListener` is too cumbersome, you could listen for callbacks from the simpler
`NativeAdApapter.NativeAdRenderListener` instead.

Finally, the `NativeAdAdapter` provides support for retrying an ad fetch for a position that is
on-screen that but failed to show an ad. By default, the adapter will not retry failed positions
after the position is on-screen. This prevents "jumpy" lists where an ad pops in after its position
is already on-screen.

To disable this and render ads at all specified positions, call `NativeAdAdapter#setRetryFailedAdPositions(true)`

For more help on the Flurry SDK, visit the 
[Yahoo Developer Network documentation](https://developer.yahoo.com/flurry/docs/publisher/code/android/).

## What's New

### Version 1.2 (January 19, 2016)

* Adds support for Flurry Expandable Ads in `ListView` streams. See the 
[ListView sample](integration-sample/src/main/java/com/yahoo/mobile/client/streamads/SampleListFragment.java)
for more.
* Removes the Flurry SDK as a compile-time dependency in the library. You can now include your own 
version of Flurry while including this library.

## Copyright

    Copyright 2015 Yahoo Inc. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.