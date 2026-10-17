package com.car.autoverdict

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.car.autoverdict.preferences.AppPreferences
import com.car.autoverdict.ui.AdBanner
import com.car.autoverdict.ui.navigation.AppRoot
import com.car.autoverdict.ui.navigation.AppScreenContent
import com.car.autoverdict.ui.navigation.NavEvent
import com.car.autoverdict.ui.navigation.ShareIntentParser
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    /** One-shot event from a share intent, consumed by AppRoot once composed. */
    private val pendingNavEvent = mutableStateOf<NavEvent?>(null)

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

        setContent {
            AvTheme {
                val savedCount by remember {
                    app.database.savedCarDao().getAllFlow().map { it.size }
                }.collectAsState(initial = 0)
                AppRoot(
                    onboardingComplete = preferences.isOnboardingComplete,
                    onOnboardingComplete = preferences::setOnboardingComplete,
                    pendingNavEvent = pendingNavEvent.value,
                    onPendingNavEventConsumed = { pendingNavEvent.value = null },
                    savedCount = savedCount,
                    adBanner = { AdBanner() },
                    screenContent = { route, host -> AppScreenContent(route, host) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavEvent.value = parseShare(intent, isRestored = false)
    }

    private fun parseShare(intent: Intent?, isRestored: Boolean): NavEvent? =
        ShareIntentParser.parse(
            action = intent?.action,
            type = intent?.type,
            text = intent?.getStringExtra(Intent.EXTRA_TEXT),
            isRestored = isRestored,
        )
}
