package com.noty.app.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.noty.app.data.AppDatabase.Companion.addAllMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Runs each migration against a database built from the committed schema files in
 * app/schemas, on a real device's SQLite. Version 5 is what v1.0.0 to v1.0.3 shipped.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate5To6_keepsNotesAndAddsEmptyReminder() {
        helper.createDatabase(testDb, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO notes (id, title, description, type, timestamp, isPinned)
                VALUES (1, 'Pinned', 'Some text', 'NOTE', 1700000000000, 1),
                       (2, 'Unpinned', NULL, 'NOTE', 1700000001000, 0)
                """.trimIndent()
            )
        }

        // Also checks the migrated table against 6.json.
        val db = helper.runMigrationsAndValidate(testDb, 6, true, AppDatabase.MIGRATION_5_6)

        db.query("SELECT id, title, description, type, timestamp, isPinned, reminderAt FROM notes ORDER BY id")
            .use { c ->
                assertEquals(2, c.count)

                assertTrue(c.moveToNext())
                assertEquals(1L, c.getLong(0))
                assertEquals("Pinned", c.getString(1))
                assertEquals("Some text", c.getString(2))
                assertEquals("NOTE", c.getString(3))
                assertEquals(1700000000000L, c.getLong(4))
                assertEquals(1, c.getInt(5))
                assertTrue(c.isNull(6))

                assertTrue(c.moveToNext())
                assertEquals(2L, c.getLong(0))
                assertTrue(c.isNull(2))
                assertEquals(0, c.getInt(5))
                assertTrue(c.isNull(6))
            }
    }

    @Test
    fun migrate6To7_renamesTimestampAndBackfillsUpdatedAt() {
        helper.createDatabase(testDb, 6).use { db ->
            db.execSQL(
                """
                INSERT INTO notes (id, title, description, type, timestamp, isPinned, reminderAt)
                VALUES (1, 'With reminder', 'Some text', 'NOTE', 1700000000000, 1, 1800000000000),
                       (2, 'Plain', NULL, 'NOTE', 1700000001000, 0, NULL)
                """.trimIndent()
            )
        }

        // Auto-migrations are picked up from @Database; this also checks the table against 7.json.
        val db = helper.runMigrationsAndValidate(testDb, 7, true)

        db.query(
            "SELECT id, title, description, type, createdAt, updatedAt, isPinned, reminderAt " +
                "FROM notes ORDER BY id"
        ).use { c ->
            assertEquals(2, c.count)

            assertTrue(c.moveToNext())
            assertEquals(1L, c.getLong(0))
            assertEquals("With reminder", c.getString(1))
            assertEquals("Some text", c.getString(2))
            assertEquals("NOTE", c.getString(3))
            assertEquals(1700000000000L, c.getLong(4))
            assertEquals(1700000000000L, c.getLong(5))
            assertEquals(1, c.getInt(6))
            assertEquals(1800000000000L, c.getLong(7))

            assertTrue(c.moveToNext())
            assertEquals(2L, c.getLong(0))
            assertTrue(c.isNull(2))
            assertEquals(1700000001000L, c.getLong(4))
            assertEquals(1700000001000L, c.getLong(5))
            assertEquals(0, c.getInt(6))
            assertTrue(c.isNull(7))
        }
    }

    @Test
    fun migrate7To8_givesEveryNoteItsOwnUuid() {
        helper.createDatabase(testDb, 7).use { db ->
            db.execSQL(
                """
                INSERT INTO notes (id, title, description, type, createdAt, updatedAt, isPinned, reminderAt)
                VALUES (1, 'First', 'Text', 'NOTE', 1700000000000, 1700000005000, 1, NULL),
                       (2, 'Second', NULL, 'WORK', 1700000001000, 1700000001000, 0, 1800000000000),
                       (3, 'Third', NULL, 'NOTE', 1700000002000, 1700000002000, 0, NULL)
                """.trimIndent()
            )
        }

        // Also checks the migrated table, including the unique index, against 8.json.
        val db = helper.runMigrationsAndValidate(testDb, 8, true, AppDatabase.MIGRATION_7_8)

        db.query("SELECT id, title, updatedAt, reminderAt, uuid FROM notes ORDER BY id").use { c ->
            val uuids = mutableSetOf<String>()
            while (c.moveToNext()) {
                val uuid = c.getString(4)
                assertEquals(4, UUID.fromString(uuid).version())
                uuids += uuid
            }
            assertEquals(3, uuids.size)

            // Nothing else changed.
            assertTrue(c.moveToFirst())
            assertEquals("First", c.getString(1))
            assertEquals(1700000005000L, c.getLong(2))
            assertTrue(c.moveToNext())
            assertEquals(1800000000000L, c.getLong(3))
        }
    }

    /**
     * Opens the oldest released schema with the app's real Room configuration, so a version
     * bump without a matching migration fails here rather than on users' phones.
     */
    @Test
    fun migrateFromOldestToLatest_withAppMigrations() {
        helper.createDatabase(testDb, 5).use { db ->
            db.execSQL(
                "INSERT INTO notes (id, title, description, type, timestamp, isPinned) " +
                    "VALUES (1, 'Kept', NULL, 'NOTE', 1700000000000, 1)"
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, testDb)
            .addAllMigrations()
            .build()
        try {
            // Opening runs the migrations and Room's own schema check against the entities.
            database.openHelper.writableDatabase
                .query("SELECT title, reminderAt, createdAt, updatedAt, uuid FROM notes WHERE id = 1")
                .use { c ->
                    assertTrue(c.moveToNext())
                    assertEquals("Kept", c.getString(0))
                    assertNull(c.getString(1))
                    assertEquals(1700000000000L, c.getLong(2))
                    assertEquals(1700000000000L, c.getLong(3))
                    UUID.fromString(c.getString(4))
                }
        } finally {
            database.close()
        }
    }
}
