package com.knockit.app

import android.app.Application
import com.knockit.app.data.db.KnockitDatabase
import com.knockit.app.data.repository.ReminderRepository

/**
 * Application class — manual dependency graph root.
 *
 * Keeping DI manual (no Hilt / Koin) means zero extra annotation processing
 * and a fully transparent object graph. ViewModels retrieve dependencies via
 * [ViewModelProvider.Factory] supplied by the owning Activity/Fragment/NavHost.
 *
 * Usage from a ViewModel factory:
 *   val repo = (context.applicationContext as KnockitApplication).reminderRepository
 */
class KnockitApplication : Application() {

    // Lazily initialised so the DB is only created on first access, not at
    // Application.onCreate() which would slow cold-start unnecessarily.
    val database: KnockitDatabase by lazy {
        KnockitDatabase.getInstance(this)
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Future: initialise crash reporting, analytics, etc.
    }
}
