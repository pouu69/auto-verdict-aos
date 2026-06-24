package com.car.autoverdict.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    // AdView는 항상 컴포즈되어 있어야 loadAd가 실행된다.
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdIds.banner
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdBanner", "onAdLoaded — banner ID=${AdIds.banner}")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.d("AdBanner", "onAdFailedToLoad code=${error.code} msg=${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
