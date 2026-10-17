package com.car.autoverdict.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ToastPaddingTest {

    private val navBar = 24.dp
    private val chrome = 131.dp

    @Test fun `tab roots sit above the banner and tab bar`() {
        assertEquals(chrome, toastBottomPadding(Route.Home, navBar, chrome))
        assertEquals(chrome, toastBottomPadding(Route.Saved, navBar, chrome))
    }

    @Test fun `settings sits above its banner`() {
        assertEquals(navBar + 50.dp + 8.dp, toastBottomPadding(Route.Settings, navBar, chrome))
    }

    @Test fun `other sub-screens sit just above the navigation bar`() {
        assertEquals(navBar + 16.dp, toastBottomPadding(Route.Licenses, navBar, chrome))
    }
}
