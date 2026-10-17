package com.car.autoverdict.ui.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareIntentParserTest {

    private val send = "android.intent.action.SEND"
    private val canonical = "https://fem.encar.com/cars/detail/41623743"

    private fun parse(
        action: String?,
        type: String?,
        text: String?,
        flags: Int = 0,
        isRestored: Boolean = false,
    ) = ShareIntentParser.parse(action, type, text, flags, isRestored)

    @Test fun `encar share text is normalized to the canonical detail url`() {
        val text = "[엔카] 현대 그랜저 IG 2.4 https://fem.encar.com/cars/detail/41623743?utm_source=share 확인해보세요"
        assertEquals(NavEvent.OpenShared(canonical), parse(send, "text/plain", text))
    }

    @Test fun `non-encar text is an invalid share`() {
        assertEquals(NavEvent.InvalidShare, parse(send, "text/plain", "https://google.com"))
    }

    @Test fun `missing text is ignored`() {
        assertNull(parse(send, "text/plain", null))
    }

    @Test fun `other actions and types are ignored`() {
        assertNull(parse("android.intent.action.MAIN", null, null))
        assertNull(parse(send, "image/png", canonical))
    }

    @Test fun `restored activity never re-handles the launch intent`() {
        assertNull(parse(send, "text/plain", canonical, isRestored = true))
    }

    @Test fun `relaunch from recents never replays the share that started the task`() {
        assertNull(parse(send, "text/plain", canonical, flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY))
        assertNull(
            parse(
                send,
                "text/plain",
                canonical,
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            ),
        )
    }

    @Test fun `share sheet launch flags do not suppress a fresh share`() {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT or
            Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
        assertEquals(NavEvent.OpenShared(canonical), parse(send, "text/plain", canonical, flags = flags))
    }
}
