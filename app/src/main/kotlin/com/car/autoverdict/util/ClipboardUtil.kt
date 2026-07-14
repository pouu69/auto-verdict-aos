package com.car.autoverdict.util

import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {
    /**
     * Returns a clean, canonical Encar detail URL if the clipboard holds one,
     * otherwise null. Share text often wraps the link in surrounding words
     * (e.g. "이 매물 어때? https://.../cars/detail/123?..."), so we extract the
     * car id and rebuild the URL rather than pasting the raw clipboard text.
     */
    fun getEncarUrl(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        return EncarUrl.extractCarId(text)?.let { EncarUrl.buildDetailUrl(it) }
    }
}
