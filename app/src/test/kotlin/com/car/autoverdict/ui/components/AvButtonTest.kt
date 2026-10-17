package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
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
        rule.onNodeWithText("저장하기").assertExists().assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test fun `primary is allowed only in large and medium`() {
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Large))
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Medium))
        assertEquals(false, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Small))
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Secondary, AvButtonSize.Small))
    }
}
