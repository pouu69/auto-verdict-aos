package com.car.autoverdict.ui.screen

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
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
@Config(application = Application::class, qualifiers = "w411dp-h2000dp")
class SettingsScreenTest {

    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(recent: Int?, saved: Int?, withGallery: Boolean = false) {
        rule.setContent {
            AvTheme {
                SettingsScreen(
                    state = SettingsUiState(recentCount = recent, savedCount = saved, versionName = "1.0.2"),
                    onClearRecent = { calls += "clearRecent" },
                    onClearSaved = { calls += "clearSaved" },
                    onPrivacyPolicy = { calls += "privacy" },
                    onLicenses = { calls += "licenses" },
                    onReplayGuide = { calls += "guide" },
                    onContact = { calls += "contact" },
                    onGallery = if (withGallery) ({ calls += "gallery" }) else null,
                    onBack = { calls += "back" },
                )
            }
        }
    }

    private fun row(title: String, value: String) = rule.onNode(hasText(title) and hasText(value))

    private val clickable = SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)

    @Test fun `counts show on the data rows and an empty one can't be tapped`() {
        show(recent = 3, saved = 0)

        row("최근 본 차 지우기", "3대").assert(clickable)
        row("저장한 차 모두 지우기", "없음").assert(!clickable)
        row("버전", "1.0.2").assert(!clickable)
    }

    @Test fun `gallery row appears only with a callback`() {
        show(recent = 0, saved = 0)
        rule.onNodeWithText("컴포넌트 갤러리").assertDoesNotExist()
    }

    @Test fun `debug gallery row opens the gallery`() {
        show(recent = 0, saved = 0, withGallery = true)
        rule.onNodeWithText("컴포넌트 갤러리").performClick()
        assertEquals(listOf("gallery"), calls)
    }

    @Test fun `clearing saved cars runs only after confirming the dialog`() {
        show(recent = 0, saved = 5)

        row("저장한 차 모두 지우기", "5대").performClick()
        rule.onNodeWithText("저장한 차 5대를 모두 지울까요?").assertExists()
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("저장한 차 5대를 모두 지울까요?").assertDoesNotExist()
        assertEquals(emptyList<String>(), calls)

        row("저장한 차 모두 지우기", "5대").performClick()
        rule.onNodeWithText("모두 지우기").performClick()
        assertEquals(listOf("clearSaved"), calls)
        rule.onNodeWithText("저장한 차 5대를 모두 지울까요?").assertDoesNotExist()
    }

    @Test fun `clearing recent views confirms with the count`() {
        show(recent = 3, saved = 0)

        row("최근 본 차 지우기", "3대").performClick()
        rule.onNodeWithText("홈에 보이는 최근 본 차 3대가 사라져요. 저장한 차는 그대로예요.").assertExists()
        rule.onNodeWithText("지우기").performClick()
        assertEquals(listOf("clearRecent"), calls)
    }

    @Test fun `contact row shows the address and opens mail`() {
        show(recent = 0, saved = 0)
        row("문의하기", "pouu69@gmail.com").performClick()
        assertEquals(listOf("contact"), calls)
    }
}
