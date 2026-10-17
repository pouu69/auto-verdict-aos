package com.car.autoverdict.home

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.rememberAvToastState
import com.car.autoverdict.ui.navigation.Route
import com.car.autoverdict.ui.navigation.ScreenHost
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h4000dp")
class HomeScreenTest {

    @get:Rule val rule = createComposeRule()

    private val navigated = mutableListOf<Route>()
    private val host = ScreenHost(
        navigate = { navigated += it },
        back = {},
        finishOnboarding = {},
        isFirstRunOnboarding = false,
        hasPendingShare = false,
        selectTab = {},
    )

    private fun setHome(recent: Flow<List<RecentViewItem>> = flowOf(emptyList())) {
        rule.setContent {
            AvTheme {
                CompositionLocalProvider(LocalAvToast provides rememberAvToastState()) {
                    HomeRoute(host = host, recentViews = recent)
                }
            }
        }
    }

    private fun item(carId: String, title: String? = "차 $carId", verdict: String? = "OK", score: Int? = 82) =
        RecentViewItem(carId, "https://fem.encar.com/cars/detail/$carId", title, verdict, score, RelativeAge.Hours(3))

    @Test fun `invalid link shows an inline error and does not navigate`() {
        setHome()
        rule.onNode(hasSetTextAction()).performTextInput("https://example.com/cars/1")
        rule.onNodeWithText("분석하기").performClick()

        rule.onNodeWithText("엔카 매물 링크가 아니에요. 매물 페이지 주소를 넣어 주세요").assertExists()
        assertTrue(navigated.isEmpty())
    }

    @Test fun `paste fills the normalized link and analyze opens it`() {
        val clipboard = ApplicationProvider.getApplicationContext<Application>().getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("", "[엔카] 2015 BMW 5시리즈 https://fem.encar.com/cars/detail/123?carid=123"))
        setHome()

        rule.onNodeWithText("붙여넣기").performClick()
        rule.onNodeWithText("https://fem.encar.com/cars/detail/123").assertExists()
        rule.onNodeWithText("분석하기").performClick()

        assertEquals(listOf(Route.Result("https://fem.encar.com/cars/detail/123", fromSaved = false)), navigated)
    }

    @Test fun `recent list folds after five rows and hides score and badge for legacy rows`() {
        val legacy = item("1", title = null, verdict = null, score = null)
        setHome(flowOf(listOf(legacy) + (2..7).map { item(it.toString()) }))

        rule.onNodeWithText("최근 본 차 7").assertExists()
        rule.onNodeWithText("엔카 매물 1").assertExists()
        rule.onAllNodesWithText("3시간 전").assertCountEquals(1)
        rule.onAllNodesWithText("82점 · 3시간 전").assertCountEquals(4)
        rule.onAllNodesWithText("괜찮아요").assertCountEquals(4)
        rule.onNodeWithText("차 6").assertDoesNotExist()

        rule.onNodeWithText("2대 더 보기").performClick()
        rule.onNodeWithText("차 7").assertExists()
        rule.onNodeWithText("접기").assertExists()
        rule.onNodeWithText("분석하고 24시간이 지나면 목록에서 사라져요").assertExists()
    }

    @Test fun `tapping a recent row opens its result`() {
        setHome(flowOf(listOf(item("42"))))

        rule.onNodeWithText("차 42").performClick()

        assertEquals(listOf(Route.Result("https://fem.encar.com/cars/detail/42", fromSaved = false)), navigated)
    }

    @Test fun `empty recent list shows the empty state without the footnote`() {
        setHome()

        rule.onNodeWithText("아직 본 차가 없어요").assertExists()
        rule.onNodeWithText("분석하고 24시간이 지나면 목록에서 사라져요").assertDoesNotExist()
    }
}
