package io.closedtest.sdk

import android.app.Application
import android.content.Context
import android.net.Uri
import io.closedtest.sdk.internal.SdkController

/**
 * Entry point for the closed-test proof SDK.
 *
 * By default SDK may auto-initialize via AndroidX Startup when
 * `io.closedtest.sdk.publishable_key` is provided in app `AndroidManifest.xml`.
 *
 * Call [initialize] manually only if you need explicit control; repeated calls are ignored.
 */
object ClosedTest {

    /**
     * Initializes networking, persistence, lifecycle observers, and optional automatic sessions.
     */
    @JvmStatic
    fun initialize(context: Context, publishableKey: String, options: ClosedTestOptions) {
        SdkController.initialize(context.applicationContext, publishableKey, options)
    }

    /** When `test_session_id` / `tester_id` query parameters are present, stores binding for subsequent events. */
    @JvmStatic
    fun handleDeepLink(uri: Uri?): Boolean = SdkController.handleDeepLink(uri)

    /** Explicit binding when deep link parsing is done by the host app. */
    @JvmStatic
    fun bindTester(testerId: String?, testSessionId: String?) {
        SdkController.bindTester(testerId, testSessionId)
    }

    @JvmStatic
    fun trackScreen(screenName: String) {
        SdkController.trackScreen(screenName)
    }

    @JvmStatic
    fun trackInteraction(category: String? = null) {
        SdkController.trackInteraction(category)
    }

    @JvmStatic
    fun trackEvent(name: String, props: Map<String, String>? = null) {
        SdkController.trackEvent(name, props)
    }

    /** Flushes queued events to the server as soon as possible (network permitting). */
    @JvmStatic
    fun flush() {
        SdkController.flush()
    }
}
