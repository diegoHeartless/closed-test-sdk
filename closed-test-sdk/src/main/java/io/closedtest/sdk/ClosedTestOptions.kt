package io.closedtest.sdk

import okhttp3.OkHttpClient

/**
 * Configuration for [ClosedTest.initialize].
 *
 * Ingest backend URL is fixed inside SDK and is not exposed in public API.
 *
 * @property heartbeatIntervalMs Interval between `heartbeat` events while the app is in the foreground.
 * @property backgroundSessionEndDelayMs After the app moves to the background, wait this long before emitting `session_end` unless the user returns.
 * @property collectInDebuggableBuilds When `false`, the SDK becomes a no-op if the host app is debuggable (`ApplicationInfo.FLAG_DEBUGGABLE`).
 * @property okHttpClient Optional shared [OkHttpClient]; when null, a sensible default client is created.
 * @property maxQueuedEvents Upper bound on persisted queue rows; oldest events are dropped when exceeded.
 * @property eventsBatchSize Max events per `/v1/events` request (clamped to 1..100).
 * @property uploadBackoffInitialMs First backoff delay after a retryable upload failure.
 * @property uploadBackoffMaxMs Cap for exponential backoff between upload retries.
 * @property proofFlowHintEnabled When true and the server returns `proofflow_test_id` on `POST /v1/init`, may show a dialog to open ProofFlow (PF-TEST). Default **true**; set **false** to opt out.
 * @property proofFlowPackageNames Installed package names to treat as ProofFlow when deciding whether to offer the hint (default Play + debug suffix).
 * @property proofFlowHintMaxShows Max times the hint dialog may be shown per install.
 * @property proofFlowHintCooldownMs Minimum time between hint prompts after “Later”.
 * @property dailyReminderEnabled When true, schedules a **local** daily notification if the app was not opened today (device local time). Not a remote FCM push.
 * @property dailyReminderHourLocal Hour of day (0–23) in the device timezone for the reminder. Default **15** (3 PM).
 * @property dailyReminderMinuteLocal Minute (0–59) for the reminder. Default **0**.
 * @property dailyPingEnabled When true, schedules a background **server** `daily_ping` at most once per calendar day (device local). Timing is approximate (WorkManager, within ~24h). Default **true**.
 * @property rosterContactPromptEnabled When true, may prompt once after first `session_start` for a Telegram handle (organizer roster). Default **false**.
 * @property screenshotFeedbackEnabled When true and init returns `organizer_telegram`, may prompt after a screenshot to share feedback with the organizer on Telegram. Default **true**.
 * @property screenshotFeedbackCooldownMs Minimum time between screenshot feedback prompts. Default **2 minutes**.
 */
data class ClosedTestOptions(
    val heartbeatIntervalMs: Long = 20_000L,
    val backgroundSessionEndDelayMs: Long = 60_000L,
    val collectInDebuggableBuilds: Boolean = true,
    val okHttpClient: OkHttpClient? = null,
    val maxQueuedEvents: Long = 10_000L,
    val eventsBatchSize: Int = 100,
    val uploadBackoffInitialMs: Long = 1_000L,
    val uploadBackoffMaxMs: Long = 60_000L,
    val proofFlowHintEnabled: Boolean = true,
    val proofFlowPackageNames: List<String> =
        listOf(
            "com.ground.proofflow",
            "com.ground.proofflow.debug",
        ),
    val proofFlowHintMaxShows: Int = 3,
    val proofFlowHintCooldownMs: Long = 7L * 24 * 60 * 60 * 1000,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHourLocal: Int = 15,
    val dailyReminderMinuteLocal: Int = 0,
    val dailyPingEnabled: Boolean = true,
    /**
     * When true, may show a one-time dialog after the first `session_start` (`cold_start`) so the
     * tester can share a Telegram username (`POST /v1/tester-contact`). Default **false** — opt in.
     */
    val rosterContactPromptEnabled: Boolean = false,
    /**
     * When true and `POST /v1/init` returns `organizer_telegram`, may show a dialog after the user
     * takes a screenshot to share feedback with the test organizer on Telegram. Default **true**.
     */
    val screenshotFeedbackEnabled: Boolean = true,
    /** Minimum interval between screenshot feedback prompts (after dismiss or share). */
    val screenshotFeedbackCooldownMs: Long = 2L * 60 * 1000,
)
