package io.closedtest.sdk.internal.dailyping

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

internal object DailyPingTracker {
    private const val PREFS = "io.closedtest.sdk.daily_ping_tracker"
    private const val KEY_LAST_SENT_DAY = "last_sent_day"

    fun wasSentToday(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val today = localDayString(nowMillis)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SENT_DAY, null) == today
    }

    fun markSentToday(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val today = localDayString(nowMillis)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SENT_DAY, today)
            .apply()
    }

    internal fun localDayString(nowMillis: Long): String {
        val zone = ZoneId.systemDefault()
        return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zone).toString()
    }
}
