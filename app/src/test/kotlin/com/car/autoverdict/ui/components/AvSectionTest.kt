package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
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
}
