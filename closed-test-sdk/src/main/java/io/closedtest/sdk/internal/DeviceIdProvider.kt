package io.closedtest.sdk.internal

import android.content.Context
import android.provider.Settings

internal object DeviceIdProvider {
    private const val PREFS = "io.closedtest.sdk.device"
    private const val KEY = "device_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)
        if (existing != null) return existing
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        )
        val created = DeviceIdDerivation.firstInstallId(androidId)
        prefs.edit().putString(KEY, created).apply()
        return created
    }
}
