package com.daksin.autoverdict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.daksin.autoverdict.preferences.AppPreferences
import com.daksin.autoverdict.ui.AdBanner
import com.daksin.autoverdict.ui.InterstitialAdManager
import com.daksin.autoverdict.ui.screen.AnalyzeScreen
import com.daksin.autoverdict.ui.screen.CompareScreen
import com.daksin.autoverdict.ui.screen.OnboardingScreen
import com.daksin.autoverdict.ui.screen.PrivacyPolicyScreen
import com.daksin.autoverdict.ui.screen.ResultScreen
import com.daksin.autoverdict.ui.screen.SavedListScreen
import com.daksin.autoverdict.ui.screen.SettingsScreen
import com.daksin.autoverdict.ui.theme.AutoVerdictTheme
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
import com.daksin.autoverdict.util.EncarUrl

class MainActivity : ComponentActivity() {

    private lateinit var appPreferences: AppPreferences
    private val pendingAnalysisUrl = mutableStateOf<String?>(null)
    private val interstitialAdManager = InterstitialAdManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appPreferences = AppPreferences(this)
        interstitialAdManager.load(this)
        handleShareIntent(intent)
        setContent {
            AutoVerdictTheme {
                AppRouter(
                    isOnboardingComplete = appPreferences.isOnboardingComplete,
                    onOnboardingComplete = { appPreferences.setOnboardingComplete() },
                    pendingUrl = pendingAnalysisUrl,
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
                interstitialAdManager.showIfReady(this) {
                    pendingAnalysisUrl.value = sharedText
                }
            }
        }
    }
}

private enum class AppScreen {
    SPLASH,
    ONBOARDING,
    MAIN,
}

@Composable
private fun AppRouter(
    isOnboardingComplete: Boolean,
    onOnboardingComplete: () -> Unit,
    pendingUrl: MutableState<String?>,
) {
    var screen by rememberSaveable {
        mutableStateOf(AppScreen.SPLASH)
    }

    when (screen) {
        AppScreen.SPLASH -> BrandSplashScreen(
            onFinished = {
                screen = if (isOnboardingComplete) AppScreen.MAIN else AppScreen.ONBOARDING
            },
        )
        AppScreen.ONBOARDING -> OnboardingScreen(
            onComplete = {
                onOnboardingComplete()
                screen = AppScreen.MAIN
            },
        )
        AppScreen.MAIN -> MainScreen(pendingUrl = pendingUrl)
    }
}

private enum class Tab(val labelRes: Int, val iconRes: Int) {
    ANALYZE(R.string.tab_analyze, android.R.drawable.ic_menu_search),
    SAVED(R.string.tab_saved, android.R.drawable.ic_menu_save),
    SETTINGS(R.string.tab_settings, android.R.drawable.ic_menu_preferences),
}

@Composable
private fun MainScreen(pendingUrl: MutableState<String?>) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.ANALYZE) }
    var compareCarIds by remember { mutableStateOf<List<String>?>(null) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var analysisUrl by remember { mutableStateOf<String?>(null) }
    var analysisFromSaved by remember { mutableStateOf(false) }

    val pending = pendingUrl.value
    LaunchedEffect(pending) {
        if (pending != null) {
            analysisUrl = pending
            analysisFromSaved = false
            pendingUrl.value = null
        }
    }

    if (showPrivacyPolicy) {
        BackHandler { showPrivacyPolicy = false }
        PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
        return
    }

    val currentAnalysisUrl = analysisUrl
    if (currentAnalysisUrl != null) {
        BackHandler { analysisUrl = null; analysisFromSaved = false }
        ResultScreen(
            url = currentAnalysisUrl,
            onBack = { analysisUrl = null; analysisFromSaved = false },
            isAlreadySaved = analysisFromSaved,
        )
        return
    }

    val currentCompareIds = compareCarIds
    if (currentCompareIds != null) {
        BackHandler { compareCarIds = null }
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
        Column(modifier = Modifier.padding(innerPadding)) {
            val modifier = Modifier.weight(1f)
            when (selectedTab) {
            Tab.ANALYZE -> AnalyzeScreen(
                modifier = modifier,
                onAnalyze = { url -> analysisUrl = url; analysisFromSaved = false },
            )
            Tab.SAVED -> SavedListScreen(
                modifier = modifier,
                onCarClick = { url -> analysisUrl = url; analysisFromSaved = true },
                onCompare = { carIds -> compareCarIds = carIds },
            )
            Tab.SETTINGS -> SettingsScreen(
                modifier = modifier,
                onPrivacyPolicy = { showPrivacyPolicy = true },
            )
            }
            AdBanner()
        }
    }
}

@Composable
private fun BrandSplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AV",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AutoVerdict",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "엔카 중고차 종합 평가",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )
        }
    }
}
