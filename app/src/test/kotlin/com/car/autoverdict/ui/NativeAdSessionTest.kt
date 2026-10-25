package com.car.autoverdict.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** The owner outlives lazy items, so scrolling must never own or restart an ad request. */
class NativeAdSessionTest {
    private class Ad(var destroyed: Int = 0)
    private lateinit var deliver: (Ad) -> Unit
    private var requests = 0
    private fun session() = NativeAdSession<Ad>(
        request = { loaded, _ -> requests++; deliver = loaded },
        destroy = { it.destroyed++ },
    )

    @Test fun `returning to an item while pending reuses the screen request`() {
        val owner = session()
        owner.load()
        owner.load() // lazy item is recomposed after scrolling away and back
        assertEquals(1, requests)
        val ad = Ad()
        deliver(ad)
        assertSame(ad, owner.ad)
        assertEquals(0, ad.destroyed)
    }

    @Test fun `loaded ad survives item recycling until screen owner closes`() {
        val owner = session()
        owner.load()
        val ad = Ad()
        deliver(ad)
        owner.load() // a newly composed view must bind this same ad
        assertSame(ad, owner.ad)
        assertEquals(1, requests)
        assertEquals(0, ad.destroyed)
        owner.dispose()
        owner.dispose()
        assertEquals(1, ad.destroyed)
        assertNull(owner.ad)
    }

    @Test fun `late success after screen departure destroys the unused ad`() {
        val owner = session()
        owner.load()
        owner.dispose()
        val late = Ad()
        deliver(late)
        assertEquals(1, late.destroyed)
        assertNull(owner.ad)
        owner.load()
        assertEquals(1, requests)
    }
}
