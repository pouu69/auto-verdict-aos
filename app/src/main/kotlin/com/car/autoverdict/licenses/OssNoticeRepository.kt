package com.car.autoverdict.licenses

import android.content.res.Resources
import androidx.annotation.RawRes
import com.car.autoverdict.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads the generated notices once per process (spec 7.3). The raw files are referenced through
 * R.raw on purpose: shrinkResources keeps statically referenced resources but may strip ones
 * looked up by name.
 */
class OssNoticeRepository(
    private val resources: Resources,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private var cached: OssCatalog? = null

    suspend fun load(): OssCatalog = mutex.withLock {
        cached ?: withContext(dispatcher) {
            OssCatalog.parse(
                noticesJson = readRaw(R.raw.oss_notices),
                googleJson = readRaw(R.raw.oss_google_notices),
            )
        }.also { cached = it }
    }

    private fun readRaw(@RawRes id: Int): String =
        resources.openRawResource(id).bufferedReader().use { it.readText() }
}
