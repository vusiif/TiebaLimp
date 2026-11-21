package com.huanchengfly.tieba.post.utils

import android.app.Activity

/**
 * Manages the global stack/list of active Activities for lifecycle tracking and batch destruction.
 */
object ActivityStackManager {
    private val activityList: MutableList<Activity> = mutableListOf()

    val activities: List<Activity>
        get() = activityList

    fun addActivity(activity: Activity) {
        if (!activityList.contains(activity)) {
            activityList.add(activity)
        }
    }

    @JvmOverloads
    fun removeActivity(activity: Activity, finish: Boolean = false) {
        if (activityList.contains(activity)) {
            activityList.remove(activity)
            if (finish) activity.finish()
        }
    }

    fun removeAllActivity() {
        for (activity in activityList) {
            activity.finish()
        }
        activityList.clear()
    }
}
