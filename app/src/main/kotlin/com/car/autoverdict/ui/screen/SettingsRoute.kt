package com.car.autoverdict.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.car.autoverdict.AutoVerdictApp
import com.car.autoverdict.BuildConfig
import com.car.autoverdict.R
import com.car.autoverdict.home.recentViewItems
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.navigation.Route
import com.car.autoverdict.ui.navigation.ScreenHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wires [SettingsScreen] to the DB, toasts and the mail app (spec 3.5). Counts are live flows,
 * so clearing drops them to zero right away (and the tab badge follows the same saved flow).
 */
@Composable
fun SettingsRoute(host: ScreenHost) {
    val context = LocalContext.current
    val toast = LocalAvToast.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as AutoVerdictApp
    val cacheDao = app.database.cacheDao()
    val savedCarDao = app.database.savedCarDao()
    val savedCars = app.savedCarsRepository
    // Same query as the home list, so the count matches what the user sees there (expired rows excluded).
    val recentCount by remember(cacheDao) { cacheDao.recentViewItems().map { it.size } }
        .collectAsStateWithLifecycle(initialValue = null)
    // Grouped like the saved tab and its badge, so one car's duplicate rows count once.
    val savedCount by remember(savedCars) { savedCars.itemCount }
        .collectAsStateWithLifecycle(initialValue = null)

    fun clear(@StringRes doneMessage: Int, block: suspend () -> Unit) {
        scope.launch {
            val message = try {
                withContext(Dispatchers.IO) { block() }
                doneMessage
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                R.string.settings_clear_failed
            }
            toast.show(context.getString(message))
        }
    }

    fun openContactMail() {
        val email = context.getString(R.string.settings_contact_email)
        val body = context.getString(R.string.settings_contact_body, BuildConfig.VERSION_NAME, Build.VERSION.RELEASE)
        // SENDTO + mailto: limits the chooser to mail apps; the user sends it from there.
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_contact_subject))
            putExtra(Intent.EXTRA_TEXT, "$body\n\n")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast.show(context.getString(R.string.settings_no_mail_app))
        }
    }

    SettingsScreen(
        state = SettingsUiState(
            recentCount = recentCount,
            savedCount = savedCount,
            versionName = BuildConfig.VERSION_NAME,
        ),
        onClearRecent = { clear(R.string.settings_clear_recent_done) { cacheDao.clearAll() } },
        onClearSaved = { clear(R.string.settings_clear_saved_done) { savedCarDao.deleteAll() } },
        onPrivacyPolicy = { host.navigate(Route.PrivacyPolicy) },
        onLicenses = { host.navigate(Route.Licenses) },
        onReplayGuide = { host.navigate(Route.Onboarding) },
        onContact = ::openContactMail,
        onGallery = if (BuildConfig.DEBUG) ({ host.navigate(Route.Gallery) }) else null,
        onBack = host.back,
    )
}
