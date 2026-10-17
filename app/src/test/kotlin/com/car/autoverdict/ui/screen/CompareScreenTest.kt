package com.car.autoverdict.ui.screen

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.compare.CompareInput
import com.car.autoverdict.compare.CompareUiState
import com.car.autoverdict.compare.buildCompareTable
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.result.sampleReport
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h4000dp")
class CompareScreenTest {

    @get:Rule val rule = createComposeRule()

    private fun row(carId: String, title: String) = SavedCarEntity(
        carId = carId, url = "https://fem.encar.com/cars/detail/$carId", title = title,
        year = 2021, mileageKm = 40_000, priceWon = 18_500_000, fuelType = "가솔린",
        score = 86, verdict = "OK", dangerCount = 0, cautionCount = 0,
        passCount = 9, unknownCount = 1, rawJson = "{}", savedAt = 0, updatedAt = 0,
    )

    @Test
    fun `renders a header per car and opens the tapped car's result`() {
        val report = sampleReport("sample-caution.json")
        val table = buildCompareTable(
            listOf(
                CompareInput(row(report.carId, report.car.title), report),
                // Engine failure: saved-summary column.
                CompareInput(row("2", "2021 현대 아반떼"), null),
            ),
        )
        val opened = mutableListOf<String>()
        rule.setContent {
            AvTheme {
                CompareScreen(state = CompareUiState.Ready(table), onBack = {}, onOpenResult = { opened += it })
            }
        }

        rule.onNodeWithText("BMW 3시리즈 (F30) 320d").assertIsDisplayed()
        rule.onNodeWithText("현대 아반떼").assertIsDisplayed()
        rule.onNodeWithText("저장 당시 요약이에요").assertIsDisplayed()

        rule.onNodeWithContentDescription("현대 아반떼, 판정 괜찮아요", substring = true).performClick()
        assertEquals(listOf("https://fem.encar.com/cars/detail/2"), opened)
    }
}
