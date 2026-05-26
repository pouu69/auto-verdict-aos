package com.daksin.autoverdict.webview

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.daksin.autoverdict.db.AppDatabase
import com.daksin.autoverdict.db.SavedCarEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class NativeBridge(
    private val database: AppDatabase,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
    private val toastContext: Context,
) {
    @JavascriptInterface
    fun saveCar(json: String) {
        scope.launch(Dispatchers.IO) {
            val obj = JSONObject(json)
            val now = System.currentTimeMillis()
            database.savedCarDao().upsert(
                SavedCarEntity(
                    carId = obj.getString("carId"),
                    url = obj.getString("url"),
                    title = obj.optString("title", ""),
                    year = if (obj.has("year") && !obj.isNull("year")) obj.getInt("year") else null,
                    mileageKm = if (obj.has("mileageKm") && !obj.isNull("mileageKm")) obj.getInt("mileageKm") else null,
                    priceWon = if (obj.has("priceWon") && !obj.isNull("priceWon")) obj.getLong("priceWon") else null,
                    fuelType = if (obj.has("fuelType") && !obj.isNull("fuelType")) obj.getString("fuelType") else null,
                    score = obj.getInt("score"),
                    verdict = obj.getString("verdict"),
                    dangerCount = obj.getInt("dangerCount"),
                    cautionCount = obj.getInt("cautionCount"),
                    passCount = obj.getInt("passCount"),
                    unknownCount = obj.getInt("unknownCount"),
                    rawJson = obj.optString("rawJson", json),
                    savedAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    @JavascriptInterface
    fun closeOverlay() {
        scope.launch(Dispatchers.Main) { onClose() }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(toastContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getClipboardUrl(): String? = null

    companion object {
        const val BRIDGE_NAME = "Android"
    }
}
