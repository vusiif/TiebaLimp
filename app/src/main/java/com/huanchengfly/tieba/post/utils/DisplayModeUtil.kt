package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

/**
 * Utility for detecting display refresh rate and unlocking high refresh rates (90Hz, 120Hz, 144Hz, 165Hz)
 * on supported hardware and OEM ROMs.
 */
object DisplayModeUtil {

    private const val TAG = "DisplayModeUtil"
    private const val DEFAULT_REFRESH_RATE = 60.0f

    /**
     * Attempts to unlock the highest available refresh rate supported by the display.
     * Guards against custom ROM exceptions, null windows, and legacy displays.
     */
    @JvmStatic
    fun adaptHighRefreshRate(activity: Activity?) {
        if (activity == null) return
        val window = try {
            activity.window
        } catch (_: Throwable) {
            null
        } ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = getDisplayFromActivity(activity) ?: return
                val supportedModes = display.supportedModes
                if (!supportedModes.isNullOrEmpty()) {
                    // Find the display mode with the maximum refresh rate
                    val maxMode = supportedModes.maxByOrNull { it.refreshRate }
                    if (maxMode != null && maxMode.refreshRate > DEFAULT_REFRESH_RATE) {
                        val layoutParams = window.attributes
                        layoutParams.preferredDisplayModeId = maxMode.modeId
                        window.attributes = layoutParams
                    }
                }
            }
        } catch (_: Throwable) {
            // Failsafe fallback: never let display mode tuning interfere with activity launch
        }
    }

    /**
     * Returns the current or maximum display refresh rate of the device.
     */
    @JvmStatic
    fun getDisplayRefreshRate(activity: Activity?): Float {
        if (activity == null) return DEFAULT_REFRESH_RATE
        return try {
            val display = getDisplayFromActivity(activity)
            if (display != null) {
                display.refreshRate
            } else {
                DEFAULT_REFRESH_RATE
            }
        } catch (_: Throwable) {
            DEFAULT_REFRESH_RATE
        }
    }

    /**
     * Safely retrieves the Display instance across Android versions.
     */
    @Suppress("DEPRECATION")
    private fun getDisplayFromActivity(activity: Activity): Display? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    activity.display
                } catch (_: Throwable) {
                    val wm = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    wm?.defaultDisplay
                }
            } else {
                val wm = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.defaultDisplay ?: activity.windowManager?.defaultDisplay
            }
        } catch (_: Throwable) {
            null
        }
    }
}
