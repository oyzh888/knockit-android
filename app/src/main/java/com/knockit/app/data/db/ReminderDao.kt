package com.knockit.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.knockit.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    /** Observe all reminders ordered by creation time (newest first). */
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Reminder>>

    /** Fetch a single reminder by its UUID string; returns null if not found. */
    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Reminder?

    /** Insert a new reminder; replaces on conflict (upsert semantics). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder)

    /** Update an existing reminder matched by primary key. */
    @Update
    suspend fun update(reminder: Reminder)

    /** Delete a reminder. */
    @Delete
    suspend fun delete(reminder: Reminder)
}
