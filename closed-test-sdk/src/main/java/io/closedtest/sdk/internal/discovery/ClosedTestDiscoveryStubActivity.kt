package io.closedtest.sdk.internal.discovery

import android.app.Activity
import android.os.Bundle

/**
 * Невидимая activity только для объявления неявного Intent — ProofFlow и другие клиенты
 * находят приложения с интегрированным SDK через [PackageManager.queryIntentActivities]
 * ([ClosedTest.DISCOVERY_INTENT_ACTION]). Не предназначена для ручного запуска пользователем.
 */
internal class ClosedTestDiscoveryStubActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
