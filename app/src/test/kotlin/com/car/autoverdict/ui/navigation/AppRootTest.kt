package com.car.autoverdict.ui.navigation

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppRootTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val url = "https://fem.encar.com/cars/detail/41623743"

    /** Every height each route's page reported, in order — a jump shows up as a second entry. */
    private val pageHeights = mutableMapOf<String, MutableList<Int>>()

    @Composable
    private fun FakeScreen(route: Route, host: ScreenHost) {
        var count by rememberSaveable { mutableIntStateOf(0) }
        val key = RouteCodec.encode(route)
        Column(Modifier.fillMaxSize().onSizeChanged { pageHeights.getOrPut(key) { mutableListOf() } += it.height }) {
            Text("route:$key")
            Text("last:${host.lastAnalyzedUrl}")
            Text("count:$count")
            TextButton(onClick = { count++ }) { Text("increment") }
            TextButton(onClick = { host.onAnalyzed(url); host.navigate(Route.Result(url, fromSaved = false)) }) { Text("open-result") }
            TextButton(onClick = { host.navigate(Route.Settings) }) { Text("open-settings") }
            TextButton(onClick = host.back) { Text("go-back") }
            TextButton(onClick = host.finishOnboarding) { Text("finish-onboarding") }
        }
    }

    @Composable
    private fun Root(
        onboardingComplete: Boolean,
        pending: NavEvent?,
        onConsumed: () -> Unit,
        onOnboardingComplete: () -> Unit = {},
    ) {
        AvTheme {
            AppRoot(
                onboardingComplete = onboardingComplete,
                onOnboardingComplete = onOnboardingComplete,
                pendingNavEvent = pending,
                onPendingNavEventConsumed = onConsumed,
                savedCount = 0,
                adBanner = {},
                screenContent = { route, host -> FakeScreen(route, host) },
            )
        }
    }

    @Test fun `back stack and last analyzed url survive recreation`() {
        val tester = StateRestorationTester(rule)
        tester.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }

        rule.onNodeWithText("open-result").performClick()
        rule.onNodeWithText("route:result|0|$url").assertExists()

        tester.emulateSavedInstanceStateRestore()

        rule.onNodeWithText("route:result|0|$url").assertExists()
        rule.onNodeWithText("go-back").performClick()
        rule.onNodeWithText("route:home").assertExists()
        rule.onNodeWithText("last:$url").assertExists()
    }

    @Test fun `share during first-run onboarding survives recreation and opens after finishing`() {
        var pending: NavEvent? by mutableStateOf(NavEvent.OpenShared(url))
        val tester = StateRestorationTester(rule)
        tester.setContent { Root(onboardingComplete = false, pending = pending, onConsumed = { pending = null }) }

        rule.onNodeWithText("route:onboarding").assertExists()
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("finish-onboarding").performClick()
        rule.onNodeWithText("route:result|0|$url").assertExists()
    }

    @Test fun `tab bar shows only on tab roots`() {
        rule.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }
        rule.onNodeWithText("홈").assertExists()
        rule.onNodeWithText("open-result").performClick()
        rule.onNodeWithText("홈").assertDoesNotExist()
    }

    @Test fun `push and pop keep page heights steady while the tab chrome animates with the tab page`() {
        rule.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }
        rule.waitForIdle()
        val homeHeight = pageHeights.getValue("home").last()
        pageHeights.clear()

        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("open-settings").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(100)
        // Mid-push: both pages are composed and the chrome is still leaving with the home page.
        rule.onNodeWithText("route:settings").assertExists()
        rule.onNodeWithText("route:home").assertExists()
        rule.onNodeWithText("홈").assertExists()
        rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithText("홈").assertDoesNotExist()
        rule.onNodeWithText("route:home").assertDoesNotExist()

        assertEquals(emptyList<Int>(), pageHeights["home"].orEmpty())
        val settingsHeights = pageHeights.getValue("settings")
        assertEquals(1, settingsHeights.size)
        // Sub-screens get the full height; tab roots stop above the chrome.
        assertTrue(settingsHeights.single() > homeHeight)

        pageHeights.clear()
        rule.onNodeWithText("go-back").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("홈").assertExists()
        rule.mainClock.advanceTimeBy(400)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.onNodeWithText("route:home").assertExists()
        assertEquals(listOf(homeHeight), pageHeights["home"])
        assertEquals(emptyList<Int>(), pageHeights["settings"].orEmpty())
    }

    @Test fun `invalid share shows a toast`() {
        val message = ApplicationProvider.getApplicationContext<Context>().getString(R.string.toast_invalid_share)
        rule.setContent { Root(onboardingComplete = true, pending = NavEvent.InvalidShare, onConsumed = {}) }
        rule.waitUntil(timeoutMillis = 3_000) {
            rule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun `screen state below the top survives a push and pop`() {
        rule.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }
        rule.onNodeWithText("increment").performClick()
        rule.onNodeWithText("count:1").assertExists()

        rule.onNodeWithText("open-settings").performClick()
        rule.onNodeWithText("route:settings").assertExists()
        rule.onNodeWithText("go-back").performClick()

        rule.onNodeWithText("route:home").assertExists()
        rule.onNodeWithText("count:1").assertExists()
    }

    @Test fun `popped screen starts fresh when opened again`() {
        rule.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }
        rule.onNodeWithText("open-settings").performClick()
        rule.onNodeWithText("increment").performClick()
        rule.onNodeWithText("count:1").assertExists()
        rule.onNodeWithText("go-back").performClick()
        rule.onNodeWithText("route:home").assertExists()

        rule.onNodeWithText("open-settings").performClick()
        rule.onNodeWithText("route:settings").assertExists()
        rule.onNodeWithText("count:0").assertExists()
    }

    @Test fun `double tap on back while the screen animates out pops only once`() {
        rule.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }
        rule.onNodeWithText("open-settings").performClick()
        rule.onNodeWithText("open-result").performClick()
        rule.onNodeWithText("route:result|0|$url").assertExists()

        rule.mainClock.autoAdvance = false
        val resultBack = rule.onAllNodesWithText("go-back")[0]
        resultBack.performClick()
        resultBack.performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.onNodeWithText("route:settings").assertExists()
        rule.onNodeWithText("route:home").assertDoesNotExist()
    }

    @Test fun `system back pops a sub-screen and is not handled on home`() {
        rule.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }
        rule.onNodeWithText("open-settings").performClick()
        rule.onNodeWithText("route:settings").assertExists()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()

        rule.onNodeWithText("route:home").assertExists()
        rule.runOnUiThread { assertFalse(rule.activity.onBackPressedDispatcher.hasEnabledCallbacks()) }
    }

    @Test fun `onboarding completion is reported only when onboarding actually closes`() {
        var completions = 0
        rule.setContent {
            Root(onboardingComplete = false, pending = null, onConsumed = {}, onOnboardingComplete = { completions++ })
        }
        rule.onNodeWithText("finish-onboarding").performClick()
        rule.onNodeWithText("route:home").assertExists()
        assertEquals(1, completions)

        // Home's host still exposes finishOnboarding; the reducer ignores it and so must the callback.
        rule.onNodeWithText("finish-onboarding").performClick()
        rule.waitForIdle()
        assertEquals(1, completions)
    }
}
