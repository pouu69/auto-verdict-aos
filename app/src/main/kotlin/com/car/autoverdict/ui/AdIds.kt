package com.car.autoverdict.ui

import com.car.autoverdict.BuildConfig

/**
 * 광고 단위 ID 공급자.
 *
 * 디버그 빌드에서는 Google 공식 테스트 광고 ID를 사용한다.
 * 개발 중 실제 광고를 노출·클릭하면 "무효 트래픽(invalid traffic)" 정책 위반으로
 * 계정이 정지될 수 있으므로, 실제 광고 ID는 릴리스 빌드에서만 사용한다.
 *
 * 테스트 ID 출처: https://developers.google.com/admob/android/test-ads
 */
object AdIds {
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val REAL_BANNER = "ca-app-pub-5353705048338468/4066384469"

    val banner: String = if (BuildConfig.DEBUG) TEST_BANNER else REAL_BANNER
}
