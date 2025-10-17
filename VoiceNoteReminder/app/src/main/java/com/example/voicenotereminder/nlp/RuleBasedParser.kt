package com.example.voicenotereminder.nlp

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

/**
 * Lightweight regex/pattern-based parser for common phrases:
 *  - "remind me to <task> at 5 pm"
 *  - "remind me tomorrow at 8:30 am to <task>"
 *  - "<task> tomorrow at 7"
 *  - "in 20 minutes to <task>"
 *  - "every day at 8 am to <task>"
 *  - "on 2025-09-10 at 14:00 <task>"
 */
class RuleBasedParser : IntentParser {

    private val time12 = Regex("""\b(\d{1,2})(?::(\d{2}))?\s?(am|pm)\b""", RegexOption.IGNORE_CASE)
    private val time24 = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val inXMinutes = Regex("""\bin\s+(\d{1,3})\s+minutes?\b""", RegexOption.IGNORE_CASE)
    private val inXHours = Regex("""\bin\s+(\d{1,2})\s+hours?\b""", RegexOption.IGNORE_CASE)
    private val tomorrow = Regex("""\btomorrow\b""", RegexOption.IGNORE_CASE)
    private val today = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)
    private val everyDay = Regex("""\bevery\s+day\b""", RegexOption.IGNORE_CASE)
    private val onDate = Regex("""\bon\s+(\d{4})-(\d{2})-(\d{2})\b""", RegexOption.IGNORE_CASE)
    private val atWord = Regex("""\bat\b""", RegexOption.IGNORE_CASE)

    override fun parse(text: String): ParsedReminder? {
        val raw = text.trim()
        if (raw.isBlank()) return null
        val lower = raw.lowercase(Locale.getDefault())

        var isRecurring = everyDay.containsMatchIn(lower)

        // Defaults
        var task = raw
        var date = LocalDate.now()
        var time: LocalTime? = null
        var dt: LocalDateTime

        // Absolute date like "on 2025-09-10"
        onDate.find(lower)?.let {
            val y = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            val d = it.groupValues[3].toInt()
            date = LocalDate.of(y, m, d)
            task = removeSpan(task, it.range)
        }

        // Relative date
        if (tomorrow.containsMatchIn(lower)) {
            date = LocalDate.now().plusDays(1)
            task = task.replace(tomorrow, "", ignoreCase = true)
        } else if (today.containsMatchIn(lower)) {
            date = LocalDate.now()
            task = task.replace(today, "", ignoreCase = true)
        }

        // "in X minutes/hours"
        inXMinutes.find(lower)?.let {
            val mins = it.groupValues[1].toLong()
            dt = LocalDateTime.now().plusMinutes(mins)
            task = removeSpan(task, it.range)
            return ParsedReminder(originalText = raw, task = cleanupTask(task), dueAt = dt, isRecurringDaily = isRecurring)
        }
        inXHours.find(lower)?.let {
            val hrs = it.groupValues[1].toLong()
            dt = LocalDateTime.now().plusHours(hrs)
            task = removeSpan(task, it.range)
            return ParsedReminder(originalText = raw, task = cleanupTask(task), dueAt = dt, isRecurringDaily = isRecurring)
        }

        // Time detection: "at 5 pm", "5:30 pm", "17:45"
        var timeFoundRange: IntRange? = null

        time12.find(lower)?.let { m ->
            val hour = m.groupValues[1].toInt()
            val min = m.groupValues[2].ifBlank { "0" }.toInt()
            val ampm = m.groupValues[3].lowercase()
            var h = hour % 12
            if (ampm == "pm") h += 12
            time = LocalTime.of(h, min)
            timeFoundRange = m.range
        }

        if (time == null) {
            time24.find(lower)?.let { m ->
                val h = m.groupValues[1].toInt()
                val min = m.groupValues[2].toInt()
                time = LocalTime.of(h, min)
                timeFoundRange = m.range
            }
        }

        if (time != null) {
            // Remove "at" near the time and the time string from task
            atWord.find(lower)?.let { atm ->
                if (timeFoundRange != null && atm.range.last + 3 >= timeFoundRange!!.first) {
                    task = removeSpan(task, atm.range)
                }
            }
            timeFoundRange?.let { task = removeSpan(task, it) }
        }

        // Heuristics for "remind me to", "to <task>" etc.
        task = task.replace(Regex("""\bremind me( to)?\b""", RegexOption.IGNORE_CASE), "",)
        task = task.replace(Regex("""\bat\b\s*$""", RegexOption.IGNORE_CASE), "",)

        val finalTime = time ?: LocalTime.now().plusMinutes(2) // default 2 min from now
        dt = LocalDateTime.of(date, finalTime)

        // If the parsed time is earlier than now today, push to next day
        if (dt.isBefore(LocalDateTime.now())) {
            dt = dt.plusDays(1)
        }

        val cleaned = cleanupTask(task)
        return if (cleaned.isBlank()) null else ParsedReminder(
            originalText = raw,
            task = cleaned,
            dueAt = dt,
            isRecurringDaily = isRecurring
        )
    }

    private fun cleanupTask(task: String): String =
        task.replace(Regex("""\s{2,}"""), " ").trim().trim('.', ',', ';', ':')

    private fun removeSpan(text: String, range: IntRange): String {
        if (range.first < 0 || range.last >= text.length) return text
        return text.removeRange(range)
    }
}
