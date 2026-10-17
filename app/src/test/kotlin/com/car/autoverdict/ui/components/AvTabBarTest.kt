package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvTabBarTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `selecting a tab reports it`() {
        var selected: MainTab? = null
        rule.setContent { AvTheme { AvTabBar(selected = MainTab.Home, onSelect = { selected = it }, savedCount = 0) } }
        rule.onNodeWithText("홈").assertIsSelected()
        rule.onNodeWithText("저장한 차").performClick()
        assertEquals(MainTab.Saved, selected)
    }

    @Test fun `saved tab shows the count when there are saved cars`() {
        rule.setContent { AvTheme { AvTabBar(selected = MainTab.Home, onSelect = {}, savedCount = 3) } }
        rule.onNodeWithText("저장한 차 3").assertExists()
    }
}
