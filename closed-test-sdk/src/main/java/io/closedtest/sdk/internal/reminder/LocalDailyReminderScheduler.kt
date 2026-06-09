package io.closedtest.sdk.internal.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.closedtest.sdk.ClosedTestOptions
import java.util.Calendar

internal object LocalDailyReminderScheduler {
    const val ACTION_DAILY_REMINDER = "io.closedtest.sdk.action.DAILY_REMINDER"
    const val REQUEST_CODE = 40_002

    private const val PREFS = "io.closedtest.sdk.daily_reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"

    fun apply(context: Context, options: ClosedTestOptions) {
        persistConfig(context, options)
        if (!options.dailyReminderEnabled) {
            cancel(context)
            return
        }
        scheduleNext(context, options.effectiveReminderHour(), options.effectiveReminderMinute())
    }

    fun rescheduleFromStoredConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, true)) {
            cancel(context)
            return
        }
        val hour = prefs.getInt(KEY_HOUR, 15).coerceIn(0, 23)
        val minute = prefs.getInt(KEY_MINUTE, 0).coerceIn(0, 59)
        scheduleNext(context, hour, minute)
    }

    fun onAlarmFired(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, true)) return
        if (!DailyLaunchTracker.wasLaunchedToday(context)) {
            LocalDailyReminderNotifier.showIfAllowed(context)
        }
        val hour = prefs.getInt(KEY_HOUR, 15).coerceIn(0, 23)
        val minute = prefs.getInt(KEY_MINUTE, 0).coerceIn(0, 59)
        scheduleNext(context, hour, minute)
    }

    internal fun nextTriggerAtMillis(
        hourLocal: Int,
        minuteLocal: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.HOUR_OF_DAY, hourLocal.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minuteLocal.coerceIn(0, 59))
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun persistConfig(context: Context, options: ClosedTestOptions) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, options.dailyReminderEnabled)
            .putInt(KEY_HOUR, options.effectiveReminderHour())
            .putInt(KEY_MINUTE, options.effectiveReminderMinute())
            .apply()
    }

    private fun scheduleNext(context: Context, hourLocal: Int, minuteLocal: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerAtMillis(hourLocal, minuteLocal)
        val pending = pendingIntent(context)
        alarmManager.cancel(pending)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    private fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, LocalDailyReminderReceiver::class.java).apply {
                action = ACTION_DAILY_REMINDER
            }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ClosedTestOptions.effectiveReminderHour(): Int = dailyReminderHourLocal.coerceIn(0, 23)

    private fun ClosedTestOptions.effectiveReminderMinute(): Int = dailyReminderMinuteLocal.coerceIn(0, 59)
}
