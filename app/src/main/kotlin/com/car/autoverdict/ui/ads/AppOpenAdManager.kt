package com.car.autoverdict.ui.ads

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.car.autoverdict.ui.AdIds
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.lang.ref.WeakReference
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Main-thread confined. Only an external share can arm an ad; onResume never shows one. */
class AppOpenAdManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("app_open_ads", Context.MODE_PRIVATE)
    private var ad: AppOpenAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var showing = false
    private var opportunity: AppOpenOpportunity? = null
    private var host = WeakReference<ComponentActivity>(null)
    private var resultHost = WeakReference<ComponentActivity>(null)
    private var resultUrl: String? = null
    private var suppressThisEntry = false
    private val _contentAdsAllowed = MutableStateFlow(true)
    val contentAdsAllowed: StateFlow<Boolean> = _contentAdsAllowed.asStateFlow()

    fun beginSharedEntry(
        activity: ComponentActivity,
        url: String,
        fromBackground: Boolean,
        onboardingComplete: Boolean,
    ) {
        opportunity = null
        host.clear()
        if (!fromBackground || !onboardingComplete || showing || !eligibleNow()) return
        // Re-sharing the visible result reuses its controller, so there will be no new source decision.
        if (resultHost.get() === activity && resultUrl == url) return
        host = WeakReference(activity)
        opportunity = AppOpenOpportunity(url, SystemClock.elapsedRealtime())
        // This also hides any outgoing tab's ad before the result route is composed.
        _contentAdsAllowed.value = false
    }

    fun onHostResumed() {
        if (!showing && opportunity == null && !suppressThisEntry) _contentAdsAllowed.value = true
    }

    fun onHostStopped() {
        opportunity = null
        host.clear()
        // A fullscreen ad can stop its host too; keep that entry suppressed on dismissal.
        if (!showing) suppressThisEntry = false
        _contentAdsAllowed.value = false
    }

    fun onResultEntered(activity: ComponentActivity, url: String) {
        resultHost = WeakReference(activity)
        resultUrl = url
    }

    fun onResultLeft(activity: ComponentActivity, url: String) {
        if (resultHost.get() === activity && resultUrl == url) {
            resultHost.clear()
            resultUrl = null
        }
        cancelEntry(activity, url)
    }

    fun cancelEntry(activity: ComponentActivity, url: String? = null) {
        if (host.get() !== activity) return
        if (url != null && opportunity?.matches(url) != true) return
        opportunity = null
        host.clear()
        if (!showing && !suppressThisEntry) _contentAdsAllowed.value = true
    }

    /** Called once only after storage/cache checks, while the result is still a loading screen. */
    fun onInitialLoadSource(activity: ComponentActivity, url: String, freshCollection: Boolean) {
        if (host.get() !== activity) return
        val entry = opportunity ?: return
        if (!entry.matches(url)) return
        opportunity = null
        host.clear()
        val canAttempt = entry.consume(url, SystemClock.elapsedRealtime()) && freshCollection &&
            eligibleNow() && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !activity.isFinishing && !activity.isDestroyed && !showing
        val ready = ad?.takeIf { AppOpenPolicy.isAdFresh(loadedAt, SystemClock.elapsedRealtime()) }
        if (!canAttempt || ready == null) {
            _contentAdsAllowed.value = !suppressThisEntry
            // Only preloads for a future share. Its callback never displays the ad.
            if (canAttempt) preload()
            return
        }
        ad = null
        showing = true
        suppressThisEntry = true
        _contentAdsAllowed.value = false
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                val today = LocalDate.now().toString()
                val count = if (prefs.getString("shown_day", null) == today) prefs.getInt("shown_count", 0) else 0
                prefs.edit().putLong("last_shown_at", System.currentTimeMillis())
                    .putString("shown_day", today).putInt("shown_count", count + 1).apply()
                Log.d(TAG, "shown")
            }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                // No content ads for the rest of this foreground visit, including the result.
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                suppressThisEntry = false
                _contentAdsAllowed.value = true
                Log.d(TAG, "show failed: ${error.code}")
                preload()
            }

            override fun onAdImpression() { Log.d(TAG, "impression") }
        }
        try {
            ready.show(activity)
        } catch (error: RuntimeException) {
            showing = false
            suppressThisEntry = false
            _contentAdsAllowed.value = true
            Log.w(TAG, "show failed", error)
            preload()
        }
    }

    fun onAnalysisCompleted() {
        prefs.edit().putBoolean("completed_analysis", true).apply()
    }

    fun preload() {
        val unitId = AdIds.appOpen ?: return
        if (loading || showing) return
        if (ad != null && AppOpenPolicy.isAdFresh(loadedAt, SystemClock.elapsedRealtime())) return
        ad = null
        loading = true
        AppOpenAd.load(appContext, unitId, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(loaded: AppOpenAd) {
                ad = loaded
                loadedAt = SystemClock.elapsedRealtime()
                loading = false
                Log.d(TAG, "loaded for a future entry")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loading = false
                Log.d(TAG, "load failed: ${error.code}")
            }
        })
    }

    private fun eligibleNow() = AdIds.appOpen != null && AppOpenPolicy.canShow(
        completedAnalysis = prefs.getBoolean("completed_analysis", false),
        lastShownAt = prefs.getLong("last_shown_at", 0),
        shownDay = prefs.getString("shown_day", null),
        shownCount = prefs.getInt("shown_count", 0),
        now = System.currentTimeMillis(),
        today = LocalDate.now().toString(),
    )

    private companion object { const val TAG = "AppOpenAd" }
}
