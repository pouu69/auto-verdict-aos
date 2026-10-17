package com.car.autoverdict.ui.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/** String form of [Route] for saved-instance state. Unknown or malformed input decodes to null. */
object RouteCodec {

    fun encode(route: Route): String = when (route) {
        Route.Onboarding -> "onboarding"
        Route.Home -> "home"
        Route.Saved -> "saved"
        Route.Settings -> "settings"
        Route.PrivacyPolicy -> "privacy"
        Route.Licenses -> "licenses"
        Route.Gallery -> "gallery"
        is Route.Result -> "result|${if (route.fromSaved) 1 else 0}|${route.url}"
        is Route.Compare -> "compare|${route.carIds.joinToString(",")}"
    }

    fun decode(value: String): Route? {
        val parts = value.split("|", limit = 3)
        return when (parts[0]) {
            "onboarding" -> Route.Onboarding
            "home" -> Route.Home
            "saved" -> Route.Saved
            "settings" -> Route.Settings
            "privacy" -> Route.PrivacyPolicy
            "licenses" -> Route.Licenses
            "gallery" -> Route.Gallery
            "result" -> if (parts.size == 3) Route.Result(parts[2], fromSaved = parts[1] == "1") else null
            "compare" -> parts.getOrNull(1)
                ?.split(",")
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
                ?.let { Route.Compare(it) }
            else -> null
        }
    }
}

/** Saved form: first element is the pending shared url ("" if none), then encoded routes. */
object NavStateCodec {

    fun encode(state: NavState): List<String> =
        listOf(state.pendingSharedUrl.orEmpty()) + state.stack.map(RouteCodec::encode)

    fun decode(saved: List<String>): NavState? {
        if (saved.isEmpty()) return null
        // Any unreadable route invalidates the whole stack, so we never restore e.g. [Settings] without its root.
        val stack = saved.drop(1).map { RouteCodec.decode(it) ?: return null }
        if (stack.isEmpty()) return null
        return NavState(stack, pendingSharedUrl = saved.first().ifEmpty { null })
    }
}

/** rememberSaveable saver for the navigation state (no Parcelize plugin needed). */
val NavStateSaver: Saver<NavState, Any> = listSaver(
    save = { NavStateCodec.encode(it) },
    restore = { NavStateCodec.decode(it) },
)
