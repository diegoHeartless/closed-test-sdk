package io.closedtest.sdk.internal.dailyping

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.closedtest.sdk.ClosedTestOptions
import java.util.concurrent.TimeUnit

internal object DailyPingScheduler {
    const val WORK_NAME = "io.closedtest.sdk.daily_ping"

    private const val PREFS = "io.closedtest.sdk.daily_ping"
    private const val KEY_ENABLED = "enabled"

    fun apply(context: Context, options: ClosedTestOptions) {
        persistEnabled(context, options.dailyPingEnabled)
        if (!options.dailyPingEnabled) {
            cancel(context)
            return
        }
        schedule(context)
    }

    fun rescheduleFromStoredConfig(context: Context) {
        val enabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
        if (!enabled) {
            cancel(context)
            return
        }
        schedule(context)
    }

    private fun schedule(context: Context) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        val request =
            PeriodicWorkRequestBuilder<DailyPingWorker>(24, TimeUnit.HOURS, 4, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    private fun persistEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
