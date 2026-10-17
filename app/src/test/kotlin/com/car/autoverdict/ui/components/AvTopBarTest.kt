package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
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
class AvTopBarTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `back button is labelled and invokes onBack`() {
        var backs = 0
        rule.setContent { AvTheme { AvTopBar(onBack = { backs++ }, title = "분석 결과", subtitle = "저장된 매물") } }
        rule.onNodeWithText("분석 결과").assertExists()
        rule.onNodeWithText("저장된 매물").assertExists()
        rule.onNodeWithContentDescription("뒤로").performClick()
        assertEquals(1, backs)
    }

    @Test fun `title has heading semantics`() {
        rule.setContent { AvTheme { AvTopBar(onBack = {}, title = "분석 결과") } }
        rule.onNodeWithText("분석 결과").assert(isHeading())
    }

    @Test fun `tab root bar has no back button`() {
        rule.setContent { AvTheme { AvTopBar(onBack = null, title = "AutoVerdict") } }
        rule.onNodeWithContentDescription("뒤로").assertDoesNotExist()
    }

    @Test fun `icon button exposes its description and click`() {
        var clicks = 0
        rule.setContent {
            AvTheme {
                AvTopBar(onBack = null, title = "AutoVerdict", actions = {
                    AvIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "설정",
                        onClick = { clicks++ },
                    )
                })
            }
        }
        rule.onNodeWithContentDescription("설정").performClick()
        assertEquals(1, clicks)
    }
}
