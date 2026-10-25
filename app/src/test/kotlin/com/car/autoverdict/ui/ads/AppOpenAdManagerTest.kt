package com.car.autoverdict.ui.ads

import android.app.Application
import androidx.activity.ComponentActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class AppOpenAdManagerTest {
    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    private val activity = activityController.get()
    private val manager = AppOpenAdManager(activity)
    private val carA = "https://fem.encar.com/cars/detail/38000000"
    private val carB = "https://fem.encar.com/cars/detail/39000000"

    private fun stop() {
        activityController.pause().stop()
        manager.onHostStopped()
    }

    private fun resume() {
        activityController.restart().start().resume()
        manager.onHostResumed()
    }

    private fun share(url: String = carA, fromBackground: Boolean = true) {
        manager.beginSharedEntry(activity, url, fromBackground, onboardingComplete = true)
    }

    @Test fun `same result shared again restores content ads without a new controller callback`() {
        manager.onAnalysisCompleted()
        manager.onResultEntered(activity, carA)
        stop()
        share()
        resume()
        // A reused ResultRoute does not run its initial source decision again.
        assertTrue(manager.contentAdsAllowed.value)
    }

    @Test fun `different shared result stays suppressed until its cache decision`() {
        manager.onAnalysisCompleted()
        manager.onResultEntered(activity, carA)
        stop()
        share(carB)
        resume()
        assertFalse(manager.contentAdsAllowed.value)
        manager.onInitialLoadSource(activity, carB, freshCollection = false)
        assertTrue(manager.contentAdsAllowed.value)
    }

    @Test fun `disposing outgoing result does not cancel a different shared result`() {
        manager.onAnalysisCompleted()
        manager.onResultEntered(activity, carA)
        stop()
        share(carB)
        manager.onResultLeft(activity, carA)
        resume()
        assertFalse(manager.contentAdsAllowed.value)
        manager.onResultEntered(activity, carB)
        manager.onInitialLoadSource(activity, carB, freshCollection = false)
        assertTrue(manager.contentAdsAllowed.value)
    }

    @Test fun `stopping cancels pending opportunity and later source callback cannot rearm it`() {
        manager.onAnalysisCompleted()
        share()
        assertFalse(manager.contentAdsAllowed.value)
        stop()
        resume()
        manager.onInitialLoadSource(activity, carA, freshCollection = true)
        assertTrue(manager.contentAdsAllowed.value)
    }

    @Test fun `first analysis never creates an app open opportunity`() {
        share()
        assertTrue(manager.contentAdsAllowed.value)
    }

    @Test fun `share received in the foreground never suppresses content ads`() {
        manager.onAnalysisCompleted()
        share(fromBackground = false)
        assertTrue(manager.contentAdsAllowed.value)
    }

    @Test fun `leaving the result cancels its pending opportunity`() {
        manager.onAnalysisCompleted()
        share()
        manager.onResultEntered(activity, carA)
        manager.onResultLeft(activity, carA)
        assertTrue(manager.contentAdsAllowed.value)
    }
}
