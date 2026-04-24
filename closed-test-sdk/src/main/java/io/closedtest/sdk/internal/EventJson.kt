package io.closedtest.sdk.internal

import io.closedtest.sdk.BuildConfig
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun JsonObjectBuilder.putAllKeys(base: JsonObject) {
    base.forEach { (k, v) -> put(k, v) }
}

internal object EventJson {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun nowIsoUtc(): String = OffsetDateTime.now(ZoneOffset.UTC).format(formatter)

    fun newEventId(): String = UUID.randomUUID().toString()

    fun buildBase(
        type: String,
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
    ): JsonObject = buildJsonObject {
        put("type", type)
        put("occurred_at", nowIsoUtc())
        put("monotonic_ms", monotonicMs)
        put("device_id", deviceId)
        put("sdk_version", BuildConfig.SDK_VERSION)
        put("app_version", appVersion)
        put("os", JsonPrimitive("android"))
        put("os_version", osVersion)
        put("event_id", newEventId())
        if (sdkSessionId != null) put("session_id", sdkSessionId)
        if (testerId != null) put("tester_id", testerId)
        if (testSessionId != null) put("test_session_id", testSessionId)
    }

    fun sessionStart(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String,
        testerId: String?,
        testSessionId: String?,
        reason: String,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "session_start",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
        put("reason", reason)
    }

    fun sessionEnd(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
        reason: String,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "session_end",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
        put("reason", reason)
    }

    fun appForeground(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "app_foreground",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
    }

    fun appBackground(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "app_background",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
    }

    fun heartbeat(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
        intervalMs: Long,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "heartbeat",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
        put("interval_ms", intervalMs)
    }

    fun screenView(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
        screenName: String,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "screen_view",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
        put("screen_name", screenName)
    }

    fun trackInteraction(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
        category: String?,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "track_interaction",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
        if (category != null) put("category", category)
    }

    fun trackEvent(
        monotonicMs: Long,
        deviceId: String,
        appVersion: String,
        osVersion: String,
        sdkSessionId: String?,
        testerId: String?,
        testSessionId: String?,
        name: String,
        props: Map<String, String>?,
    ): JsonObject = buildJsonObject {
        putAllKeys(
            buildBase(
                type = "track_event",
                monotonicMs = monotonicMs,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sdkSessionId,
                testerId = testerId,
                testSessionId = testSessionId,
            ),
        )
        put("name", name)
        val safe = PropsSanitizer.sanitize(props)
        if (safe != null) {
            put(
                "props",
                JsonObject(safe.mapValues { (_, v) -> JsonPrimitive(v) }),
            )
        }
    }
}
