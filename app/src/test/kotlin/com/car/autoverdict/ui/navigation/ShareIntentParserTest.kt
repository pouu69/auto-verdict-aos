package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareIntentParserTest {

    private val send = "android.intent.action.SEND"
    private val canonical = "https://fem.encar.com/cars/detail/41623743"

    @Test fun `encar share text is normalized to the canonical detail url`() {
        val text = "[엔카] 현대 그랜저 IG 2.4 https://fem.encar.com/cars/detail/41623743?utm_source=share 확인해보세요"
        assertEquals(NavEvent.OpenShared(canonical), ShareIntentParser.parse(send, "text/plain", text, isRestored = false))
    }

    @Test fun `non-encar text is an invalid share`() {
        assertEquals(NavEvent.InvalidShare, ShareIntentParser.parse(send, "text/plain", "https://google.com", isRestored = false))
    }

    @Test fun `missing text is ignored`() {
        assertNull(ShareIntentParser.parse(send, "text/plain", null, isRestored = false))
    }

    @Test fun `other actions and types are ignored`() {
        assertNull(ShareIntentParser.parse("android.intent.action.MAIN", null, null, isRestored = false))
        assertNull(ShareIntentParser.parse(send, "image/png", canonical, isRestored = false))
    }

    @Test fun `restored activity never re-handles the launch intent`() {
        assertNull(ShareIntentParser.parse(send, "text/plain", canonical, isRestored = true))
    }
}
