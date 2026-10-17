package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteCodecTest {

    private val url = "https://fem.encar.com/cars/detail/41623743"

    @Test fun `every route survives an encode-decode round trip`() {
        val routes = listOf(
            Route.Onboarding, Route.Home, Route.Saved, Route.Settings,
            Route.PrivacyPolicy, Route.Licenses, Route.Gallery,
            Route.Result(url, fromSaved = false),
            Route.Result(url, fromSaved = true),
            Route.Compare(listOf("111", "222")),
            Route.LicenseDetail("lib:jetpack-compose"),
            Route.LicenseDetail("google:Animal Sniffer"),
        )
        routes.forEach { assertEquals(it, RouteCodec.decode(RouteCodec.encode(it))) }
    }

    @Test fun `result url keeps separator characters intact`() {
        val odd = Route.Result("https://x.test/a|b?c=1|2", fromSaved = false)
        assertEquals(odd, RouteCodec.decode(RouteCodec.encode(odd)))
    }

    @Test fun `malformed values decode to null`() {
        assertNull(RouteCodec.decode("nope"))
        assertNull(RouteCodec.decode("result|1"))
        assertNull(RouteCodec.decode("compare|"))
        assertNull(RouteCodec.decode("license|"))
        assertNull(RouteCodec.decode("license"))
        assertNull(RouteCodec.decode(""))
    }

    @Test fun `tab roots map to tabs and back`() {
        assertEquals(MainTab.Home, Route.Home.tabOrNull())
        assertEquals(MainTab.Saved, Route.Saved.tabOrNull())
        assertNull(Route.Settings.tabOrNull())
        assertEquals(Route.Home, MainTab.Home.route)
        assertEquals(Route.Saved, MainTab.Saved.route)
    }
}
