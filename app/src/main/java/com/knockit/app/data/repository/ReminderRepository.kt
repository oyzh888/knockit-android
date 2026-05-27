package com.knockit.app.data.repository

import com.knockit.app.data.db.ReminderDao
import com.knockit.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for [Reminder] data.
 *
 * The repository abstracts the Room DAO so that ViewModels (and future
 * remote data sources) never touch the database layer directly.
 */
class ReminderRepository(private val dao: ReminderDao) {

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /** Stream of all reminders, ordered newest-first. Emits on every DB change. */
    val allReminders: Flow<List<Reminder>> = dao.getAll()

    /**
     * Fetch a single reminder by its UUID.
     * @return the [Reminder] or `null` if it does not exist.
     */
    suspend fun getById(id: String): Reminder? = dao.getById(id)

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Insert a new reminder (or replace it if the same [Reminder.id] already
     * exists — upsert semantics delegated to the DAO).
     */
    suspend fun insert(reminder: Reminder) = dao.insert(reminder)

    /** Update an existing reminder matched by [Reminder.id]. */
    suspend fun update(reminder: Reminder) = dao.update(reminder)

    /** Permanently delete a reminder. */
    suspend fun delete(reminder: Reminder) = dao.delete(reminder)

    /**
     * Toggle the [Reminder.isActive] flag without touching other fields.
     * Convenience wrapper used when a user enables/disables a reminder from
     * the list screen.
     */
    suspend fun setActive(reminder: Reminder, active: Boolean) {
        dao.update(reminder.copy(isActive = active))
    }
}
