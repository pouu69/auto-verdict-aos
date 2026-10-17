package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
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
class AvEmptyStateTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `shows both actions and routes each click`() {
        var primary = 0
        var secondary = 0
        rule.setContent {
            AvTheme {
                AvEmptyState(
                    icon = Icons.Rounded.WifiOff,
                    title = "엔카 응답이 늦어요",
                    actionLabel = "다시 시도",
                    onAction = { primary++ },
                    secondaryActionLabel = "엔카에서 보기",
                    onSecondaryAction = { secondary++ },
                )
            }
        }
        rule.onNodeWithText("엔카 응답이 늦어요").assert(isHeading())
        rule.onNodeWithText("다시 시도").performClick()
        rule.onNodeWithText("엔카에서 보기").performClick()
        assertEquals(1, primary)
        assertEquals(1, secondary)
    }

    @Test fun `secondary action is optional`() {
        rule.setContent {
            AvTheme {
                AvEmptyState(icon = Icons.Rounded.WifiOff, title = "제목", actionLabel = "다시 시도", onAction = {})
            }
        }
        rule.onNodeWithText("다시 시도").assertExists()
        rule.onNodeWithText("엔카에서 보기").assertDoesNotExist()
    }
}
