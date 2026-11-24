package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.litepal.LitePal

object AppDatabaseManager {
    private const val TAG = "AppDatabaseManager"
    const val SP_NAME = "database_optimizer"
    const val KEY_LAST_OPTIMIZED_VERSION = "last_optimized_db_version"
    const val CURRENT_DB_VERSION = 41 // Matches litepal.xml version 41

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isInitialized = false

    /**
     * All index statements required to optimize SQLite queries across LitePal models.
     */
    val indexStatements: List<String> = listOf(
        // History: order by timestamp desc, count desc / where type = ? / where data = ? and type = ?
        "CREATE INDEX IF NOT EXISTS idx_history_type_time ON history(type, timestamp DESC, count DESC);",
        "CREATE INDEX IF NOT EXISTS idx_history_data_type ON history(data, type);",

        // Draft: where hash = ?
        "CREATE INDEX IF NOT EXISTS idx_draft_hash ON draft(hash);",

        // FloorBookmark: where threadId = ? order by floor asc / where threadId = ? and postId = ?
        "CREATE INDEX IF NOT EXISTS idx_floor_bookmark_thread ON floorbookmark(threadId, floor ASC);",
        "CREATE INDEX IF NOT EXISTS idx_floor_bookmark_thread_post ON floorbookmark(threadId, postId);",

        // ThreadSnapshot: where threadId = ? / order by updateTime desc
        "CREATE INDEX IF NOT EXISTS idx_thread_snapshot_thread ON threadsnapshot(threadId);",
        "CREATE INDEX IF NOT EXISTS idx_thread_snapshot_time ON threadsnapshot(updateTime DESC);",

        // SearchHistory & SearchPostHistory
        "CREATE INDEX IF NOT EXISTS idx_search_history_time ON searchhistory(timestamp DESC);",
        "CREATE INDEX IF NOT EXISTS idx_search_post_history_content ON searchposthistory(content);",
        "CREATE INDEX IF NOT EXISTS idx_search_post_history_time ON searchposthistory(timestamp DESC);",

        // Block: where category = ?
        "CREATE INDEX IF NOT EXISTS idx_block_category ON block(category);",

        // Account: where uid = ? / where name = ?
        "CREATE INDEX IF NOT EXISTS idx_account_uid ON account(uid);",
        "CREATE INDEX IF NOT EXISTS idx_account_name ON account(name);",

        // TopForum: where forumId = ?
        "CREATE INDEX IF NOT EXISTS idx_top_forum_forumId ON topforum(forumId);"
    )

    /**
     * Asynchronously warm up database, enable WAL, and build secondary indexes.
     */
    @JvmStatic
    fun initAsync(context: Context) {
        scope.launch {
            init(context)
        }
    }

    /**
     * Synchronously optimize SQLite database configuration on worker thread.
     */
    @WorkerThread
    @JvmStatic
    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            runCatching {
                val db = LitePal.getDatabase() ?: return
                configurePragmas(db)
                ensureIndexes(context, db)
                optimize(db)
                isInitialized = true
                Log.d(TAG, "Database optimization and indexing completed successfully.")
            }.onFailure { e ->
                Log.e(TAG, "Failed to optimize database", e)
            }
        }
    }

    /**
     * Enable WAL (Write-Ahead Logging) and high-performance in-memory temp store.
     */
    @JvmStatic
    fun configurePragmas(db: SQLiteDatabase) {
        runCatching {
            // Enable WAL mode: readers never block writers, writers never block readers
            if (!db.isWriteAheadLoggingEnabled) {
                val walEnabled = db.enableWriteAheadLogging()
                Log.d(TAG, "enableWriteAheadLogging result: $walEnabled")
            }

            // In WAL mode, synchronous = NORMAL is crash-safe and avoids flushing to disk on every single transaction
            db.execSQL("PRAGMA synchronous = NORMAL;")

            // Store temporary tables and sorting B-trees in memory instead of disk
            db.execSQL("PRAGMA temp_store = MEMORY;")

            // 2MB page cache (negative number means KiB in SQLite)
            db.execSQL("PRAGMA cache_size = -2000;")
        }.onFailure { e ->
            Log.e(TAG, "Failed to configure database pragmas", e)
        }
    }

    /**
     * Ensure composite and secondary indexes exist for frequent query patterns.
     */
    @JvmStatic
    fun ensureIndexes(context: Context, db: SQLiteDatabase, force: Boolean = false) {
        runCatching {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            val lastVersion = sp.getInt(KEY_LAST_OPTIMIZED_VERSION, -1)
            if (!force && lastVersion == CURRENT_DB_VERSION) {
                // Indexes are already verified for this DB version
                return
            }

            for (sql in indexStatements) {
                runCatching {
                    db.execSQL(sql)
                }.onFailure { e ->
                    Log.w(TAG, "Failed to execute index statement: $sql", e)
                }
            }

            sp.edit().putInt(KEY_LAST_OPTIMIZED_VERSION, CURRENT_DB_VERSION).apply()
            Log.d(TAG, "All secondary indexes created/verified for DB version $CURRENT_DB_VERSION.")
        }.onFailure { e ->
            Log.e(TAG, "Failed to ensure indexes", e)
        }
    }

    /**
     * Run PRAGMA optimize to update query planner statistics.
     */
    @JvmStatic
    fun optimize(db: SQLiteDatabase) {
        runCatching {
            db.execSQL("PRAGMA optimize;")
        }.onFailure { e ->
            Log.w(TAG, "Failed to run PRAGMA optimize", e)
        }
    }

    /**
     * Explicit WAL checkpoint to flush log pages back to the database file if needed.
     */
    @WorkerThread
    @JvmStatic
    fun checkpoint(db: SQLiteDatabase? = null) {
        runCatching {
            val targetDb = db ?: runCatching { LitePal.getDatabase() }.getOrNull() ?: return
            if (targetDb.isWriteAheadLoggingEnabled) {
                targetDb.rawQuery("PRAGMA wal_checkpoint(PASSIVE);", null).use { cursor ->
                    cursor.moveToFirst()
                }
                Log.d(TAG, "WAL checkpoint completed.")
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to perform WAL checkpoint", e)
        }
    }

    /**
     * Check whether WAL mode is active on the database.
     */
    @JvmStatic
    fun isWalEnabled(db: SQLiteDatabase? = null): Boolean {
        return runCatching {
            val targetDb = db ?: runCatching { LitePal.getDatabase() }.getOrNull() ?: return false
            targetDb.isWriteAheadLoggingEnabled
        }.getOrDefault(false)
    }
}
