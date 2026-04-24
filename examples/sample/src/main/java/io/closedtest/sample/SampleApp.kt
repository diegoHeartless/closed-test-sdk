package io.closedtest.sample

import android.app.Application
import io.closedtest.sdk.ClosedTest
import io.closedtest.sdk.ClosedTestOptions

class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ClosedTest.initialize(
            this,
            BuildConfig.PUBLISHABLE_KEY,
            ClosedTestOptions(
                baseUrl = BuildConfig.INGEST_BASE_URL,
                collectInDebuggableBuilds = true,
            ),
        )
    }
}
