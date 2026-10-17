package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavStateCodecTest {

    private val url = "https://fem.encar.com/cars/detail/41623743"

    @Test fun `state with pending url round trips`() {
        val s = NavState(listOf(Route.Onboarding), pendingSharedUrl = url)
        assertEquals(s, NavStateCodec.decode(NavStateCodec.encode(s)))
    }

    @Test fun `state without pending url round trips`() {
        val s = NavState(listOf(Route.Home, Route.Result(url, fromSaved = true)))
        assertEquals(s, NavStateCodec.decode(NavStateCodec.encode(s)))
    }

    @Test fun `unreadable saved state decodes to null so the initial state is used`() {
        assertNull(NavStateCodec.decode(listOf("")))
        assertNull(NavStateCodec.decode(listOf("", "garbage")))
        assertNull(NavStateCodec.decode(listOf("", "home", "garbage")))
        assertNull(NavStateCodec.decode(emptyList()))
    }
}
