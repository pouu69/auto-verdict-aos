package com.car.autoverdict.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class ShareGuideSheetTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `open Encar closes the sheet and opens Encar once`() {
        var dismissed = 0
        var opened = 0
        rule.setContent {
            AvTheme {
                var open by remember { mutableStateOf(true) }
                if (open) {
                    ShareGuideSheet(
                        onDismiss = { dismissed++; open = false },
                        onOpenEncar = { opened++ },
                    )
                }
            }
        }
        rule.onNodeWithText("엔카 앱에서").assertExists()
        rule.onNodeWithText("엔카 열기").performScrollTo().performClick()
        rule.waitForIdle()

        assertEquals(1, dismissed)
        assertEquals(1, opened)
        rule.onNodeWithText("엔카에서 공유하는 법").assertDoesNotExist()
    }
}
