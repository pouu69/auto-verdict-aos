package com.car.autoverdict.ui.screen

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
@Config(application = Application::class, qualifiers = "w411dp-h4000dp")
class PrivacyPolicyScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `summary shows the key sections as headings and a link row to the full policy`() {
        var opened = 0
        rule.setContent {
            AvTheme { PrivacyPolicyContent(onBack = {}, onOpenFullPolicy = { opened++ }) }
        }

        rule.onNodeWithText("분석한 차 정보는 내 기기에만 저장돼요").assertExists()
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        listOf("처리하는 정보", "저장 위치와 기간", "지우는 방법", "광고", "엔카와의 통신", "권한", "문의").forEach {
            rule.onNode(hasText(it) and heading).assertExists()
        }
        // The ad section must disclose what AdMob sends instead of claiming nothing leaves the device.
        rule.onNodeWithText("Google로 전송돼요", substring = true).assertExists()

        rule.onNode(hasText("전체 방침 보기") and hasText("웹에서 열려요")).performClick()
        assertEquals(1, opened)
    }
}
