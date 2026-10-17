package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.unit.dp
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
class AvSectionTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `header title has heading semantics`() {
        rule.setContent { AvTheme { AvSectionHeader(title = "저장한 매물") } }
        rule.onNodeWithText("저장한 매물").assert(isHeading())
    }

    @Test fun `header action click invokes onAction`() {
        var actions = 0
        rule.setContent {
            AvTheme { AvSectionHeader(title = "저장한 매물", actionLabel = "전체 보기", onAction = { actions++ }) }
        }
        rule.onNodeWithText("전체 보기").performClick()
        assertEquals(1, actions)
    }

    @Test fun `header action is hidden without onAction`() {
        rule.setContent { AvTheme { AvSectionHeader(title = "저장한 매물", actionLabel = "전체 보기", onAction = null) } }
        rule.onNodeWithText("전체 보기").assertDoesNotExist()
    }

    @Test fun `plain header is not a button`() {
        rule.setContent { AvTheme { AvSectionHeader(title = "저장한 매물", count = 3) } }
        rule.onNodeWithText("저장한 매물 3")
            .assert(isHeading())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test fun `collapsible header is a heading button that toggles its body`() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            AvTheme {
                Column {
                    AvSectionHeader(title = "확인할 것", count = 2, expanded = expanded, onToggle = { expanded = !expanded })
                    AvCollapsibleBody(expanded = expanded) { Text("본문") }
                }
            }
        }
        val header = rule.onNodeWithText("확인할 것 2")
        header.assert(isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "펼쳐짐"))
            .assert(clickLabel("확인할 것 접기"))
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("본문").assertExists()

        header.performClick()
        rule.onNodeWithText("본문").assertDoesNotExist()
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "접힘"))
            .assert(clickLabel("확인할 것 펼치기"))

        header.performClick()
        rule.onNodeWithText("본문").assertExists()
    }

    private fun clickLabel(label: String) = SemanticsMatcher("onClick label is $label") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }
}
