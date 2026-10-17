package com.car.autoverdict.result

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.ReportParser
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Counts every [ResultActions] callback so tests can assert exactly what fired. */
internal class RecordingActions {
    val calls = mutableListOf<String>()

    val actions = ResultActions(
        onBack = { calls += "back" },
        onOpenInEncar = { calls += "openInEncar" },
        onRetry = { calls += "retry" },
        onApplyUpdate = { calls += "applyUpdate" },
        onUserScrolled = { calls += "userScrolled" },
        onSave = { calls += "save" },
        onUnsave = { calls += "unsave" },
        onShareSummary = { calls += "shareSummary" },
        onShareQuestions = { calls += "shareQuestions" },
        onCopyQuestions = { calls += "copyQuestions" },
        onCopyQuestion = { calls += "copyQuestion:$it" },
    )

    fun count(name: String) = calls.count { it == name }
}

internal fun sampleReport(name: String): EvaluationReport {
    val text = requireNotNull(RecordingActions::class.java.classLoader?.getResource("report/$name")) {
        "missing report/$name"
    }.readText()
    return (ReportParser.parse(text) as ReportParser.Result.Ok).report
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h4000dp")
class ResultScreenTest {

    @get:Rule val rule = createComposeRule()

    private val recorder = RecordingActions()
    private val caution by lazy { sampleReport("sample-caution.json") }

    private fun show(state: ResultUiState) {
        rule.setContent {
            AvTheme {
                ResultScreen(
                    state = state,
                    actions = recorder.actions,
                    bannerAd = { Text("banner-ad") },
                    loadingAd = { Text("loading-ad") },
                )
            }
        }
    }

    private fun stateIs(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test fun `top bar has back, title and the Encar chip`() {
        show(ResultUiState.Loading(LoadingStage.OpeningPage))
        rule.onNodeWithText("분석 결과").assertExists()
        rule.onNodeWithText("저장된 매물").assertDoesNotExist()
        rule.onNodeWithContentDescription("뒤로").performClick()
        rule.onNodeWithText("엔카에서 보기").performClick()
        assertEquals(listOf("back", "openInEncar"), recorder.calls)
    }

    @Test fun `loading marks earlier steps done and shows only the loading ad`() {
        show(ResultUiState.Loading(LoadingStage.FetchingHistory))
        rule.onNodeWithText("매물 페이지 열기").assert(stateIs("완료"))
        rule.onNodeWithText("이력·진단 가져오기").assert(stateIs("진행 중"))
        rule.onNodeWithText("12가지 기준으로 평가").assert(stateIs("대기 중"))
        rule.onNodeWithText("loading-ad").assertExists()
        rule.onNodeWithText("banner-ad").assertDoesNotExist()
    }

    @Test fun `evaluating step is active after both collector steps`() {
        show(ResultUiState.Loading(LoadingStage.Evaluating))
        rule.onNodeWithText("매물 페이지 열기").assert(stateIs("완료"))
        rule.onNodeWithText("이력·진단 가져오기").assert(stateIs("완료"))
        rule.onNodeWithText("12가지 기준으로 평가").assert(stateIs("진행 중"))
    }

    @Test fun `each error kind shows its copy and buttons`() {
        var state: ResultUiState by mutableStateOf(ResultUiState.Error(ResultErrorKind.Network))
        rule.setContent {
            AvTheme {
                ResultScreen(
                    state = state,
                    actions = recorder.actions,
                    bannerAd = { Text("banner-ad") },
                    loadingAd = { Text("loading-ad") },
                )
            }
        }
        data class Case(val kind: ResultErrorKind, val title: String, val retry: Boolean, val encar: Boolean, val back: Boolean)
        val cases = listOf(
            Case(ResultErrorKind.Network, "인터넷 연결을 확인해 주세요", retry = true, encar = false, back = false),
            Case(ResultErrorKind.Timeout, "엔카 응답이 늦어요", retry = true, encar = true, back = false),
            Case(ResultErrorKind.PageUnreadable, "엔카 페이지를 읽지 못했어요", retry = true, encar = true, back = false),
            Case(ResultErrorKind.EngineFailed, "결과를 계산하지 못했어요", retry = true, encar = false, back = false),
            Case(ResultErrorKind.InvalidUrl, "엔카 매물 주소가 아니에요", retry = false, encar = false, back = true),
        )
        for (case in cases) {
            state = ResultUiState.Error(case.kind)
            rule.waitForIdle()
            recorder.calls.clear()
            rule.onNodeWithText(case.title).assertExists()
            rule.onNodeWithText("banner-ad").assertExists()
            rule.onNodeWithText("loading-ad").assertDoesNotExist()

            if (case.retry) rule.onNodeWithText("다시 시도").performClick() else rule.onNodeWithText("다시 시도").assertDoesNotExist()
            // The top-bar chip is hidden on error screens, so any "엔카에서 보기" is the body button.
            if (case.encar) rule.onNodeWithText("엔카에서 보기").performClick() else rule.onNodeWithText("엔카에서 보기").assertDoesNotExist()
            if (case.back) rule.onNodeWithText("돌아가기").performClick() else rule.onNodeWithText("돌아가기").assertDoesNotExist()

            val expected = listOfNotNull("retry".takeIf { case.retry }, "openInEncar".takeIf { case.encar }, "back".takeIf { case.back })
            assertEquals("${case.kind}", expected, recorder.calls)
        }
    }

    @Test fun `deleted listing offers to go back`() {
        show(ResultUiState.Deleted)
        rule.onNodeWithText("판매가 끝났거나 삭제된 매물이에요").assertExists()
        rule.onNodeWithText("banner-ad").assertExists()
        rule.onNodeWithText("돌아가기").performClick()
        assertEquals(listOf("back"), recorder.calls)
    }

    @Test fun `unsaved result offers save and shows the bottom banner`() {
        show(ResultUiState.Ready(caution, saved = false, banner = null))
        rule.onNodeWithText("저장된 매물").assertDoesNotExist()
        rule.onNodeWithText("banner-ad").assertExists()
        rule.onNodeWithText("loading-ad").assertDoesNotExist()
        rule.onNodeWithText("저장하기").performClick()
        assertEquals(listOf("save"), recorder.calls)
    }

    @Test fun `share button is labelled and shares the summary`() {
        show(ResultUiState.Ready(caution, saved = false, banner = null))
        rule.onNodeWithContentDescription("결과 공유").performClick()
        assertEquals(listOf("shareSummary"), recorder.calls)
    }

    @Test fun `saved result unsaves only after the dialog is confirmed`() {
        show(ResultUiState.Ready(caution, saved = true, banner = null))
        rule.onNodeWithText("저장된 매물").assertExists()

        rule.onNodeWithText("저장됨").performClick()
        rule.onNodeWithText("저장을 취소할까요?").assertExists()
        rule.onNodeWithText("그대로 두기").performClick()
        rule.onNodeWithText("저장을 취소할까요?").assertDoesNotExist()
        assertEquals(0, recorder.count("unsave"))

        rule.onNodeWithText("저장됨").performClick()
        rule.onNodeWithText("취소하기").performClick()
        rule.onNodeWithText("저장을 취소할까요?").assertDoesNotExist()
        assertEquals(listOf("unsave"), recorder.calls)
    }

    @Test fun `unsave dialog survives recreation`() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            AvTheme { ResultScreen(state = ResultUiState.Ready(caution, saved = true, banner = null), actions = recorder.actions) }
        }
        rule.onNodeWithText("저장됨").performClick()
        rule.onNodeWithText("저장을 취소할까요?").assertExists()

        tester.emulateSavedInstanceStateRestore()

        rule.onNodeWithText("저장을 취소할까요?").assertExists()
    }

    @Test fun `unsave dialog closes for good when the car is unsaved elsewhere`() {
        var saved by mutableStateOf(true)
        rule.setContent {
            AvTheme { ResultScreen(state = ResultUiState.Ready(caution, saved = saved, banner = null), actions = recorder.actions) }
        }
        rule.onNodeWithText("저장됨").performClick()
        rule.onNodeWithText("저장을 취소할까요?").assertExists()

        saved = false
        rule.waitForIdle()
        rule.onNodeWithText("저장을 취소할까요?").assertDoesNotExist()

        // Saving again must not bring back the stale dialog.
        saved = true
        rule.waitForIdle()
        rule.onNodeWithText("저장을 취소할까요?").assertDoesNotExist()
        assertEquals(emptyList<String>(), recorder.calls)
    }

    @Test fun `save button ignores taps while saving`() {
        show(ResultUiState.Ready(caution, saved = false, banner = null, saving = true))
        rule.onNodeWithText("저장하기").performClick()
        rule.onAllNodesWithText("저장을 취소할까요?").assertCountEquals(0)
        assertEquals(emptyList<String>(), recorder.calls)
    }
}
