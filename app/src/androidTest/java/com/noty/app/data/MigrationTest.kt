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
                .query("SELECT title, reminderAt FROM notes WHERE id = 1")
                .use { c ->
                    assertTrue(c.moveToNext())
                    assertEquals("Kept", c.getString(0))
                    assertNull(c.getString(1))
                }
        } finally {
            database.close()
        }
    }
}
