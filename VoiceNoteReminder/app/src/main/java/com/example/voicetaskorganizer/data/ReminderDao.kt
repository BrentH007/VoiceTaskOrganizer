package com.example.voicetaskorganizer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY dueAt ASC")
    fun getAll(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Query("UPDATE reminders SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Reminder?

    @Query("SELECT * FROM reminders")
    suspend fun listAllOnce(): List<Reminder>
}
