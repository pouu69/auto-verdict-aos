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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
    /** Last link handed to analysis; the home screen skips re-filling it from the clipboard. */
    val lastAnalyzedUrl: String?,
    val onAnalyzed: (String) -> Unit,
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
    var lastAnalyzedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var bottomChromeHeight by remember { mutableStateOf(0.dp) }
    val toast = rememberAvToastState()
    // Keeps each stacked screen's rememberSaveable state while it is covered (spec 7.2).
    val saveableStateHolder = rememberSaveableStateHolder()
    val density = LocalDensity.current
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
        lastAnalyzedUrl = lastAnalyzedUrl,
        onAnalyzed = { lastAnalyzedUrl = it },
    )
    val top = navState.top
    val currentTab = top.tabOrNull()
    // While the chrome animates out with a tab page it keeps showing that tab as selected.
    var lastTab by remember { mutableStateOf(currentTab ?: MainTab.Home) }
    if (currentTab != null) lastTab = currentTab
    val animateChrome = transition == NavTransition.Push || transition == NavTransition.Pop

    CompositionLocalProvider(LocalAvToast provides toast) {
        Box(modifier = Modifier.fillMaxSize().background(AvTheme.colors.background)) {
            // Pages always get the full height, so nothing resizes when the chrome comes or goes.
            AnimatedContent(
                targetState = top,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { transitionFor(transition) },
                label = "route",
            ) { route ->
                // Opaque, so a page sliding in hides whatever is under it (the chrome included).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(pageInsets(route, bottomChromeHeight))
                        .background(AvTheme.colors.background),
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
                Column(
                    modifier = Modifier.onSizeChanged {
                        // Keep the last real height so tab pages never jump while the chrome is gone.
                        if (it.height > 0) bottomChromeHeight = with(density) { it.height.toDp() }
                    },
                ) {
                    adBanner()
                    AvTabBar(
                        selected = lastTab,
                        // Taps on the chrome while it leaves belong to a screen that is no longer on top.
                        onSelect = { if (navState.top.tabOrNull() != null) dispatch(NavEvent.SelectTab(it)) },
                        savedCount = savedCount,
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
