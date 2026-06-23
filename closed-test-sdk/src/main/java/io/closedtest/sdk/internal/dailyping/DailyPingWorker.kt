package io.closedtest.sdk.internal.dailyping

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.closedtest.sdk.internal.SdkController

internal class DailyPingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        when (SdkController.runDailyPingWork()) {
            DailyPingWorkResult.Sent,
            DailyPingWorkResult.AlreadySentToday,
            DailyPingWorkResult.Skipped,
            -> Result.success()
            DailyPingWorkResult.Retry -> Result.retry()
        }
}
