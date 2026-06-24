package com.car.autoverdict.util

import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {
    fun getEncarUrl(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        return if (EncarUrl.isEncarDetail(text)) text else null
    }
}
