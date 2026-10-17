package com.car.autoverdict.result

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.result.sections.AllRulesSection
import com.car.autoverdict.result.sections.CountChips
import com.car.autoverdict.result.sections.ExcludedSection
import com.car.autoverdict.result.sections.FactGrid
import com.car.autoverdict.result.sections.IssuesSection
import com.car.autoverdict.result.sections.RepairsSection
import com.car.autoverdict.result.sections.ResultBannerCard
import com.car.autoverdict.result.sections.SellerQuestionsSection
import com.car.autoverdict.result.sections.TimelineSection
import com.car.autoverdict.result.sections.VerdictHeader
import com.car.autoverdict.ui.components.AvBottomCta
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvDialog
import com.car.autoverdict.ui.components.AvEmptyState
import com.car.autoverdict.ui.components.AvLoadingSteps
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.LoadingStep
import com.car.autoverdict.ui.components.StepState
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Everything the result screen can ask of its host. [ResultScreen] itself holds no data state. */
data class ResultActions(
    val onBack: () -> Unit,
    val onOpenInEncar: () -> Unit,
    val onRetry: () -> Unit,
    val onApplyUpdate: () -> Unit,
    val onUserScrolled: () -> Unit,
    val onSave: () -> Unit,
    /** Called after the user confirms the unsave dialog (dialog lives in ResultScreen). */
    val onUnsave: () -> Unit,
    val onShareSummary: () -> Unit,
    val onShareQuestions: () -> Unit,
    val onCopyQuestions: () -> Unit,
)

/**
 * Result screen for one listing (spec 5). Renders [state] only; the controller and the
 * platform effects (share, clipboard, toasts) live in the caller. The outer column applies
 * the navigation-bar inset once, so the bottom CTA is told not to add it again.
 */
@Composable
fun ResultScreen(
    state: ResultUiState,
    actions: ResultActions,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    bannerAd: @Composable () -> Unit = {},
    loadingAd: @Composable () -> Unit = {},
) {
    val colors = AvTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding(),
    ) {
        // Error and deleted screens already offer their own way out; InvalidUrl has nothing to open.
        val showEncarChip = state is ResultUiState.Loading || state is ResultUiState.Ready
        AvTopBar(
            onBack = actions.onBack,
            title = stringResource(R.string.result_title),
            subtitle = if (state is ResultUiState.Ready && state.saved) stringResource(R.string.result_subtitle_saved) else null,
            actions = { if (showEncarChip) OpenInEncarChip(onClick = actions.onOpenInEncar) },
        )
        when (state) {
            is ResultUiState.Loading -> {
                LoadingBody(stage = state.stage, loadingAd = loadingAd, modifier = Modifier.weight(1f))
            }
            is ResultUiState.Ready -> {
                ReadyBody(state = state, actions = actions, listState = listState, modifier = Modifier.weight(1f))
                ReadyCta(state = state, actions = actions)
                bannerAd()
            }
            is ResultUiState.Error -> {
                ErrorBody(kind = state.kind, actions = actions, modifier = Modifier.weight(1f))
                bannerAd()
            }
            ResultUiState.Deleted -> {
                CenteredState(modifier = Modifier.weight(1f)) {
                    AvEmptyState(
                        icon = Icons.Rounded.Block,
                        title = stringResource(R.string.result_deleted),
                        actionLabel = stringResource(R.string.result_go_back),
                        onAction = actions.onBack,
                    )
                }
                bannerAd()
            }
        }
    }
}

@Composable
private fun LoadingBody(stage: LoadingStage, loadingAd: @Composable () -> Unit, modifier: Modifier) {
    val colors = AvTheme.colors
    val labels = listOf(
        stringResource(R.string.result_loading_open_page),
        stringResource(R.string.result_loading_fetch_history),
        stringResource(R.string.result_loading_evaluate),
    )
    val activeIndex = when (stage) {
        LoadingStage.OpeningPage -> 0
        LoadingStage.FetchingHistory -> 1
        LoadingStage.Evaluating -> 2
    }
    val steps = labels.mapIndexed { index, label ->
        val stepState = when {
            index < activeIndex -> StepState.Done
            index == activeIndex -> StepState.Active
            else -> StepState.Pending
        }
        LoadingStep(label, stepState)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AvTheme.spacing.screenHorizontal, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.result_loading_title),
            style = AvTheme.typography.title2,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(24.dp))
        AvLoadingSteps(steps = steps)
        Spacer(Modifier.height(32.dp))
        // MREC (300x250) while waiting (spec 5.4).
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { loadingAd() }
    }
}

private data class ErrorCopy(
    val icon: ImageVector,
    @StringRes val title: Int,
    val retry: Boolean,
    val openInEncar: Boolean,
)

/** Spec 5.1 error table. InvalidUrl can't be retried, so it only offers the way back. */
private fun errorCopy(kind: ResultErrorKind): ErrorCopy = when (kind) {
    ResultErrorKind.Network -> ErrorCopy(Icons.Rounded.WifiOff, R.string.result_error_network, retry = true, openInEncar = false)
    ResultErrorKind.Timeout -> ErrorCopy(Icons.Rounded.HourglassEmpty, R.string.result_error_timeout, retry = true, openInEncar = true)
    ResultErrorKind.PageUnreadable ->
        ErrorCopy(Icons.Rounded.ErrorOutline, R.string.result_error_page_unreadable, retry = true, openInEncar = true)
    ResultErrorKind.EngineFailed -> ErrorCopy(Icons.Rounded.ErrorOutline, R.string.result_error_engine, retry = true, openInEncar = false)
    ResultErrorKind.InvalidUrl -> ErrorCopy(Icons.Rounded.LinkOff, R.string.result_error_invalid_url, retry = false, openInEncar = false)
}

@Composable
private fun ErrorBody(kind: ResultErrorKind, actions: ResultActions, modifier: Modifier) {
    val copy = errorCopy(kind)
    CenteredState(modifier = modifier) {
        AvEmptyState(
            icon = copy.icon,
            title = stringResource(copy.title),
            actionLabel = stringResource(if (copy.retry) R.string.result_retry else R.string.result_go_back),
            onAction = if (copy.retry) actions.onRetry else actions.onBack,
            secondaryActionLabel = if (copy.openInEncar) stringResource(R.string.result_open_in_encar) else null,
            onSecondaryAction = if (copy.openInEncar) actions.onOpenInEncar else null,
        )
    }
}

@Composable
private fun CenteredState(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ReadyBody(
    state: ResultUiState.Ready,
    actions: ResultActions,
    listState: LazyListState,
    modifier: Modifier,
) {
    val onUserScrolled by rememberUpdatedState(actions.onUserScrolled)
    // Report the first time the list leaves the top: after that the controller turns refreshes
    // into a banner. Position, not drag gestures, so TalkBack scroll actions, mouse wheels and
    // keyboard scrolling count too; scrolling back to the top (after "보기") never fires it.
    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollBackward }.first { it }
        onUserScrolled()
    }
    val scope = rememberCoroutineScope()
    val questions = remember(state.report) { state.report.issues.mapNotNull { it.sellerQuestion } }
    Column(modifier = modifier.fillMaxWidth()) {
        // Pinned above the list: "update available" only appears after the user has scrolled,
        // so it has to stay visible wherever the list is.
        state.banner?.let { banner ->
            ResultBannerCard(
                banner = banner,
                onApplyUpdate = {
                    actions.onApplyUpdate()
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onRetry = actions.onRetry,
            )
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            reportSections(report = state.report, questions = questions, hasBanner = state.banner != null, actions = actions)
        }
    }
}

/** Spec 5.2 items 3-11, top to bottom, all expanded. Empty sections are left out with their divider. */
private fun LazyListScope.reportSections(
    report: EvaluationReport,
    questions: List<String>,
    hasBanner: Boolean,
    actions: ResultActions,
) {
    item(key = "header") {
        Column(modifier = Modifier.padding(top = if (hasBanner) 4.dp else 8.dp, bottom = 24.dp)) {
            VerdictHeader(report)
            Spacer(Modifier.height(16.dp))
            CountChips(report.counts)
            if (report.facts.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                FactGrid(report.facts)
            }
        }
    }
    report.repairs?.let { repairs -> section("repairs") { RepairsSection(repairs) } }
    if (report.issues.isNotEmpty()) section("issues") { IssuesSection(report.issues) }
    if (report.timeline.isNotEmpty()) section("timeline") { TimelineSection(report.timeline) }
    section("rules") { AllRulesSection(report.rules) }
    if (report.excluded.isNotEmpty()) section("excluded") { ExcludedSection(report.excluded) }
    if (questions.isNotEmpty()) {
        section("questions") {
            SellerQuestionsSection(questions = questions, onCopy = actions.onCopyQuestions, onShare = actions.onShareQuestions)
        }
    }
}

/** A grey band, then the section. */
private fun LazyListScope.section(key: String, content: @Composable () -> Unit) {
    item(key = "$key-divider") { AvSectionDivider() }
    item(key = key) { content() }
}

@Composable
private fun ReadyCta(state: ResultUiState.Ready, actions: ResultActions) {
    var confirmUnsave by rememberSaveable { mutableStateOf(false) }
    // Unsaved elsewhere (or by a refresh): drop the pending confirm so re-saving doesn't revive it.
    LaunchedEffect(state.saved) { if (!state.saved) confirmUnsave = false }
    AvBottomCta(windowInsets = WindowInsets(0)) {
        CtaIconButton(
            icon = Icons.Rounded.Share,
            contentDescription = stringResource(R.string.result_cd_share),
            onClick = actions.onShareSummary,
        )
        AvButton(
            text = stringResource(if (state.saved) R.string.result_saved else R.string.result_save),
            onClick = { if (state.saved) confirmUnsave = true else actions.onSave() },
            modifier = Modifier.weight(1f),
            style = if (state.saved) AvButtonStyle.Secondary else AvButtonStyle.Primary,
            size = AvButtonSize.Large,
            loading = state.saving,
        )
    }
    if (confirmUnsave && state.saved) {
        AvDialog(
            title = stringResource(R.string.result_unsave_title),
            message = stringResource(R.string.result_unsave_message),
            confirmLabel = stringResource(R.string.result_unsave_confirm),
            onConfirm = {
                confirmUnsave = false
                actions.onUnsave()
            },
            dismissLabel = stringResource(R.string.result_unsave_dismiss),
            onDismiss = { confirmUnsave = false },
            destructive = true,
        )
    }
}

/** Square companion to a Large button in the bottom CTA (same height and radius). */
@Composable
private fun CtaIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = AvTheme.colors
    val shape = AvTheme.shapes.buttonL
    Box(
        modifier = Modifier
            .avPressable(onClick = onClick, focusShape = shape)
            .semantics { this.contentDescription = contentDescription }
            .size(AvButtonSize.Large.minHeight)
            .clip(shape)
            .background(colors.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun OpenInEncarChip(onClick: () -> Unit) {
    val colors = AvTheme.colors
    val shape = AvTheme.shapes.chip
    // minimumInteractiveComponentSize is outermost: it keeps the 48dp touch target (centring the
    // pill inside it) while avPressable sizes to the pill, so the focus ring follows the pill.
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .avPressable(onClick = onClick, focusShape = shape)
            .clip(shape)
            .background(colors.accentWeak)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.result_open_in_encar),
            style = AvTheme.typography.buttonS,
            color = colors.accentText,
        )
    }
}
