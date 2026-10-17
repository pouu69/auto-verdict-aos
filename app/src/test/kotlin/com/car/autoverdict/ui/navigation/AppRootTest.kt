package com.car.autoverdict.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppRootTest {

    @get:Rule val rule = createComposeRule()

    private val url = "https://fem.encar.com/cars/detail/41623743"

    @Composable
    private fun FakeScreen(route: Route, host: ScreenHost) {
        Column {
            Text("route:${RouteCodec.encode(route)}")
            Text("last:${host.lastAnalyzedUrl}")
            TextButton(onClick = { host.onAnalyzed(url); host.navigate(Route.Result(url, fromSaved = false)) }) { Text("open-result") }
            TextButton(onClick = host.back) { Text("go-back") }
            TextButton(onClick = host.finishOnboarding) { Text("finish-onboarding") }
        }
    }

    @Composable
    private fun Root(onboardingComplete: Boolean, pending: NavEvent?, onConsumed: () -> Unit) {
        AvTheme {
            AppRoot(
                onboardingComplete = onboardingComplete,
                onOnboardingComplete = {},
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

    @Test fun `invalid share shows a toast`() {
        val message = "엔카 매물 링크만 확인할 수 있어요"
        rule.setContent { Root(onboardingComplete = true, pending = NavEvent.InvalidShare, onConsumed = {}) }
        rule.waitUntil(timeoutMillis = 3_000) {
            rule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
