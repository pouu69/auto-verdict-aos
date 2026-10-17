package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
class AvListRowTest {

    @get:Rule val rule = createComposeRule()

    private val mergedRow = hasText("그랜저 IG") and hasText("8.2만km · 2,180만원")

    @Test fun `clickable row is one button node that reads title and subtitle together`() {
        var clicks = 0
        rule.setContent {
            AvTheme { AvListRow(title = "그랜저 IG", subtitle = "8.2만km · 2,180만원", onClick = { clicks++ }) }
        }
        rule.onNode(mergedRow)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(1, clicks)
    }

    @Test fun `static row still merges its texts`() {
        rule.setContent { AvTheme { AvListRow(title = "그랜저 IG", subtitle = "8.2만km · 2,180만원") } }
        rule.onNode(mergedRow).assertExists()
    }
}
