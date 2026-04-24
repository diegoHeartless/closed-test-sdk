package io.closedtest.sdk.internal

import android.content.Context

internal class TokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("io.closedtest.sdk.tokens", Context.MODE_PRIVATE)

    var sessionToken: String?
        get() = prefs.getString(KEY_SESSION, null)
        set(value) {
            prefs.edit().putString(KEY_SESSION, value).apply()
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) {
            prefs.edit().putString(KEY_REFRESH, value).apply()
        }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).remove(KEY_REFRESH).apply()
    }

    companion object {
        private const val KEY_SESSION = "session_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
