package com.car.autoverdict.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/** Bottom-anchored 320x50 banner used across the main screens. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AdViewHost(
        adSize = AdSize.BANNER,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * 300x250 medium rectangle — shown on the "분석중" loading screen where there is
 * empty space and the user is already waiting. Wraps its own width so it can be
 * centered by the caller.
 */
@Composable
fun AdMediumRectangle(modifier: Modifier = Modifier) {
    AdViewHost(
        adSize = AdSize.MEDIUM_RECTANGLE,
        modifier = modifier.wrapContentWidth(Alignment.CenterHorizontally),
    )
}

@Composable
private fun AdViewHost(adSize: AdSize, modifier: Modifier = Modifier) {
    // AdView는 항상 컴포즈되어 있어야 loadAd가 실행된다.
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                // AdMob 배너 형식 단위는 여러 배너 사이즈(300x250 포함)를 서빙하므로
                // 별도 단위 없이 동일 ID를 재사용한다. 디버그는 테스트 ID.
                adUnitId = AdIds.banner
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdBanner", "onAdLoaded — size=$adSize ID=${AdIds.banner}")
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
