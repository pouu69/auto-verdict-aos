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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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

    private fun clickLabel(label: String) = SemanticsMatcher("onClick label is $label") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    private fun stateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    /** Timeline rows top to bottom as "date label" (a row's first text is its yyyy-MM date). */
    private fun timelineRows(): List<String> =
        rule.onAllNodes(timelineRow).fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.top }
            .map { node -> node.config[SemanticsProperties.Text].take(2).joinToString(" ") { it.text } }

    private val timelineRow = SemanticsMatcher("timeline row") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text?.matches(Regex("\\d{4}-\\d{2}")) == true
    }

    private val newestFive = listOf(
        "2026-04 보험 미가입",
        "2026-04 소유자 변경",
        "2024-09 소유자 변경",
        "2024-02 소유자 변경",
        "2023-11 보험 미가입",
    )

    @Test fun `verdict header shows summary, score and headline`() {
        show(caution)
        rule.onNodeWithText("2015년 · 12만km · 디젤 · 890만원").assertExists()
        rule.onNode(hasText("89점") and hasContentDescription("100점 만점에 89점")).assertExists()
        heading("사기 전에 2가지는 꼭 확인하세요").assertExists()
    }

    @Test fun `verdict header leads with the car name above the summary`() {
        show(caution)
        val name = rule.onNodeWithText("BMW 3시리즈 (F30) 320d").fetchSemanticsNode().boundsInRoot
        val summary = rule.onNodeWithText("2015년 · 12만km · 디젤 · 890만원").fetchSemanticsNode().boundsInRoot
        assertTrue("name $name should sit above summary $summary", name.bottom <= summary.top)
        // The listing title stays out of the header when a name is given.
        rule.onAllNodesWithText("2015 BMW 3시리즈 (F30)").assertCountEquals(0)
    }

    @Test fun `verdict header falls back to the listing title without a name`() {
        show(minimal)
        rule.onNodeWithText("차량 정보 없음").assertExists()
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
        rule.onNodeWithText("교환·수리 부위가 없고 주요 장치 31개 항목이 양호해요").assertExists()
        rule.onNodeWithText("교환·수리 부위가 없어요").assertDoesNotExist()
        rule.onNodeWithText("그 외", substring = true).assertDoesNotExist()
        // Issues
        heading("확인할 것 2").assertExists()
        // Issues repeat in the full rule list on purpose (spec 4): the card keeps the sentence,
        // the rule row shows the short summary.
        rule.onAllNodesWithText("주인이 5번 바뀌었어요", substring = true).assertCountEquals(1)
        rule.onAllNodesWithText("소유자 변경 5회", substring = true).assertCountEquals(1)
        rule.onAllNodesWithText("왜 중요한가요").assertCountEquals(2)
        rule.onNodeWithText("판매자에게: 주인이 5번 바뀐 이유를 알 수 있을까요?").assertExists()
        // Timeline
        // The timeline title and the "이력" category label in the full rule list.
        rule.onAllNodes(hasText("이력") and isHeading()).assertCountEquals(2)
        // Ten entries: the newest five show, the older ones wait behind "더 보기".
        rule.onNodeWithText("2023-11~2024-01").assertExists()
        rule.onNodeWithText("신차 등록").assertDoesNotExist()
        rule.onNodeWithText("이전 이력 5개 더 보기").performClick()
        rule.onNodeWithText("신차 등록").assertExists()
        assertTrue(rule.onAllNodesWithText("소유자 변경").fetchSemanticsNodes().size >= 5)
        // All rules with category sub-headers
        heading("전체 항목 11").assertExists()
        rule.onNodeWithText("차량 상태").assertExists()
        rule.onNodeWithText("투명성").assertExists()
        rule.onNodeWithText("성능점검 기록을 공개했어요").assertExists()
        // Excluded
        heading("평가에서 빠진 항목 1").assertExists()
        rule.onNodeWithText("가격 적정성").assertExists()
        rule.onNodeWithText("신차 가격 정보가 없어 평가하지 않았어요").assertExists()
        // Seller questions
        heading("판매자에게 물어볼 것 2").assertExists()
        rule.onNodeWithText("1. 주인이 5번 바뀐 이유를 알 수 있을까요?").assertExists()
        rule.onNodeWithText("2. 2023년 11월~2024년 1월, 2026년 4월~7월에 보험 없이 운행했나요?").assertExists()
    }

    @Test fun `timeline shows the newest five first and folds the rest`() {
        show(caution)
        assertEquals(newestFive, timelineRows())
        rule.onNodeWithText("접기").assertDoesNotExist()
        rule.onNode(hasText("이전 이력 5개 더 보기") and clickLabel("이전 이력 펼치기") and stateDescription("접힘"))
            .assertExists()
    }

    @Test fun `more expands every entry newest first and fold collapses again`() {
        show(caution)
        rule.onNodeWithText("이전 이력 5개 더 보기").performClick()
        val older = listOf(
            "2023-11 소유자 변경",
            "2018-04 소유자 변경",
            "2015-09 사고 · 내차 피해",
            "2015-05 사고 · 내차 피해",
            "2015-04 신차 등록",
        )
        assertEquals(newestFive + older, timelineRows())
        rule.onNodeWithText("이전 이력", substring = true).assertDoesNotExist()

        rule.onNode(hasText("접기") and clickLabel("이력 접기") and stateDescription("펼쳐짐")).performClick()
        assertEquals(newestFive, timelineRows())
        rule.onNodeWithText("이전 이력 5개 더 보기").assertExists()
        rule.onNodeWithText("접기").assertDoesNotExist()
    }

    @Test fun `six entries all show without a button`() {
        show(caution.copy(timeline = caution.timeline.takeLast(6)))
        assertEquals(newestFive + "2023-11 소유자 변경", timelineRows())
        rule.onNodeWithText("더 보기", substring = true).assertDoesNotExist()
        rule.onNodeWithText("접기").assertDoesNotExist()
    }

    @Test fun `seven entries fold to five and count the two hidden`() {
        show(caution.copy(timeline = caution.timeline.takeLast(7)))
        assertEquals(newestFive, timelineRows())
        rule.onNodeWithText("이전 이력 2개 더 보기").assertExists()
    }

    @Test fun `expanded timeline survives recreation`() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(caution, saved = false, banner = null), actions = recorder.actions)
            }
        }
        rule.onNodeWithText("이전 이력 5개 더 보기").performClick()
        tester.emulateSavedInstanceStateRestore()
        assertEquals(10, timelineRows().size)
        rule.onNodeWithText("접기").assertExists()
    }

    @Test fun `a different report starts collapsed again`() {
        var report by mutableStateOf(caution)
        rule.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(report, saved = false, banner = null), actions = recorder.actions)
            }
        }
        rule.onNodeWithText("이전 이력 5개 더 보기").performClick()
        assertEquals(10, timelineRows().size)
        // A refresh swaps in a new report under the same timeline item.
        report = caution.copy(score = caution.score - 1)
        rule.waitForIdle()
        assertEquals(newestFive, timelineRows())
    }

    /** Section title rows (clickable headings), not the rule-category sub-headers. */
    private fun sectionHeader(title: String) = rule.onNode(hasText(title) and isHeading() and hasClickAction())

    private val sectionTitles = listOf("교환·수리 부위", "확인할 것 2", "이력", "전체 항목 11", "평가에서 빠진 항목 1", "판매자에게 물어볼 것 2")

    @Test fun `every section starts expanded behind a heading button`() {
        show(caution)
        rule.onAllNodes(isHeading() and hasClickAction()).assertCountEquals(sectionTitles.size)
        sectionTitles.forEach { title ->
            val name = title.substringBeforeLast(' ').takeIf { title.last().isDigit() } ?: title
            sectionHeader(title)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assert(stateDescription("펼쳐짐"))
                .assert(clickLabel("$name 접기"))
        }
        // The verdict header and count chips aren't sections: always shown, never buttons.
        rule.onNode(hasText("사기 전에 2가지는 꼭 확인하세요") and isHeading()).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test fun `tapping a section title folds and unfolds its body`() {
        show(caution)
        sectionHeader("확인할 것 2").performClick()
        rule.onNodeWithText("판매자에게: 주인이 5번 바뀐 이유를 알 수 있을까요?").assertDoesNotExist()
        sectionHeader("확인할 것 2").assert(stateDescription("접힘")).assert(clickLabel("확인할 것 펼치기"))
        // Other sections are untouched.
        rule.onNodeWithText("성능점검 기록을 공개했어요").assertExists()

        sectionHeader("전체 항목 11").performClick()
        rule.onNodeWithText("성능점검 기록을 공개했어요").assertDoesNotExist()
        rule.onNodeWithText("가격 적정성").assertExists()

        sectionHeader("확인할 것 2").performClick()
        rule.onNodeWithText("판매자에게: 주인이 5번 바뀐 이유를 알 수 있을까요?").assertExists()
        sectionHeader("전체 항목 11").performClick()
        rule.onNodeWithText("성능점검 기록을 공개했어요").assertExists()
    }

    @Test fun `folding the timeline section keeps its show-older state`() {
        show(caution)
        rule.onNodeWithText("이전 이력 5개 더 보기").performClick()
        sectionHeader("이력").performClick()
        assertEquals(emptyList<String>(), timelineRows())
        rule.onNodeWithText("접기").assertDoesNotExist()
        sectionHeader("이력").performClick()
        assertEquals(10, timelineRows().size)
        rule.onNodeWithText("접기").assertExists()
    }

    @Test fun `folded sections survive recreation`() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(caution, saved = false, banner = null), actions = recorder.actions)
            }
        }
        sectionHeader("확인할 것 2").performClick()
        sectionHeader("판매자에게 물어볼 것 2").performClick()
        tester.emulateSavedInstanceStateRestore()
        sectionHeader("확인할 것 2").assert(stateDescription("접힘"))
        sectionHeader("판매자에게 물어볼 것 2").assert(stateDescription("접힘"))
        sectionHeader("전체 항목 11").assert(stateDescription("펼쳐짐"))
        rule.onNodeWithText("판매자에게: 주인이 5번 바뀐 이유를 알 수 있을까요?").assertDoesNotExist()
        rule.onNodeWithText("복사").assertDoesNotExist()
    }

    @Test fun `a newer report keeps the sections the user folded`() {
        var report by mutableStateOf(caution)
        rule.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(report, saved = false, banner = null), actions = recorder.actions)
            }
        }
        sectionHeader("전체 항목 11").performClick()
        report = caution.copy(score = caution.score - 1)
        rule.waitForIdle()
        sectionHeader("전체 항목 11").assert(stateDescription("접힘"))
        rule.onNodeWithText("성능점검 기록을 공개했어요").assertDoesNotExist()
        sectionHeader("확인할 것 2").assert(stateDescription("펼쳐짐"))
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
        // The last section can sit partly under the bottom CTA; whether reaching it scrolls
        // depends on the report's length, so only the button calls are checked.
        rule.onNodeWithText("복사").performScrollTo().performClick()
        rule.onNodeWithText("공유").performScrollTo().performClick()
        assertEquals(listOf("copyQuestions", "shareQuestions"), recorder.calls - "userScrolled")
    }

    @Test fun `minimal report hides empty sections and shows the login banner`() {
        show(minimal, banner = ResultBanner.LoginRequired)
        rule.onNodeWithText("엔카에 로그인하면 더 많은 항목을 확인할 수 있어요").assertExists()
        heading("교환·수리 부위").assertDoesNotExist()
        // Only the rule category "이력" — no timeline section.
        rule.onAllNodes(hasText("이력") and isHeading()).assertCountEquals(1)
        rule.onAllNodesWithText("평가에서 빠진 항목", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("판매자에게 물어볼 것", substring = true).assertCountEquals(0)
        heading("확인할 것 1").assertExists()
        heading("전체 항목 3").assertExists()
        rule.onNodeWithText("통과 2").assertExists()
        rule.onNodeWithText("미확인 1").assertExists()
        // No facts, no car summary.
        rule.onAllNodesWithText("소유자 변경").assertCountEquals(2) // the issue card and the rule row only
    }

    @Test fun `all-rules rows show the short summary and cards keep the sentence`() {
        show(caution)
        rule.onNode(hasText("엔카진단") and hasText("엔카진단 통과 차량")).assertExists()
        rule.onNodeWithText("엔카진단을 통과했어요").assertDoesNotExist()
        rule.onNode(hasText("누유") and hasText("누유 없음")).assertExists()
        rule.onNodeWithText("누유는 없어요").assertDoesNotExist()
        rule.onNode(hasText("자차보험 공백") and hasText("자차보험 공백 6개월 · 2구간")).assertExists()
        // The issue card for the same rule still reads the full sentence.
        rule.onNode(hasText("자차보험 없이 운행한 기간이 6개월 있어요") and hasText("왜 중요한가요")).assertExists()
        // No summary: the row falls back to the message.
        rule.onNode(hasText("성능점검 공개") and hasText("성능점검 기록을 공개했어요")).assertExists()
    }

    @Test fun `all-rules rows fall back to the message in reports without summaries`() {
        show(minimal)
        // The issue card and the rule row both read the message.
        rule.onAllNodesWithText("엔카에 로그인하면 확인할 수 있어요", substring = true).assertCountEquals(2)
        rule.onNode(hasText("보험이력 공개") and hasText("보험이력을 공개했어요")).assertExists()
    }

    // Legacy Robolectric graphics measure text as nearly zero-width, so nothing would ever wrap.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun `a long summary wraps instead of being cut`() {
        val long = "엔카진단 통과 차량 · 프레임 정상 · 외판 교환 없음 · 주요 장치 31개 항목 양호 · 성능점검 기록 공개"
        show(caution.copy(rules = caution.rules.map { if (it.ruleId == "R03") it.copy(summary = long) else it }))
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNode(hasText(long), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertTrue("lines=${layout.lineCount}", layout.lineCount > 1)
        assertFalse(layout.hasVisualOverflow)
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

    @Test fun `repairs with nothing listed and no systems checked say so in one line`() {
        show(caution.copy(repairs = Repairs(panels = emptyList(), systems = SystemSummary(emptyList(), okCount = 0))))
        rule.onNodeWithText("교환·수리 부위가 없어요").assertExists()
        rule.onNodeWithText("양호", substring = true).assertDoesNotExist()
    }

    @Test fun `repairs with only flagged systems keep the ok count line`() {
        val repairs = Repairs(
            panels = emptyList(),
            systems = SystemSummary(
                flagged = listOf(SystemItem(group = "오일누유", name = "실린더 블록 / 오일팬", status = "미세누유", tone = Tone.Warning)),
                okCount = 12,
            ),
        )
        show(caution.copy(repairs = repairs))
        rule.onNodeWithText("그 외 12개 항목 양호").assertExists()
        rule.onNodeWithText("교환·수리 부위가 없", substring = true).assertDoesNotExist()
    }

    @Test fun `banners show their copy and actions`() {
        var banner: ResultBanner? by mutableStateOf(ResultBanner.SoldOut)
        rule.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(caution, saved = true, banner = banner), actions = recorder.actions)
            }
        }
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
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

    /** Off-screen items aren't composed, so bring the timeline item in before looking for its button. */
    private fun expandTimeline() {
        rule.onNode(hasScrollToKeyAction()).performScrollToKey("timeline")
        rule.onNodeWithText("이전 이력 5개 더 보기").performScrollTo().performClick()
        rule.waitForIdle()
    }

    @Test fun `folding the timeline from far down brings its header back into view`() {
        show()
        expandTimeline()
        rule.onNodeWithText("접기").performScrollTo()
        rule.waitForIdle()
        // The phone screen is shorter than the expanded timeline, so its header is above the viewport.
        val before = listState.layoutInfo.visibleItemsInfo.first { it.key == "timeline" }
        assertTrue(before.offset < 0)

        rule.onNodeWithText("접기").performClick()
        rule.waitForIdle()
        val first = listState.layoutInfo.visibleItemsInfo.first()
        assertEquals("timeline", first.key)
        assertEquals(0, first.offset)
    }

    @Test fun `folding a section whose title has scrolled away brings the title back`() {
        show()
        rule.onNode(hasScrollToKeyAction()).performScrollToKey("rules")
        rule.waitForIdle()
        val index = listState.layoutInfo.visibleItemsInfo.first { it.key == "rules" }.index
        rule.runOnIdle { scope.launch { listState.scrollToItem(index, scrollOffset = 300) } }
        rule.waitForIdle()
        assertTrue(listState.layoutInfo.visibleItemsInfo.first { it.key == "rules" }.offset < 0)

        // The title is off screen, so fold it the way TalkBack would.
        rule.onNode(hasText("전체 항목 11") and isHeading()).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        val first = listState.layoutInfo.visibleItemsInfo.first()
        assertEquals("rules", first.key)
        assertEquals(0, first.offset)
        rule.onNodeWithText("성능점검 기록을 공개했어요").assertDoesNotExist()
    }

    @Test fun `folding while the header is on screen does not scroll`() {
        show()
        expandTimeline()
        val index = listState.layoutInfo.visibleItemsInfo.first { it.key == "timeline" }.index
        // The divider on top, so the header sits below the viewport's top edge.
        rule.runOnIdle { scope.launch { listState.scrollToItem(index - 1) } }
        rule.waitForIdle()
        assertTrue(listState.layoutInfo.visibleItemsInfo.first { it.key == "timeline" }.offset > 0)

        // The fold button is below the fold here; trigger it the way TalkBack would.
        rule.onNodeWithText("접기").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithText("이전 이력 5개 더 보기").assertExists()
        assertEquals(index - 1, listState.firstVisibleItemIndex)
        assertEquals(0, listState.firstVisibleItemScrollOffset)
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
