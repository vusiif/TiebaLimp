package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litepal.LitePal
import java.util.concurrent.TimeUnit

/**
 * Pure retention policy and SQL builder for periodic SQLite database maintenance.
 */
object DatabaseMaintenancePolicy {
    const val KEY_LAST_MAINTENANCE_TIME = "last_maintenance_timestamp"
    val MAINTENANCE_INTERVAL_MS = TimeUnit.HOURS.toMillis(24)

    const val DRAFT_RETENTION_DAYS = 30
    const val HISTORY_RETENTION_DAYS = 90
    const val SEARCH_HISTORY_RETENTION_DAYS = 90

    const val MAX_HISTORY_ROWS = 2000
    const val MAX_SEARCH_HISTORY_ROWS = 200

    fun isMaintenanceDue(
        lastRunMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        intervalMs: Long = MAINTENANCE_INTERVAL_MS,
    ): Boolean {
        if (lastRunMs <= 0L) return true
        return (nowMs - lastRunMs) >= intervalMs
    }

    fun computeCutoffTimestamp(nowMs: Long, retentionDays: Int): Long {
        if (retentionDays <= 0) return nowMs
        return nowMs - TimeUnit.DAYS.toMillis(retentionDays.toLong())
    }

    fun buildRowCapDeleteSql(tableName: String, maxRows: Int): String {
        val safeMax = maxRows.coerceAtLeast(1)
        return "DELETE FROM $tableName WHERE id NOT IN (SELECT id FROM $tableName ORDER BY timestamp DESC LIMIT $safeMax);"
    }
}

data class MaintenanceReport(
    val expiredDraftsDeleted: Int = 0,
    val expiredHistoryDeleted: Int = 0,
    val expiredSearchDeleted: Int = 0,
    val cappedExecuted: Boolean = false,
    val ranSuccessfully: Boolean = false,
)

/**
 * Coordinates periodic, non-blocking storage compaction and TTL pruning across LitePal tables.
 * Throttled to run at most once every 24 hours after a cold-start grace delay.
 */
object DatabaseMaintenanceCoordinator {
    private const val TAG = "DatabaseMaintenance"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @JvmStatic
    fun runMaintenanceIfDueAsync(context: Context?, initialDelayMs: Long = 3500L) {
        if (context == null) return
        val appContext = context.applicationContext
        scope.launch {
            if (initialDelayMs > 0L) {
                delay(initialDelayMs)
            }
            runMaintenanceIfDue(appContext)
        }
    }

    @JvmStatic
    suspend fun runMaintenanceIfDue(
        context: Context?,
        force: Boolean = false,
        nowMs: Long = System.currentTimeMillis(),
    ): MaintenanceReport {
        if (context == null) return MaintenanceReport()

        val sp = runCatching {
            context.getSharedPreferences(AppDatabaseManager.SP_NAME, Context.MODE_PRIVATE)
        }.getOrNull() ?: return MaintenanceReport()

        val lastRun = sp.getLong(DatabaseMaintenancePolicy.KEY_LAST_MAINTENANCE_TIME, 0L)
        if (!force && !DatabaseMaintenancePolicy.isMaintenanceDue(lastRun, nowMs)) {
            return MaintenanceReport()
        }

        val expiredDrafts = runCatching {
            DraftHelper.cleanExpiredDrafts(DatabaseMaintenancePolicy.DRAFT_RETENTION_DAYS)
        }.getOrDefault(0)

        val historyCutoff = DatabaseMaintenancePolicy.computeCutoffTimestamp(
            nowMs,
            DatabaseMaintenancePolicy.HISTORY_RETENTION_DAYS
        ).toString()
        val expiredHistory = runCatching {
            LitePal.deleteAll(History::class.java, "timestamp > 0 AND timestamp < ?", historyCutoff)
        }.getOrDefault(0)

        val searchCutoff = DatabaseMaintenancePolicy.computeCutoffTimestamp(
            nowMs,
            DatabaseMaintenancePolicy.SEARCH_HISTORY_RETENTION_DAYS
        ).toString()
        val expiredSearch = runCatching {
            val s1 = LitePal.deleteAll(SearchHistory::class.java, "timestamp > 0 AND timestamp < ?", searchCutoff)
            val s2 = LitePal.deleteAll(SearchPostHistory::class.java, "timestamp > 0 AND timestamp < ?", searchCutoff)
            s1 + s2
        }.getOrDefault(0)

        var cappedExecuted = false
        val db: SQLiteDatabase? = runCatching { LitePal.getDatabase() }.getOrNull()
        if (db != null) {
            runCatching {
                db.execSQL(
                    DatabaseMaintenancePolicy.buildRowCapDeleteSql(
                        "history",
                        DatabaseMaintenancePolicy.MAX_HISTORY_ROWS
                    )
                )
                db.execSQL(
                    DatabaseMaintenancePolicy.buildRowCapDeleteSql(
                        "searchhistory",
                        DatabaseMaintenancePolicy.MAX_SEARCH_HISTORY_ROWS
                    )
                )
                db.execSQL(
                    DatabaseMaintenancePolicy.buildRowCapDeleteSql(
                        "searchposthistory",
                        DatabaseMaintenancePolicy.MAX_SEARCH_HISTORY_ROWS
                    )
                )
                cappedExecuted = true
            }

            if (expiredDrafts + expiredHistory + expiredSearch > 0 || cappedExecuted) {
                AppDatabaseManager.optimize(db)
                AppDatabaseManager.checkpoint(db, AppDatabaseManager.CheckpointMode.TRUNCATE)
            }
        }

        sp.edit().putLong(DatabaseMaintenancePolicy.KEY_LAST_MAINTENANCE_TIME, nowMs).apply()
        val report = MaintenanceReport(
            expiredDraftsDeleted = expiredDrafts,
            expiredHistoryDeleted = expiredHistory,
            expiredSearchDeleted = expiredSearch,
            cappedExecuted = cappedExecuted,
            ranSuccessfully = true,
        )
        runCatching {
            Log.d(
                TAG,
                "Maintenance finished: drafts=$expiredDrafts, history=$expiredHistory, search=$expiredSearch, capped=$cappedExecuted"
            )
        }
        return report
    }
}
