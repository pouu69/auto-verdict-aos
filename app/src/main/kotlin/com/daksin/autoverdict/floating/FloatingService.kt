package com.daksin.autoverdict.floating

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.daksin.autoverdict.AutoVerdictApp
import com.daksin.autoverdict.R
import com.daksin.autoverdict.util.EncarUrl

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingButton: View? = null
    private var overlayManager: OverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        showFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val urlFromIntent = intent?.getStringExtra(EXTRA_URL)
        if (urlFromIntent != null) {
            startAnalysis(urlFromIntent)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeFloatingButton()
        overlayManager?.destroy()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, AutoVerdictApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_notification_title))
            .setContentText(getString(R.string.floating_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showFloatingButton() {
        val size = dpToPx(52)
        val iconPadding = dpToPx(14)
        val button = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_av_icon)
            setBackgroundResource(R.drawable.ic_av_button)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            elevation = dpToPx(8).toFloat()
        }

        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(200)
        }

        setupDragAndTap(button, params)
        windowManager.addView(button, params)
        floatingButton = button
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > DRAG_THRESHOLD_SQ) {
                        isDragging = true
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "ACTION_UP isDragging=$isDragging")
                    if (!isDragging) onFloatingButtonTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun onFloatingButtonTap() {
        Log.d(TAG, "onFloatingButtonTap called")
        val intent = Intent(this, com.daksin.autoverdict.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    fun startAnalysis(url: String) {
        val carId = EncarUrl.extractCarId(url) ?: return
        if (overlayManager == null) {
            val app = application as AutoVerdictApp
            overlayManager = OverlayManager(this, windowManager, app.database)
        }
        overlayManager?.show(url, carId)
    }

    private fun removeFloatingButton() {
        floatingButton?.let { windowManager.removeView(it); floatingButton = null }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "FloatingService"
        private const val NOTIFICATION_ID = 1
        private const val DRAG_THRESHOLD_SQ = 400
        const val EXTRA_URL = "extra_url"
    }
}
