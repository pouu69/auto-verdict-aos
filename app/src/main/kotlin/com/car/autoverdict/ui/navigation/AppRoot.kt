package com.car.autoverdict.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvTabBar
import com.car.autoverdict.ui.components.AvToastHost
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.rememberAvToastState
import com.car.autoverdict.ui.theme.AvTheme

/** What a screen may do to the app shell. Built by [AppRoot], consumed by [AppScreenContent]. */
data class ScreenHost(
    val navigate: (Route) -> Unit,
    val back: () -> Unit,
    val finishOnboarding: () -> Unit,
    /** True only for the onboarding shown at first launch; false when replayed from settings. */
    val isFirstRunOnboarding: Boolean,
    /** A share arrived during first-run onboarding and opens once it finishes. */
    val hasPendingShare: Boolean,
    /** Jumps to a tab root, clearing sub-screens (e.g. a toast's "보기" after saving). */
    val selectTab: (MainTab) -> Unit,
)

/**
 * App shell: owns the back stack, toast host, tab bar and banner slot. Screens and the
 * banner are injected so tests can run without AdMob or WebView (spec 12).
 */
@Composable
fun AppRoot(
    onboardingComplete: Boolean,
    onOnboardingComplete: () -> Unit,
    pendingNavEvent: NavEvent?,
    onPendingNavEventConsumed: () -> Unit,
    savedCount: Int,
    adBanner: @Composable () -> Unit,
    screenContent: @Composable (Route, ScreenHost) -> Unit,
) {
    var navState by rememberSaveable(stateSaver = NavStateSaver) {
        mutableStateOf(AppNavigator.initial(onboardingComplete))
    }
    var transition by remember { mutableStateOf(NavTransition.None) }
    val toast = rememberAvToastState()
    // Keeps each stacked screen's rememberSaveable state while it is covered (spec 7.2).
    val saveableStateHolder = rememberSaveableStateHolder()
    val invalidShareMessage = stringResource(R.string.toast_invalid_share)

    fun dispatch(event: NavEvent) {
        val previous = navState
        val result = AppNavigator.reduce(previous, event)
        navState = result.state
        transition = result.transition
        val onboardingClosed = previous.top == Route.Onboarding && result.state.top != Route.Onboarding
        if (event == NavEvent.OnboardingFinished && onboardingClosed) {
            onOnboardingComplete()
        }
        // Routes that left the stack must start fresh if opened again.
        val remaining = result.state.stack.mapTo(HashSet(), RouteCodec::encode)
        previous.stack.map(RouteCodec::encode).filterNot { it in remaining }.forEach(saveableStateHolder::removeState)
        if (result.message == NavMessage.InvalidShare) {
            toast.show(invalidShareMessage)
        }
    }

    LaunchedEffect(pendingNavEvent) {
        if (pendingNavEvent != null) {
            dispatch(pendingNavEvent)
            onPendingNavEventConsumed()
        }
    }

    BackHandler(enabled = AppNavigator.canHandleBack(navState)) { dispatch(NavEvent.Back) }

    // Each screen gets a host bound to its own route. During a transition the outgoing screen
    // is still composed and tappable; its callbacks are ignored so a double tap can't pop twice.
    fun hostFor(route: Route) = ScreenHost(
        navigate = { if (navState.top == route) dispatch(NavEvent.Open(it)) },
        back = { if (navState.top == route) dispatch(NavEvent.Back) },
        finishOnboarding = { if (navState.top == route) dispatch(NavEvent.OnboardingFinished) },
        isFirstRunOnboarding = route == Route.Onboarding && AppNavigator.isFirstRunOnboarding(navState),
        hasPendingShare = navState.pendingSharedUrl != null,
        // Not gated on the top route: a toast action may fire after its screen left, and
        // repeating SelectTab is a no-op, so a double tap can't stack anything.
        selectTab = { dispatch(NavEvent.SelectTab(it)) },
    )
    val top = navState.top
    val currentTab = top.tabOrNull()
    // While the chrome animates out with a tab page it keeps showing that tab as selected.
    var lastTab by remember { mutableStateOf(currentTab ?: MainTab.Home) }
    SideEffect { if (currentTab != null) lastTab = currentTab }
    val animateChrome = transition == NavTransition.Push || transition == NavTransition.Pop
    // Computed, not measured: a tab page first composed while the chrome is hidden (share at cold
    // start, restored sub-screen) must already stop at the right height, or its first pop jumps.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarHeight = TabBarDividerHeight + TabBarRowHeight + navBarBottom
    val bottomChromeHeight = BannerSlotHeight + tabBarHeight

    CompositionLocalProvider(LocalAvToast provides toast) {
        Box(modifier = Modifier.fillMaxSize().background(AvTheme.colors.background)) {
            // Pages always get the full height, so nothing resizes when the chrome comes or goes.
            AnimatedContent(
                targetState = top,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { transitionFor(transition) },
                label = "route",
            ) { route ->
                // Opaque and a hit target, so a page sliding in hides the chrome under it and
                // keeps taps (e.g. on the leaving banner) from reaching it.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(pageInsets(route, bottomChromeHeight))
                        .background(AvTheme.colors.background)
                        .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
                ) {
                    saveableStateHolder.SaveableStateProvider(RouteCodec.encode(route)) {
                        screenContent(route, hostFor(route))
                    }
                }
            }
            // Outside AnimatedContent so switching tabs keeps the same AdView (spec 10). On push it
            // slides out with the tab page (then the AdView is released); on pop it comes back with
            // it. Drawn under the pages so an incoming sub-screen slides over it.
            AnimatedVisibility(
                visible = currentTab != null,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(-1f),
                enter = if (animateChrome) chromeEnter else EnterTransition.None,
                exit = if (animateChrome) chromeExit else ExitTransition.None,
                label = "tabChrome",
            ) {
                // Fixed heights keep the chrome exactly bottomChromeHeight tall, whether or not
                // the ad has loaded.
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(BannerSlotHeight)) { adBanner() }
                    AvTabBar(
                        selected = currentTab ?: lastTab,
                        // Taps on the chrome while it leaves belong to a screen that is no longer on top.
                        onSelect = { if (navState.top.tabOrNull() != null) dispatch(NavEvent.SelectTab(it)) },
                        savedCount = savedCount,
                        modifier = Modifier.height(tabBarHeight),
                    )
                }
            }
            AvToastHost(
                state = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(toastOffset(currentTab != null, bottomChromeHeight)),
            )
        }
    }
}

/**
 * Tab roots stop above the banner + tab bar and consume that height, so a screen's imePadding()
 * only adds the part of the keyboard overlapping it. Other routes run to the bottom edge.
 */
private fun pageInsets(route: Route, bottomChromeHeight: Dp): Modifier =
    if (route.tabOrNull() != null) {
        Modifier
            .padding(bottom = bottomChromeHeight)
            .consumeWindowInsets(PaddingValues(bottom = bottomChromeHeight))
    } else {
        Modifier
    }

// AdSize.BANNER is 320x50; the slot keeps that height before the ad loads (and in tests).
private val BannerSlotHeight = 50.dp

// AvTabBar: 1dp divider over a 56dp row, plus the navigation bar inset below it.
private val TabBarDividerHeight = 1.dp
private val TabBarRowHeight = 56.dp

// Matches the outgoing tab page's slide on push (and the incoming one's on pop).
private val chromeEnter = slideInHorizontally(tween(250)) { -it / 4 } + fadeIn(tween(250))
private val chromeExit = slideOutHorizontally(tween(250)) { -it / 4 } + fadeOut(tween(250))

private fun toastOffset(onTabRoot: Boolean, bottomChromeHeight: Dp): Modifier =
    if (onTabRoot) {
        Modifier.padding(bottom = bottomChromeHeight)
    } else {
        Modifier.navigationBarsPadding().padding(bottom = 16.dp)
    }

private fun AnimatedContentTransitionScope<Route>.transitionFor(transition: NavTransition): ContentTransform =
    when (transition) {
        NavTransition.Push ->
            slideInHorizontally(tween(250)) { it } togetherWith slideOutHorizontally(tween(250)) { -it / 4 }
        NavTransition.Pop ->
            (slideInHorizontally(tween(250)) { -it / 4 } togetherWith slideOutHorizontally(tween(250)) { it })
                .apply { targetContentZIndex = -1f }
        NavTransition.None, NavTransition.Replace ->
            EnterTransition.None togetherWith ExitTransition.None
    }
