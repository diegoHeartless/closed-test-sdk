package io.closedtest.sdk.internal.dailyping

internal enum class DailyPingWorkResult {
    Sent,
    AlreadySentToday,
    Skipped,
    Retry,
}
