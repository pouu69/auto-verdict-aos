package com.daksin.autoverdict.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.daksin.autoverdict.util.EncarUrl

@Suppress("DEPRECATION") // AccessibilityNodeInfo.recycle() deprecated but needed for older APIs
class AutoVerdictAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 300
            packageNames = MONITORED_PACKAGES
        }
        serviceInfo = info
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        when {
            packageName in BROWSER_PACKAGES -> extractBrowserUrl(event)
            packageName == ENCAR_PACKAGE -> extractEncarAppUrl(event)
        }
    }

    private fun extractBrowserUrl(@Suppress("UNUSED_PARAMETER") event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val urlBarNode = findUrlBar(rootNode) ?: return
            val url = urlBarNode.text?.toString() ?: return
            // Browser URL bars sometimes show without scheme
            val fullUrl = if (!url.startsWith("http")) "https://$url" else url
            if (EncarUrl.isEncarDetail(fullUrl)) {
                DetectedUrlHolder.setUrl(fullUrl)
                Log.d(TAG, "Detected encar URL from browser: $fullUrl")
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun extractEncarAppUrl(@Suppress("UNUSED_PARAMETER") event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        try {
            findEncarCarId(rootNode)?.let { carId ->
                val url = EncarUrl.buildDetailUrl(carId)
                DetectedUrlHolder.setUrl(url)
                Log.d(TAG, "Detected encar URL from app: $url")
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun findUrlBar(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Common browser URL bar view IDs
        val urlBarIds = listOf(
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/search_box_text",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "org.mozilla.firefox:id/url_bar_title",
            "com.brave.browser:id/url_bar",
        )
        for (id in urlBarIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNullOrEmpty()) continue
            return nodes[0]
        }
        // Fallback: try to find by EditText with URL-like content
        return findEditTextWithUrl(node)
    }

    private fun findEditTextWithUrl(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText" && node.text != null) {
            val text = node.text.toString()
            if (text.contains("encar.com")) return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditTextWithUrl(child)
            if (result != null) return result
            child.recycle()
        }
        return null
    }

    private fun findEncarCarId(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString()
        if (text != null) {
            EncarUrl.extractCarId(text)?.let { return it }
        }
        node.contentDescription?.toString()?.let { desc ->
            EncarUrl.extractCarId(desc)?.let { return it }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEncarCarId(child)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        DetectedUrlHolder.clear()
        Log.d(TAG, "Accessibility service destroyed")
    }

    companion object {
        private const val TAG = "A11yService"
        private const val ENCAR_PACKAGE = "com.encar.encar"
        private val BROWSER_PACKAGES = arrayOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.opera.browser",
            "com.microsoft.emmx",
        )
        private val MONITORED_PACKAGES = BROWSER_PACKAGES + ENCAR_PACKAGE
    }
}
