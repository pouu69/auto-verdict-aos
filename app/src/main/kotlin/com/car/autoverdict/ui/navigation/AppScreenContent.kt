package com.car.autoverdict.ui.navigation

import androidx.compose.runtime.Composable
import com.car.autoverdict.BuildConfig
import com.car.autoverdict.ui.gallery.ComponentGalleryScreen
import com.car.autoverdict.ui.screen.AnalyzeScreen
import com.car.autoverdict.ui.screen.CompareScreen
import com.car.autoverdict.ui.screen.LicensesScreen
import com.car.autoverdict.ui.screen.OnboardingScreen
import com.car.autoverdict.ui.screen.PrivacyPolicyScreen
import com.car.autoverdict.ui.screen.ResultScreen
import com.car.autoverdict.ui.screen.SavedListScreen
import com.car.autoverdict.ui.screen.SettingsScreen

/** Maps each [Route] to its real screen. Kept apart from AppRoot so tests can inject fakes. */
@Composable
fun AppScreenContent(route: Route, host: ScreenHost) {
    when (route) {
        Route.Onboarding -> OnboardingScreen(onComplete = host.finishOnboarding)
        Route.Home -> AnalyzeScreen(
            lastAnalyzedUrl = host.lastAnalyzedUrl,
            onOpenSettings = { host.navigate(Route.Settings) },
            onAnalyze = { url ->
                host.onAnalyzed(url)
                host.navigate(Route.Result(url, fromSaved = false))
            },
        )
        Route.Saved -> SavedListScreen(
            onCarClick = { url -> host.navigate(Route.Result(url, fromSaved = true)) },
            onCompare = { ids -> host.navigate(Route.Compare(ids)) },
        )
        Route.Settings -> SettingsScreen(
            onBack = host.back,
            onPrivacyPolicy = { host.navigate(Route.PrivacyPolicy) },
            onLicenses = { host.navigate(Route.Licenses) },
            onReplayGuide = { host.navigate(Route.Onboarding) },
            onGallery = if (BuildConfig.DEBUG) ({ host.navigate(Route.Gallery) }) else null,
        )
        Route.PrivacyPolicy -> PrivacyPolicyScreen(onBack = host.back)
        Route.Licenses -> LicensesScreen(onBack = host.back)
        // Debug-only: release builds never reach it, and R8 drops the gallery code (spec 6).
        Route.Gallery -> if (BuildConfig.DEBUG) ComponentGalleryScreen(onBack = host.back)
        is Route.Result -> ResultScreen(url = route.url, onBack = host.back, isAlreadySaved = route.fromSaved)
        is Route.Compare -> CompareScreen(carIds = route.carIds, onBack = host.back)
    }
}
