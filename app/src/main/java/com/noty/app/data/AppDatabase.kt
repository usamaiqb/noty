package com.noty.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isSticky INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Rename isSticky → isPinned. SQLite RENAME COLUMN requires API 30+,
        // so we use the table-copy pattern to support minSdk 26.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE notes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        type TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isPinned INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO notes_new SELECT id, title, description, type, timestamp, isSticky FROM notes")
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noty_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                // Only use destructive migration in debug builds to prevent user data loss
                val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebuggable) {
                    builder.fallbackToDestructiveMigration(true)
                }
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
