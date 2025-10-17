package com.example.voicetaskorganizer.nlp

import android.content.Context
import com.example.voicetaskorganizer.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ParsedReminder(
    val originalText: String,
    val task: String,
    val dueAt: LocalDateTime,
    val isRecurringDaily: Boolean
) {
    fun dueDateTimeString(context: Context): String {
        val date = dueAt.toLocalDate()
        val time = dueAt.toLocalTime()
        return context.getString(
            R.string.parsed_datetime_fmt,
            date.toString(),
            time.toString()
        )
    }
}
