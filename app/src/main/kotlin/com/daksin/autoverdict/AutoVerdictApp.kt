package com.daksin.autoverdict

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.daksin.autoverdict.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoVerdictApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        purgeExpiredCache()
    }

    private fun purgeExpiredCache() {
        CoroutineScope(Dispatchers.IO).launch {
            database.cacheDao().purgeExpired()
        }
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
