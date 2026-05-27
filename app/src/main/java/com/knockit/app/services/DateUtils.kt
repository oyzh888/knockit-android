package com.knockit.app.services

import android.content.Context
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    /**
     * Parse an ISO 8601 string (e.g. "2025-06-01T14:30:00Z" or with offset)
     * into epoch milliseconds.
     *
     * Falls back to legacy [SimpleDateFormat] on API < 26.
     */
    fun parseIso8601ToMillis(isoString: String): Long {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Handle strings that may lack an offset (treat as local time)
                val normalized = when {
                    isoString.endsWith("Z") -> isoString
                    isoString.length == 19 -> isoString + "Z" // "yyyy-MM-ddTHH:mm:ss" → UTC
                    else -> isoString
                }
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val temporal = formatter.parseBest(
                    normalized,
                    ZonedDateTime::from,
                    java.time.temporal.TemporalQuery { t -> LocalDateTime.from(t).atZone(ZoneId.systemDefault()) }
                )
                Instant.from(temporal).toEpochMilli()
            } else {
                legacyParse(isoString)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot parse ISO 8601 string: $isoString", e)
        }
    }

    private fun legacyParse(isoString: String): Long {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                    if (fmt.contains("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(isoString)!!.time
            } catch (_: Exception) {
            }
        }
        throw IllegalArgumentException("Unrecognized ISO 8601 format: $isoString")
    }

    /**
     * Format epoch milliseconds into a human-readable string:
     *  - Today   → "今天 14:30"
     *  - Tomorrow → "明天 08:00"
     *  - Within 7 days → "周一 09:00"
     *  - Otherwise → "6月1日 14:30"
     */
    fun millisToDisplayString(millis: Long, context: Context): String {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFmt.format(Date(millis))

        val targetCal = Calendar.getInstance().apply { timeInMillis = millis }
        val nowCal = Calendar.getInstance()

        val todayYear = nowCal.get(Calendar.YEAR)
        val todayDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val targetYear = targetCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)

        return when {
            targetYear == todayYear && targetDay == todayDay ->
                "今天 $timeStr"

            targetYear == todayYear && targetDay == todayDay + 1 ->
                "明天 $timeStr"

            targetYear == todayYear && targetDay > todayDay && targetDay <= todayDay + 6 -> {
                val dayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                val dayName = dayNames[targetCal.get(Calendar.DAY_OF_WEEK) - 1]
                "$dayName $timeStr"
            }

            else -> {
                val dateFmt = SimpleDateFormat("M月d日", Locale.getDefault())
                "${dateFmt.format(Date(millis))} $timeStr"
            }
        }
    }
}
