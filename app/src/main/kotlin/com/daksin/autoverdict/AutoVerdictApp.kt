package com.daksin.autoverdict

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.daksin.autoverdict.db.AppDatabase

class AutoVerdictApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AutoVerdict 서비스",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "플로팅 버튼 서비스 실행 중"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "autoverdict_floating"
    }
}
