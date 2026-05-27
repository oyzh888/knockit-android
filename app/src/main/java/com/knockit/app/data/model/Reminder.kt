package com.knockit.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for a Knockit reminder.
 *
 * Field mapping vs. iOS version:
 *  id              → UUID string (matches iOS id: UUID)
 *  title           → String
 *  triggerAt       → Long epoch-milliseconds (iOS: Date)
 *  repeatRule      → "none" | "daily" | "weekly" (iOS: RepeatRule enum)
 *  intervalMinutes → Int? nullable (iOS: intervalMinutes: Int?)
 *  createdAt       → Long epoch-milliseconds (iOS: Date)
 *  isActive        → Boolean
 *  type            → raw string value of ReminderType (iOS: ReminderType enum)
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    /** Epoch milliseconds of the next (or first) trigger time. */
    val triggerAt: Long,
    /**
     * Repeat cadence:
     *  "none"   – one-shot reminder
     *  "daily"  – repeat every day at the same time
     *  "weekly" – repeat every week on the same weekday
     */
    val repeatRule: String = RepeatRule.NONE,
    /** Interval-based repeat in minutes (e.g. every 30 minutes for water reminders). */
    val intervalMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    /**
     * One of the ReminderType raw values:
     * "custom" | "prayer" | "medicine" | "water" | "baby" | "exercise" | "sleep"
     */
    val type: String = ReminderType.CUSTOM.rawValue,
)

/** Constant values for [Reminder.repeatRule] — avoids stringly-typed mistakes. */
object RepeatRule {
    const val NONE = "none"
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
}
