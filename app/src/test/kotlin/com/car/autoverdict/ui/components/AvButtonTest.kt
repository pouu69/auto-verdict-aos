package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.LightAvColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `click invokes onClick`() {
        var clicks = 0
        rule.setContent { AvTheme { AvButton(text = "저장하기", onClick = { clicks++ }) } }
        rule.onNodeWithText("저장하기").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test fun `disabled button ignores clicks`() {
        var clicks = 0
        rule.setContent { AvTheme { AvButton(text = "저장하기", onClick = { clicks++ }, enabled = false) } }
        rule.onNodeWithText("저장하기").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test fun `loading button keeps its label for accessibility but ignores clicks`() {
        var clicks = 0
        rule.setContent { AvTheme { AvButton(text = "저장하기", onClick = { clicks++ }, loading = true) } }
        rule.onNodeWithText("저장하기").assertExists().assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "진행 중"))
            .performClick()
        assertEquals(0, clicks)
    }

    @Test fun `small button still has a 48dp touch target`() {
        rule.setContent {
            AvTheme { AvButton(text = "더보기", onClick = {}, style = AvButtonStyle.Secondary, size = AvButtonSize.Small) }
        }
        rule.onNodeWithText("더보기").assertTouchHeightIsEqualTo(48.dp)
    }

    @Test fun `primary is allowed only in large and medium`() {
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Large))
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Medium))
        assertEquals(false, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Small))
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Secondary, AvButtonSize.Small))
    }

    @Test fun `disabled buttons are light grey with a readable grey label for every style`() {
        val c = LightAvColors
        AvButtonStyle.entries.forEach { style ->
            assertEquals(c.surfaceMuted to c.textTertiary, avButtonColors(style, enabled = false, colors = c))
        }
        assertEquals(c.accent to c.onAccent, avButtonColors(AvButtonStyle.Primary, enabled = true, colors = c))
    }
}
