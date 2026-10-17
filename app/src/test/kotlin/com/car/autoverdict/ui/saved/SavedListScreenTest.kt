package com.car.autoverdict.ui.saved

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.db.AppDatabase
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.preferences.AppPreferences
import com.car.autoverdict.saved.SavedCarsRepository
import com.car.autoverdict.saved.SavedSort
import com.car.autoverdict.ui.components.AvToastHost
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.rememberAvToastState
import com.car.autoverdict.ui.screen.SavedListRoute
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class SavedListScreenTest {

    @get:Rule val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = SavedCarsRepository(db.savedCarDao(), scope)
    private val preferences = AppPreferences(context)
    private val compared = mutableListOf<List<String>>()

    @Before fun setUp() {
        preferences.savedSort = SavedSort.Recent
        runBlocking {
            db.savedCarDao().upsertAll(
                listOf(
                    row("100", "2015 BMW 5시리즈", score = 72, verdict = "CAUTION", savedAt = 4_000, caution = 2, pass = 8),
                    row("200", "2021 현대 아반떼", score = 86, verdict = "OK", savedAt = 3_000, danger = 1, pass = 9),
                    // Twin rows of one car (dummy-ad id + original id): shown once.
                    row("300", "2018 기아 K5", score = 60, verdict = "NEVER", savedAt = 2_000, danger = 2),
                    row("301", "2018 기아 K5", score = 60, verdict = "NEVER", savedAt = 1_000, danger = 2, vehicleId = "300"),
                    row("400", "2019 제네시스 G80", score = 90, verdict = "OK", savedAt = 500, pass = 10),
                ),
            )
        }
        rule.setContent {
            AvTheme {
                val toast = rememberAvToastState()
                CompositionLocalProvider(LocalAvToast provides toast) {
                    Box {
                        SavedListRoute(
                            repository = repository,
                            preferences = preferences,
                            onOpenResult = {},
                            onCompare = { compared += it },
                            onGoHome = {},
                        )
                        AvToastHost(toast, Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText("BMW 5시리즈").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Test fun `groups twin rows and uses the result screen's count labels`() {
        rule.onNodeWithText("4대").assertExists()
        rule.onAllNodesWithText("기아 K5").assertCountEquals(1)
        rule.onNodeWithText("주의 2").assertExists()
        rule.onNodeWithText("통과 9").assertExists()
        // dangerCount on a non-NEVER row is all hidden; two on a NEVER row cannot be split.
        rule.onNodeWithText("비공개 1").assertExists()
        rule.onNodeWithText("위험·비공개 2").assertExists()
        rule.onAllNodesWithText("양호", substring = true).assertCountEquals(0)
    }

    @Test fun `sort change reorders the list and is remembered`() {
        assertOrder("BMW 5시리즈", "현대 아반떼", "기아 K5", "제네시스 G80")

        rule.onNodeWithContentDescription("정렬, 최근 저장순").performClick()
        rule.onNodeWithText("점수 높은 순").performClick()
        rule.waitForIdle()

        assertOrder("제네시스 G80", "현대 아반떼", "BMW 5시리즈", "기아 K5")
        assertEquals(SavedSort.Score, preferences.savedSort)
    }

    @Test fun `selection mode caps at three and compares in on-screen order`() {
        rule.onNodeWithText("비교").performClick()
        rule.onNodeWithText("비교할 차 선택").assertExists()
        rule.onNodeWithText("2대 이상 골라 주세요").assertIsNotEnabled()

        rule.onNodeWithText("기아 K5").performClick()
        rule.onNodeWithText("BMW 5시리즈").performClick()
        rule.onNodeWithText("2대 선택").assertExists()
        rule.onNodeWithText("2대 비교하기").assertIsEnabled()

        rule.onNodeWithText("현대 아반떼").performClick()
        rule.onNodeWithText("제네시스 G80").performClick()
        rule.onNodeWithText("최대 3대까지 비교할 수 있어요").assertExists()

        rule.onNodeWithText("3대 비교하기").performClick()
        assertEquals(listOf(listOf("100", "200", "300")), compared)
        rule.onNodeWithText("저장한 차").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `delete then undo restores every grouped row`() {
        rule.onNodeWithText("기아 K5").performCustomAccessibilityActionWithLabel("삭제")
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText("기아 K5").fetchSemanticsNodes().isEmpty() }
        assertEquals(0, runBlocking { db.savedCarDao().getByCarIds(listOf("300", "301")) }.size)

        rule.onNodeWithText("삭제했어요").assertExists()
        rule.onNodeWithText("되돌리기").performClick()
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText("기아 K5").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, runBlocking { db.savedCarDao().getByCarIds(listOf("300", "301")) }.size)
        assertOrder("BMW 5시리즈", "현대 아반떼", "기아 K5", "제네시스 G80")
    }

    private fun assertOrder(vararg names: String) {
        val tops = names.map { rule.onNodeWithText(it).getUnclippedBoundsInRoot().top }
        assertEquals(names.toList(), names.zip(tops).sortedBy { it.second }.map { it.first })
    }

    private fun row(
        carId: String,
        title: String,
        score: Int,
        verdict: String,
        savedAt: Long,
        danger: Int = 0,
        caution: Int = 0,
        pass: Int = 0,
        vehicleId: String = carId,
    ) = SavedCarEntity(
        carId = carId,
        url = "https://fem.encar.com/cars/detail/$carId",
        title = title,
        year = title.take(4).toInt(),
        mileageKm = 98_000,
        priceWon = 12_900_000,
        fuelType = "디젤",
        score = score,
        verdict = verdict,
        dangerCount = danger,
        cautionCount = caution,
        passCount = pass,
        unknownCount = 0,
        rawJson = """{"preloadedState":{"cars":{"base":{"vehicleId":$vehicleId}}}}""",
        savedAt = savedAt,
        updatedAt = savedAt,
    )

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
