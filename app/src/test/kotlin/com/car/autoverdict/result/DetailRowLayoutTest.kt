package com.car.autoverdict.result

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.car.autoverdict.report.DetailBlock
import com.car.autoverdict.report.DetailRow
import com.car.autoverdict.report.DetailSheet
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.result.sections.DetailSheetContent
import com.car.autoverdict.result.sections.keepWordsTogether
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Emulator QA (2026-10-17): detail rows wrapped short labels and split values mid-word.
 * Legacy Robolectric graphics measure text as nearly zero-width, so these need native graphics.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DetailRowLayoutTest {

    @get:Rule val rule = createComposeRule()

    private val criterion = "내차·타차 수리비 중 큰 쪽이 국산 200만원·수입 400만원 이하"

    /** Sheet widths to sweep, so wherever the value wraps a mid-word split would show somewhere. */
    private val widths = (160..360 step 5).toList()

    private fun showCriterionAtEveryWidth() {
        val sheet = DetailSheet(title = "판정 기준", blocks = listOf(DetailBlock(rows = listOf(DetailRow("통과", criterion)))))
        rule.setContent {
            AvTheme {
                Column {
                    widths.forEach { Box(Modifier.width(it.dp)) { DetailSheetContent(sheet = sheet) } }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun showSheet(report: EvaluationReport, key: String) {
        rule.setContent {
            AvTheme { ResultDetailSheet(report = report, openKey = key, onDismiss = {}, onCopyQuestion = {}) }
        }
        rule.waitForIdle()
    }

    private fun layoutsOf(text: String): List<TextLayoutResult> {
        val nodes = rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("no text node reads $text", nodes.isNotEmpty())
        return List(nodes.size) { i ->
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onAllNodesWithText(text, useUnmergedTree = true)[i]
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.single()
        }
    }

    @Test fun `a period label stays on one line at normal size`() {
        val report = sampleReport("encar-41878112.json")
        showSheet(report, DetailKey.rule("R08"))
        val label = layoutsOf("2023년 11월~2024년 1월").first()
        assertEquals(1, label.lineCount)
        assertFalse(label.hasVisualOverflow)
    }

    @Test fun `a wrapping value breaks between words, never inside 400만원`() {
        showCriterionAtEveryWidth()
        val values = layoutsOf(criterion)
        assertEquals(widths.size, values.size)
        assertTrue("the value should wrap at most widths", values.count { it.lineCount > 1 } > widths.size / 2)
        values.forEach { value -> assertBreaksOnlyBetweenWords(value) }
    }

    // Phrase breaking needs API 33; older platforms get word joiners instead.
    @Config(sdk = [32])
    @Test fun `a wrapping value breaks between words before API 33 too`() {
        showCriterionAtEveryWidth()
        val values = layoutsOf(keepWordsTogether(criterion))
        assertEquals(widths.size, values.size)
        assertTrue("the value should wrap at most widths", values.count { it.lineCount > 1 } > widths.size / 2)
        values.forEach { value -> assertBreaksOnlyBetweenWords(value) }
    }

    private fun assertBreaksOnlyBetweenWords(layout: TextLayoutResult) {
        val text = layout.layoutInput.text.text
        val lines = List(layout.lineCount) { text.substring(layout.getLineStart(it), layout.getLineEnd(it)) }
        for (i in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(i)
            assertTrue("line ${i + 1} of $lines ends mid-word", text[end - 1].isWhitespace())
        }
        assertTrue("400만원 was split: $lines", lines.any { "400만원" in it.replace(WORD_JOINER, "") })
    }

    @Test fun `keeping words together joins every character inside a word only`() {
        val joined = keepWordsTogether("400만원 이하")
        assertEquals("4${WORD_JOINER}0${WORD_JOINER}0${WORD_JOINER}만${WORD_JOINER}원 이${WORD_JOINER}하", joined)
        assertEquals("400만원 이하", joined.replace(WORD_JOINER, ""))
        assertEquals("", keepWordsTogether(""))
        assertEquals("a  b", keepWordsTogether("a  b"))
    }

    private companion object {
        const val WORD_JOINER = "\u2060"
    }
}
