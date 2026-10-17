package com.car.autoverdict.ui.navigation

import android.content.Intent
import com.car.autoverdict.util.EncarUrl

/**
 * Turns a share intent into a navigation event. Pure so it can be unit tested. A share is
 * handled only when it is new: the activity passes `isRestored = savedInstanceState != null`
 * so rotation or process restore never replays the launch intent over the restored back
 * stack (spec 7.2), and a relaunch from Recents carries FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
 * with the share that originally started the task.
 */
object ShareIntentParser {

    private const val ACTION_SEND = "android.intent.action.SEND"
    private const val TYPE_TEXT = "text/plain"

    fun parse(action: String?, type: String?, text: String?, flags: Int, isRestored: Boolean): NavEvent? {
        if (isRestored) return null
        if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        if (action != ACTION_SEND || type != TYPE_TEXT) return null
        if (text == null) return null
        val carId = EncarUrl.extractCarId(text) ?: return NavEvent.InvalidShare
        return NavEvent.OpenShared(EncarUrl.buildDetailUrl(carId))
    }
}
