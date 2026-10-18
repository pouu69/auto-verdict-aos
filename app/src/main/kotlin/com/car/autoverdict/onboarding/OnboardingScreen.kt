package com.car.autoverdict.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvBottomCta
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvIllustrationIcon
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.ShareSheetIllustration
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import kotlinx.coroutines.launch

/** First run is the `[Onboarding]` root; Replay is pushed from settings ("사용법 다시 보기"). */
enum class OnboardingMode { FirstRun, Replay }

private const val PAGE_COUNT = 2

/**
 * Two-page onboarding (spec 4): what the app checks, then how to share from Encar.
 * First run offers [건너뛰기] and no back; Replay has back only and ends with "닫기".
 * Insets: [AvTopBar] owns the status bar, [AvBottomCta] the navigation bar.
 */
@Composable
fun OnboardingScreen(
    mode: OnboardingMode,
    hasPendingShare: Boolean,
    onFinish: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
    val isFirstRun = mode == OnboardingMode.FirstRun

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        AvTopBar(
            onBack = onBack,
            actions = {
                if (isFirstRun && !isLastPage) {
                    AvButton(
                        text = stringResource(R.string.onboarding_skip),
                        onClick = onFinish,
                        style = AvButtonStyle.Secondary,
                        size = AvButtonSize.Small,
                    )
                }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { page ->
            // Pages scroll so large fonts and small screens don't clip (spec 4.1).
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
            ) {
                when (page) {
                    0 -> ValuePage()
                    else -> SharePage()
                }
            }
        }
        PageIndicator(
            current = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp),
        )
        if (isFirstRun && hasPendingShare) {
            Text(
                text = stringResource(R.string.onboarding_pending_share),
                style = typography.caption1,
                color = colors.accentText,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = AvTheme.spacing.screenHorizontal)
                    .padding(bottom = 12.dp),
            )
        }
        AvBottomCta {
            val label = when {
                !isLastPage -> stringResource(R.string.onboarding_next)
                isFirstRun -> stringResource(R.string.onboarding_start)
                else -> stringResource(R.string.onboarding_close)
            }
            AvButton(
                text = label,
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ValuePage() {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val horizontal = AvTheme.spacing.screenHorizontal
    Column(modifier = Modifier.padding(horizontal = horizontal)) {
        AvIllustrationIcon(icon = Icons.Rounded.FactCheck, tone = AvTone.Accent, size = 96.dp)
        Spacer(Modifier.height(24.dp))
        PageTitle(stringResource(R.string.onboarding_value_title))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_value_body),
            style = typography.body2,
            color = colors.textSecondary,
        )
    }
    Spacer(Modifier.height(16.dp))
    // AvListRow carries its own screen padding, so the rows sit outside the padded column.
    ValuePoint(Icons.Rounded.CarCrash, AvTone.Danger, stringResource(R.string.onboarding_value_point_history))
    ValuePoint(Icons.Rounded.People, AvTone.Warning, stringResource(R.string.onboarding_value_point_owner))
    ValuePoint(Icons.Rounded.Build, AvTone.Neutral, stringResource(R.string.onboarding_value_point_inspection))
}

@Composable
private fun ValuePoint(icon: ImageVector, tone: AvTone, title: String) {
    AvListRow(
        title = title,
        leading = { AvIllustrationIcon(icon = icon, tone = tone, size = 40.dp) },
    )
}

@Composable
private fun SharePage() {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = Modifier.padding(horizontal = AvTheme.spacing.screenHorizontal)) {
        PageTitle(stringResource(R.string.onboarding_share_title))
        Spacer(Modifier.height(16.dp))
        ShareStep(number = 1, text = stringResource(R.string.onboarding_share_step_open))
        ShareStep(number = 2, text = stringResource(R.string.onboarding_share_step_pick))
        Spacer(Modifier.height(16.dp))
        ShareSheetIllustration(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_share_tip),
            style = typography.caption1,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(
        text = text,
        style = AvTheme.typography.title1,
        color = AvTheme.colors.textPrimary,
        modifier = Modifier.semantics { heading() },
    )
}

/** Numbered step, styled like the share guide sheet's steps. */
@Composable
private fun ShareStep(number: Int, text: String) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.accentWeak),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = typography.caption2, color = colors.accentText)
        }
        Text(text = text, style = typography.body2, color = colors.textPrimary, modifier = Modifier.weight(1f))
    }
}

/** Dots: the current page is a 20dp accent pill. Read as one "n쪽 중 m쪽" node (spec 9). */
@Composable
private fun PageIndicator(current: Int, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val description = stringResource(R.string.onboarding_page_cd, current + 1, PAGE_COUNT)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PAGE_COUNT) { index ->
            val active = index == current
            val width by animateDpAsState(targetValue = if (active) 20.dp else 8.dp, label = "pageDotWidth")
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (active) colors.accent else colors.border),
            )
        }
    }
}
