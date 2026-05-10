package io.closedtest.sdk.internal

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.closedtest.sdk.ClosedTestOptions
import io.closedtest.sdk.R
import java.lang.ref.WeakReference

/**
 * После успешного [POST /v1/init], если в ответе есть `proofflow_test_id` и включено в [ClosedTestOptions],
 * показывает диалог с переходом по PF-TEST (`proofflow://test/{id}`). См. ProofFlow `STATS_AND_DEEPLINKS_DRAFT.md` §3.1.1.
 */
internal object ProofFlowHintPresenter {

    private const val PREFS = "io.closedtest.sdk.proof_flow_hint"
    private const val KEY_NEVER = "never"
    private const val KEY_SHOW_COUNT = "show_count"
    private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"

    private val lock = Any()

    @Volatile
    private var callbacksRegistered = false

    private var cachedOptions: ClosedTestOptions? = null

    @Volatile
    private var pendingTestId: String? = null

    @Volatile
    private var dialogShowing = false

    private var weakTopActivity: WeakReference<Activity>? = null

    fun scheduleAfterInit(application: Application, options: ClosedTestOptions, proofflowTestId: String?) {
        if (!options.proofFlowHintEnabled || proofflowTestId.isNullOrBlank()) return
        if (!shouldShowHint(application, options)) return
        if (!isProofFlowInstalled(application, options)) return

        synchronized(lock) {
            cachedOptions = options
            pendingTestId = proofflowTestId.trim()
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
                        if (pendingTestId != null && !dialogShowing && canShowOnActivity(activity)) {
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

    private fun shouldShowHint(context: Context, options: ClosedTestOptions): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(KEY_NEVER, false)) return false
        val count = p.getInt(KEY_SHOW_COUNT, 0)
        if (count >= options.proofFlowHintMaxShows) return false
        val last = p.getLong(KEY_LAST_PROMPT_MS, 0L)
        val now = System.currentTimeMillis()
        if (last > 0L && now - last < options.proofFlowHintCooldownMs) return false
        return true
    }

    private fun isProofFlowInstalled(context: Context, options: ClosedTestOptions): Boolean {
        val pm = context.packageManager
        for (pkg in options.proofFlowPackageNames) {
            val q = pkg.trim()
            if (q.isEmpty()) continue
            try {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(q, 0)
                return true
            } catch (_: Throwable) {
                continue
            }
        }
        return false
    }

    private fun tryShowDialog(activity: Activity) {
        val testId = pendingTestId ?: return
        val opts = cachedOptions ?: return
        if (dialogShowing) return
        if (!shouldShowHint(activity, opts)) {
            pendingTestId = null
            return
        }

        dialogShowing = true

        val dlg =
            AlertDialog.Builder(activity)
                .setTitle(R.string.closed_test_sdk_proof_flow_hint_title)
                .setMessage(R.string.closed_test_sdk_proof_flow_hint_message)
                .setPositiveButton(R.string.closed_test_sdk_proof_flow_hint_open) { d, _ ->
                    pendingTestId = null
                    openProofFlow(activity, testId)
                    d.dismiss()
                }
                .setNegativeButton(R.string.closed_test_sdk_proof_flow_hint_later) { d, _ ->
                    pendingTestId = null
                    d.dismiss()
                }
                .setNeutralButton(R.string.closed_test_sdk_proof_flow_hint_never) { d, _ ->
                    activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_NEVER, true).apply()
                    pendingTestId = null
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
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = p.getInt(KEY_SHOW_COUNT, 0)
        p.edit()
            .putInt(KEY_SHOW_COUNT, count + 1)
            .putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis())
            .apply()
    }

    private fun openProofFlow(activity: Activity, testId: String) {
        val id = testId.trim()
        val uri =
            Uri.Builder()
                .scheme("proofflow")
                .authority("test")
                .appendPath(id)
                .build()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            activity.startActivity(intent)
        } catch (_: Throwable) {
            // No handler — ignore (host may lack ProofFlow despite package check race).
        }
    }
}
