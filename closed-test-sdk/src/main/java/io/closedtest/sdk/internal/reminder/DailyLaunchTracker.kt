package io.closedtest.sdk.internal.reminder

import android.content.Context
import java.time.LocalDate

/**
 * Tracks whether the host app was brought to the foreground on the current local calendar day.
 */
internal object DailyLaunchTracker {
    private const val PREFS = "io.closedtest.sdk.daily_launch"
    private const val KEY_LAST_LOCAL_DATE = "last_local_date"

    fun recordLaunch(context: Context) {
        val today = LocalDate.now().toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_LOCAL_DATE, today)
            .apply()
    }

    fun wasLaunchedToday(context: Context): Boolean {
        val today = LocalDate.now().toString()
        val stored =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_LOCAL_DATE, null)
        return stored == today
    }
}
