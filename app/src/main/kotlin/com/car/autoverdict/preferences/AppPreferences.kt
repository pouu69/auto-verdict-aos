package com.car.autoverdict.preferences

import android.content.Context
import android.content.SharedPreferences
import com.car.autoverdict.saved.SavedSort

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isOnboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun setOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    /** Saved-list order; unknown stored values read as the default. */
    var savedSort: SavedSort
        get() = SavedSort.fromKey(prefs.getString(KEY_SAVED_SORT, null))
        set(value) {
            prefs.edit().putString(KEY_SAVED_SORT, value.key).apply()
        }

    companion object {
        private const val PREFS_NAME = "autoverdict_prefs"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_SAVED_SORT = "saved_sort"
    }
}
