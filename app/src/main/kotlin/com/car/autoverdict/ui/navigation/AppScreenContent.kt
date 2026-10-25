package com.car.autoverdict.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.BuildConfig
import com.car.autoverdict.home.HomeRoute
import com.car.autoverdict.onboarding.OnboardingMode
import com.car.autoverdict.onboarding.OnboardingScreen
import com.car.autoverdict.preferences.AppPreferences
import com.car.autoverdict.result.ResultRoute
import com.car.autoverdict.ui.ResultNativeAd
import com.car.autoverdict.ui.rememberResultNativeAdState
import com.car.autoverdict.ui.gallery.ComponentGalleryScreen
import com.car.autoverdict.ui.screen.CompareRoute
import com.car.autoverdict.ui.screen.LicenseDetailScreen
import com.car.autoverdict.ui.screen.LicensesScreen
import com.car.autoverdict.ui.screen.PrivacyPolicyScreen
import com.car.autoverdict.ui.screen.SavedListRoute
import com.car.autoverdict.ui.screen.SettingsRoute

/** Maps each [Route] to its real screen. Kept apart from AppRoot so tests can inject fakes. */
@Composable
fun AppScreenContent(route: Route, host: ScreenHost) {
    when (route) {
        Route.Onboarding -> {
            val mode = if (host.isFirstRunOnboarding) OnboardingMode.FirstRun else OnboardingMode.Replay
            OnboardingScreen(
                mode = mode,
                hasPendingShare = host.hasPendingShare,
                onFinish = host.finishOnboarding,
                onBack = host.back.takeIf { mode == OnboardingMode.Replay },
            )
        }
        Route.Home -> HomeRoute(host)
        Route.Saved -> {
            val context = LocalContext.current
            val app = context.applicationContext as AutoVerdictApp
            val preferences = remember(context) { AppPreferences(context) }
            SavedListRoute(
                repository = app.savedCarsRepository,
                preferences = preferences,
                onOpenResult = { url -> host.navigate(Route.Result(url, fromSaved = true)) },
                onCompare = { ids -> host.navigate(Route.Compare(ids)) },
                onGoHome = { host.selectTab(MainTab.Home) },
            )
        }
        Route.Settings -> SettingsRoute(host)
        Route.PrivacyPolicy -> PrivacyPolicyScreen(onBack = host.back)
        Route.Licenses -> LicensesScreen(
            onBack = host.back,
            onOpenDetail = { id -> host.navigate(Route.LicenseDetail(id)) },
        )
        is Route.LicenseDetail -> LicenseDetailScreen(id = route.id, onBack = host.back)
        // Debug-only: release builds never reach it, and R8 drops the gallery code (spec 6).
        Route.Gallery -> if (BuildConfig.DEBUG) ComponentGalleryScreen(onBack = host.back)
        // Saved status comes from the DB, so route.fromSaved is ignored (kept for saved-state compatibility).
        is Route.Result -> {
            val app = LocalContext.current.applicationContext as AutoVerdictApp
            val nativeAdState = rememberResultNativeAdState(route.url)
            ResultRoute(
                url = route.url,
                host = host,
                repository = app.resultRepository,
                evaluator = app.ruleEngine,
                bannerAd = { ResultNativeAd(nativeAdState) },
            )
        }
        is Route.Compare -> {
            val app = LocalContext.current.applicationContext as AutoVerdictApp
            CompareRoute(
                carIds = route.carIds,
                repository = app.savedCarsRepository,
                evaluator = app.ruleEngine,
                onBack = host.back,
                onOpenResult = { url -> host.navigate(Route.Result(url, fromSaved = true)) },
            )
        }
    }
}
