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
 * Deep links may [storeExplicit] a fresher `df_{token}` when the app was already installed.
 * Failures are swallowed — init must not depend on referrer availability.
 */
internal object InstallReferrerReader {
    private const val PREFS = "io.closedtest.sdk.install_referrer"
    private const val KEY_VALUE = "install_referrer"
    private const val READ_TIMEOUT_MS = 4_000L
    private val DEEP_LINK_REFERRER_KEYS = listOf("referrer", "install_referrer", "df_referrer")

    fun peekCached(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_VALUE, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** Stores tracked invite referrer from a deep link (overrides empty or stale Play referrer). */
    fun storeExplicit(context: Context, referrer: String) {
        val value = referrer.trim().takeIf { it.isNotEmpty() } ?: return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VALUE, value)
            .apply()
    }

    fun parseFromDeepLink(uri: android.net.Uri): String? {
        for (key in DEEP_LINK_REFERRER_KEYS) {
            val raw = uri.getQueryParameter(key)?.trim().orEmpty()
            if (raw.isNotEmpty()) return raw
        }
        return null
    }

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
