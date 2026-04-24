package io.closedtest.sdk.internal

import java.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class IngestApi(
    baseUrl: String,
    private val client: OkHttpClient,
) {
    private val root: String = baseUrl.trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun postInit(request: InitRequestDto): Result<InitResponseDto> =
        postJson("$root/v1/init", requestBody = json.encodeToString(InitRequestDto.serializer(), request), bearer = null)

    fun postRefresh(refreshToken: String): Result<InitResponseDto> {
        val body = json.encodeToString(RefreshRequestDto.serializer(), RefreshRequestDto(refreshToken))
        return postJson("$root/v1/session/refresh", body, bearer = null)
    }

    fun postEvents(bodyJson: String, bearer: String): Result<Unit> =
        try {
            val req = Request.Builder()
                .url("$root/v1/events")
                .addHeader("Authorization", "Bearer $bearer")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(JSON))
                .build()
            client.newCall(req).execute().use { resp ->
                when (resp.code) {
                    in 200..299 -> Result.success(Unit)
                    else -> Result.failure(IngestHttpException(resp.code, resp.body?.string()))
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }

    private fun postJson(url: String, requestBody: String, bearer: String?): Result<InitResponseDto> =
        try {
            val builder = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody(JSON))
            if (bearer != null) builder.addHeader("Authorization", "Bearer $bearer")
            client.newCall(builder.build()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return Result.failure(IngestHttpException(resp.code, raw))
                }
                try {
                    Result.success(json.decodeFromString(InitResponseDto.serializer(), raw))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

internal class IngestHttpException(val code: Int, val body: String?) : Exception("HTTP $code: $body")

@Serializable
internal data class InitRequestDto(
    @SerialName("publishable_key") val publishableKey: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("sdk_version") val sdkVersion: String,
    @SerialName("app_version") val appVersion: String,
    val os: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("test_session_id") val testSessionId: String? = null,
    @SerialName("tester_id") val testerId: String? = null,
)

@Serializable
internal data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
internal data class InitResponseDto(
    @SerialName("session_token") val sessionToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("session_expires_at") val sessionExpiresAt: String,
    @SerialName("refresh_expires_at") val refreshExpiresAt: String,
    @SerialName("server_heartbeat_interval_ms") val serverHeartbeatIntervalMs: Long? = null,
    @SerialName("ingest_enabled") val ingestEnabled: Boolean? = null,
)
