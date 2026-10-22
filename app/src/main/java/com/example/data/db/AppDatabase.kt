package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DailyHealthLog
import com.example.data.model.DailyJournalEntry
import com.example.data.model.FixedScheduleItem
import com.example.data.model.TodoTask

@Database(
    entities = [
        TodoTask::class,
        DailyHealthLog::class,
        DailyJournalEntry::class,
        FixedScheduleItem::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun healthDao(): HealthDao
    abstract fun journalDao(): JournalDao
    abstract fun fixedScheduleDao(): FixedScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_tasks ADD COLUMN subTasks TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_tasks ADD COLUMN postponeCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todo_tasks ADD COLUMN scheduledSlotStart TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todo_tasks ADD COLUMN scheduledSlotEnd TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todo_tasks ADD COLUMN reminderMinutesBefore INTEGER NOT NULL DEFAULT 15")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fixed_schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        category TEXT NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_secretary_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
