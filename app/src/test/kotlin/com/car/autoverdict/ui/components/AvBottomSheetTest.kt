package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The open sheet's height (its window's bottom minus the drag handle's top) and its window's height, in px. */
internal fun ComposeContentTestRule.sheetAndWindowHeight(): Pair<Float, Float> {
    val handle = onNodeWithContentDescription("시트 닫기").fetchSemanticsNode().boundsInRoot
    val window = onNode(isRoot() and hasAnyDescendant(hasContentDescription("시트 닫기"))).fetchSemanticsNode().boundsInRoot
    return (window.bottom - handle.top) to window.height
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
@OptIn(ExperimentalMaterial3Api::class)
class AvBottomSheetTest {

    @get:Rule val rule = createComposeRule()

    private fun show(lines: Int, maxHeightFraction: Float?) {
        rule.setContent {
            AvTheme {
                AvBottomSheet(onDismiss = {}, title = "제목", maxHeightFraction = maxHeightFraction) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        repeat(lines) { Text("줄 ${it + 1}", Modifier.height(40.dp)) }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun px(dp: Int) = with(rule.density) { dp.dp.toPx() }

    @Test fun `a capped sheet stays within its share of the screen and scrolls the rest`() {
        show(lines = 60, maxHeightFraction = 0.5f)
        val (sheet, window) = rule.sheetAndWindowHeight()
        assertTrue("sheet $sheet should be at most half of $window", sheet <= window * 0.5f + px(1))
        // Long content fills the cap rather than stopping well short of it.
        assertTrue("sheet $sheet should fill about half of $window", sheet >= window * 0.5f - px(40))

        rule.onNodeWithText("줄 60").assertIsNotDisplayed()
        rule.onNode(hasScrollAction() and hasAnyDescendant(hasText("줄 1"))).performScrollToNode(hasText("줄 60"))
        rule.onNodeWithText("줄 60").assertIsDisplayed()
    }

    @Test fun `a capped sheet with short content wraps it`() {
        show(lines = 3, maxHeightFraction = 0.5f)
        val (sheet, window) = rule.sheetAndWindowHeight()
        assertTrue("sheet $sheet should wrap well under half of $window", sheet < window * 0.35f)
        rule.onNodeWithText("줄 3").assertIsDisplayed()
    }

    @Test fun `without a cap the sheet still grows to the full screen`() {
        show(lines = 60, maxHeightFraction = null)
        val (sheet, window) = rule.sheetAndWindowHeight()
        assertTrue("sheet $sheet should take nearly all of $window", sheet > window * 0.9f)
    }
}
