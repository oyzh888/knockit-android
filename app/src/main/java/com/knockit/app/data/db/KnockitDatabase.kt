package com.knockit.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.knockit.app.data.model.Reminder

@Database(
    entities = [Reminder::class],
    version = 1,
    exportSchema = false,
)
abstract class KnockitDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        private const val DATABASE_NAME = "knockit.db"

        @Volatile
        private var INSTANCE: KnockitDatabase? = null

        /**
         * Returns the singleton database instance, creating it on first access.
         * Thread-safe via double-checked locking.
         */
        fun getInstance(context: Context): KnockitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): KnockitDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                KnockitDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
