package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class AvTextFieldTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `shows placeholder when empty`() {
        rule.setContent { AvTheme { AvTextField(value = "", onValueChange = {}, placeholder = "엔카 매물 링크 붙여넣기") } }
        rule.onNodeWithText("엔카 매물 링크 붙여넣기").assertIsDisplayed()
    }

    @Test fun `shows error text under the field`() {
        rule.setContent {
            AvTheme {
                AvTextField(value = "abc", onValueChange = {}, placeholder = "p", errorText = "엔카 매물 링크가 아니에요")
            }
        }
        rule.onNodeWithText("엔카 매물 링크가 아니에요").assertIsDisplayed()
    }

    @Test fun `error is exposed on the editable field semantics`() {
        rule.setContent {
            AvTheme {
                AvTextField(value = "abc", onValueChange = {}, placeholder = "p", errorText = "엔카 매물 링크가 아니에요")
            }
        }
        rule.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test fun `clear button appears with text and calls onClear`() {
        var cleared = 0
        rule.setContent {
            AvTheme { AvTextField(value = "abc", onValueChange = {}, placeholder = "p", onClear = { cleared++ }) }
        }
        rule.onNodeWithContentDescription("지우기").performClick()
        assertEquals(1, cleared)
    }

    @Test fun `action button shows only while empty and calls onAction`() {
        var value by mutableStateOf("")
        var pasted = 0
        rule.setContent {
            AvTheme {
                AvTextField(
                    value = value,
                    onValueChange = {},
                    placeholder = "p",
                    onClear = {},
                    actionLabel = "붙여넣기",
                    onAction = { pasted++ },
                )
            }
        }
        rule.onNodeWithText("붙여넣기").performClick()
        assertEquals(1, pasted)

        value = "abc"
        rule.onNodeWithText("붙여넣기").assertDoesNotExist()
        rule.onNodeWithContentDescription("지우기").assertIsDisplayed()
    }
}
