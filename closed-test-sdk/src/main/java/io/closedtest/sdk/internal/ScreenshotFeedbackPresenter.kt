package io.closedtest.sdk.internal

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import io.closedtest.sdk.ClosedTestOptions
import io.closedtest.sdk.R
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * When [POST /v1/init] returns `organizer_telegram`, listens for screenshots and offers to share
 * feedback with the organizer (Telegram share intent or chat deep link).
 */
internal object ScreenshotFeedbackPresenter {

    private const val PREFS = "io.closedtest.sdk.screenshot_feedback"
    private const val KEY_NEVER = "never"
    private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
    private const val MEDIA_LOOKUP_DELAY_MS = 450L
    private const val DETECTION_DEBOUNCE_MS = 1_200L

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var callbacksRegistered = false

    @Volatile
    private var mediaObserverRegistered = false

    private var cachedOptions: ClosedTestOptions? = null

    @Volatile
    private var organizerTelegram: String? = null

    @Volatile
    private var pendingPrompt = false

    @Volatile
    private var dialogShowing = false

    @Volatile
    private var lastDetectionMs = 0L

    private var weakTopActivity: WeakReference<Activity>? = null
    private val captureCallbacks = WeakHashMap<Activity, Activity.ScreenCaptureCallback>()

    private var mediaObserver: ContentObserver? = null

    fun configureAfterInit(application: Application, options: ClosedTestOptions, rawOrganizerTelegram: String?) {
        val username = TelegramUsername.normalize(rawOrganizerTelegram.orEmpty())
        synchronized(lock) {
            cachedOptions = options
            organizerTelegram = username
            if (!isFeatureEnabled(application, options, username)) {
                disableDetection(application)
                return
            }
            ensureCallbacks(application)
            ensureMediaObserver(application)
        }
    }

    private fun isFeatureEnabled(context: Context, options: ClosedTestOptions, username: String?): Boolean {
        if (!options.screenshotFeedbackEnabled) return false
        if (username.isNullOrBlank()) return false
        if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_NEVER, false)) return false
        return true
    }

    private fun ensureCallbacks(application: Application) {
        if (callbacksRegistered) return
        callbacksRegistered = true
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) {
                    weakTopActivity = WeakReference(activity)
                    registerScreenCaptureCallback(activity)
                }

                override fun onActivityResumed(activity: Activity) {
                    weakTopActivity = WeakReference(activity)
                    if (pendingPrompt && !dialogShowing && canShowOnActivity(activity)) {
                        tryShowDialog(activity)
                    }
                }

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) {
                    unregisterScreenCaptureCallback(activity)
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    unregisterScreenCaptureCallback(activity)
                    if (weakTopActivity?.get() === activity) {
                        weakTopActivity = null
                    }
                }
            },
        )
    }

    private fun ensureMediaObserver(application: Application) {
        if (mediaObserverRegistered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        mediaObserverRegistered = true
        val observer =
            object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    onChange(selfChange, null)
                }

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    onScreenshotDetected()
                }
            }
        mediaObserver = observer
        val resolver = application.contentResolver
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
    }

    private fun disableDetection(application: Application) {
        organizerTelegram = null
        pendingPrompt = false
        mediaObserver?.let { observer ->
            try {
                application.contentResolver.unregisterContentObserver(observer)
            } catch (_: Throwable) {
            }
        }
        mediaObserver = null
        mediaObserverRegistered = false
    }

    private fun registerScreenCaptureCallback(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        if (captureCallbacks.containsKey(activity)) return
        val callback =
            Activity.ScreenCaptureCallback {
                onScreenshotDetected()
            }
        captureCallbacks[activity] = callback
        activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
    }

    private fun unregisterScreenCaptureCallback(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val callback = captureCallbacks.remove(activity) ?: return
        try {
            activity.unregisterScreenCaptureCallback(callback)
        } catch (_: Throwable) {
        }
    }

    private fun onScreenshotDetected() {
        val opts = cachedOptions ?: return
        val username = organizerTelegram ?: return
        val now = System.currentTimeMillis()
        if (now - lastDetectionMs < DETECTION_DEBOUNCE_MS) return
        lastDetectionMs = now

        val activity = weakTopActivity?.get()
        if (activity == null || !canShowOnActivity(activity)) {
            pendingPrompt = true
            return
        }
        if (!shouldShowPrompt(activity, opts)) return

        mainHandler.postDelayed({
            val act = weakTopActivity?.get()
            if (act != null && canShowOnActivity(act) && shouldShowPrompt(act, opts)) {
                tryShowDialog(act)
            } else {
                pendingPrompt = true
            }
        }, MEDIA_LOOKUP_DELAY_MS)
    }

    private fun shouldShowPrompt(context: Context, options: ClosedTestOptions): Boolean {
        if (!isFeatureEnabled(context, options, organizerTelegram)) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_PROMPT_MS, 0L)
        val now = System.currentTimeMillis()
        return last <= 0L || now - last >= options.screenshotFeedbackCooldownMs
    }

    private fun canShowOnActivity(activity: Activity): Boolean {
        if (activity.isFinishing) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) return false
        val cn = activity.componentName?.className ?: activity.javaClass.name
        if (cn.contains("ClosedTestDiscoveryStubActivity")) return false
        return true
    }

    private fun tryShowDialog(activity: Activity) {
        val username = organizerTelegram ?: return
        val opts = cachedOptions ?: return
        if (dialogShowing) return
        if (!shouldShowPrompt(activity, opts)) {
            pendingPrompt = false
            return
        }

        dialogShowing = true
        pendingPrompt = false
        SdkController.trackEvent("screenshot_feedback_prompt_shown", null)

        val dlg =
            AlertDialog.Builder(activity)
                .setTitle(R.string.closed_test_sdk_screenshot_feedback_title)
                .setMessage(
                    activity.getString(
                        R.string.closed_test_sdk_screenshot_feedback_message,
                        "@$username",
                    ),
                )
                .setPositiveButton(R.string.closed_test_sdk_screenshot_feedback_share) { d, _ ->
                    shareScreenshot(activity, username)
                    d.dismiss()
                }
                .setNegativeButton(R.string.closed_test_sdk_screenshot_feedback_later) { d, _ ->
                    d.dismiss()
                }
                .setNeutralButton(R.string.closed_test_sdk_screenshot_feedback_never) { d, _ ->
                    activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_NEVER, true).apply()
                    (activity.application as? Application)?.let { disableDetection(it) }
                    d.dismiss()
                }
                .create()

        dlg.setOnShowListener {
            recordPromptShown(activity)
        }
        dlg.setOnDismissListener {
            dialogShowing = false
        }
        dlg.show()
    }

    private fun recordPromptShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis())
            .apply()
    }

    private fun shareScreenshot(activity: Activity, username: String) {
        val sinceMs = System.currentTimeMillis() - 15_000L
        val imageUri = ScreenshotMediaLocator.findLatestScreenshotUri(activity, sinceMs)
        if (imageUri != null) {
            val shareText =
                activity.getString(
                    R.string.closed_test_sdk_screenshot_feedback_share_text,
                    "@$username",
                )
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            try {
                activity.startActivity(
                    Intent.createChooser(
                        intent,
                        activity.getString(R.string.closed_test_sdk_screenshot_feedback_chooser),
                    ),
                )
                SdkController.trackEvent("screenshot_feedback_shared", mapOf("with_image" to "true"))
                return
            } catch (_: Throwable) {
                // Fall through to Telegram deep link.
            }
        }
        openTelegramChat(activity, username)
        SdkController.trackEvent(
            "screenshot_feedback_shared",
            mapOf("with_image" to if (imageUri != null) "failed" else "false"),
        )
    }

    private fun openTelegramChat(activity: Activity, username: String) {
        val uri = Uri.parse("https://t.me/$username")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            activity.startActivity(intent)
        } catch (_: Throwable) {
            // No Telegram handler — ignore.
        }
    }
}
