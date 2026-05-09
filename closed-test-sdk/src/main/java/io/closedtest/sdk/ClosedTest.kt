package io.closedtest.sdk

import android.app.Application
import android.content.Context
import android.net.Uri
import io.closedtest.sdk.internal.SdkController

/**
 * Entry point for the closed-test proof SDK.
 *
 * By default SDK auto-initializes via AndroidX Startup when `io.closedtest.sdk.auto_init_enabled`
 * is true (default). **`publishable_key`** is optional: omit or leave empty for **Base** ingest
 * (identity by package + build type + version); set for **Advanced** ingest with server-side key policy.
 *
 * Call [initialize] manually only if you need explicit control; repeated calls are ignored.
 */
object ClosedTest {

    /**
     * Суффикс authority для ContentProvider discovery в host-приложении.
     * Полный authority: `BuildConfig.APPLICATION_ID` host-приложения + это значение.
     *
     * ProofFlow проверяет наличие провайдера через `PackageManager.resolveContentProvider`
     * или `ContentResolver` для URI с этим authority (см. `docs/SDK_USAGE.md`).
     */
    const val DISCOVERY_AUTHORITY_SUFFIX: String = ".closedtest.discovery"

    /**
     * Неявный Intent для обнаружения приложений с SDK на устройстве (`queryIntentActivities`).
     * Должен совпадать с `<action>` в манифесте библиотеки и с запросом `<queries><intent>` у клиентов (ProofFlow).
     */
    const val DISCOVERY_INTENT_ACTION: String = "io.closedtest.sdk.DISCOVERY"

    /** Authority строки вида `content://com.example.anyapp.closedtest.discovery/…` для указанного `applicationId`. */
    @JvmStatic
    fun discoveryAuthority(applicationId: String): String = "$applicationId$DISCOVERY_AUTHORITY_SUFFIX"

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
