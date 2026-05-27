package com.daksin.autoverdict

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.daksin.autoverdict.floating.FloatingService
import com.daksin.autoverdict.ui.screen.AnalyzeScreen
import com.daksin.autoverdict.ui.screen.SavedListScreen
import com.daksin.autoverdict.ui.screen.SettingsScreen
import com.daksin.autoverdict.ui.theme.AutoVerdictTheme
import com.daksin.autoverdict.ui.theme.Primary
import com.daksin.autoverdict.ui.theme.TextSecondary
import com.daksin.autoverdict.util.EncarUrl

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        setContent {
            AutoVerdictTheme {
                MainScreen(onAnalyze = ::launchAnalysis)
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

private enum class Tab(val labelRes: Int, val iconRes: Int) {
    ANALYZE(R.string.tab_analyze, android.R.drawable.ic_menu_search),
    SAVED(R.string.tab_saved, android.R.drawable.ic_menu_save),
    SETTINGS(R.string.tab_settings, android.R.drawable.ic_menu_preferences),
}

@Composable
private fun MainScreen(onAnalyze: (String) -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.ANALYZE) }

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
            Tab.SAVED -> SavedListScreen(modifier = modifier, onCarClick = onAnalyze)
            Tab.SETTINGS -> SettingsScreen(modifier = modifier)
        }
    }
}
