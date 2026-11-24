package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.AppDatabaseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDatabaseManagerTest {

    @Test
    fun testDatabaseConstants() {
        assertEquals(41, AppDatabaseManager.CURRENT_DB_VERSION)
        assertEquals("database_optimizer", AppDatabaseManager.SP_NAME)
        assertEquals("last_optimized_db_version", AppDatabaseManager.KEY_LAST_OPTIMIZED_VERSION)
    }

    @Test
    fun testIndexStatementsIntegrity() {
        val statements = AppDatabaseManager.indexStatements
        assertTrue("Index statements list should not be empty", statements.isNotEmpty())
        assertTrue("Should have at least 10 secondary indexes", statements.size >= 10)

        val expectedTables = setOf(
            "history",
            "draft",
            "floorbookmark",
            "threadsnapshot",
            "searchhistory",
            "searchposthistory",
            "block",
            "account",
            "topforum"
        )

        val targetedTables = mutableSetOf<String>()

        for (sql in statements) {
            assertTrue("Statement must start with CREATE INDEX IF NOT EXISTS: $sql",
                sql.startsWith("CREATE INDEX IF NOT EXISTS"))
            assertTrue("Statement must end with semicolon: $sql", sql.endsWith(";"))
            assertTrue("Index name must start with idx_: $sql", sql.contains("idx_"))

            for (table in expectedTables) {
                if (sql.contains("ON $table(", ignoreCase = true)) {
                    targetedTables.add(table)
                }
            }
        }

        // Verify that every critical LitePal model table has at least one secondary index
        for (table in expectedTables) {
            assertTrue("Table '$table' must be covered by index statements",
                targetedTables.contains(table))
        }
    }

    @Test
    fun testNullDatabaseSafety() {
        // Must handle null database references safely without crashing
        val walEnabled = AppDatabaseManager.isWalEnabled(null)
        assertFalse("WAL check on null db should be false", walEnabled)

        // Checkpoint on null should not throw
        AppDatabaseManager.checkpoint(null)
    }

    @Test
    fun testCheckpointModes() {
        val modes = AppDatabaseManager.CheckpointMode.values()
        assertEquals(4, modes.size)
        assertTrue(modes.any { it.name == "PASSIVE" })
        assertTrue(modes.any { it.name == "FULL" })
        assertTrue(modes.any { it.name == "RESTART" })
        assertTrue(modes.any { it.name == "TRUNCATE" })

        // Ensure all checkpoint modes handle null database gracefully
        for (mode in modes) {
            AppDatabaseManager.checkpoint(null, mode)
        }
    }

    @Test
    fun testScheduleCheckpoint() {
        // Asynchronous scheduled checkpoint must not throw on null database
        AppDatabaseManager.scheduleCheckpoint(delayMillis = 10L, mode = AppDatabaseManager.CheckpointMode.TRUNCATE)
        AppDatabaseManager.scheduleCheckpoint(delayMillis = 10L, mode = AppDatabaseManager.CheckpointMode.PASSIVE)
    }
}
