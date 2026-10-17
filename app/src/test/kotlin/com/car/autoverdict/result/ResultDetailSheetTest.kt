package com.car.autoverdict.result

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Spec 4.1: tapping summary rows opens a bottom sheet with the full source data. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h4000dp")
class ResultDetailSheetTest {

    @get:Rule val rule = createComposeRule()

    private val recorder = RecordingActions()
    private val caution by lazy { sampleReport("sample-caution.json") }

    private fun show(report: EvaluationReport = caution) {
        rule.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(report, saved = false, banner = null), actions = recorder.actions)
            }
        }
    }

    private fun heading(text: String) = rule.onNode(hasText(text) and isHeading())

    private val opensDetail = SemanticsMatcher("onClick label is 자세히 보기") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == "자세히 보기"
    }

    private val notClickable = SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick)

    private val ownerIssueCard = hasText("판매자에게: 주인이 5번 바뀐 이유를 알 수 있을까요?") and opensDetail

    /** R07's sheet is the only place with this block title. */
    private fun assertOwnerSheetOpen() = heading("소유자 변경 기록").assertExists()

    private fun assertNoSheet() {
        rule.onNodeWithContentDescription("시트 닫기").assertDoesNotExist()
    }

    @Test fun `tapping an issue card opens its sheet with every block`() {
        show()
        assertNoSheet()
        rule.onNode(ownerIssueCard).performClick()

        rule.onNodeWithContentDescription("시트 닫기").assertExists()
        heading("소유자 변경").assertExists()
        // Why it matters, then each block with its rows and note.
        rule.onNode(hasText("왜 중요한가요") and isHeading()).assertExists()
        assertOwnerSheetOpen()
        heading("판정 기준").assertExists()
        heading("데이터 출처").assertExists()
        rule.onNode(hasText("변경 횟수") and hasText("5회")).assertExists()
        rule.onNode(hasText("1번째") and hasText("2018년 4월")).assertExists()
        rule.onNodeWithText("소유자 변경 3회부터").assertExists()
        rule.onNodeWithText("보험이력 조회 시점 기준이에요").assertExists()
        // The seller question with its own copy button.
        heading("판매자에게 물어보기").assertExists()
    }

    @Test fun `tapping a rule row opens its sheet`() {
        show()
        rule.onNode(hasText("보험처리 규모") and opensDetail).performClick()

        heading("2015년 5월 · 내차 피해").assertExists()
        heading("2015년 9월 · 내차 피해").assertExists()
        rule.onNode(hasText("내차 피해") and hasText("2,090,000원")).assertExists()
        rule.onNodeWithText("수입차 내차 피해 400만원 초과").assertExists()
        // A passing rule has no why paragraph or question in its sheet.
        heading("판매자에게 물어보기").assertDoesNotExist()
        rule.onAllNodes(hasText("왜 중요한가요") and isHeading()).assertCountEquals(0)
    }

    @Test fun `rule rows without a sheet are not clickable`() {
        show()
        rule.onNode(hasText("엔카진단 통과 차량")).assert(notClickable)
        rule.onNode(hasText("엔카진단 통과 차량")).performClick()
        assertNoSheet()
    }

    @Test fun `timeline entries with a sheet open it, others are not clickable`() {
        show()
        rule.onNode(hasText("2026-04") and hasText("소유자 변경")).assert(notClickable)
        rule.onNode(hasText("2026-04~2026-07") and opensDetail).performClick()

        heading("보험 미가입").assertExists()
        rule.onNode(hasText("개월 수") and hasText("3개월")).assertExists()
    }

    @Test fun `an older timeline entry opens its own sheet`() {
        show()
        rule.onNodeWithText("이전 이력 5개 더 보기").performClick()
        rule.onNode(hasText("2015-04") and hasText("신차 등록")).assert(notClickable)
        rule.onNode(hasText("2015-05") and opensDetail).performClick()

        heading("2015년 5월 사고").assertExists()
        rule.onNodeWithText("내차 피해 1건").assertExists()
        rule.onNode(hasText("수리비 합계") and hasText("950,000원")).assertExists()
    }

    @Test fun `repairs offer the full inspection when the report has it`() {
        show()
        rule.onNode(hasText("성능점검 전체 보기") and opensDetail).performClick()

        heading("성능점검 전체").assertExists()
        heading("사고·단순수리").assertExists()
        heading("외판·골격 부위").assertExists()
        heading("원동기").assertExists()
        rule.onNodeWithText("교환·수리한 부위가 없어요").assertExists()
        rule.onNode(hasText("오일누유 · 실린더 커버") and hasText("없음")).assertExists()
    }

    @Test fun `repairs without the inspection sheet have no full-inspection row`() {
        show(caution.copy(repairs = caution.repairs!!.copy(sheet = null)))
        rule.onNodeWithText("성능점검 전체 보기").assertDoesNotExist()
    }

    @Test fun `older reports without sheets keep plain rows`() {
        show(
            caution.copy(
                issues = caution.issues.map { it.copy(detail = null) },
                rules = caution.rules.map { it.copy(detail = null) },
                timeline = caution.timeline.map { it.copy(sheet = null) },
                repairs = caution.repairs!!.copy(sheet = null),
            ),
        )
        rule.onAllNodes(opensDetail).assertCountEquals(0)
    }

    @Test fun `every tappable row says what it does`() {
        show()
        // 2 issue cards + rule rows R07, R08, R10 + the inspection row + 1 timeline entry
        // (the 2015 accident waits behind "더 보기").
        rule.onAllNodes(opensDetail).assertCountEquals(2 + 3 + 1 + 1)
        rule.onNodeWithText("이전 이력 5개 더 보기").performClick()
        rule.onAllNodes(opensDetail).assertCountEquals(2 + 3 + 1 + 2)
    }

    @Test fun `dismissing closes the sheet`() {
        show()
        rule.onNode(ownerIssueCard).performClick()
        assertOwnerSheetOpen()

        rule.onNodeWithContentDescription("시트 닫기").performSemanticsAction(SemanticsActions.Dismiss)
        rule.waitForIdle()
        heading("소유자 변경 기록").assertDoesNotExist()
        assertNoSheet()
        // The rows still open it again.
        rule.onNode(ownerIssueCard).performClick()
        assertOwnerSheetOpen()
    }

    @Test fun `the open sheet survives recreation`() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(caution, saved = false, banner = null), actions = recorder.actions)
            }
        }
        rule.onNode(ownerIssueCard).performClick()
        assertOwnerSheetOpen()

        tester.emulateSavedInstanceStateRestore()

        assertOwnerSheetOpen()
    }

    @Test fun `a newer report without the sheet closes it for good`() {
        var report by mutableStateOf(caution)
        rule.setContent {
            AvTheme {
                ResultScreen(state = ResultUiState.Ready(report, saved = false, banner = null), actions = recorder.actions)
            }
        }
        rule.onNode(ownerIssueCard).performClick()
        assertOwnerSheetOpen()

        report = caution.copy(issues = caution.issues.map { it.copy(detail = null) }, rules = caution.rules.map { it.copy(detail = null) })
        rule.waitForIdle()
        assertNoSheet()

        // The old report coming back must not revive the stale sheet.
        report = caution
        rule.waitForIdle()
        assertNoSheet()
    }

    @Test fun `copy in a rule sheet copies that question`() {
        show()
        // Fold the seller-question section so its own "복사" button is out of the way.
        rule.onNode(hasText("판매자에게 물어볼 것 2") and isHeading() and hasClickAction()).performClick()
        rule.onAllNodesWithText("복사").assertCountEquals(0)

        rule.onNode(ownerIssueCard).performClick()
        rule.onNodeWithText("복사").performClick()
        assertEquals(listOf("copyQuestion:주인이 5번 바뀐 이유를 알 수 있을까요?"), recorder.calls - "userScrolled")
    }
}
