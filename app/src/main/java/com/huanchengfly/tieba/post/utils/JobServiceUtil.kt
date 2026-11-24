package com.huanchengfly.tieba.post.utils

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import com.huanchengfly.tieba.post.services.NotifyJobService

object JobServiceUtil {
    private const val TAG = "JobServiceUtil"
    private const val SP_NAME = "appData"
    private const val KEY_JOB_ID = "jobId"
    const val DEFAULT_PERIODIC_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes

    @JvmStatic
    fun getJobId(context: Context?): Int {
        if (context == null) return (Math.random() * 100000).toInt()
        return runCatching {
            val sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            var jobId = sharedPreferences.getInt(KEY_JOB_ID, -1)
            if (jobId == -1) {
                jobId = (Math.random() * 100000).toInt()
                sharedPreferences.edit()
                    .putInt(KEY_JOB_ID, jobId)
                    .apply()
            }
            jobId
        }.getOrElse {
            (Math.random() * 100000).toInt()
        }
    }

    @JvmStatic
    fun isJobScheduled(context: Context?, jobId: Int? = null): Boolean {
        if (context == null) return false
        val targetJobId = jobId ?: getJobId(context)
        return runCatching {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                jobScheduler.getPendingJob(targetJobId) != null
            } else {
                jobScheduler.allPendingJobs.any { it.id == targetJobId }
            }
        }.getOrDefault(false)
    }

    @JvmStatic
    @JvmOverloads
    @WorkerThread
    fun scheduleNotifyJob(
        context: Context?,
        force: Boolean = false,
        intervalMillis: Long = DEFAULT_PERIODIC_INTERVAL_MS
    ): Boolean {
        if (context == null) return false
        return runCatching {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return false
            val jobId = getJobId(context)

            if (!force && isJobScheduled(context, jobId)) {
                Log.d(TAG, "NotifyJob is already scheduled (jobId=$jobId). Skipping redundant IPC.")
                return true
            }

            val builder = JobInfo.Builder(
                jobId,
                ComponentName(context, NotifyJobService::class.java)
            )
                .setPersisted(true)
                .setPeriodic(intervalMillis)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setRequiresBatteryNotLow(true)
            }

            val result = jobScheduler.schedule(builder.build())
            val success = result == JobScheduler.RESULT_SUCCESS
            Log.d(TAG, "Scheduled NotifyJob (jobId=$jobId, success=$success)")
            success
        }.getOrElse { e ->
            Log.e(TAG, "Failed to schedule NotifyJob", e)
            false
        }
    }

    @JvmStatic
    fun cancelNotifyJob(context: Context?): Boolean {
        if (context == null) return false
        return runCatching {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return false
            val jobId = getJobId(context)
            jobScheduler.cancel(jobId)
            Log.d(TAG, "Cancelled NotifyJob (jobId=$jobId)")
            true
        }.getOrDefault(false)
    }
}
