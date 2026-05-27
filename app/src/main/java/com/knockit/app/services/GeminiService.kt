package com.knockit.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android equivalent of the iOS GeminiService.
 * Calls Gemini 2.0 Flash to parse a natural-language reminder string
 * into a list of [ParsedReminder] objects.
 *
 * Uses [HttpURLConnection] — no extra networking dependency required.
 */
object GeminiService {

    private const val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    // ─────────────────────── Public API ────────────────────────────

    /**
     * Parse [input] into a list of reminders.
     *
     * @param input   Natural-language reminder text from the user.
     * @param apiKey  Gemini API key (default: [ApiKeys.GEMINI_API_KEY]).
     * @throws GeminiException on network or parse errors.
     */
    @Throws(GeminiException::class)
    suspend fun parseReminder(
        input: String,
        apiKey: String = ApiKeys.GEMINI_API_KEY
    ): List<ParsedReminder> = withContext(Dispatchers.IO) {

        val prompt = buildPrompt(input)
        val requestJson = buildRequestJson(prompt)
        val rawResponse = post(BASE_URL, apiKey, requestJson)
        parseResponse(rawResponse)
    }

    // ─────────────────────── Prompt builder ────────────────────────

    private fun buildPrompt(userInput: String): String {
        val now = Date()
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        val dayFmt = SimpleDateFormat("EEEE", Locale.US)

        return """
You are a reminder parsing assistant. Parse the user's natural language input into structured reminder data.
Current time: ${isoFmt.format(now)}
Current day: ${dayFmt.format(now)}
User input: "$userInput"

Return a JSON array of reminder objects. Each object must have:
- "title": string — short, clear reminder title
- "trigger_at": string — ISO 8601 datetime (include timezone offset, e.g. "2025-06-01T09:00:00+08:00")
- "repeat_rule": string — one of "none", "daily", "weekly"
- "interval_minutes": integer or null — minutes between repeats (null if not interval-based)
- "type": string — one of "custom", "prayer", "medicine", "water", "baby", "exercise", "sleep"

Rules:
- If the user says "every X minutes/hours", set interval_minutes and repeat_rule = "none".
- If the user says "every day at X", set repeat_rule = "daily" and interval_minutes = null.
- If the user says "every week / every Monday", set repeat_rule = "weekly".
- For a one-time reminder, set repeat_rule = "none" and interval_minutes = null.
- Infer the type from context (e.g. "drink water" → "water", "take medicine" → "medicine").
- If multiple reminders are implied, return multiple objects.
- Always return valid JSON — no markdown fences, no explanatory text.
        """.trimIndent()
    }

    // ─────────────────────── Request builder ───────────────────────

    private fun buildRequestJson(prompt: String): String {
        val schema = JSONObject().apply {
            put("type", "ARRAY")
            put("items", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply { put("type", "STRING") })
                    put("trigger_at", JSONObject().apply { put("type", "STRING") })
                    put("repeat_rule", JSONObject().apply { put("type", "STRING") })
                    put("interval_minutes", JSONObject().apply { put("type", "INTEGER") })
                    put("type", JSONObject().apply { put("type", "STRING") })
                })
            })
        }

        val generationConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
            put("responseSchema", schema)
        }

        return JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(
                        JSONObject().apply { put("text", prompt) }
                    ))
                }
            ))
            put("generationConfig", generationConfig)
        }.toString()
    }

    // ─────────────────────── HTTP ──────────────────────────────────

    private fun post(urlString: String, apiKey: String, body: String): String {
        val url = URL("$urlString?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream
            else connection.errorStream

            val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                .use { it.readText() }

            if (responseCode !in 200..299) {
                throw GeminiException("HTTP $responseCode: $response")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    // ─────────────────────── Response parser ───────────────────────

    private fun parseResponse(raw: String): List<ParsedReminder> {
        return try {
            val root = JSONObject(raw)
            // Gemini wraps the response in candidates[0].content.parts[0].text
            val text = root
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val array = JSONArray(text)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ParsedReminder(
                    title = obj.getString("title"),
                    triggerAt = obj.optString("trigger_at").takeIf { it.isNotBlank() }
                        ?: throw GeminiException("Missing trigger_at in item $i"),
                    repeatRule = obj.optString("repeat_rule", "none").takeIf { it.isNotBlank() },
                    intervalMinutes = if (obj.isNull("interval_minutes")) null
                    else obj.optInt("interval_minutes", 0).takeIf { it > 0 },
                    type = obj.optString("type", "custom").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: GeminiException) {
            throw e
        } catch (e: Exception) {
            throw GeminiException("Failed to parse Gemini response: ${e.message}", e)
        }
    }
}

// ──────────────────────────── Data classes ─────────────────────────────────

/**
 * One reminder parsed from the Gemini API response.
 * Convert to a [com.knockit.app.data.model.Reminder] via the repository layer.
 */
data class ParsedReminder(
    val title: String,
    /** ISO 8601 datetime string, e.g. "2025-06-01T14:30:00+08:00" */
    val triggerAt: String,
    /** "none" | "daily" | "weekly" | null */
    val repeatRule: String?,
    /** Interval in minutes for repeat-by-interval reminders; null otherwise. */
    val intervalMinutes: Int?,
    /** ReminderType raw value: "custom" | "prayer" | "medicine" | "water" | "baby" | "exercise" | "sleep" */
    val type: String?
)

class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)
