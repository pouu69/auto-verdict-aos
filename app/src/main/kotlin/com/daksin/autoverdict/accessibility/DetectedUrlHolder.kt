package com.daksin.autoverdict.accessibility

object DetectedUrlHolder {
    @Volatile
    private var lastUrl: String? = null

    @Volatile
    private var lastDetectedAt: Long = 0

    fun setUrl(url: String) {
        lastUrl = url
        lastDetectedAt = System.currentTimeMillis()
    }

    // URL is valid for 60 seconds after detection
    fun getUrl(): String? {
        val url = lastUrl ?: return null
        if (System.currentTimeMillis() - lastDetectedAt > EXPIRY_MS) {
            clear()
            return null
        }
        return url
    }

    fun clear() {
        lastUrl = null
        lastDetectedAt = 0
    }

    private const val EXPIRY_MS = 60_000L
}
