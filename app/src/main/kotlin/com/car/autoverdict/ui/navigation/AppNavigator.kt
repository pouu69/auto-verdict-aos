package com.car.autoverdict.ui.navigation

import com.car.autoverdict.util.EncarUrl

/** Back stack plus a share link received before first-run onboarding finished. */
data class NavState(
    val stack: List<Route>,
    val pendingSharedUrl: String? = null,
) {
    init {
        require(stack.isNotEmpty()) { "Back stack must never be empty" }
    }

    val top: Route get() = stack.last()
}

sealed interface NavEvent {
    /** Pushes [route]. Tab roots ([Route.Home], [Route.Saved]) must be reached via [SelectTab], not Open. */
    data class Open(val route: Route) : NavEvent
    data object Back : NavEvent
    data class SelectTab(val tab: MainTab) : NavEvent
    data class OpenShared(val url: String) : NavEvent
    data object InvalidShare : NavEvent
    data object OnboardingFinished : NavEvent
}

/** How AppRoot should animate the change. Replace and None render without animation. */
enum class NavTransition { None, Push, Pop, Replace }

enum class NavMessage { InvalidShare }

data class NavResult(
    val state: NavState,
    val transition: NavTransition,
    /** False when the event should fall through to the system (back on a root). */
    val handled: Boolean = true,
    val message: NavMessage? = null,
)

/** Pure navigation reducer — every rule in spec 7.2's event table lives here. */
object AppNavigator {

    fun initial(onboardingComplete: Boolean): NavState =
        NavState(listOf(if (onboardingComplete) Route.Home else Route.Onboarding))

    fun canHandleBack(state: NavState): Boolean = back(state).handled

    fun reduce(state: NavState, event: NavEvent): NavResult = when (event) {
        is NavEvent.Open -> open(state, event.route)
        NavEvent.Back -> back(state)
        is NavEvent.SelectTab -> selectTab(state, event.tab)
        is NavEvent.OpenShared -> openShared(state, event.url)
        NavEvent.InvalidShare -> NavResult(state, NavTransition.None, message = NavMessage.InvalidShare)
        NavEvent.OnboardingFinished -> finishOnboarding(state)
    }

    private fun open(state: NavState, route: Route): NavResult {
        val top = state.top
        if (route is Route.Result && top is Route.Result && sameCar(route.url, top.url)) {
            return NavResult(state, NavTransition.None)
        }
        return NavResult(state.copy(stack = state.stack + route), NavTransition.Push)
    }

    private fun back(state: NavState): NavResult = when {
        state.stack.size > 1 -> NavResult(state.copy(stack = state.stack.dropLast(1)), NavTransition.Pop)
        state.top == Route.Saved -> NavResult(state.copy(stack = listOf(Route.Home)), NavTransition.Replace)
        else -> NavResult(state, NavTransition.None, handled = false)
    }

    private fun selectTab(state: NavState, tab: MainTab): NavResult {
        val root = tab.route
        return if (state.stack == listOf(root)) {
            NavResult(state, NavTransition.None)
        } else {
            NavResult(state.copy(stack = listOf(root)), NavTransition.Replace)
        }
    }

    private fun openShared(state: NavState, url: String): NavResult =
        if (isFirstRunOnboarding(state)) {
            NavResult(state.copy(pendingSharedUrl = url), NavTransition.None)
        } else {
            NavResult(
                NavState(listOf(Route.Home, Route.Result(url, fromSaved = false))),
                NavTransition.Replace,
            )
        }

    private fun finishOnboarding(state: NavState): NavResult {
        // A stale callback (e.g. a double tap while the screen animates out) must not pop anything else.
        if (state.top != Route.Onboarding) return NavResult(state, NavTransition.None)
        if (!isFirstRunOnboarding(state)) {
            return NavResult(state.copy(stack = state.stack.dropLast(1)), NavTransition.Pop)
        }
        val pending = state.pendingSharedUrl
        val stack = if (pending != null) {
            listOf(Route.Home, Route.Result(pending, fromSaved = false))
        } else {
            listOf(Route.Home)
        }
        return NavResult(NavState(stack), NavTransition.Replace)
    }

    /** Onboarding is the whole stack only at first launch; a replay sits on top of settings. */
    internal fun isFirstRunOnboarding(state: NavState) = state.stack == listOf(Route.Onboarding)

    private fun sameCar(a: String, b: String): Boolean {
        val idA = EncarUrl.extractCarId(a)
        return if (idA != null) idA == EncarUrl.extractCarId(b) else a == b
    }
}
