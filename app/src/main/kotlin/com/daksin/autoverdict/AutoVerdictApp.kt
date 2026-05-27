package com.daksin.autoverdict

import android.app.Application
import com.daksin.autoverdict.db.AppDatabase
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoVerdictApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        purgeExpiredCache()
    }

    private fun purgeExpiredCache() {
        CoroutineScope(Dispatchers.IO).launch {
            database.cacheDao().purgeExpired()
        }
    }
}
