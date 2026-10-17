package com.car.autoverdict.result

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.car.autoverdict.R
import com.car.autoverdict.engine.ReportEvaluator
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.navigation.ScreenHost
import com.car.autoverdict.util.EncarUrl

/**
 * Wires [ResultScreen] to a [ResultController] for [url] and to the platform: share sheet,
 * clipboard, the Encar page and toasts (spec 5.3). The controller lives as long as this route
 * stays composed; leaving the screen cancels collection and evaluation, while DB writes
 * already started (save, unsave, cache) still finish.
 */
@Composable
fun ResultRoute(
    url: String,
    host: ScreenHost,
    repository: ResultRepository,
    evaluator: ReportEvaluator,
    bannerAd: @Composable () -> Unit,
    loadingAd: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val toast = LocalAvToast.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val controller = remember(url) { ResultController(url, repository, evaluator, scope) }
    // start() is idempotent; launching from an effect keeps coroutine work out of composition.
    LaunchedEffect(controller) { controller.start() }
    val state by controller.state.collectAsStateWithLifecycle()

    // A toast action may run after this screen left; always reach the latest host.
    val currentHost by rememberUpdatedState(host)
    LaunchedEffect(controller, lifecycleOwner) {
        // Events are buffered by the controller, so ones sent while stopped show on return.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            controller.events.collect { event ->
                val copy = event.toastCopy()
                toast.show(
                    message = context.getString(copy.message),
                    actionLabel = copy.action?.let(context::getString),
                    onAction = if (event == ResultEvent.Saved) ({ currentHost.selectTab(MainTab.Saved) }) else null,
                )
            }
        }
    }

    val shareText = (state as? ResultUiState.Ready)?.report?.shareText
    val actions = ResultActions(
        onBack = host.back,
        // Canonical detail page: share links carry tracking query params we don't need to reopen.
        onOpenInEncar = {
            EncarUrl.extractCarId(url)?.let(EncarUrl::buildDetailUrl)?.let { detail ->
                context.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(detail)))
            }
        },
        onRetry = controller::retry,
        onApplyUpdate = controller::applyUpdate,
        onUserScrolled = controller::onUserScrolled,
        onSave = controller::save,
        onUnsave = controller::unsave,
        onShareSummary = { shareText?.summary?.let(context::shareText) },
        onShareQuestions = { shareText?.sellerQuestions?.let(context::shareText) },
        onCopyQuestions = {
            shareText?.sellerQuestions?.let { text ->
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.result_clip_label_questions), text))
                // Android 13+ shows its own copy confirmation; a second one would be noise.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    toast.show(context.getString(R.string.result_toast_copied))
                }
            }
        },
    )
    ResultScreen(state = state, actions = actions, bannerAd = bannerAd, loadingAd = loadingAd)
}

/** Toast copy for each controller event (spec 5.1 retry, 5.3). Only Saved has an action. */
internal data class ToastCopy(@StringRes val message: Int, @StringRes val action: Int? = null)

internal fun ResultEvent.toastCopy(): ToastCopy = when (this) {
    ResultEvent.Saved -> ToastCopy(R.string.result_toast_saved, R.string.result_toast_saved_action)
    ResultEvent.Unsaved -> ToastCopy(R.string.result_toast_unsaved)
    ResultEvent.SaveFailed -> ToastCopy(R.string.result_toast_save_failed)
    ResultEvent.RetryFailed -> ToastCopy(R.string.result_toast_retry_failed)
}

private fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    startSafely(Intent.createChooser(send, null))
}

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No browser / share target installed; nothing sensible to offer instead.
        Log.w("ResultRoute", "no activity for ${intent.action}", e)
    }
}
