package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
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

    @Test fun `small pressable behind minimumInteractiveComponentSize keeps a 48dp touch target`() {
        var clicks = 0
        rule.setContent {
            AvTheme {
                // The tagged wrapper measures the laid-out slot; the clickable itself is pill-sized.
                Box(Modifier.testTag("slot")) {
                    Box(
                        Modifier
                            .minimumInteractiveComponentSize()
                            .avPressable(onClick = { clicks++ }, focusShape = RoundedCornerShape(999.dp))
                            .size(width = 80.dp, height = 24.dp),
                    )
                }
            }
        }
        val slot = rule.onNodeWithTag("slot")
        slot.assertHeightIsEqualTo(48.dp)
        // Just inside the 48dp slot but outside the 24dp-tall pill.
        slot.performTouchInput { click(Offset(centerX, 2f)) }
        rule.runOnIdle { assertEquals(1, clicks) }
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
