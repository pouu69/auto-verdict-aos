package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvBottomCtaTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `renders its buttons`() {
        rule.setContent {
            AvTheme {
                AvBottomCta {
                    AvButton(text = "판매자에게 물어볼 것", onClick = {}, modifier = Modifier.weight(1f))
                }
            }
        }
        rule.onNodeWithText("판매자에게 물어볼 것").assertIsDisplayed()
    }

    @Test fun `custom window insets replace the default bottom padding`() {
        rule.setContent {
            AvTheme {
                Column {
                    AvBottomCta(modifier = Modifier.testTag("none"), windowInsets = WindowInsets(0)) {
                        AvButton(text = "a", onClick = {}, modifier = Modifier.weight(1f))
                    }
                    AvBottomCta(modifier = Modifier.testTag("inset"), windowInsets = WindowInsets(bottom = 24.dp)) {
                        AvButton(text = "b", onClick = {}, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        val none = rule.onNodeWithTag("none").getUnclippedBoundsInRoot().height
        val inset = rule.onNodeWithTag("inset").getUnclippedBoundsInRoot().height
        assertEquals(24f, (inset - none).value, 0.5f)
    }
}
