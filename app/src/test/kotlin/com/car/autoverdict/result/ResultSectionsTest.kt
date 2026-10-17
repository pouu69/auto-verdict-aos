package com.car.autoverdict.result

import android.app.Application
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.RepairPanel
import com.car.autoverdict.report.Repairs
import com.car.autoverdict.report.SystemItem
import com.car.autoverdict.report.SystemSummary
import com.car.autoverdict.report.Tone
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tall screen so the LazyColumn composes every section (off-screen items have no nodes). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h4000dp")
class ResultSectionsTest {

    @get:Rule val rule = createComposeRule()

    private val recorder = RecordingActions()
    private val caution by lazy { sampleReport("sample-caution.json") }
    private val minimal by lazy { sampleReport("sample-minimal.json") }

    private fun show(report: EvaluationReport, banner: ResultBanner? = null, refreshing: Boolean = false) {
        rule.setContent {
            AvTheme {
                ResultScreen(
                    state = ResultUiState.Ready(report, saved = false, banner = banner, refreshing = refreshing),
                    actions = recorder.actions,
                )
            }
        }
    }

    private fun heading(text: String) = rule.onNode(hasText(text) and isHeading())

    @Test fun `verdict header shows summary, score and headline`() {
        show(caution)
        rule.onNodeWithText("2015년 · 12만km · 디젤 · 890만원").assertExists()
        rule.onNode(hasText("89점") and hasContentDescription("100점 만점에 89점")).assertExists()
        heading("사기 전에 2가지는 꼭 확인하세요").assertExists()
    }

    @Test fun `count chips hide zero counts`() {
        show(caution)
        rule.onNodeWithText("주의 2").assertExists()
        rule.onNodeWithText("통과 9").assertExists()
        rule.onNodeWithText("평가 제외 1").assertExists()
        rule.onAllNodesWithText("위험", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("비공개", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("미확인", substring = true).assertCountEquals(0)
    }

    @Test fun `danger and hidden chips appear when counted`() {
        show(caution.copy(counts = caution.counts.copy(danger = 1, hidden = 2)))
        rule.onNodeWithText("위험 1").assertExists()
        rule.onNodeWithText("비공개 2").assertExists()
    }

    @Test fun `full sample renders every section expanded`() {
        show(caution)
        // Facts
        rule.onNodeWithText("5회").assertExists()
        rule.onNodeWithText("자가용").assertExists()
        // Repairs: nothing replaced, systems all fine
        heading("교환·수리 부위").assertExists()
        rule.onNodeWithText("교환·수리 부위가 없어요").assertExists()
        rule.onNodeWithText("그 외 31개 항목 양호").assertExists()
        // Issues
        heading("확인할 것 2").assertExists()
        // Issues repeat in the full rule list on purpose (spec 4), so the message shows twice.
        rule.onAllNodesWithText("주인이 5번 바뀌었어요", substring = true).assertCountEquals(2)
        rule.onAllNodesWithText("왜 중요한가요").assertCountEquals(2)
        rule.onNodeWithText("판매자에게: 주인이 5번 바뀐 이유를 알 수 있을까요?").assertExists()
        // Timeline
        // The timeline title and the "이력" category label in the full rule list.
        rule.onAllNodes(hasText("이력") and isHeading()).assertCountEquals(2)
        rule.onNodeWithText("신차 등록").assertExists()
        assertTrue(rule.onAllNodesWithText("소유자 변경").fetchSemanticsNodes().size >= 5)
        rule.onNodeWithText("2023-11~2024-01").assertExists()
        // All rules with category sub-headers
        heading("전체 항목 11").assertExists()
        rule.onNodeWithText("차량 상태").assertExists()
        rule.onNodeWithText("투명성").assertExists()
        rule.onNodeWithText("성능점검 기록을 공개했어요").assertExists()
        // Excluded
        heading("평가에서 빠진 항목").assertExists()
        rule.onNodeWithText("가격 적정성").assertExists()
        rule.onNodeWithText("신차 가격 정보가 없어 평가하지 않았어요").assertExists()
        // Seller questions
        heading("판매자에게 물어볼 것 2").assertExists()
        rule.onNodeWithText("1. 주인이 5번 바뀐 이유를 알 수 있을까요?").assertExists()
        rule.onNodeWithText("2. 2023년 11월~2024년 1월, 2026년 4월~7월에 보험 없이 운행했나요?").assertExists()
    }

    @Test fun `section titles are headings`() {
        show(caution)
        // Top bar title + headline + six sections + four rule categories.
        rule.onAllNodes(isHeading()).assertCountEquals(12)
        heading("차량 상태").assertExists()
        heading("투명성").assertExists()
    }

    @Test fun `seller question buttons copy and share`() {
        show(caution)
        // The last section can sit partly under the bottom CTA even on the tall screen.
        // performScrollTo uses the semantics scroll action (like TalkBack), so it leaves the top.
        rule.onNodeWithText("복사").performScrollTo().performClick()
        rule.onNodeWithText("공유").performScrollTo().performClick()
        assertEquals(listOf("userScrolled", "copyQuestions", "shareQuestions"), recorder.calls)
    }

    @Test fun `minimal report hides empty sections and shows the login banner`() {
        show(minimal, banner = ResultBanner.LoginRequired)
        rule.onNodeWithText("엔카에 로그인하면 더 많은 항목을 확인할 수 있어요").assertExists()
        heading("교환·수리 부위").assertDoesNotExist()
        // Only the rule category "이력" — no timeline section.
        rule.onAllNodes(hasText("이력") and isHeading()).assertCountEquals(1)
        heading("평가에서 빠진 항목").assertDoesNotExist()
        rule.onAllNodesWithText("판매자에게 물어볼 것", substring = true).assertCountEquals(0)
        heading("확인할 것 1").assertExists()
        heading("전체 항목 3").assertExists()
        rule.onNodeWithText("통과 2").assertExists()
        rule.onNodeWithText("미확인 1").assertExists()
        // No facts, no car summary.
        rule.onAllNodesWithText("소유자 변경").assertCountEquals(2) // the issue card and the rule row only
    }

    @Test fun `repairs list panels and flagged systems`() {
        val repairs = Repairs(
            panels = listOf(RepairPanel(name = "쿼터 패널(우)", status = "판금·용접", rank = "2랭크", tone = Tone.Warning)),
            systems = SystemSummary(
                flagged = listOf(SystemItem(group = "오일누유", name = "실린더 블록 / 오일팬", status = "미세누유", tone = Tone.Warning)),
                okCount = 30,
            ),
        )
        show(caution.copy(repairs = repairs))
        rule.onNodeWithText("쿼터 패널(우)").assertExists()
        rule.onNodeWithText("2랭크").assertExists()
        rule.onNodeWithText("판금·용접").assertExists()
        rule.onNodeWithText("오일누유 · 실린더 블록 / 오일팬").assertExists()
        rule.onNodeWithText("미세누유").assertExists()
        rule.onNodeWithText("그 외 30개 항목 양호").assertExists()
        rule.onNodeWithText("교환·수리 부위가 없어요").assertDoesNotExist()
    }

    @Test fun `banners show their copy and actions`() {
        var banner: ResultBanner? by mutableStateOf(ResultBanner.SoldOut)
        rule.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(caution, saved = true, banner = banner), actions = recorder.actions)
            }
        }
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        fun clickLabel(label: String) = SemanticsMatcher("onClick label is $label") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == label
        }
        rule.onNode(hasText("판매가 끝난 매물이에요 · 저장 당시 결과예요") and polite).assertExists()

        banner = ResultBanner.UpdateAvailable
        rule.onNode(hasText("최신 정보가 있어요") and polite).assertExists()
        rule.onNode(hasText("보기") and clickLabel("최신 결과 보기")).performClick()
        rule.waitForIdle()

        banner = ResultBanner.PartialData
        rule.onNode(hasText("일부 정보를 가져오지 못했어요") and polite).assertExists()
        rule.onNode(hasText("다시 시도") and clickLabel("정보 다시 가져오기")).performClick()

        banner = null
        rule.onNodeWithText("일부 정보를 가져오지 못했어요").assertDoesNotExist()
        assertEquals(listOf("applyUpdate", "retry"), recorder.calls)
    }

    private val spinning = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

    @Test fun `refreshing swaps the partial-data retry for a progress label and ignores taps`() {
        show(caution, banner = ResultBanner.PartialData, refreshing = true)
        rule.onNodeWithText("일부 정보를 가져오지 못했어요").assertExists()
        rule.onNodeWithText("다시 시도").assertDoesNotExist()
        rule.onNodeWithText("다시 가져오는 중").assertExists().performClick()
        // The banner already shows the progress, so there is no second bar under the top bar.
        rule.onAllNodes(spinning, useUnmergedTree = true).assertCountEquals(1)
        assertEquals(emptyList<String>(), recorder.calls)
    }

    @Test fun `refreshing without a banner shows a slim progress bar under the top bar`() {
        show(caution, refreshing = true)
        rule.onNode(spinning and hasContentDescription("다시 가져오는 중")).assertExists()
        rule.onAllNodes(spinning, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun `refreshing under another banner still shows the slim progress bar`() {
        show(caution, banner = ResultBanner.LoginRequired, refreshing = true)
        rule.onNodeWithContentDescription("다시 가져오는 중").assertExists()
    }

    @Test fun `no progress while not refreshing`() {
        show(caution, banner = ResultBanner.PartialData)
        rule.onNodeWithText("다시 시도").assertExists()
        rule.onNodeWithText("다시 가져오는 중").assertDoesNotExist()
        rule.onAllNodes(spinning, useUnmergedTree = true).assertCountEquals(0)
    }
}

/** Default (phone-sized) screen so the list can actually scroll. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ResultScreenScrollTest {

    @get:Rule val rule = createComposeRule()

    private val recorder = RecordingActions()
    private lateinit var listState: LazyListState
    private lateinit var scope: CoroutineScope

    private fun show(banner: ResultBanner? = null) {
        rule.setContent {
            listState = rememberLazyListState()
            scope = rememberCoroutineScope()
            AvTheme {
                ResultScreen(
                    state = ResultUiState.Ready(sampleReport("sample-caution.json"), saved = true, banner = banner),
                    actions = recorder.actions,
                    listState = listState,
                )
            }
        }
        rule.waitForIdle()
    }

    @Test fun `leaving the top is reported once and scrolling to the top is not`() {
        show()
        rule.runOnIdle { scope.launch { listState.animateScrollToItem(0) } }
        rule.waitForIdle()
        assertEquals(emptyList<String>(), recorder.calls)

        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(listOf("userScrolled"), recorder.calls)
    }

    @Test fun `accessibility scroll action counts as the first scroll`() {
        show()
        // TalkBack (and keyboard/mouse wheel) scroll without a drag gesture.
        rule.onNode(hasScrollAction()).performScrollToIndex(3)
        rule.waitForIdle()
        assertEquals(listOf("userScrolled"), recorder.calls)
    }

    @Test fun `update banner stays visible while scrolled and its action returns to the top`() {
        show(banner = ResultBanner.UpdateAvailable)
        rule.runOnIdle { scope.launch { listState.scrollToItem(4) } }
        rule.waitForIdle()
        assertEquals(4, listState.firstVisibleItemIndex)

        rule.onNodeWithText("보기").performClick()
        rule.waitForIdle()

        // Leaving the top counted once; returning to it doesn't count again.
        assertEquals(listOf("userScrolled", "applyUpdate"), recorder.calls)
        assertEquals(0, listState.firstVisibleItemIndex)
    }
}
