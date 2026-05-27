package com.knockit.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android equivalent of the iOS PrayerService.
 * Fetches Islamic prayer times from api.aladhan.com.
 *
 * Uses [HttpURLConnection] — no extra dependency required.
 */
object PrayerService {

    /** Calculation method 2 = Islamic Society of North America (ISNA). Adjust as needed. */
    private const val METHOD = 2

    /**
     * Fetch today's prayer times for the given GPS coordinates.
     *
     * @param latitude  Device latitude.
     * @param longitude Device longitude.
     * @return [PrayerTimings] with five named prayer times in "HH:mm" format.
     * @throws PrayerServiceException on network or parse failures.
     */
    @Throws(PrayerServiceException::class)
    suspend fun fetchPrayerTimes(latitude: Double, longitude: Double): PrayerTimings =
        withContext(Dispatchers.IO) {

            val urlString =
                "https://api.aladhan.com/v1/timings" +
                        "?latitude=$latitude&longitude=$longitude&method=$METHOD"

            val raw = get(urlString)
            parseTimings(raw)
        }

    // ─────────────────────── HTTP ──────────────────────────────────

    private fun get(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream
            else connection.errorStream

            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                .use { it.readText() }

            if (responseCode !in 200..299) {
                throw PrayerServiceException("HTTP $responseCode: $body")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    // ─────────────────────── Parser ────────────────────────────────

    private fun parseTimings(raw: String): PrayerTimings {
        return try {
            val timings = JSONObject(raw)
                .getJSONObject("data")
                .getJSONObject("timings")

            // Strip any suffix like " (PST)" — keep only "HH:mm"
            fun clean(raw: String) = raw.split(" ").first().trim()

            PrayerTimings(
                fajr = clean(timings.getString("Fajr")),
                dhuhr = clean(timings.getString("Dhuhr")),
                asr = clean(timings.getString("Asr")),
                maghrib = clean(timings.getString("Maghrib")),
                isha = clean(timings.getString("Isha"))
            )
        } catch (e: PrayerServiceException) {
            throw e
        } catch (e: Exception) {
            throw PrayerServiceException("Failed to parse prayer timings: ${e.message}", e)
        }
    }
}

// ──────────────────────────── Data classes ─────────────────────────────────

/**
 * The five obligatory Islamic prayer times for a given day and location.
 * Each time is a "HH:mm" string in local time.
 */
data class PrayerTimings(
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
) {
    /** Ordered list of all five prayers, suitable for display or scheduling. */
    val allPrayers: List<Prayer>
        get() = listOf(
            Prayer("Fajr", fajr),
            Prayer("Dhuhr", dhuhr),
            Prayer("Asr", asr),
            Prayer("Maghrib", maghrib),
            Prayer("Isha", isha)
        )
}

data class Prayer(val name: String, val time: String)

class PrayerServiceException(message: String, cause: Throwable? = null) : Exception(message, cause)
