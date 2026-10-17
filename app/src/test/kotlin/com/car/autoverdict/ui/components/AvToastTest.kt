package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvToastTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `show displays the message without a caller scope`() {
        lateinit var toast: AvToastState
        rule.setContent {
            AvTheme {
                toast = rememberAvToastState()
                AvToastHost(state = toast)
            }
        }
        rule.runOnIdle { toast.show("저장했어요") }
        rule.waitUntil(timeoutMillis = 2_000) {
            rule.onAllNodesWithText("저장했어요").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
