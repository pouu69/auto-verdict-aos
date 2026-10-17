package com.car.autoverdict.util

import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {
    /**
     * Returns the primary clip as text, or null when the clipboard is empty or unreadable.
     * Call only from a user action (the paste button): Android 10+ returns null to apps
     * without window focus, and Android 12+ shows a system toast on every read.
     */
    fun readText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip?.takeIf { it.itemCount > 0 } ?: return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }
}
