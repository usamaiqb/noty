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
import java.util.UUID

// Every released version's schema is committed under app/schemas. v1.0.0 shipped at
// version 5, so there are no migrations from earlier versions: no user has them.
@Database(
    entities = [Note::class],
    version = 8,
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
         * Gives every note a uuid. Hand-written rather than an auto-migration because the
         * unique index can only be created once each row has its own value, and Room's
         * auto-migration creates indices before onPostMigrate runs. The uuids come from
         * Kotlin rather than SQL, so they are proper random (version 4) UUIDs.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.query("SELECT id FROM notes").use { cursor ->
                    while (cursor.moveToNext()) {
                        db.execSQL(
                            "UPDATE notes SET uuid = ? WHERE id = ?",
                            arrayOf<Any>(UUID.randomUUID().toString(), cursor.getLong(0))
                        )
                    }
                }
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notes_uuid` ON `notes` (`uuid`)")
            }
        }

        /**
         * Adds every hand-written migration. Auto-migrations come from the @Database
         * annotation. The migration tests use this too.
         */
        fun RoomDatabase.Builder<AppDatabase>.addAllMigrations() = addMigrations(MIGRATION_5_6, MIGRATION_7_8)

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
