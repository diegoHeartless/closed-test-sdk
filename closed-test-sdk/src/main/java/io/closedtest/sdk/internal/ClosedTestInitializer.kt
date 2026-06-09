package io.closedtest.sdk.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.startup.Initializer
import io.closedtest.sdk.ClosedTest
import io.closedtest.sdk.ClosedTestOptions

/**
 * Auto-init entrypoint via AndroidX Startup.
 *
 * Host app may configure:
 * - <meta-data android:name="io.closedtest.sdk.publishable_key" android:value="pk_..." /> (optional; Advanced ingest)
 * - <meta-data android:name="io.closedtest.sdk.auto_init_enabled" android:value="true|false" />
 * - <meta-data android:name="io.closedtest.sdk.proofflow_hint_enabled" android:value="true|false" /> — выключить подсказку: `false` (по умолчанию при отсутствии ключа — **включено**).
 * - <meta-data android:name="io.closedtest.sdk.daily_reminder_enabled" android:value="true|false" /> — локальное напоминание (по умолчанию **true**, 15:00 local).
 * - <meta-data android:name="io.closedtest.sdk.daily_reminder_hour" android:value="0-23" />
 * - <meta-data android:name="io.closedtest.sdk.daily_reminder_minute" android:value="0-59" />
 */
internal class ClosedTestInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext
        val appInfo = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
        val meta = appInfo.metaData

        val autoInitEnabled = meta?.getBoolean(META_AUTO_INIT_ENABLED, true) ?: true
        if (!autoInitEnabled) return

        val publishableKey = meta?.getString(META_PUBLISHABLE_KEY)?.trim().orEmpty()

        val proofFlowHintEnabled = meta?.getBoolean(META_PROOFFLOW_HINT_ENABLED, true) ?: true
        val dailyReminderEnabled = meta?.getBoolean(META_DAILY_REMINDER_ENABLED, true) ?: true
        val dailyReminderHour = meta?.getInt(META_DAILY_REMINDER_HOUR, 15).coerceIn(0, 23)
        val dailyReminderMinute = meta?.getInt(META_DAILY_REMINDER_MINUTE, 0).coerceIn(0, 59)

        // initialize() is idempotent on SDK side; empty key uses Base ingest (package/build/version tuple).
        ClosedTest.initialize(
            app,
            publishableKey,
            ClosedTestOptions(
                proofFlowHintEnabled = proofFlowHintEnabled,
                dailyReminderEnabled = dailyReminderEnabled,
                dailyReminderHourLocal = dailyReminderHour,
                dailyReminderMinuteLocal = dailyReminderMinute,
            ),
        )
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

    companion object {
        const val META_PUBLISHABLE_KEY = "io.closedtest.sdk.publishable_key"
        const val META_AUTO_INIT_ENABLED = "io.closedtest.sdk.auto_init_enabled"
        const val META_PROOFFLOW_HINT_ENABLED = "io.closedtest.sdk.proofflow_hint_enabled"
        const val META_DAILY_REMINDER_ENABLED = "io.closedtest.sdk.daily_reminder_enabled"
        const val META_DAILY_REMINDER_HOUR = "io.closedtest.sdk.daily_reminder_hour"
        const val META_DAILY_REMINDER_MINUTE = "io.closedtest.sdk.daily_reminder_minute"
    }
}
