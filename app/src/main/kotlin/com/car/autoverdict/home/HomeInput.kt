package com.car.autoverdict.home

import androidx.annotation.StringRes
import com.car.autoverdict.R
import com.car.autoverdict.util.EncarUrl

/**
 * Pure rules for the home link field. Typed and pasted text go through the same
 * [EncarUrl] extraction as share intents, so share text like "[엔카] 차명 https://…" is
 * reduced to the canonical detail URL before it reaches `Route.Result`.
 */
object HomeInput {

    sealed interface Submit {
        data class Go(val url: String) : Submit
        data object Invalid : Submit
        data object Blank : Submit
    }

    sealed interface Paste {
        data class Filled(val url: String) : Paste
        data object Empty : Paste
        data object NotEncar : Paste
    }

    fun submit(text: String): Submit {
        if (text.isBlank()) return Submit.Blank
        return normalize(text)?.let(Submit::Go) ?: Submit.Invalid
    }

    fun paste(clipText: String?): Paste {
        if (clipText.isNullOrBlank()) return Paste.Empty
        return normalize(clipText)?.let(Paste::Filled) ?: Paste.NotEncar
    }

    private fun normalize(text: String): String? =
        EncarUrl.extractCarId(text)?.let(EncarUrl::buildDetailUrl)
}

/** Inline error under the home link field. */
enum class HomeError(@StringRes val messageRes: Int) {
    Invalid(R.string.home_error_invalid),
    ClipboardEmpty(R.string.home_error_clipboard_empty),
    ClipboardNotEncar(R.string.home_error_clipboard_not_encar),
}
