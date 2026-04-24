package io.closedtest.sdk.internal

import kotlin.random.Random

/**
 * Exponential backoff with jitter for upload retries (caps exponent to avoid overflow).
 */
internal class UploadBackoff(
    private val initialMs: Long,
    private val maxMs: Long,
) {
    private var exponent: Int = 0

    fun reset() {
        exponent = 0
    }

    /** Returns delay to wait before the next attempt; increases internal step. */
    fun nextDelayMs(): Long {
        val shift = exponent.coerceAtMost(12)
        exponent++
        val base = (initialMs * (1L shl shift)).coerceAtMost(maxMs)
        val jitter = Random.nextLong(0, (initialMs.coerceAtMost(500L)).coerceAtLeast(1L))
        return (base + jitter).coerceAtMost(maxMs)
    }
}
