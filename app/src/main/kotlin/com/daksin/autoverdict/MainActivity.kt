package com.daksin.autoverdict

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.daksin.autoverdict.floating.FloatingService
import com.daksin.autoverdict.preferences.AppPreferences
import com.daksin.autoverdict.ui.screen.AnalyzeScreen
import com.daksin.autoverdict.ui.screen.CompareScreen
import com.daksin.autoverdict.ui.screen.OnboardingScreen
import com.daksin.autoverdict.ui.screen.PermissionSetupScreen
import com.daksin.autoverdict.ui.screen.PrivacyPolicyScreen
import com.daksin.autoverdict.ui.screen.SavedListScreen
import com.daksin.autoverdict.ui.screen.SettingsScreen
import com.daksin.autoverdict.ui.theme.AutoVerdictTheme
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
import com.daksin.autoverdict.util.EncarUrl

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or not — foreground service works either way, just notification may be hidden */ }

    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)
        requestNotificationPermissionIfNeeded()
        handleShareIntent(intent)
        setContent {
            AutoVerdictTheme {
                AppRouter(
                    isOnboardingComplete = appPreferences.isOnboardingComplete,
                    onOnboardingComplete = { appPreferences.setOnboardingComplete() },
                    onAnalyze = ::launchAnalysis,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            if (EncarUrl.isEncarDetail(sharedText)) {
                launchAnalysis(sharedText)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun launchAnalysis(url: String) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "오버레이 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            startActivity(intent)
            return
        }
        val serviceIntent = Intent(this, FloatingService::class.java).apply {
            putExtra(FloatingService.EXTRA_URL, url)
        }
        startForegroundService(serviceIntent)
    }
}

private enum class AppScreen {
    ONBOARDING,
    PERMISSION_SETUP,
    MAIN,
}

@Composable
private fun AppRouter(
    isOnboardingComplete: Boolean,
    onOnboardingComplete: () -> Unit,
    onAnalyze: (String) -> Unit,
) {
    var screen by rememberSaveable {
        mutableStateOf(if (isOnboardingComplete) AppScreen.MAIN else AppScreen.ONBOARDING)
    }

    when (screen) {
        AppScreen.ONBOARDING -> OnboardingScreen(
            onComplete = { screen = AppScreen.PERMISSION_SETUP },
        )
        AppScreen.PERMISSION_SETUP -> PermissionSetupScreen(
            onComplete = {
                onOnboardingComplete()
                screen = AppScreen.MAIN
            },
        )
        AppScreen.MAIN -> MainScreen(onAnalyze = onAnalyze)
    }
}

private enum class Tab(val labelRes: Int, val iconRes: Int) {
    ANALYZE(R.string.tab_analyze, android.R.drawable.ic_menu_search),
    SAVED(R.string.tab_saved, android.R.drawable.ic_menu_save),
    SETTINGS(R.string.tab_settings, android.R.drawable.ic_menu_preferences),
}

@Composable
private fun MainScreen(onAnalyze: (String) -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.ANALYZE) }
    var compareCarIds by remember { mutableStateOf<List<String>?>(null) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
        return
    }

    val currentCompareIds = compareCarIds
    if (currentCompareIds != null) {
        CompareScreen(
            carIds = currentCompareIds,
            onBack = { compareCarIds = null },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                painter = painterResource(id = tab.iconRes),
                                contentDescription = stringResource(tab.labelRes),
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            Tab.ANALYZE -> AnalyzeScreen(modifier = modifier, onAnalyze = onAnalyze)
            Tab.SAVED -> SavedListScreen(
                modifier = modifier,
                onCarClick = onAnalyze,
                onCompare = { carIds -> compareCarIds = carIds },
            )
            Tab.SETTINGS -> SettingsScreen(
                modifier = modifier,
                onPrivacyPolicy = { showPrivacyPolicy = true },
            )
        }
    }
}
