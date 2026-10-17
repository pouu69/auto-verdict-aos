package com.car.autoverdict

import android.app.Application
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.engine.RuleEngine
import com.car.autoverdict.engine.WebViewEngineHost
import com.car.autoverdict.result.DefaultResultRepository
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoVerdictApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    /** One headless engine for the whole app; it creates and recycles its WebView itself (spec 3.3). */
    val ruleEngine: RuleEngine by lazy { RuleEngine({ WebViewEngineHost(this) }) }

    val resultRepository: DefaultResultRepository by lazy { DefaultResultRepository(this, database) }

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
