package com.car.autoverdict.webview

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.util.EncarUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class NativeBridge(
    private val database: AppDatabase,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
    private val appContext: Context,
) {
    @JavascriptInterface
    fun saveCar(json: String) {
        if (!scope.isActive) return
        scope.launch(Dispatchers.IO) {
            try {
                val obj = JSONObject(json)
                val carId = obj.optString("carId", "")
                if (carId.isBlank() || EncarUrl.extractCarId("https://fem.encar.com/cars/detail/$carId") == null) return@launch
                val now = System.currentTimeMillis()
                database.savedCarDao().upsert(
                    SavedCarEntity(
                        carId = carId,
                        url = obj.optString("url", ""),
                        title = obj.optString("title", "").take(200),
                        year = obj.optInt("year").takeIf { obj.has("year") && !obj.isNull("year") },
                        mileageKm = obj.optInt("mileageKm").takeIf { obj.has("mileageKm") && !obj.isNull("mileageKm") },
                        priceWon = obj.optLong("priceWon").takeIf { obj.has("priceWon") && !obj.isNull("priceWon") },
                        fuelType = if (obj.has("fuelType") && !obj.isNull("fuelType")) obj.getString("fuelType").take(50) else null,
                        score = obj.optInt("score", 0).coerceIn(0, 100),
                        verdict = obj.optString("verdict", "UNKNOWN").take(20),
                        dangerCount = obj.optInt("dangerCount", 0).coerceAtLeast(0),
                        cautionCount = obj.optInt("cautionCount", 0).coerceAtLeast(0),
                        passCount = obj.optInt("passCount", 0).coerceAtLeast(0),
                        unknownCount = obj.optInt("unknownCount", 0).coerceAtLeast(0),
                        rawJson = obj.optString("rawJson", json),
                        savedAt = now,
                        updatedAt = now,
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, "saveCar failed", e)
            }
        }
    }

    @JavascriptInterface
    fun closeOverlay() {
        if (!scope.isActive) return
        scope.launch(Dispatchers.Main) { onClose() }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        if (!scope.isActive) return
        scope.launch(Dispatchers.Main) {
            Toast.makeText(appContext, message.take(100), Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getClipboardUrl(): String? = null

    companion object {
        const val BRIDGE_NAME = "Android"
        private const val TAG = "NativeBridge"
    }
}
