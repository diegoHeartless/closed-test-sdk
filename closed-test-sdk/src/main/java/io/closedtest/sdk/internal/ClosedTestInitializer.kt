package io.closedtest.sdk.internal

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.startup.Initializer
import io.closedtest.sdk.ClosedTest
import io.closedtest.sdk.ClosedTestInit
import io.closedtest.sdk.ClosedTestOptions

/**
 * Auto-init entrypoint via AndroidX Startup.
 *
 * Host app may configure:
 * - <meta-data android:name="io.closedtest.sdk.owner_email" android:value="dev@example.com" /> (**required**)
 * - <meta-data android:name="io.closedtest.sdk.google_group_url" android:value="https://groups.google.com/g/..." /> (**required**)
 * - <meta-data android:name="io.closedtest.sdk.invite_link" android:value="https://play.google.com/apps/testing/..." /> (**required**)
 * - <meta-data android:name="io.closedtest.sdk.publishable_key" android:value="pk_..." /> (optional; Advanced ingest)
 * - <meta-data android:name="io.closedtest.sdk.auto_init_enabled" android:value="true|false" />
 * - <meta-data android:name="io.closedtest.sdk.proofflow_hint_enabled" android:value="true|false" /> — выключить подсказку: `false` (по умолчанию при отсутствии ключа — **включено**).
 * - <meta-data android:name="io.closedtest.sdk.daily_reminder_enabled" android:value="true|false" /> — локальное напоминание (по умолчанию **true**, 15:00 local).
 * - <meta-data android:name="io.closedtest.sdk.daily_reminder_hour" android:value="0-23" />
 * - <meta-data android:name="io.closedtest.sdk.daily_reminder_minute" android:value="0-59" />
 * - <meta-data android:name="io.closedtest.sdk.daily_ping_enabled" android:value="true|false" /> — фоновый `daily_ping` на ingest (по умолчанию **true**, ~раз в сутки).
 * - <meta-data android:name="io.closedtest.sdk.roster_contact_prompt_enabled" android:value="true|false" /> — Telegram self-report after first session (default **false**).
 * - <meta-data android:name="io.closedtest.sdk.screenshot_feedback_enabled" android:value="true|false" /> — screenshot → share with organizer on Telegram when init returns `organizer_telegram` (default **true**).
 *
 * Without all required channel meta-data, auto-init is skipped — call [ClosedTest.initialize] with
 * [ClosedTestInit] from code instead.
 */
internal class ClosedTestInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext
        val appInfo = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
        val meta = appInfo.metaData

        val autoInitEnabled = meta?.getBoolean(META_AUTO_INIT_ENABLED, true) ?: true
        if (!autoInitEnabled) return

        val ownerEmail = meta?.getString(META_OWNER_EMAIL)?.trim().orEmpty()
        val googleGroupUrl = meta?.getString(META_GOOGLE_GROUP_URL)?.trim().orEmpty()
        val inviteLink = meta?.getString(META_INVITE_LINK)?.trim().orEmpty()
        if (ownerEmail.isEmpty() || googleGroupUrl.isEmpty() || inviteLink.isEmpty()) {
            Log.w(
                TAG,
                "auto-init skipped: set meta-data $META_OWNER_EMAIL, $META_GOOGLE_GROUP_URL, and " +
                    "$META_INVITE_LINK (or call ClosedTest.initialize with ClosedTestInit)",
            )
            return
        }

        val publishableKey = meta?.getString(META_PUBLISHABLE_KEY)?.trim().orEmpty()

        val proofFlowHintEnabled = meta?.getBoolean(META_PROOFFLOW_HINT_ENABLED, true) ?: true
        val dailyReminderEnabled = meta?.getBoolean(META_DAILY_REMINDER_ENABLED, true) ?: true
        val dailyReminderHour = (meta?.getInt(META_DAILY_REMINDER_HOUR, 15) ?: 15).coerceIn(0, 23)
        val dailyReminderMinute = (meta?.getInt(META_DAILY_REMINDER_MINUTE, 0) ?: 0).coerceIn(0, 59)
        val dailyPingEnabled = meta?.getBoolean(META_DAILY_PING_ENABLED, true) ?: true
        val rosterContactPromptEnabled = meta?.getBoolean(META_ROSTER_CONTACT_PROMPT_ENABLED, false) ?: false
        val screenshotFeedbackEnabled = meta?.getBoolean(META_SCREENSHOT_FEEDBACK_ENABLED, true) ?: true

        // initialize() is idempotent on SDK side; empty key uses Base ingest (package/build/version tuple).
        ClosedTest.initialize(
            app,
            ClosedTestInit(
                ownerEmail = ownerEmail,
                googleGroupUrl = googleGroupUrl,
                inviteLink = inviteLink,
                publishableKey = publishableKey.ifEmpty { null },
            ),
            ClosedTestOptions(
                proofFlowHintEnabled = proofFlowHintEnabled,
                dailyReminderEnabled = dailyReminderEnabled,
                dailyReminderHourLocal = dailyReminderHour,
                dailyReminderMinuteLocal = dailyReminderMinute,
                dailyPingEnabled = dailyPingEnabled,
                rosterContactPromptEnabled = rosterContactPromptEnabled,
                screenshotFeedbackEnabled = screenshotFeedbackEnabled,
            ),
        )
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

    companion object {
        private const val TAG = "ClosedTestInitializer"
        const val META_OWNER_EMAIL = "io.closedtest.sdk.owner_email"
        const val META_GOOGLE_GROUP_URL = "io.closedtest.sdk.google_group_url"
        const val META_INVITE_LINK = "io.closedtest.sdk.invite_link"
        const val META_PUBLISHABLE_KEY = "io.closedtest.sdk.publishable_key"
        const val META_AUTO_INIT_ENABLED = "io.closedtest.sdk.auto_init_enabled"
        const val META_PROOFFLOW_HINT_ENABLED = "io.closedtest.sdk.proofflow_hint_enabled"
        const val META_DAILY_REMINDER_ENABLED = "io.closedtest.sdk.daily_reminder_enabled"
        const val META_DAILY_REMINDER_HOUR = "io.closedtest.sdk.daily_reminder_hour"
        const val META_DAILY_REMINDER_MINUTE = "io.closedtest.sdk.daily_reminder_minute"
        const val META_DAILY_PING_ENABLED = "io.closedtest.sdk.daily_ping_enabled"
        const val META_ROSTER_CONTACT_PROMPT_ENABLED = "io.closedtest.sdk.roster_contact_prompt_enabled"
        const val META_SCREENSHOT_FEEDBACK_ENABLED = "io.closedtest.sdk.screenshot_feedback_enabled"
    }
}
