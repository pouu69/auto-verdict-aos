package com.car.autoverdict.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.ui.navigation.Route
import com.car.autoverdict.ui.navigation.ScreenHost
import com.car.autoverdict.util.ClipboardUtil
import kotlinx.coroutines.flow.Flow

/**
 * Wires [HomeScreen] to the platform (spec 3.5): recent views from the cache, clipboard reads
 * on [붙여넣기] only, the share guide sheet and navigation to `Route.Result` with the
 * normalized URL. [recentViews] is injectable so tests can skip the DB and the minute ticker.
 */
@Composable
fun HomeRoute(
    host: ScreenHost,
    recentViews: Flow<List<RecentViewItem>> = rememberRecentViews(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val openEncar = rememberOpenEncar()
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<HomeError?>(null) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    // Collected only while resumed, so the minute ticker stops in the background.
    val recent by recentViews.collectAsStateWithLifecycle(initialValue = null, minActiveState = Lifecycle.State.RESUMED)

    HomeScreen(
        input = input,
        onInputChange = {
            input = it
            error = null
        },
        error = error,
        onPaste = {
            when (val result = HomeInput.paste(ClipboardUtil.readText(context))) {
                is HomeInput.Paste.Filled -> {
                    // Fill only; an old link may still sit on the clipboard, so don't analyze yet.
                    input = result.url
                    error = null
                }
                // Leave the field alone rather than pouring arbitrary text into it.
                HomeInput.Paste.Empty -> error = HomeError.ClipboardEmpty
                HomeInput.Paste.NotEncar -> error = HomeError.ClipboardNotEncar
            }
        },
        onSubmit = {
            when (val result = HomeInput.submit(input)) {
                is HomeInput.Submit.Go -> {
                    input = ""
                    error = null
                    focusManager.clearFocus()
                    host.navigate(Route.Result(result.url, fromSaved = false))
                }
                HomeInput.Submit.Invalid -> error = HomeError.Invalid
                HomeInput.Submit.Blank -> Unit
            }
        },
        recent = recent,
        onOpenRecent = { host.navigate(Route.Result(it.url, fromSaved = false)) },
        onOpenSettings = { host.navigate(Route.Settings) },
        onOpenShareGuide = { showGuide = true },
    )

    if (showGuide) {
        ShareGuideSheet(onDismiss = { showGuide = false }, onOpenEncar = openEncar)
    }
}

/** Recent views from the app cache, re-queried every minute so expired rows drop out and ages stay fresh. */
@Composable
private fun rememberRecentViews(): Flow<List<RecentViewItem>> {
    val cacheDao = (LocalContext.current.applicationContext as AutoVerdictApp).database.cacheDao()
    return remember(cacheDao) { cacheDao.recentViewItems() }
}
