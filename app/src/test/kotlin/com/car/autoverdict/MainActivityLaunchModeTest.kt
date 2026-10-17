package com.car.autoverdict

import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Repeat shares must reach the existing MainActivity through onNewIntent. Only singleTask
 * guarantees that: with standard/singleTop a share whose intent matches the task's root
 * intent just brings the task to front, and a share sheet that launches into the sender's
 * task gets a second instance there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MainActivityLaunchModeTest {

    @Test fun `main activity is single task`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val info = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, info.launchMode)
    }
}
