package com.knockit.app.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.knockit.app.R

/**
 * BroadcastReceiver fired by [AlarmManager] for one-shot, daily, and weekly reminders.
 *
 * Intent extras:
 *   EXTRA_TITLE       – notification title text
 *   EXTRA_REMINDER_ID – UUID string of the reminder (used as notification id)
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"

        /**
         * Show a notification immediately.  Also called from [ReminderWorker].
         */
        fun showNotification(context: Context, title: String, reminderId: String) {
            val notificationId = reminderId.hashCode()

            // Tap action: open launcher activity
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT

            val contentPendingIntent = if (launchIntent != null)
                PendingIntent.getActivity(context, notificationId, launchIntent, pendingFlags)
            else null

            val notification = NotificationCompat.Builder(
                context,
                NotificationService.CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_notification)   // must exist in drawable
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notification_tap_to_open))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .apply { if (contentPendingIntent != null) setContentIntent(contentPendingIntent) }
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notificationId, notification)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        showNotification(context, title, reminderId)
    }
}
