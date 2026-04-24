package io.closedtest.sdk.internal

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.closedtest.sdk.BuildConfig
import io.closedtest.sdk.ClosedTestOptions
import io.closedtest.sdk.internal.db.AppDatabase
import io.closedtest.sdk.internal.db.QueuedEventDao
import io.closedtest.sdk.internal.db.QueuedEventEntity
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

internal object SdkController {

    private const val FLUSH_DEBOUNCE_MS = 1_500L
    private const val MAX_UPLOAD_RETRIES_SAME_BATCH = 12
    private const val PREFS_RUNTIME = "io.closedtest.sdk.runtime"
    private const val KEY_INGEST_ENABLED = "ingest_enabled"

    private val lock = Any()
    private var initialized = false
    private var noop = false

    private lateinit var appCtx: Context
    private lateinit var publishableKey: String
    private lateinit var options: ClosedTestOptions
    private lateinit var db: AppDatabase
    private lateinit var dao: QueuedEventDao
    private lateinit var tokenStore: TokenStore
    private lateinit var bindingStore: BindingStore
    private lateinit var ingest: IngestApi
    private lateinit var deviceId: String
    private lateinit var appVersion: String
    private lateinit var osVersion: String

    private val mainHandler = Handler(Looper.getMainLooper())
    private var sdkScope: CoroutineScope? = null

    @Volatile
    private var heartbeatMs: Long = 20_000L

    @Volatile
    private var logicalSessionOpen: Boolean = false

    @Volatile
    private var firstSessionInProcess: Boolean = true

    @Volatile
    private var currentSdkSessionId: String? = null

    private var heartbeatJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val flushRunnable = Runnable {
        sdkScope?.launch { flushBlocking() }
    }

    private val sessionEndRunnable = Runnable {
        sdkScope?.launch { emitSessionEndIfNeeded() }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            onAppForeground()
        }

        override fun onStop(owner: LifecycleOwner) {
            onAppBackground()
        }
    }

    private val memoryCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            if (!initialized || noop) return
            when (level) {
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
                -> sdkScope?.launch { flushBlocking() }
            }
        }

        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) = Unit

        override fun onLowMemory() {
            if (!initialized || noop) return
            sdkScope?.launch { flushBlocking() }
        }
    }

    fun initialize(context: Context, publishableKey: String, options: ClosedTestOptions) {
        synchronized(lock) {
            check(!initialized) { "ClosedTest.initialize already called" }
            initialized = true
            val app = context.applicationContext
            appCtx = app
            this.publishableKey = publishableKey
            this.options = options
            @Suppress("DEPRECATION")
            val appInfo = app.packageManager.getApplicationInfo(app.packageName, 0)
            val debuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (debuggable && !options.collectInDebuggableBuilds) {
                noop = true
                return
            }

            deviceId = DeviceIdProvider.getOrCreate(app)
            @Suppress("DEPRECATION")
            appVersion = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "unknown"
            osVersion = android.os.Build.VERSION.SDK_INT.toString()
            db = AppDatabase.build(app)
            dao = db.queuedEventDao()
            tokenStore = TokenStore(app)
            bindingStore = BindingStore(app)
            val client = options.okHttpClient ?: OkHttpClient()
            ingest = IngestApi(options.baseUrl, client)
            heartbeatMs = options.heartbeatIntervalMs

            val dispatcher = Executors.newSingleThreadExecutor { r ->
                Thread(r, "closed-test-sdk")
            }.asCoroutineDispatcher()
            sdkScope = CoroutineScope(SupervisorJob() + dispatcher)

            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            app.registerComponentCallbacks(memoryCallbacks)

            sdkScope?.launch {
                performHandshake()
                flushBlocking()
            }
        }
    }

    fun handleDeepLink(uri: Uri?): Boolean {
        if (!initialized || uri == null) return false
        val tester = uri.getQueryParameter("tester_id")
            ?: uri.getQueryParameter("testerId")
        val session = uri.getQueryParameter("test_session_id")
            ?: uri.getQueryParameter("testSessionId")
        if (tester == null && session == null) return false
        bindTester(tester, session)
        return true
    }

    fun bindTester(testerId: String?, testSessionId: String?) {
        if (!initialized || noop) return
        bindingStore.testerId = testerId
        bindingStore.testSessionId = testSessionId
    }

    fun trackScreen(screenName: String) {
        enqueueUserEvent {
            EventJson.screenView(
                monotonicMs = SystemClock.elapsedRealtime(),
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = currentSdkSessionId,
                testerId = bindingStore.testerId,
                testSessionId = bindingStore.testSessionId,
                screenName = screenName,
            )
        }
    }

    fun trackInteraction(category: String?) {
        enqueueUserEvent {
            EventJson.trackInteraction(
                monotonicMs = SystemClock.elapsedRealtime(),
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = currentSdkSessionId,
                testerId = bindingStore.testerId,
                testSessionId = bindingStore.testSessionId,
                category = category,
            )
        }
    }

    fun trackEvent(name: String, props: Map<String, String>?) {
        enqueueUserEvent {
            EventJson.trackEvent(
                monotonicMs = SystemClock.elapsedRealtime(),
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = currentSdkSessionId,
                testerId = bindingStore.testerId,
                testSessionId = bindingStore.testSessionId,
                name = name,
                props = props,
            )
        }
    }

    fun flush() {
        if (!initialized || noop) return
        sdkScope?.launch { flushBlocking() }
    }

    private fun enqueueUserEvent(builder: () -> JsonObject) {
        if (!initialized || noop || !ingestAllowed()) return
        sdkScope?.launch {
            enqueueJsonObject(builder())
        }
    }

    private fun onAppForeground() {
        if (!initialized || noop || !ingestAllowed()) return
        mainHandler.removeCallbacks(sessionEndRunnable)
        sdkScope?.launch {
            val monotonic = SystemClock.elapsedRealtime()
            if (!logicalSessionOpen) {
                logicalSessionOpen = true
                val sid = UUID.randomUUID().toString()
                currentSdkSessionId = sid
                val reason = if (firstSessionInProcess) {
                    firstSessionInProcess = false
                    "cold_start"
                } else {
                    "resume"
                }
                enqueueJsonObject(
                    EventJson.sessionStart(
                        monotonicMs = monotonic,
                        deviceId = deviceId,
                        appVersion = appVersion,
                        osVersion = osVersion,
                        sdkSessionId = sid,
                        testerId = bindingStore.testerId,
                        testSessionId = bindingStore.testSessionId,
                        reason = reason,
                    ),
                )
            }
            enqueueJsonObject(
                EventJson.appForeground(
                    monotonicMs = monotonic,
                    deviceId = deviceId,
                    appVersion = appVersion,
                    osVersion = osVersion,
                    sdkSessionId = currentSdkSessionId,
                    testerId = bindingStore.testerId,
                    testSessionId = bindingStore.testSessionId,
                ),
            )
            startHeartbeatLoop()
            performHandshake()
        }
    }

    private fun onAppBackground() {
        if (!initialized || noop || !ingestAllowed()) return
        stopHeartbeatLoop()
        sdkScope?.launch {
            enqueueJsonObject(
                EventJson.appBackground(
                    monotonicMs = SystemClock.elapsedRealtime(),
                    deviceId = deviceId,
                    appVersion = appVersion,
                    osVersion = osVersion,
                    sdkSessionId = currentSdkSessionId,
                    testerId = bindingStore.testerId,
                    testSessionId = bindingStore.testSessionId,
                ),
            )
            mainHandler.postDelayed(sessionEndRunnable, options.backgroundSessionEndDelayMs)
            sdkScope?.launch {
                delay(300)
                flushBlocking()
            }
        }
    }

    private suspend fun emitSessionEndIfNeeded() {
        if (!logicalSessionOpen) return
        logicalSessionOpen = false
        val sid = currentSdkSessionId
        currentSdkSessionId = null
        enqueueJsonObject(
            EventJson.sessionEnd(
                monotonicMs = SystemClock.elapsedRealtime(),
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                sdkSessionId = sid,
                testerId = bindingStore.testerId,
                testSessionId = bindingStore.testSessionId,
                reason = "background_timeout",
            ),
        )
    }

    private fun startHeartbeatLoop() {
        stopHeartbeatLoop()
        val scope = sdkScope ?: return
        heartbeatJob = scope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(heartbeatMs)
                if (!logicalSessionOpen || !ingestAllowed()) break
                val now = SystemClock.elapsedRealtime()
                val interval = (now - last).coerceAtLeast(1L)
                last = now
                enqueueJsonObject(
                    EventJson.heartbeat(
                        monotonicMs = now,
                        deviceId = deviceId,
                        appVersion = appVersion,
                        osVersion = osVersion,
                        sdkSessionId = currentSdkSessionId,
                        testerId = bindingStore.testerId,
                        testSessionId = bindingStore.testSessionId,
                        intervalMs = interval,
                    ),
                )
            }
        }
    }

    private fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun enqueueJsonObject(element: JsonObject) {
        if (!ingestAllowed()) return
        val line = json.encodeToString(JsonObject.serializer(), element)
        dao.insert(
            QueuedEventEntity(
                json = line,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        trimQueueIfNeeded()
        scheduleFlushDebounced()
    }

    private fun maxQueueRows(): Long = options.maxQueuedEvents.coerceIn(100L, 500_000L)

    private fun batchSize(): Int = options.eventsBatchSize.coerceIn(1, 100)

    private suspend fun trimQueueIfNeeded() {
        while (dao.count() > maxQueueRows()) {
            val batch = dao.oldestIds(500)
            if (batch.isEmpty()) break
            dao.deleteIds(batch)
        }
    }

    private fun scheduleFlushDebounced() {
        mainHandler.removeCallbacks(flushRunnable)
        mainHandler.postDelayed(flushRunnable, FLUSH_DEBOUNCE_MS)
    }

    private suspend fun performHandshake() {
        if (!ingestAllowed()) return
        val req = InitRequestDto(
            publishableKey = publishableKey,
            deviceId = deviceId,
            sdkVersion = BuildConfig.SDK_VERSION,
            appVersion = appVersion,
            os = "android",
            osVersion = osVersion,
            testSessionId = bindingStore.testSessionId,
            testerId = bindingStore.testerId,
        )
        val result = withContext(Dispatchers.IO) { ingest.postInit(req) }
        result.onSuccess { applyInitResponse(it) }
    }

    private fun applyInitResponse(dto: InitResponseDto) {
        tokenStore.sessionToken = dto.sessionToken
        tokenStore.refreshToken = dto.refreshToken
        val enabled = dto.ingestEnabled ?: true
        appCtx.getSharedPreferences(PREFS_RUNTIME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INGEST_ENABLED, enabled)
            .apply()
        dto.serverHeartbeatIntervalMs?.let { heartbeatMs = it }
    }

    private fun ingestAllowed(): Boolean {
        if (!initialized || noop) return false
        return appCtx.getSharedPreferences(PREFS_RUNTIME, Context.MODE_PRIVATE)
            .getBoolean(KEY_INGEST_ENABLED, true)
    }

    private suspend fun refreshTokens(): Boolean {
        val rt = tokenStore.refreshToken ?: return false
        val result = withContext(Dispatchers.IO) { ingest.postRefresh(rt) }
        return result.fold(
            onSuccess = {
                applyInitResponse(it)
                true
            },
            onFailure = { false },
        )
    }

    private suspend fun flushBlocking() {
        if (!ingestAllowed()) return
        var token = tokenStore.sessionToken
        if (token.isNullOrBlank()) {
            performHandshake()
            token = tokenStore.sessionToken
            if (token.isNullOrBlank()) return
        }
        val backoff = UploadBackoff(
            initialMs = options.uploadBackoffInitialMs.coerceAtLeast(200L),
            maxMs = options.uploadBackoffMaxMs.coerceAtLeast(options.uploadBackoffInitialMs),
        )
        var retriesSameBatch = 0
        while (true) {
            val batch = dao.peek(batchSize())
            if (batch.isEmpty()) break
            val events = batch.map { json.parseToJsonElement(it.json).jsonObject }
            val body = buildJsonObject {
                put("batch_id", UUID.randomUUID().toString())
                put("sent_at", EventJson.nowIsoUtc())
                put("events", JsonArray(events))
            }
            val bodyStr = json.encodeToString(body)
            val post = withContext(Dispatchers.IO) { ingest.postEvents(bodyStr, token!!) }
            if (post.isSuccess) {
                dao.deleteIds(batch.map { it.id })
                backoff.reset()
                retriesSameBatch = 0
                continue
            }
            val err = post.exceptionOrNull()
            if (err is IngestHttpException && err.code == 401) {
                if (refreshTokens()) {
                    token = tokenStore.sessionToken
                    if (!token.isNullOrBlank()) {
                        retriesSameBatch = 0
                        backoff.reset()
                        continue
                    }
                }
                break
            }
            if (isRetryableUploadFailure(err)) {
                retriesSameBatch++
                if (retriesSameBatch > MAX_UPLOAD_RETRIES_SAME_BATCH) break
                delay(backoff.nextDelayMs())
                continue
            }
            break
        }
    }

    private fun isRetryableUploadFailure(err: Throwable?): Boolean {
        if (err is IOException) return true
        if (err !is IngestHttpException) return false
        return when (err.code) {
            408, 425, 429 -> true
            in 500..599 -> true
            else -> false
        }
    }
}
