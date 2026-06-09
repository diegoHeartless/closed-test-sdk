package io.closedtest.sdk.internal.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.closedtest.sdk.R

internal object LocalDailyReminderNotifier {
    const val CHANNEL_ID = "closed_test_daily_reminder"
    private const val NOTIFICATION_ID = 40_001

    fun showIfAllowed(context: Context) {
        if (!notificationsAllowed(context)) return
        ensureChannel(context)
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending =
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(resolveSmallIcon(context))
                .setContentTitle(context.getString(R.string.closed_test_sdk_daily_reminder_title))
                .setContentText(context.getString(R.string.closed_test_sdk_daily_reminder_body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun notificationsAllowed(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.closed_test_sdk_daily_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.closed_test_sdk_daily_reminder_channel_desc)
            }
        nm.createNotificationChannel(channel)
    }

    private fun resolveSmallIcon(context: Context): Int {
        val appInfo = context.applicationInfo
        return if (appInfo.icon != 0) appInfo.icon else android.R.drawable.ic_dialog_info
    }
}
