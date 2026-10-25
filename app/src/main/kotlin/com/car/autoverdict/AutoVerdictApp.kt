package com.car.autoverdict

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.engine.RuleEngine
import com.car.autoverdict.engine.WebViewEngineHost
import com.car.autoverdict.licenses.OssNoticeRepository
import com.car.autoverdict.result.DefaultResultRepository
import com.car.autoverdict.saved.SavedCarsRepository
import com.car.autoverdict.ui.ads.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoVerdictApp : Application() {

    val appOpenAds: AppOpenAdManager by lazy { AppOpenAdManager(this) }

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    /** One headless engine for the whole app; it creates and recycles its WebView itself (spec 3.3). */
    val ruleEngine: RuleEngine by lazy { RuleEngine({ WebViewEngineHost(this) }) }

    val resultRepository: DefaultResultRepository by lazy { DefaultResultRepository(this, database) }

    /** App-scoped so the grouped list is shared across tabs and an undo outlives the screen (spec 3.5, 4.6). */
    val savedCarsRepository: SavedCarsRepository by lazy {
        SavedCarsRepository(database.savedCarDao(), CoroutineScope(SupervisorJob()))
    }

    /** Parsed once and kept: the list and detail screens share it across navigation and rotation. */
    val ossNoticeRepository: OssNoticeRepository by lazy { OssNoticeRepository(resources) }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {
            Handler(Looper.getMainLooper()).post { appOpenAds.preload() }
        }
        purgeExpiredCache()
    }

    private fun purgeExpiredCache() {
        CoroutineScope(Dispatchers.IO).launch {
            database.cacheDao().purgeExpired()
        }
    }
}
