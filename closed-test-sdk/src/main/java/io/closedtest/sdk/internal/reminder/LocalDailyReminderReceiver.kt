package io.closedtest.sdk.internal.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal class LocalDailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != LocalDailyReminderScheduler.ACTION_DAILY_REMINDER) return
        LocalDailyReminderScheduler.onAlarmFired(context.applicationContext)
    }
}

internal class LocalDailyReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        LocalDailyReminderScheduler.rescheduleFromStoredConfig(context.applicationContext)
    }
}
