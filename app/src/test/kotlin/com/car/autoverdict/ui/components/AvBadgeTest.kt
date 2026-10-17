package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvBadgeTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `badge shows its text`() {
        rule.setContent { AvTheme { AvBadge(text = "괜찮아요", tone = AvTone.Success) } }
        rule.onNodeWithText("괜찮아요").assertIsDisplayed()
    }

    @Test fun `fact tile merges label and value into one node`() {
        rule.setContent { AvTheme { AvFactTile(label = "주행거리", value = "8.2만km") } }
        rule.onNode(hasText("주행거리") and hasText("8.2만km")).assertExists()
    }
}
