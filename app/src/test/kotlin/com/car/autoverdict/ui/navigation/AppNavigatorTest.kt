package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {

    private val url = "https://fem.encar.com/cars/detail/41623743"
    private val otherUrl = "https://fem.encar.com/cars/detail/99999999"

    private fun state(vararg routes: Route, pending: String? = null) = NavState(routes.toList(), pending)

    private fun reduce(s: NavState, e: NavEvent) = AppNavigator.reduce(s, e)

    // 앱 시작
    @Test fun `initial stack depends on onboarding completion`() {
        assertEquals(listOf(Route.Onboarding), AppNavigator.initial(onboardingComplete = false).stack)
        assertEquals(listOf(Route.Home), AppNavigator.initial(onboardingComplete = true).stack)
    }

    // 탭
    @Test fun `selecting another tab replaces the stack with that root`() {
        val r = reduce(state(Route.Home), NavEvent.SelectTab(MainTab.Saved))
        assertEquals(listOf(Route.Saved), r.state.stack)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `reselecting the current tab changes nothing`() {
        val s = state(Route.Saved)
        val r = reduce(s, NavEvent.SelectTab(MainTab.Saved))
        assertEquals(s, r.state)
        assertEquals(NavTransition.None, r.transition)
    }

    // push / pop
    @Test fun `open pushes the route`() {
        val r = reduce(state(Route.Home), NavEvent.Open(Route.Settings))
        assertEquals(listOf(Route.Home, Route.Settings), r.state.stack)
        assertEquals(NavTransition.Push, r.transition)
    }

    @Test fun `back pops when the stack has more than one route`() {
        val r = reduce(state(Route.Home, Route.Settings, Route.Licenses), NavEvent.Back)
        assertEquals(listOf(Route.Home, Route.Settings), r.state.stack)
        assertEquals(NavTransition.Pop, r.transition)
        assertTrue(r.handled)
    }

    @Test fun `back on saved tab goes to home tab`() {
        val r = reduce(state(Route.Saved), NavEvent.Back)
        assertEquals(listOf(Route.Home), r.state.stack)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `back on home or first-run onboarding is left to the system`() {
        assertFalse(reduce(state(Route.Home), NavEvent.Back).handled)
        assertFalse(reduce(state(Route.Onboarding), NavEvent.Back).handled)
    }

    @Test fun `canHandleBack mirrors the back rules`() {
        assertFalse(AppNavigator.canHandleBack(state(Route.Home)))
        assertFalse(AppNavigator.canHandleBack(state(Route.Onboarding)))
        assertTrue(AppNavigator.canHandleBack(state(Route.Saved)))
        assertTrue(AppNavigator.canHandleBack(state(Route.Home, Route.Settings)))
    }

    // 공유
    @Test fun `valid share after onboarding opens home then result`() {
        val r = reduce(state(Route.Saved, Route.Compare(listOf("1", "2"))), NavEvent.OpenShared(url))
        assertEquals(listOf(Route.Home, Route.Result(url, fromSaved = false)), r.state.stack)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `valid share during first-run onboarding is kept as pending`() {
        val r = reduce(state(Route.Onboarding), NavEvent.OpenShared(url))
        assertEquals(listOf(Route.Onboarding), r.state.stack)
        assertEquals(url, r.state.pendingSharedUrl)
        assertEquals(NavTransition.None, r.transition)
    }

    @Test fun `invalid share keeps the stack and asks for a toast`() {
        val s = state(Route.Home, Route.Settings)
        val r = reduce(s, NavEvent.InvalidShare)
        assertEquals(s, r.state)
        assertEquals(NavMessage.InvalidShare, r.message)
    }

    // 온보딩 완료
    @Test fun `finishing first-run onboarding with a pending share opens it`() {
        val r = reduce(state(Route.Onboarding, pending = url), NavEvent.OnboardingFinished)
        assertEquals(listOf(Route.Home, Route.Result(url, fromSaved = false)), r.state.stack)
        assertNull(r.state.pendingSharedUrl)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `finishing first-run onboarding without a pending share goes home`() {
        val r = reduce(state(Route.Onboarding), NavEvent.OnboardingFinished)
        assertEquals(listOf(Route.Home), r.state.stack)
    }

    @Test fun `finishing a replayed onboarding pops back to settings`() {
        val r = reduce(state(Route.Home, Route.Settings, Route.Onboarding), NavEvent.OnboardingFinished)
        assertEquals(listOf(Route.Home, Route.Settings), r.state.stack)
        assertEquals(NavTransition.Pop, r.transition)
    }

    @Test fun `finishing onboarding when it is no longer on top does nothing`() {
        val s = state(Route.Home, Route.Settings)
        val r = reduce(s, NavEvent.OnboardingFinished)
        assertEquals(s, r.state)
        assertEquals(NavTransition.None, r.transition)
    }

    // 중복 Result
    @Test fun `opening the same car twice does not stack a second result`() {
        val s = state(Route.Home, Route.Result(url, fromSaved = false))
        val r = reduce(s, NavEvent.Open(Route.Result("$url?utm_source=app", fromSaved = true)))
        assertEquals(s, r.state)
        assertEquals(NavTransition.None, r.transition)
    }

    @Test fun `opening a different car pushes`() {
        val s = state(Route.Home, Route.Result(url, fromSaved = false))
        val r = reduce(s, NavEvent.Open(Route.Result(otherUrl, fromSaved = false)))
        assertEquals(3, r.state.stack.size)
        assertEquals(NavTransition.Push, r.transition)
    }
}
