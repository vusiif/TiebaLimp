package com.huanchengfly.tieba.post.utils

import android.app.ActivityManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litepal.LitePal

object AppDatabaseManager {
    private const val TAG = "AppDatabaseManager"
    const val SP_NAME = "database_optimizer"
    const val KEY_LAST_OPTIMIZED_VERSION = "last_optimized_db_version"
    const val CURRENT_DB_VERSION = 41 // Matches litepal.xml version 41

    enum class CheckpointMode {
        PASSIVE,
        FULL,
        RESTART,
        TRUNCATE
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isInitialized = false

    @Volatile
    private var pendingCheckpointJob: Job? = null

    private inline fun logDebug(message: () -> String) {
        runCatching { Log.d(TAG, message()) }
    }

    private inline fun logWarn(throwable: Throwable? = null, message: () -> String) {
        runCatching {
            if (throwable != null) Log.w(TAG, message(), throwable)
            else Log.w(TAG, message())
        }
    }

    private inline fun logError(throwable: Throwable? = null, message: () -> String) {
        runCatching {
            if (throwable != null) Log.e(TAG, message(), throwable)
            else Log.e(TAG, message())
        }
    }

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
        DatabaseMaintenanceCoordinator.runMaintenanceIfDueAsync(context)
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
                configurePragmas(db, context)
                ensureIndexes(context, db)
                optimize(db)
                isInitialized = true
                logDebug { "Database optimization and indexing completed successfully." }
            }.onFailure { e ->
                logError(e) { "Failed to optimize database" }
            }
        }
    }

    /**
     * Enable WAL (Write-Ahead Logging), high-performance in-memory temp store,
     * busy timeout, and memory-mapped I/O.
     */
    @JvmStatic
    @JvmOverloads
    fun configurePragmas(db: SQLiteDatabase, context: Context? = null) {
        runCatching {
            // Enable WAL mode: readers never block writers, writers never block readers
            if (!db.isWriteAheadLoggingEnabled) {
                val walEnabled = db.enableWriteAheadLogging()
                logDebug { "enableWriteAheadLogging result: $walEnabled" }
            }

            // In WAL mode, synchronous = NORMAL is crash-safe and avoids flushing to disk on every single transaction
            db.execSQL("PRAGMA synchronous = NORMAL;")

            // Store temporary tables and sorting B-trees in memory instead of disk
            db.execSQL("PRAGMA temp_store = MEMORY;")

            // 2MB page cache (negative number means KiB in SQLite)
            db.execSQL("PRAGMA cache_size = -2000;")

            // Avoid transient SQLiteDatabaseLockedException on concurrent background writes
            db.execSQL("PRAGMA busy_timeout = 3000;")

            // Standardize auto-checkpoint page count
            db.execSQL("PRAGMA wal_autocheckpoint = 1000;")

            // Memory-mapped I/O (up to 64MB on normal devices; disabled on low-RAM devices)
            val isLowRam = context?.let { ctx ->
                runCatching {
                    (ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice
                }.getOrNull()
            } == true
            val mmapSize = if (isLowRam) 0L else 64L * 1024L * 1024L
            db.execSQL("PRAGMA mmap_size = $mmapSize;")
        }.onFailure { e ->
            logError(e) { "Failed to configure database pragmas" }
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
                    logWarn(e) { "Failed to execute index statement: $sql" }
                }
            }

            sp.edit().putInt(KEY_LAST_OPTIMIZED_VERSION, CURRENT_DB_VERSION).apply()
            logDebug { "All secondary indexes created/verified for DB version $CURRENT_DB_VERSION." }
        }.onFailure { e ->
            logError(e) { "Failed to ensure indexes" }
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
            logWarn(e) { "Failed to run PRAGMA optimize" }
        }
    }

    /**
     * Explicit WAL checkpoint to flush log pages back to the database file.
     * When [mode] is [CheckpointMode.TRUNCATE], the WAL file is shrunk to 0 bytes.
     */
    @WorkerThread
    @JvmStatic
    @JvmOverloads
    fun checkpoint(db: SQLiteDatabase? = null, mode: CheckpointMode = CheckpointMode.PASSIVE) {
        runCatching {
            val targetDb = db ?: runCatching { LitePal.getDatabase() }.getOrNull() ?: return
            if (targetDb.isWriteAheadLoggingEnabled) {
                targetDb.rawQuery("PRAGMA wal_checkpoint(${mode.name});", null).use { cursor ->
                    cursor.moveToFirst()
                }
                logDebug { "WAL checkpoint(${mode.name}) completed." }
            }
        }.onFailure { e ->
            logError(e) { "Failed to perform WAL checkpoint(${mode.name})" }
        }
    }

    /**
     * Asynchronously schedules a debounced WAL checkpoint on Dispatchers.IO.
     * Consolidates burst write checkpoints into a single low-overhead background operation.
     */
    @JvmStatic
    @JvmOverloads
    fun scheduleCheckpoint(delayMillis: Long = 5000L, mode: CheckpointMode = CheckpointMode.PASSIVE) {
        synchronized(this) {
            pendingCheckpointJob?.cancel()
            pendingCheckpointJob = scope.launch {
                delay(delayMillis)
                checkpoint(mode = mode)
            }
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
