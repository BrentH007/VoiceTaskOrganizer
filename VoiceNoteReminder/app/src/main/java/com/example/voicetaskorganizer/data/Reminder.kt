package com.example.voicetaskorganizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val originalText: String,
    val task: String,
    val dueAt: LocalDateTime,
    val isRecurringDaily: Boolean = false,
    val completed: Boolean = false
)

