package io.closedtest.sdk.internal

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reads Play Install Referrer once per install (cached locally).
 * Failures are swallowed — init must not depend on referrer availability.
 */
internal object InstallReferrerReader {
    private const val PREFS = "io.closedtest.sdk.install_referrer"
    private const val KEY_VALUE = "install_referrer"
    private const val READ_TIMEOUT_MS = 4_000L

    fun peekCached(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_VALUE, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    suspend fun readOnce(context: Context): String? {
        peekCached(context)?.let { return it }
        val app = context.applicationContext
        val value =
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(READ_TIMEOUT_MS) { readFromPlayStore(app) }
            }?.trim()?.takeIf { it.isNotEmpty() }
        if (value != null) {
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_VALUE, value)
                .apply()
        }
        return value
    }

    private suspend fun readFromPlayStore(context: Context): String? =
        suspendCancellableCoroutine { cont ->
            val client = InstallReferrerClient.newBuilder(context).build()
            cont.invokeOnCancellation { runCatching { client.endConnection() } }

            client.startConnection(
                object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        if (!cont.isActive) {
                            runCatching { client.endConnection() }
                            return
                        }
                        val referrer =
                            if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                                runCatching { client.installReferrer.installReferrer }.getOrNull()
                            } else {
                                null
                            }
                        runCatching { client.endConnection() }
                        cont.resume(referrer)
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        if (cont.isActive) {
                            cont.resume(null)
                        }
                    }
                },
            )
        }
}
