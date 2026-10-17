package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvPressableTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `indication factories with the same focus shape are equal so the node is reused`() {
        assertEquals(AvPressIndication(null), AvPressIndication(null))
        assertEquals(AvPressIndication(RoundedCornerShape(16.dp)), AvPressIndication(RoundedCornerShape(16.dp)))
        assertEquals(AvPressIndication(CircleShape).hashCode(), AvPressIndication(CircleShape).hashCode())
        assertNotEquals(AvPressIndication(CircleShape), AvPressIndication(RoundedCornerShape(16.dp)))
        assertNotEquals(AvPressIndication(null), AvPressIndication(CircleShape))
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test fun `keyboard-focused primary button still clicks on Enter`() {
        var clicks = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            AvTheme { AvButton(text = "저장하기", onClick = { clicks++ }) }
        }
        // Clickables are only focusable outside touch mode, as with a hardware keyboard / D-pad.
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val button = rule.onNodeWithText("저장하기")
        button.requestFocus().assertIsFocused()
        button.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(1, clicks) }
    }
}
