package com.car.autoverdict.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.car.autoverdict.ui.ads.LocalContentAdsAllowed
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/** Both the shell and the ad use this size, reserving space before the request finishes. */
@Composable
private fun adaptiveBannerSize(): AdSize {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration.screenWidthDp, configuration.orientation) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, configuration.screenWidthDp)
    }
}

@Composable
fun adaptiveBannerSlotHeight(): Dp =
    if (LocalContentAdsAllowed.current && AdIds.banner != null) adaptiveBannerSize().height.dp + 16.dp else 0.dp

/** Home/saved share one placement. The shell adds separation from content and tab controls. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!LocalContentAdsAllowed.current) return
    val unitId = AdIds.banner ?: return
    val adSize = adaptiveBannerSize()
    key(unitId, adSize.width, adSize.height) {
        val adViewRef = remember { mutableStateOf<AdView?>(null) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adViewRef.value?.pause()
                    Lifecycle.Event.ON_RESUME -> adViewRef.value?.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        Box(modifier = modifier.fillMaxWidth().height(adSize.height.dp), contentAlignment = Alignment.Center) {
            AndroidView(
                onRelease = { view -> adViewRef.value = null; view.destroy() },
                factory = { context ->
                    AdView(context).also { view ->
                        adViewRef.value = view
                        view.setAdSize(adSize)
                        view.adUnitId = unitId
                        view.adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.d("MainBanner", "load_failed code=${error.code}")
                            }
                            override fun onAdImpression() { Log.d("MainBanner", "impression") }
                        }
                        view.loadAd(AdRequest.Builder().build())
                    }
                },
            )
        }
    }
}
