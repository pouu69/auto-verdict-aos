package com.car.autoverdict.home

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.util.EncarUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class ShareGuideSheetTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `guide teaches only the browser flow and open Encar closes the sheet once`() {
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
        rule.onNodeWithText("엔카 앱에서").assertDoesNotExist()
        // The inline menu icon is read out by name, so the step stays a full sentence.
        rule.onNodeWithText("오른쪽 위 점 세 개 메뉴를 눌러요", substring = true).assertExists()
        rule.onNodeWithText("열린 메뉴에서 공유를 눌러요").assertExists()
        rule.onNodeWithText("공유 목록에서 AutoVerdict를 골라요").assertExists()
        // Illustrations are decorative: their labels stay out of the semantics tree.
        rule.onNodeWithText("방문 기록").assertDoesNotExist()

        rule.onNodeWithText("브라우저에서 엔카 열기").performScrollTo().performClick()
        rule.waitForIdle()

        assertEquals(1, dismissed)
        assertEquals(1, opened)
        rule.onNodeWithText("엔카에서 공유하는 법").assertDoesNotExist()
    }

    @Test fun `open Encar targets a browser first, then falls back to a plain link`() {
        val tried = mutableListOf<Intent>()
        var browserResolves = true
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Application>()) {
            override fun startActivity(intent: Intent) {
                tried += intent
                if (intent.selector != null && !browserResolves) throw ActivityNotFoundException()
            }
        }

        assertTrue(context.openEncarInBrowser())
        val browser = tried.single()
        assertEquals(Intent.ACTION_VIEW, browser.action)
        assertEquals(EncarUrl.HOME_URL, browser.dataString)
        assertTrue(browser.selector!!.hasCategory(Intent.CATEGORY_APP_BROWSER))

        tried.clear()
        browserResolves = false
        assertTrue(context.openEncarInBrowser())
        assertEquals(2, tried.size)
        assertNull(tried[1].selector)
        assertEquals(EncarUrl.HOME_URL, tried[1].dataString)
    }
}
