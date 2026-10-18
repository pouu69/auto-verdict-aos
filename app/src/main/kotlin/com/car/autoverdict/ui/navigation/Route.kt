package com.car.autoverdict.ui.navigation

/** Navigation destinations (spec 7.2). Only small values live here — they are saved in a Bundle. */
sealed interface Route {
    data object Onboarding : Route
    data object Home : Route
    data object Saved : Route
    data object Settings : Route
    data object PrivacyPolicy : Route
    data object Licenses : Route
    /** [id] is a library or Google-bundled component id from the notices data; never contains "|". */
    data class LicenseDetail(val id: String) : Route
    data object Gallery : Route
    data class Result(val url: String, val fromSaved: Boolean) : Route
    data class Compare(val carIds: List<String>) : Route
}

val MainTab.route: Route
    get() = when (this) {
        MainTab.Home -> Route.Home
        MainTab.Saved -> Route.Saved
    }

/** Non-null only for tab roots — the tab bar and banner render only then. */
fun Route.tabOrNull(): MainTab? = when (this) {
    Route.Home -> MainTab.Home
    Route.Saved -> MainTab.Saved
    else -> null
}
