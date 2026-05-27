package com.daksin.autoverdict.webview

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.daksin.autoverdict.db.AppDatabase
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeBridgeTest {

    private lateinit var db: AppDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var bridge: NativeBridge
    private var closeCalled = false

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        closeCalled = false

        bridge = NativeBridge(
            database = db,
            scope = scope,
            onClose = { closeCalled = true },
            appContext = ApplicationProvider.getApplicationContext(),
        )
    }

    @After
    fun teardown() {
        scope.cancel()
        db.close()
    }

    @Test
    fun `saveCar with valid JSON saves entity`() {
        bridge.saveCar(validCarJson())
        waitForIo()

        val saved = runBlocking { db.savedCarDao().getByCarId("41623743") }
        assertNotNull(saved)
        assertEquals("41623743", saved?.carId)
        assertEquals(85, saved?.score)
        assertEquals("OK", saved?.verdict)
    }

    @Test
    fun `saveCar stores nullable fields correctly`() {
        bridge.saveCar(validCarJson())
        waitForIo()

        val saved = runBlocking { db.savedCarDao().getByCarId("41623743") }
        assertEquals(2022, saved?.year)
        assertEquals(35000, saved?.mileageKm)
        assertEquals(25000000L, saved?.priceWon)
        assertEquals("가솔린", saved?.fuelType)
    }

    @Test
    fun `saveCar with null optional fields saves nulls`() {
        val json = """
        {
            "carId": "123",
            "url": "https://fem.encar.com/cars/detail/123",
            "title": "Test",
            "score": 70,
            "verdict": "CAUTION",
            "dangerCount": 0,
            "cautionCount": 2,
            "passCount": 6,
            "unknownCount": 1,
            "rawJson": "{}"
        }
        """.trimIndent()

        bridge.saveCar(json)
        waitForIo()

        val saved = runBlocking { db.savedCarDao().getByCarId("123") }
        assertNotNull(saved)
        assertNull(saved?.year)
        assertNull(saved?.mileageKm)
        assertNull(saved?.priceWon)
        assertNull(saved?.fuelType)
    }

    @Test
    fun `saveCar with empty carId does not save`() {
        bridge.saveCar(validCarJson().replace("41623743", ""))
        waitForIo()

        assertEquals(0, runBlocking { db.savedCarDao().count() })
    }

    @Test
    fun `saveCar with non-numeric carId does not save`() {
        bridge.saveCar(validCarJson().replace("41623743", "abc-not-valid"))
        waitForIo()

        assertEquals(0, runBlocking { db.savedCarDao().count() })
    }

    @Test
    fun `saveCar with malformed JSON does not crash`() {
        bridge.saveCar("not json at all")
        waitForIo()

        assertEquals(0, runBlocking { db.savedCarDao().count() })
    }

    @Test
    fun `saveCar with empty JSON object does not crash`() {
        bridge.saveCar("{}")
        waitForIo()

        assertEquals(0, runBlocking { db.savedCarDao().count() })
    }

    @Test
    fun `saveCar clamps score over 100 to 100`() {
        bridge.saveCar(validCarJson(score = 150))
        waitForIo()

        val saved = runBlocking { db.savedCarDao().getByCarId("41623743") }
        assertEquals(100, saved?.score)
    }

    @Test
    fun `saveCar clamps negative score to 0`() {
        bridge.saveCar(validCarJson(score = -10))
        waitForIo()

        val saved = runBlocking { db.savedCarDao().getByCarId("41623743") }
        assertEquals(0, saved?.score)
    }

    @Test
    fun `saveCar truncates long title to 200 chars`() {
        val longTitle = "A".repeat(300)
        bridge.saveCar(validCarJson(title = longTitle))
        waitForIo()

        val saved = runBlocking { db.savedCarDao().getByCarId("41623743") }
        assertEquals(200, saved?.title?.length)
    }

    @Test
    fun `saveCar clamps negative dangerCount to 0`() {
        bridge.saveCar(validCarJson(dangerCount = -5))
        waitForIo()

        val saved = runBlocking { db.savedCarDao().getByCarId("41623743") }
        assertEquals(0, saved?.dangerCount)
    }

    @Test
    fun `closeOverlay invokes onClose callback`() {
        bridge.closeOverlay()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(true, closeCalled)
    }

    private fun waitForIo() {
        Thread.sleep(100)
    }

    private fun validCarJson(
        score: Int = 85,
        title: String = "2022 현대 투싼",
        dangerCount: Int = 0,
    ): String = """
    {
        "carId": "41623743",
        "url": "https://fem.encar.com/cars/detail/41623743",
        "title": "$title",
        "year": 2022,
        "mileageKm": 35000,
        "priceWon": 25000000,
        "fuelType": "가솔린",
        "score": $score,
        "verdict": "OK",
        "dangerCount": $dangerCount,
        "cautionCount": 1,
        "passCount": 8,
        "unknownCount": 0,
        "rawJson": "{}"
    }
    """.trimIndent()
}
