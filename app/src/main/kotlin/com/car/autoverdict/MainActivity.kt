package com.car.autoverdict

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.car.autoverdict.preferences.AppPreferences
import com.car.autoverdict.ui.AdBanner
import com.car.autoverdict.ui.ads.LocalContentAdsAllowed
import com.car.autoverdict.ui.navigation.AppRoot
import com.car.autoverdict.ui.navigation.AppScreenContent
import com.car.autoverdict.ui.navigation.NavEvent
import com.car.autoverdict.ui.navigation.ShareIntentParser
import com.car.autoverdict.ui.theme.AvTheme

class MainActivity : ComponentActivity() {

    /** One-shot event from a share intent, consumed by AppRoot once composed. */
    private val pendingNavEvent = mutableStateOf<NavEvent?>(null)
    private var wasStopped = true

    private val app: AutoVerdictApp get() = application as AutoVerdictApp

    override fun onCreate(savedInstanceState: Bundle?) {
        // The only splash: onboarding state is a synchronous SharedPreferences read,
        // so no keep-on-screen condition is needed (spec 7.2).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val preferences = AppPreferences(this)
        val app = application as AutoVerdictApp
        pendingNavEvent.value = parseShare(intent, isRestored = savedInstanceState != null)
        if (savedInstanceState == null) armSharedEntry(pendingNavEvent.value, fromBackground = true)

        setContent {
            AvTheme {
                AutoVerdictAppContent(
                    app = app,
                    preferences = preferences,
                    pendingNavEvent = pendingNavEvent.value,
                    onPendingNavEventConsumed = { pendingNavEvent.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavEvent.value = parseShare(intent, isRestored = false)
        armSharedEntry(pendingNavEvent.value, fromBackground = wasStopped)
    }

    override fun onResume() {
        super.onResume()
        wasStopped = false
        app.appOpenAds.onHostResumed()
    }

    override fun onStop() {
        wasStopped = true
        app.appOpenAds.onHostStopped()
        super.onStop()
    }

    override fun onDestroy() {
        app.appOpenAds.cancelEntry(this)
        super.onDestroy()
    }

    private fun armSharedEntry(event: NavEvent?, fromBackground: Boolean) {
        app.appOpenAds.cancelEntry(this)
        if (event is NavEvent.OpenShared) {
            app.appOpenAds.beginSharedEntry(
                activity = this,
                url = event.url,
                fromBackground = fromBackground,
                onboardingComplete = AppPreferences(this).isOnboardingComplete,
            )
        }
    }

    private fun parseShare(intent: Intent?, isRestored: Boolean): NavEvent? =
        ShareIntentParser.parse(
            action = intent?.action,
            type = intent?.type,
            text = intent?.getStringExtra(Intent.EXTRA_TEXT),
            flags = intent?.flags ?: 0,
            isRestored = isRestored,
        )
}

/**
 * Collects the saved count here so its updates recompose only this scope, not setContent's root.
 * The count is of grouped items, so twin rows of one car count once (spec 3.4).
 */
@Composable
private fun AutoVerdictAppContent(
    app: AutoVerdictApp,
    preferences: AppPreferences,
    pendingNavEvent: NavEvent?,
    onPendingNavEventConsumed: () -> Unit,
) {
    val savedCount by remember { app.savedCarsRepository.itemCount }
        .collectAsStateWithLifecycle(initialValue = 0)
    val contentAdsAllowed by app.appOpenAds.contentAdsAllowed.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalContentAdsAllowed provides contentAdsAllowed) {
        AppRoot(
            onboardingComplete = preferences.isOnboardingComplete,
            onOnboardingComplete = preferences::setOnboardingComplete,
            pendingNavEvent = pendingNavEvent,
            onPendingNavEventConsumed = onPendingNavEventConsumed,
            savedCount = savedCount,
            adBanner = { AdBanner() },
            screenContent = { route, host -> AppScreenContent(route, host) },
        )
    }
}
