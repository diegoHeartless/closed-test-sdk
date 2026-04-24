package io.closedtest.sdk.internal

import android.content.Context

internal class BindingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("io.closedtest.sdk.binding", Context.MODE_PRIVATE)

    var testerId: String?
        get() = prefs.getString(KEY_TESTER, null)
        set(value) {
            if (value == null) prefs.edit().remove(KEY_TESTER).apply()
            else prefs.edit().putString(KEY_TESTER, value).apply()
        }

    var testSessionId: String?
        get() = prefs.getString(KEY_SESSION, null)
        set(value) {
            if (value == null) prefs.edit().remove(KEY_SESSION).apply()
            else prefs.edit().putString(KEY_SESSION, value).apply()
        }

    companion object {
        private const val KEY_TESTER = "tester_id"
        private const val KEY_SESSION = "test_session_id"
    }
}
