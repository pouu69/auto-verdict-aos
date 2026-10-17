package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.car.autoverdict.ui.theme.AvTheme
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
}
