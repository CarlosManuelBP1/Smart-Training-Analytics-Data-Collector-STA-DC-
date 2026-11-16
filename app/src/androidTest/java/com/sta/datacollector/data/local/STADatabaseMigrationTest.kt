package com.sta.datacollector.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class STADatabaseMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = STADatabase::class.java,
    )

    @After
    fun deleteDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationFrom1To2PreservesParticipantsAndCreatesSessionTables() {
        migrationHelper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO participants (
                    id, nickname, heightCm, weightKg, experienceLevel,
                    isActive, createdAt, updatedAt
                ) VALUES (
                    'legacy-participant', 'Legado', 180.0, 82.5, 'advanced',
                    1, 1000, 1000
                )
                """.trimIndent()
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            MIGRATION_1_2,
        ).use { migratedDatabase ->
            migratedDatabase.query(
                "SELECT id, nickname, heightCm, weightKg, experienceLevel, " +
                    "isActive, createdAt, updatedAt FROM participants"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-participant", cursor.getString(0))
                assertEquals("Legado", cursor.getString(1))
                assertEquals(180f, cursor.getFloat(2))
                assertEquals(82.5f, cursor.getFloat(3))
                assertEquals("advanced", cursor.getString(4))
                assertEquals(1, cursor.getInt(5))
                assertEquals(1_000L, cursor.getLong(6))
                assertEquals(1_000L, cursor.getLong(7))
            }

            val expectedTables = setOf(
                "participants",
                "training_sessions",
                "session_participants",
                "exercise_sets",
            )
            migratedDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            ).use { cursor ->
                val tableNames = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
                assertTrue(tableNames.containsAll(expectedTables))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "sta-migration-test"
    }
}
