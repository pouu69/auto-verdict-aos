package com.car.autoverdict.result

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.car.autoverdict.collector.Collector
import com.car.autoverdict.collector.CollectorWebView
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.db.CacheEntity
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.report.EvaluationReport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Room + [CollectorWebView] backed [ResultRepository]. DAO calls are main-safe suspend functions.
 * [mainDispatcher] must run on the main looper (collectors are main-thread only); tests swap it.
 */
class DefaultResultRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val collectorFactory: (Context) -> Collector = ::CollectorWebView,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : ResultRepository {

    private val mainHandler = Handler(Looper.getMainLooper())

    override suspend fun savedCar(carId: String): SavedCarEntity? =
        db.savedCarDao().getByCarId(carId)

    override suspend fun cachedRaw(carId: String): String? =
        db.cacheDao().getValid(carId, clock())?.rawInputJson?.takeIf { it.isNotEmpty() }

    override suspend fun collect(
        url: String,
        carId: String,
        onStage: (CollectStage) -> Unit,
    ): CollectOutcome = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val collector = collectorFactory(context)
            // Every caller of destroyOnce runs on the main thread; destroy exactly once.
            var destroyed = false
            fun destroyOnce() {
                if (destroyed) return
                destroyed = true
                collector.destroy()
            }
            cont.invokeOnCancellation { mainHandler.post(::destroyOnce) }
            try {
                collector.collect(url, carId, onStage = onStage) { result ->
                    val outcome = when (result) {
                        is CollectorWebView.Result.Success -> CollectOutcome.Success(result.json)
                        is CollectorWebView.Result.Error -> CollectOutcome.Failed(result.kind)
                        CollectorWebView.Result.Deleted -> CollectOutcome.Deleted
                    }
                    // The collector still touches its WebView after the callback returns
                    // (e.g. loading about:blank), so destroy on the next main-loop turn.
                    mainHandler.post(::destroyOnce)
                    if (cont.isActive) cont.resume(outcome)
                }
            } catch (e: Exception) {
                destroyOnce()
                throw e
            }
        }
    }

    override suspend fun writeCache(report: EvaluationReport, rawJson: String) {
        val now = clock()
        db.cacheDao().upsert(
            CacheEntity(
                carId = report.carId,
                url = report.url,
                title = report.car.title,
                score = report.score,
                verdict = report.verdict,
                resultJson = "",
                rawInputJson = rawJson,
                cachedAt = now,
                expiresAt = now + CACHE_TTL_MS,
            ),
        )
    }

    override suspend fun save(report: EvaluationReport, rawJson: String, savedAt: Long?): Long {
        val now = clock()
        val written = savedAt ?: now
        val counts = report.counts
        db.savedCarDao().upsert(
            SavedCarEntity(
                carId = report.carId,
                url = report.url,
                title = report.car.title.take(200),
                year = report.car.year,
                mileageKm = report.car.mileageKm,
                priceWon = report.car.priceWon,
                fuelType = report.car.fuel?.take(50),
                score = report.score.coerceIn(0, 100),
                verdict = report.verdict,
                // Saved list keeps one "danger" bucket: killer + hidden (spec 4).
                dangerCount = counts.danger + counts.hidden,
                cautionCount = counts.warning,
                passCount = counts.pass,
                unknownCount = counts.unknown,
                rawJson = rawJson,
                savedAt = written,
                updatedAt = now,
            ),
        )
        return written
    }

    override suspend fun delete(carId: String) {
        db.savedCarDao().deleteByCarId(carId)
    }

    private companion object {
        const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
