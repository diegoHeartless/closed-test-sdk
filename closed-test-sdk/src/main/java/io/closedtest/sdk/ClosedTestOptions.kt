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
)
