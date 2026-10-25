package com.car.autoverdict.ui

import com.car.autoverdict.BuildConfig

/** Debug always uses Google's demo inventory: https://developers.google.com/admob/android/test-ads */
object AdIds {
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    private const val MAIN_BANNER = "ca-app-pub-5353705048338468/1748874472"
    private const val RESULT_NATIVE = "ca-app-pub-5353705048338468/2138526753"
    private const val APP_OPEN = "ca-app-pub-5353705048338468/2486011110"

    val banner: String? = selectAdUnit(BuildConfig.DEBUG, TEST_BANNER, MAIN_BANNER)
    val resultNative: String? = selectAdUnit(BuildConfig.DEBUG, TEST_NATIVE, RESULT_NATIVE)
    val appOpen: String? = selectAdUnit(BuildConfig.DEBUG, TEST_APP_OPEN, APP_OPEN)
}

/** Missing/invalid production configuration disables the placement instead of serving demo ads. */
internal fun selectAdUnit(debug: Boolean, testId: String, productionId: String): String? =
    if (debug) testId else productionId.takeIf {
        it.matches(Regex("ca-app-pub-5353705048338468/[0-9]{10}"))
    }
