package com.daksin.autoverdict.ui

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun load(activity: Activity) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            activity,
            // 테스트 광고 ID — AdMob에서 전면 광고 단위 생성 후 교체
            "ca-app-pub-5353705048338468/9597912744",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d("InterstitialAd", "Failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            },
        )
    }

    fun showIfReady(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                load(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                load(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
