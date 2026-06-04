package io.closedtest.sdk.internal

import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Install-scoped device id for ingest roster.
 *
 * Uses a deterministic UUID (v3) from [androidId] when available so reinstall
 * does not inflate roster; falls back to random UUID v4 when ANDROID_ID is missing or invalid.
 * Raw ANDROID_ID is never sent on the wire.
 */
internal object DeviceIdDerivation {
    private const val NAME_NAMESPACE_PREFIX = "io.closedtest.sdk:install:v1:"
    private const val EMULATOR_BUG_ANDROID_ID = "9774d56d682e549c"
    private const val ZERO_ANDROID_ID = "0000000000000000"

    fun fromAndroidSettingsId(androidId: String?): String? {
        val raw = androidId?.trim().orEmpty()
        if (raw.isEmpty() || raw == EMULATOR_BUG_ANDROID_ID || raw == ZERO_ANDROID_ID) {
            return null
        }
        val bytes = (NAME_NAMESPACE_PREFIX + raw).toByteArray(StandardCharsets.UTF_8)
        return UUID.nameUUIDFromBytes(bytes).toString()
    }

    fun firstInstallId(androidId: String?): String =
        fromAndroidSettingsId(androidId) ?: UUID.randomUUID().toString()
}
