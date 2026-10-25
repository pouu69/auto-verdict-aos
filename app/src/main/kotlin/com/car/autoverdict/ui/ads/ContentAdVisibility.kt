package com.car.autoverdict.ui.ads

import androidx.compose.runtime.compositionLocalOf

/** A shared app-open entry owns its loading screen; no banner/native ad may sit underneath it. */
val LocalContentAdsAllowed = compositionLocalOf { true }
