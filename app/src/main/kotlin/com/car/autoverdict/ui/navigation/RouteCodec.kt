package com.car.autoverdict.ui.navigation

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
