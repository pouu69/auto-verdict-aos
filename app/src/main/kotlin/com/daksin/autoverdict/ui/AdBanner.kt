package com.daksin.autoverdict.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    var adLoaded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = adLoaded,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-5353705048338468/4066384469"
                    adListener = object : AdListener() {
                        override fun onAdLoaded() { adLoaded = true }
                        override fun onAdFailedToLoad(error: LoadAdError) { adLoaded = false }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = modifier.fillMaxWidth(),
        )
    }
}
