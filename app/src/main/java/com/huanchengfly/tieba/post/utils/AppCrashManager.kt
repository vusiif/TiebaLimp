package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.activities.CrashRecoveryActivity
import com.huanchengfly.tieba.post.ui.page.webview.WebResourceInterceptor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Enterprise crash supervisor, continuous crash-loop detector, and disaster recovery manager.
 */
object AppCrashManager : Thread.UncaughtExceptionHandler {
    private const val TAG = "AppCrashManager"
    const val SP_NAME = "crash_recovery"
    const val KEY_LAST_CRASH_TIME = "last_crash_time"
    const val KEY_CRASH_COUNT = "crash_count"
    const val KEY_IS_CRASH_LOOP = "is_crash_loop"

    // Continuous crash window: 60 seconds
    const val CRASH_LOOP_WINDOW_MS = 60_000L
    // Threshold to trigger safe mode / crash loop alert
    const val CRASH_LOOP_THRESHOLD = 2

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private val isCrashing = AtomicBoolean(false)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * Resets crash count once the app runs stably for an initial grace period.
     */
    fun markAppStartedSuccessfully(context: Context) {
        runCatching {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putInt(KEY_CRASH_COUNT, 0)
                .putBoolean(KEY_IS_CRASH_LOOP, false)
                .apply()
        }
    }

    fun isCrashLoop(context: Context): Boolean {
        return runCatching {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            sp.getBoolean(KEY_IS_CRASH_LOOP, false)
        }.getOrDefault(false)
    }

    /**
     * Pure calculation logic for crash loop detection.
     * Returns Pair(newCount, isLoop).
     */
    fun calculateCrashLoop(
        lastCrashTime: Long,
        prevCount: Int,
        currentTime: Long,
        windowMs: Long = CRASH_LOOP_WINDOW_MS,
        threshold: Int = CRASH_LOOP_THRESHOLD
    ): Pair<Int, Boolean> {
        val newCount = if (currentTime - lastCrashTime in 0 until windowMs) {
            prevCount + 1
        } else {
            1
        }
        val isLoop = newCount >= threshold
        return Pair(newCount, isLoop)
    }

    /**
     * Checks and updates crash count in SharedPreferences. Returns whether crash loop threshold was reached.
     */
    fun recordCrashAndCheckLoop(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        return runCatching {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            val lastCrashTime = sp.getLong(KEY_LAST_CRASH_TIME, 0L)
            val prevCount = sp.getInt(KEY_CRASH_COUNT, 0)

            val (newCount, isLoop) = calculateCrashLoop(lastCrashTime, prevCount, now)

            sp.edit()
                .putLong(KEY_LAST_CRASH_TIME, now)
                .putInt(KEY_CRASH_COUNT, newCount)
                .putBoolean(KEY_IS_CRASH_LOOP, isLoop)
                .commit()

            isLoop
        }.getOrDefault(false)
    }

    fun buildCrashReport(context: Context?, throwable: Throwable, isLoop: Boolean = false): String {
        val sb = StringBuilder()
        val pInfo: PackageInfo? = context?.let { ctx ->
            runCatching {
                val pm = ctx.packageManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(ctx.packageName, 0)
                }
            }.getOrNull()
        }

        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val crashTimeStr = timeFormat.format(Date())

        sb.append("========== TiebaLimp Crash Report ==========\n")
        sb.append("Time: ").append(crashTimeStr).append("\n")
        val vCode = pInfo?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode.toLong()
        } ?: 0L
        sb.append("App Version: ").append(pInfo?.versionName ?: "unknown")
            .append(" (").append(vCode).append(")\n")
        sb.append("OS Version: Android ").append(Build.VERSION.RELEASE ?: "unknown")
            .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("Device: ").append(Build.MANUFACTURER ?: "unknown").append(" ")
            .append(Build.MODEL ?: "unknown").append(" (").append(Build.PRODUCT ?: "unknown").append(")\n")
        val abis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "unknown"
        sb.append("ABIs: ").append(abis).append("\n")
        sb.append("Crash Loop Detected: ").append(if (isLoop) "YES (Multiple crashes within 60s)" else "NO").append("\n")
        sb.append("--------------------------------------------\n")
        sb.append("Exception: ").append(throwable.javaClass.name).append(": ").append(throwable.message ?: "No message").append("\n\n")

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        sb.append(sw.toString())

        return sb.toString()
    }

    fun saveCrashReportToDisk(context: Context, report: String): File? {
        return runCatching {
            val dir = File(context.filesDir, "crash_logs")
            if (!dir.exists()) dir.mkdirs()

            // Keep at most 5 historical logs
            val files = dir.listFiles()?.sortedByDescending { it.lastModified() }
            if (files != null && files.size > 5) {
                files.drop(5).forEach { runCatching { it.delete() } }
            }

            val file = File(dir, "crash_${System.currentTimeMillis()}.txt")
            file.writeText(report)

            // Also update latest_crash.txt
            val latest = File(dir, "latest_crash.txt")
            latest.writeText(report)
            file
        }.getOrNull()
    }

    /**
     * Performs a non-destructive safe recovery reset.
     * Clears disk caches, Webview caches, Sketch caches, and resets volatile states.
     * Preserves user account credentials, login cookies, and offline SQLite data.
     */
    fun performSafeReset(context: Context) {
        runCatching {
            // 1. Clear WebResourceInterceptor cache
            WebResourceInterceptor.clearCache()
        }
        runCatching {
            // 2. Clear volatile cache directories
            context.cacheDir?.deleteRecursively()
            context.codeCacheDir?.deleteRecursively()
        }
        runCatching {
            // 3. Clear WebView app cache directory
            val dataDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.dataDir
            } else {
                context.filesDir.parentFile
            }
            if (dataDir != null) {
                File(dataDir, "app_webview").deleteRecursively()
            }
        }
        runCatching {
            // 4. Reset crash count
            markAppStartedSuccessfully(context)
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!isCrashing.compareAndSet(false, true)) {
            // Secondary crash in the middle of crash handling
            defaultHandler?.uncaughtException(t, e)
            return
        }

        try {
            val app = App.INSTANCE
            val isLoop = recordCrashAndCheckLoop(app)
            val report = buildCrashReport(app, e, isLoop)
            saveCrashReportToDisk(app, report)

            // Launch CrashRecoveryActivity in :crash process
            val intent = Intent(app, CrashRecoveryActivity::class.java).apply {
                putExtra(CrashRecoveryActivity.EXTRA_CRASH_REPORT, report)
                putExtra(CrashRecoveryActivity.EXTRA_IS_CRASH_LOOP, isLoop)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            app.startActivity(intent)

            // Terminate main process cleanly
            app.removeAllActivity()
            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (fatal: Throwable) {
            Log.e(TAG, "Fatal error inside crash handler", fatal)
            defaultHandler?.uncaughtException(t, e)
        }
    }
}
