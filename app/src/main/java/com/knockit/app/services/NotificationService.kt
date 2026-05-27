package com.knockit.app.services

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.knockit.app.data.model.Reminder
import com.knockit.app.data.model.RepeatRule
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Android equivalent of the iOS UNUserNotification-based NotificationService.
 *
 * Strategy:
 *  - intervalMinutes != null → [WorkManager] [PeriodicWorkRequest] (min 15 min enforced by OS)
 *  - repeatRule == "daily"   → [AlarmManager.setRepeating] every 24 h
 *  - repeatRule == "weekly"  → [AlarmManager.setRepeating] every 7 days
 *  - otherwise               → [AlarmManager.setExactAndAllowWhileIdle] one-shot
 */
object NotificationService {

    const val CHANNEL_ID = "knockit_reminders"
    private const val CHANNEL_NAME = "Reminders"

    // Request code for Android 13+ POST_NOTIFICATIONS permission
    const val REQUEST_CODE_NOTIFICATIONS = 1001

    // ─────────────────────────── Channel ───────────────────────────

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Knockit reminder notifications"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // ─────────────────────────── Permission ────────────────────────

    /**
     * On Android 13+ (API 33) request POST_NOTIFICATIONS at runtime.
     * Suspends until the user responds; returns true if granted.
     * On earlier versions always returns true (permission is implicit).
     */
    suspend fun requestPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        if (checkPermission(activity)) return true

        return suspendCancellableCoroutine { cont ->
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATIONS
            )
            // The activity must call cont.resume(granted) from onRequestPermissionsResult.
            // See NotificationPermissionCallback helper below.
            NotificationPermissionCallback.register(REQUEST_CODE_NOTIFICATIONS) { granted ->
                if (cont.isActive) cont.resume(granted)
            }
        }
    }

    fun checkPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ─────────────────────────── Schedule ──────────────────────────

    fun scheduleNotification(context: Context, reminder: Reminder) {
        when {
            reminder.intervalMinutes != null -> scheduleInterval(context, reminder)
            reminder.repeatRule == RepeatRule.DAILY -> scheduleDailyAlarm(context, reminder)
            reminder.repeatRule == RepeatRule.WEEKLY -> scheduleWeeklyAlarm(context, reminder)
            else -> scheduleOneShot(context, reminder)
        }
    }

    // ─── Interval (WorkManager) ───────────────────────────────────

    private fun scheduleInterval(context: Context, reminder: Reminder) {
        val intervalMinutes = reminder.intervalMinutes ?: return
        // WorkManager enforces a minimum of 15 minutes; clamp silently.
        val effectiveMinutes = maxOf(intervalMinutes.toLong(), 15L)

        val inputData = Data.Builder()
            .putString(ReminderWorker.KEY_TITLE, reminder.title)
            .putString(ReminderWorker.KEY_REMINDER_ID, reminder.id)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            effectiveMinutes, TimeUnit.MINUTES
        )
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            reminder.id,                          // unique tag per reminder
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    // ─── Daily (AlarmManager repeating) ──────────────────────────

    private fun scheduleDailyAlarm(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, reminder) ?: return

        // First trigger: use triggerAt; subsequent triggers every 24 hours
        val triggerAtMillis = alignToFuture(reminder.triggerAt, TimeUnit.DAYS.toMillis(1))

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    // ─── Weekly (AlarmManager repeating) ─────────────────────────

    private fun scheduleWeeklyAlarm(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, reminder) ?: return

        val weekMillis = TimeUnit.DAYS.toMillis(7)
        val triggerAtMillis = alignToFuture(reminder.triggerAt, weekMillis)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            weekMillis,
            pendingIntent
        )
    }

    // ─── One-shot (AlarmManager exact) ───────────────────────────

    private fun scheduleOneShot(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, reminder) ?: return

        if (reminder.triggerAt <= System.currentTimeMillis()) return  // already in the past

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAt,
                pendingIntent
            )
        }
    }

    // ─────────────────────────── Cancel ────────────────────────────

    fun cancelNotification(context: Context, reminderId: String) {
        // Cancel WorkManager job (interval reminders)
        WorkManager.getInstance(context).cancelUniqueWork(reminderId)

        // Cancel AlarmManager alarm (one-shot / daily / weekly)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_NO_CREATE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            flags
        )
        pendingIntent?.let { alarmManager.cancel(it) }

        // Dismiss any visible notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(reminderId.hashCode())
    }

    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
        // Note: individual AlarmManager alarms can only be cancelled by recreating
        // their PendingIntent; callers should iterate stored reminders and call
        // cancelNotification() for each, or store alarm IDs separately.
    }

    // ─────────────────────────── Helpers ───────────────────────────

    /**
     * Build the [PendingIntent] that [NotificationReceiver] listens for.
     * Returns null if the intent cannot be created (shouldn't happen in practice).
     */
    private fun buildPendingIntent(context: Context, reminder: Reminder): PendingIntent? {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TITLE, reminder.title)
            putExtra(NotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(context, reminder.id.hashCode(), intent, flags)
    }

    /**
     * If [triggerAtMillis] is already in the past, advance it by [periodMillis]
     * until it is in the future.  Prevents immediately-expired repeating alarms.
     */
    private fun alignToFuture(triggerAtMillis: Long, periodMillis: Long): Long {
        var t = triggerAtMillis
        val now = System.currentTimeMillis()
        while (t <= now) t += periodMillis
        return t
    }
}

// ─────────────────────── Permission callback bridge ──────────────────────────

/**
 * Lightweight bridge so the coroutine in [NotificationService.requestPermission] can
 * receive the result from [Activity.onRequestPermissionsResult].
 *
 * In your Activity/Fragment:
 * ```kotlin
 * override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
 *     super.onRequestPermissionsResult(requestCode, permissions, grantResults)
 *     NotificationPermissionCallback.dispatch(requestCode, grantResults)
 * }
 * ```
 */
object NotificationPermissionCallback {
    private val callbacks = mutableMapOf<Int, (Boolean) -> Unit>()

    fun register(requestCode: Int, callback: (Boolean) -> Unit) {
        callbacks[requestCode] = callback
    }

    fun dispatch(requestCode: Int, grantResults: IntArray) {
        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
        callbacks.remove(requestCode)?.invoke(granted)
    }
}
