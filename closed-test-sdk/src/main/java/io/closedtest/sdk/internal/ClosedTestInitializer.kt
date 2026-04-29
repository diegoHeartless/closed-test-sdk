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
 * - <meta-data android:name="io.closedtest.sdk.publishable_key" android:value="pk_..." />
 * - <meta-data android:name="io.closedtest.sdk.auto_init_enabled" android:value="true|false" />
 */
internal class ClosedTestInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext
        val appInfo = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
        val meta = appInfo.metaData

        val autoInitEnabled = meta?.getBoolean(META_AUTO_INIT_ENABLED, true) ?: true
        if (!autoInitEnabled) return

        val publishableKey = meta?.getString(META_PUBLISHABLE_KEY)?.trim().orEmpty()
        if (publishableKey.isEmpty()) return

        // initialize() is idempotent on SDK side; safe with manual fallback in host app.
        ClosedTest.initialize(app, publishableKey, ClosedTestOptions())
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

    companion object {
        const val META_PUBLISHABLE_KEY = "io.closedtest.sdk.publishable_key"
        const val META_AUTO_INIT_ENABLED = "io.closedtest.sdk.auto_init_enabled"
    }
}
