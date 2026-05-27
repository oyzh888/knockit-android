package com.knockit.app.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * [CoroutineWorker] that fires a notification for an interval-based reminder.
 *
 * Scheduled as a [androidx.work.PeriodicWorkRequest] by [NotificationService.scheduleNotification].
 *
 * Input data keys:
 *   KEY_TITLE       – reminder title
 *   KEY_REMINDER_ID – reminder UUID string
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_REMINDER_ID = "reminder_id"
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE)
            ?: return Result.failure()
        val reminderId = inputData.getString(KEY_REMINDER_ID)
            ?: return Result.failure()

        NotificationReceiver.showNotification(applicationContext, title, reminderId)
        return Result.success()
    }
}
