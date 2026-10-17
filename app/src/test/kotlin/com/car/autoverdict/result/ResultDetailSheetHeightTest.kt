package com.car.autoverdict.result

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.car.autoverdict.report.DetailBlock
import com.car.autoverdict.report.DetailRow
import com.car.autoverdict.report.DetailSheet
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.ui.components.sheetAndWindowHeight
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Spec 4.1 (user decision 2026-10-17): the detail sheet opens at half the screen and no taller. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp")
class ResultDetailSheetHeightTest {

    @get:Rule val rule = createComposeRule()

    private val caution by lazy { sampleReport("sample-caution.json") }

    private val longInspection = DetailSheet(
        title = "성능점검 전체",
        blocks = List(8) { b ->
            DetailBlock(title = "블록 ${b + 1}", rows = List(8) { r -> DetailRow("부위 ${b + 1}-${r + 1}", "없음") })
        } + DetailBlock(title = "마지막 블록", rows = listOf(DetailRow("마지막 부위", "양호"))),
    )

    private fun showRepairs(report: EvaluationReport) {
        rule.setContent {
            AvTheme {
                ResultDetailSheet(report = report, openKey = DetailKey.REPAIRS, onDismiss = {}, onCopyQuestion = {})
            }
        }
        rule.waitForIdle()
    }

    @Test fun `a long sheet stops at half the screen and scrolls inside`() {
        showRepairs(caution.copy(repairs = caution.repairs!!.copy(sheet = longInspection)))
        val (sheet, window) = rule.sheetAndWindowHeight()
        val tolerance = with(rule.density) { 1.dp.toPx() }
        assertTrue("sheet $sheet should be at most half of $window", sheet <= window * 0.5f + tolerance)

        val lastRow = hasText("마지막 부위") and hasText("양호")
        rule.onNode(lastRow).assertIsNotDisplayed()
        rule.onNode(hasScrollAction() and hasAnyDescendant(hasText("성능점검 전체"))).performScrollToNode(lastRow)
        rule.onNode(lastRow).assertIsDisplayed()
        rule.onNode(hasText("마지막 블록")).assertIsDisplayed()
    }

    @Test fun `a short sheet wraps its content`() {
        val short = DetailSheet(title = "성능점검 전체", blocks = listOf(DetailBlock(rows = listOf(DetailRow("원동기", "양호")))))
        showRepairs(caution.copy(repairs = caution.repairs!!.copy(sheet = short)))
        val (sheet, window) = rule.sheetAndWindowHeight()
        assertTrue("sheet $sheet should wrap well under half of $window", sheet < window * 0.35f)
        rule.onNode(hasText("원동기") and hasText("양호")).assertIsDisplayed()
    }
}
