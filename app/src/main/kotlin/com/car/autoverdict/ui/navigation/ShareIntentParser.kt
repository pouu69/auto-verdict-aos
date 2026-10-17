package com.car.autoverdict.ui.navigation

import com.car.autoverdict.util.EncarUrl

/**
 * Turns a share intent into a navigation event. Pure so it can be unit tested; the
 * activity passes `isRestored = savedInstanceState != null` so rotation or process
 * restore never replays the launch intent over the restored back stack (spec 7.2).
 */
object ShareIntentParser {

    private const val ACTION_SEND = "android.intent.action.SEND"
    private const val TYPE_TEXT = "text/plain"

    fun parse(action: String?, type: String?, text: String?, isRestored: Boolean): NavEvent? {
        if (isRestored) return null
        if (action != ACTION_SEND || type != TYPE_TEXT) return null
        if (text == null) return null
        val carId = EncarUrl.extractCarId(text) ?: return NavEvent.InvalidShare
        return NavEvent.OpenShared(EncarUrl.buildDetailUrl(carId))
    }
}
