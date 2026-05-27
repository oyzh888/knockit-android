package com.knockit.app.data.model

/**
 * Mirrors the iOS ReminderType enum.
 *
 * [rawValue]              – stored in the DB (Reminder.type column)
 * [displayName]           – human-readable label shown in UI
 * [iconName]              – Material Icons name (use with Icons.Default / Icons.Rounded)
 * [defaultTitle]          – pre-filled title when creating a reminder of this type
 * [defaultIntervalMinutes]– suggested interval for interval-based types; null for one-shot types
 * [defaultRepeatRule]     – suggested repeat cadence for this type
 */
enum class ReminderType(
    val rawValue: String,
    val displayName: String,
    val iconName: String,
    val defaultTitle: String,
    val defaultIntervalMinutes: Int?,
    val defaultRepeatRule: String,
) {
    CUSTOM(
        rawValue = "custom",
        displayName = "Custom",
        iconName = "Notifications",
        defaultTitle = "Reminder",
        defaultIntervalMinutes = null,
        defaultRepeatRule = RepeatRule.NONE,
    ),
    BABY(
        rawValue = "baby",
        displayName = "Baby",
        iconName = "ChildCare",
        defaultTitle = "Baby Care",
        defaultIntervalMinutes = 180,   // every 3 hours
        defaultRepeatRule = RepeatRule.NONE,
    ),
    PRAYER(
        rawValue = "prayer",
        displayName = "Prayer",
        iconName = "SelfImprovement",
        defaultTitle = "Prayer Time",
        defaultIntervalMinutes = null,
        defaultRepeatRule = RepeatRule.DAILY,
    ),
    MEDICINE(
        rawValue = "medicine",
        displayName = "Medicine",
        iconName = "Medication",
        defaultTitle = "Take Medicine",
        defaultIntervalMinutes = null,
        defaultRepeatRule = RepeatRule.DAILY,
    ),
    WATER(
        rawValue = "water",
        displayName = "Water",
        iconName = "WaterDrop",
        defaultTitle = "Drink Water",
        defaultIntervalMinutes = 60,    // every hour
        defaultRepeatRule = RepeatRule.NONE,
    ),
    EXERCISE(
        rawValue = "exercise",
        displayName = "Exercise",
        iconName = "FitnessCenter",
        defaultTitle = "Time to Exercise",
        defaultIntervalMinutes = null,
        defaultRepeatRule = RepeatRule.DAILY,
    ),
    SLEEP(
        rawValue = "sleep",
        displayName = "Sleep",
        iconName = "Bedtime",
        defaultTitle = "Bedtime",
        defaultIntervalMinutes = null,
        defaultRepeatRule = RepeatRule.DAILY,
    );

    companion object {
        /** Convert a raw DB string back to the enum; defaults to [CUSTOM] if unknown. */
        fun fromRawValue(raw: String): ReminderType =
            entries.firstOrNull { it.rawValue == raw } ?: CUSTOM
    }
}
