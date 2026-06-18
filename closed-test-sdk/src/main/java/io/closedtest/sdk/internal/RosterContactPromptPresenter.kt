package io.closedtest.sdk.internal

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.os.Build
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import io.closedtest.sdk.ClosedTestOptions
import io.closedtest.sdk.R
import java.lang.ref.WeakReference

/**
 * Optional one-time dialog after the first `session_start` (`cold_start`) so the tester can share
 * a Telegram username with the organizer via [POST /v1/tester-contact].
 */
internal object RosterContactPromptPresenter {

    private const val PREFS = "io.closedtest.sdk.roster_contact"
    private const val KEY_NEVER = "never"
    private const val KEY_SUBMITTED = "submitted"
    private const val KEY_PROMPTED = "prompted"

    private val lock = Any()

    @Volatile
    private var callbacksRegistered = false

    private var cachedOptions: ClosedTestOptions? = null

    @Volatile
    private var pending = false

    @Volatile
    private var dialogShowing = false

    private var weakTopActivity: WeakReference<Activity>? = null

    private var submitHandler: ((String, (Result<Unit>) -> Unit) -> Unit)? = null

    fun scheduleAfterFirstSession(
        application: Application,
        options: ClosedTestOptions,
        onSubmit: (String, (Result<Unit>) -> Unit) -> Unit,
    ) {
        if (!options.rosterContactPromptEnabled) return
        if (!shouldPrompt(application)) return

        synchronized(lock) {
            cachedOptions = options
            submitHandler = onSubmit
            pending = true
            ensureCallbacks(application)
            weakTopActivity?.get()?.let { act ->
                if (canShowOnActivity(act)) tryShowDialog(act)
            }
        }
    }

    private fun ensureCallbacks(application: Application) {
        if (callbacksRegistered) return
        callbacksRegistered = true
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) {
                    weakTopActivity = WeakReference(activity)
                    synchronized(lock) {
                        if (pending && !dialogShowing && canShowOnActivity(activity)) {
                            tryShowDialog(activity)
                        }
                    }
                }

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    if (weakTopActivity?.get() === activity) {
                        weakTopActivity = null
                    }
                }
            },
        )
    }

    private fun canShowOnActivity(activity: Activity): Boolean {
        if (activity.isFinishing) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) return false
        val cn = activity.componentName?.className ?: activity.javaClass.name
        if (cn.contains("ClosedTestDiscoveryStubActivity")) return false
        return true
    }

    private fun shouldPrompt(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(KEY_NEVER, false)) return false
        if (p.getBoolean(KEY_SUBMITTED, false)) return false
        if (p.getBoolean(KEY_PROMPTED, false)) return false
        return true
    }

    private fun tryShowDialog(activity: Activity) {
        if (!pending) return
        if (dialogShowing) return
        val opts = cachedOptions ?: return
        if (!opts.rosterContactPromptEnabled) {
            pending = false
            return
        }
        if (!shouldPrompt(activity)) {
            pending = false
            return
        }

        dialogShowing = true
        val input =
            EditText(activity).apply {
                hint = activity.getString(R.string.closed_test_sdk_roster_contact_input_hint)
                inputType = InputType.TYPE_CLASS_TEXT
                setSingleLine()
            }
        val padding = (activity.resources.displayMetrics.density * 20).toInt()
        input.setPadding(padding, padding / 2, padding, 0)

        val dlg =
            AlertDialog.Builder(activity)
                .setTitle(R.string.closed_test_sdk_roster_contact_title)
                .setMessage(R.string.closed_test_sdk_roster_contact_message)
                .setView(input)
                .setPositiveButton(R.string.closed_test_sdk_roster_contact_submit, null)
                .setNegativeButton(R.string.closed_test_sdk_roster_contact_later) { d, _ ->
                    pending = false
                    markPrompted(activity)
                    d.dismiss()
                }
                .setNeutralButton(R.string.closed_test_sdk_roster_contact_never) { d, _ ->
                    activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_NEVER, true)
                        .apply()
                    pending = false
                    d.dismiss()
                }
                .create()

        dlg.setOnShowListener {
            markPrompted(activity)
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val normalized = TelegramUsername.normalize(input.text?.toString().orEmpty())
                if (normalized == null) {
                    Toast.makeText(
                        activity,
                        R.string.closed_test_sdk_roster_contact_invalid,
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@setOnClickListener
                }
                val handler = submitHandler
                if (handler == null) {
                    dlg.dismiss()
                    return@setOnClickListener
                }
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                handler(normalized) { result ->
                    activity.runOnUiThread {
                        dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        result.fold(
                            onSuccess = {
                                activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(KEY_SUBMITTED, true)
                                    .apply()
                                pending = false
                                Toast.makeText(
                                    activity,
                                    R.string.closed_test_sdk_roster_contact_saved,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                dlg.dismiss()
                            },
                            onFailure = {
                                Toast.makeText(
                                    activity,
                                    R.string.closed_test_sdk_roster_contact_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }
                }
            }
        }
        dlg.setOnDismissListener {
            dialogShowing = false
        }
        dlg.show()
    }

    private fun markPrompted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROMPTED, true)
            .apply()
    }
}
