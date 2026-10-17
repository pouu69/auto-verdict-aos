package com.car.autoverdict.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.BuildConfig
import com.car.autoverdict.result.ResultRoute
import com.car.autoverdict.ui.AdBanner
import com.car.autoverdict.ui.AdMediumRectangle
import com.car.autoverdict.ui.gallery.ComponentGalleryScreen
import com.car.autoverdict.ui.screen.AnalyzeScreen
import com.car.autoverdict.ui.screen.CompareScreen
import com.car.autoverdict.ui.screen.LicenseDetailScreen
import com.car.autoverdict.ui.screen.LicensesScreen
import com.car.autoverdict.ui.screen.OnboardingScreen
import com.car.autoverdict.ui.screen.PrivacyPolicyScreen
import com.car.autoverdict.ui.screen.SavedListScreen
import com.car.autoverdict.ui.screen.SettingsRoute

/** Maps each [Route] to its real screen. Kept apart from AppRoot so tests can inject fakes. */
@Composable
fun AppScreenContent(route: Route, host: ScreenHost) {
    when (route) {
        Route.Onboarding -> OnboardingScreen(onComplete = host.finishOnboarding)
        Route.Home -> AnalyzeScreen(
            onOpenSettings = { host.navigate(Route.Settings) },
            onAnalyze = { url -> host.navigate(Route.Result(url, fromSaved = false)) },
        )
        Route.Saved -> SavedListScreen(
            onCarClick = { url -> host.navigate(Route.Result(url, fromSaved = true)) },
            onCompare = { ids -> host.navigate(Route.Compare(ids)) },
        )
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
            ResultRoute(
                url = route.url,
                host = host,
                repository = app.resultRepository,
                evaluator = app.ruleEngine,
                bannerAd = { AdBanner(Modifier.fillMaxWidth()) },
                loadingAd = { AdMediumRectangle() },
            )
        }
        is Route.Compare -> CompareScreen(carIds = route.carIds, onBack = host.back)
    }
}
