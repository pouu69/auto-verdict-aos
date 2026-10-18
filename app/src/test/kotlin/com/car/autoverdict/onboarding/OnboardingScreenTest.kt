package com.car.autoverdict.onboarding

import android.app.Application
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
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class OnboardingScreenTest {

    @get:Rule val rule = createComposeRule()

    private var finished = 0
    private var backs = 0

    private fun show(mode: OnboardingMode, hasPendingShare: Boolean = false) {
        rule.setContent {
            AvTheme {
                OnboardingScreen(
                    mode = mode,
                    hasPendingShare = hasPendingShare,
                    onFinish = { finished++ },
                    onBack = if (mode == OnboardingMode.Replay) ({ backs++ }) else null,
                )
            }
        }
    }

    @Test fun `first run offers skip without back, and start on the last page finishes`() {
        show(OnboardingMode.FirstRun)
        rule.onNodeWithText("건너뛰기").assertExists()
        rule.onNodeWithContentDescription("뒤로").assertDoesNotExist()
        rule.onNodeWithContentDescription("2쪽 중 1쪽").assertExists()

        rule.onNodeWithText("다음").performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("2쪽 중 2쪽").assertExists()
        rule.onNodeWithText("건너뛰기").assertDoesNotExist()
        rule.onNodeWithText("시작하기").performClick()
        assertEquals(1, finished)
    }

    @Test fun `replay has back only and ends with close`() {
        show(OnboardingMode.Replay)
        rule.onNodeWithText("건너뛰기").assertDoesNotExist()
        rule.onNodeWithContentDescription("뒤로").performClick()
        assertEquals(1, backs)

        rule.onNodeWithText("다음").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("시작하기").assertDoesNotExist()
        rule.onNodeWithText("닫기").performClick()
        assertEquals(1, finished)
    }

    @Test fun `first run with a pending share explains it will be analyzed`() {
        show(OnboardingMode.FirstRun, hasPendingShare = true)
        rule.onNodeWithText("공유한 매물은 시작하면 바로 분석해요").assertExists()
    }
}
