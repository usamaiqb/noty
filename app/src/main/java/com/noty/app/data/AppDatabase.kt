package com.noty.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Every released version's schema is committed under app/schemas. v1.0.0 shipped at
// version 5, so there are no migrations from earlier versions: no user has them.
@Database(
    entities = [Note::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 6, to = 7, spec = AppDatabase.Migration6To7::class)]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    /**
     * timestamp becomes createdAt, and updatedAt is added. Room rebuilds the table for the
     * rename (SQLite's RENAME COLUMN needs API 30). Existing notes count as last edited when
     * they were created, which is the best the old data can say.
     */
    @RenameColumn(tableName = "notes", fromColumnName = "timestamp", toColumnName = "createdAt")
    class Migration6To7 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE notes SET updatedAt = createdAt")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER")
            }
        }

        /**
         * Adds every hand-written migration. Auto-migrations come from the @Database
         * annotation. The migration tests use this too.
         */
        fun RoomDatabase.Builder<AppDatabase>.addAllMigrations() = addMigrations(MIGRATION_5_6)

        // No destructive fallback, in debug builds either: a missing or broken migration
        // should fail loudly in development, not wipe the database and hide the bug.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noty_database"
                )
                    .addAllMigrations()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
