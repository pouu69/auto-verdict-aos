package com.car.autoverdict.result

import android.app.Application
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.car.autoverdict.MainActivity
import com.car.autoverdict.db.SavedCarEntity
import com.car.autoverdict.engine.EngineResult
import com.car.autoverdict.engine.ReportEvaluator
import com.car.autoverdict.report.DataGap
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.report.GapReason
import com.car.autoverdict.report.GapSource
import com.car.autoverdict.ui.components.AvToastHost
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.rememberAvToastState
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.navigation.ScreenHost
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException

/** ResultRoute against a fake repository: controller events become toasts, actions become intents. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h4000dp")
class ResultRouteTest {

    @get:Rule val rule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val caution = sampleReport("sample-caution.json")
    private val repo = FakeRepository()
    private val calls = mutableListOf<String>()
    private val host = ScreenHost(
        navigate = { calls += "navigate" },
        back = { calls += "back" },
        finishOnboarding = {},
        lastAnalyzedUrl = null,
        onAnalyzed = {},
        selectTab = { calls += "tab:$it" },
    )

    /** Cached [report] is shown right away (no collector), like a second visit within 24h. */
    private fun show(
        report: EvaluationReport = caution,
        url: String = URL,
        lifecycleOwner: LifecycleOwner? = null,
    ) {
        repo.cache = RAW
        val evaluator = ReportEvaluator { raw ->
            if (raw == RAW) EngineResult.Success(report) else EngineResult.Failed("unknown raw")
        }
        rule.setContent {
            AvTheme {
                val toast = rememberAvToastState()
                CompositionLocalProvider(LocalAvToast provides toast) {
                    Box {
                        val owner = lifecycleOwner ?: LocalLifecycleOwner.current
                        CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                            ResultRoute(
                                url = url,
                                host = host,
                                repository = repo,
                                evaluator = evaluator,
                                bannerAd = { Text("banner-ad") },
                                loadingAd = { Text("loading-ad") },
                            )
                        }
                        AvToastHost(state = toast, modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
        waitForText(if (repo.saved != null) "저장됨" else "저장하기")
    }

    private fun waitForText(text: String) = rule.waitUntil(timeoutMillis = 3_000) {
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun `saving toasts with an action that opens the saved tab`() {
        show()
        rule.onNodeWithText("저장하기").performClick()
        waitForText("저장했어요")
        assertEquals(1, repo.saves)

        rule.onNodeWithText("보기").performClick()
        rule.waitUntil(timeoutMillis = 3_000) { calls.isNotEmpty() }
        assertEquals(listOf("tab:${MainTab.Saved}"), calls)
    }

    @Test fun `unsaving toasts after the confirm dialog`() {
        repo.saved = savedEntity()
        show()
        rule.onNodeWithText("저장됨").performClick()
        rule.onNodeWithText("취소하기").performClick()
        waitForText("저장을 취소했어요")
        assertEquals(1, repo.deletes)
    }

    @Test fun `a failed save toasts`() {
        repo.failWrites = true
        show()
        rule.onNodeWithText("저장하기").performClick()
        waitForText("저장하지 못했어요")
    }

    @Test fun `in-place retry shows progress and toasts when it fails`() {
        show(caution.copy(dataGaps = listOf(DataGap(GapSource.Inspection, GapReason.Failed))))
        rule.onNodeWithText("다시 시도").performClick()
        waitForText("다시 가져오는 중")
        rule.onNodeWithText("다시 시도").assertDoesNotExist()

        rule.runOnIdle { repo.collects.single().complete(CollectOutcome.Failed(CollectErrorKind.Network)) }
        waitForText("다시 가져오지 못했어요")
        // The report stays, with its retry back.
        rule.onNodeWithText("다시 시도").assertExists()
        rule.onNodeWithText("다시 가져오는 중").assertDoesNotExist()
    }

    @Test fun `share opens a chooser with the result summary`() {
        show()
        rule.onNodeWithContentDescription("결과 공유").performClick()
        assertSharedText(caution.shareText.summary)
    }

    @Test fun `seller questions can be shared`() {
        show()
        rule.onNodeWithText("공유").performScrollTo().performClick()
        assertSharedText(requireNotNull(caution.shareText.sellerQuestions))
    }

    @Config(sdk = [32])
    @Test fun `copying seller questions toasts up to API 32`() {
        show()
        rule.onNodeWithText("복사").performScrollTo().performClick()
        waitForText("복사했어요")
        assertClipboardHoldsQuestions()
    }

    @Test fun `copying seller questions leaves the confirmation to the system on API 33+`() {
        show()
        rule.onNodeWithText("복사").performScrollTo().performClick()
        rule.waitForIdle()
        assertClipboardHoldsQuestions()
        rule.onNodeWithText("복사했어요").assertDoesNotExist()
    }

    private fun assertClipboardHoldsQuestions() {
        val clip = requireNotNull(app.getSystemService(ClipboardManager::class.java).primaryClip)
        assertEquals(caution.shareText.sellerQuestions, clip.getItemAt(0).text.toString())
    }

    @Test fun `open in Encar views the canonical listing url`() {
        show(url = "$URL?pageid=dc_carsearch&listAdvType=pic&carid=$CAR_ID&view_type=normal")
        rule.onNodeWithText("엔카에서 보기").performClick()
        val intent = requireNotNull(shadowOf(app).nextStartedActivity) { "nothing started" }
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(URL, intent.dataString)
    }

    @Test fun `an event sent while stopped toasts once the screen is started again`() {
        val owner = ManualLifecycleOwner()
        show(lifecycleOwner = owner)
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        rule.onNodeWithText("저장하기").performClick()
        rule.waitUntil(timeoutMillis = 3_000) { repo.saves == 1 }
        rule.waitForIdle()
        rule.onNodeWithText("저장했어요").assertDoesNotExist()

        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        waitForText("저장했어요")
    }

    /** Lifecycle the test moves by hand (starts RESUMED, like a visible screen). */
    private class ManualLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    private fun assertSharedText(expected: String) {
        val chooser = requireNotNull(shadowOf(app).nextStartedActivity) { "nothing started" }
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals(expected, send.getStringExtra(Intent.EXTRA_TEXT))
        // The app itself accepts shared text; it must not offer itself as a target.
        @Suppress("DEPRECATION")
        val excluded = chooser.getParcelableArrayExtra(Intent.EXTRA_EXCLUDE_COMPONENTS)?.toList().orEmpty()
        assertEquals(listOf(ComponentName(app, MainActivity::class.java)), excluded)
    }

    private class FakeRepository : ResultRepository {
        var saved: SavedCarEntity? = null
        var cache: String? = null
        var failWrites = false
        var saves = 0
        var deletes = 0
        val collects = mutableListOf<CompletableDeferred<CollectOutcome>>()

        override suspend fun savedCar(carId: String) = saved
        override suspend fun cachedRaw(carId: String) = cache

        override suspend fun collect(url: String, carId: String, onStage: (CollectStage) -> Unit): CollectOutcome {
            val result = CompletableDeferred<CollectOutcome>()
            collects += result
            return result.await()
        }

        override suspend fun writeCache(report: EvaluationReport, rawJson: String) = Unit

        override suspend fun save(report: EvaluationReport, rawJson: String, savedAt: Long?): Long {
            if (failWrites) throw IOException("disk full")
            saves++
            saved = savedEntity()
            return savedAt ?: 1L
        }

        override suspend fun delete(carId: String) {
            if (failWrites) throw IOException("disk full")
            deletes++
            saved = null
        }
    }

    private companion object {
        const val CAR_ID = "41878112"
        const val URL = "https://fem.encar.com/cars/detail/$CAR_ID"
        const val RAW = "{\"raw\":true}"

        fun savedEntity() = SavedCarEntity(
            carId = CAR_ID,
            url = URL,
            title = "2015 BMW 3시리즈 (F30)",
            year = 2015,
            mileageKm = 119_529,
            priceWon = 8_900_000L,
            fuelType = "디젤",
            score = 89,
            verdict = "CAUTION",
            dangerCount = 0,
            cautionCount = 2,
            passCount = 9,
            unknownCount = 0,
            rawJson = RAW,
            savedAt = 1L,
            updatedAt = 1L,
        )
    }
}
