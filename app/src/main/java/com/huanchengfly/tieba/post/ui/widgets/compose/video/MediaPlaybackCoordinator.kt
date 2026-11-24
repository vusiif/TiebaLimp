package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.util.Log
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

/**
 * Global coordinator for video and media playback.
 * Enforces mutual exclusion (only one video plays at any given time),
 * manages off-screen and background auto-pause, and handles low-memory release.
 */
object MediaPlaybackCoordinator {
    private const val TAG = "MediaPlaybackCoord"

    private val activeControllers = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<VideoPlayerController, Boolean>())
    )

    @Volatile
    private var currentPlayingController: WeakReference<VideoPlayerController>? = null

    private inline fun logDebug(message: () -> String) {
        runCatching {
            Log.d(TAG, message())
        }
    }

    /**
     * Register a controller to track active players.
     */
    fun register(controller: VideoPlayerController) {
        activeControllers.add(controller)
    }

    /**
     * Unregister a controller when it is released.
     */
    fun unregister(controller: VideoPlayerController) {
        activeControllers.remove(controller)
        if (currentPlayingController?.get() == controller) {
            currentPlayingController = null
        }
    }

    /**
     * Request playback focus for a controller.
     * Automatically pauses any other currently playing controller.
     */
    fun requestPlayback(controller: VideoPlayerController) {
        register(controller)
        val current = currentPlayingController?.get()
        if (current != null && current != controller) {
            logDebug { "Pausing previous video player to give playback focus to new player" }
            runCatching { current.pause() }
        }
        currentPlayingController = WeakReference(controller)
    }

    /**
     * Pause all active video players (e.g. on Activity ON_PAUSE/ON_STOP or memory pressure).
     */
    fun pauseAll() {
        logDebug { "pauseAll() invoked, pausing ${activeControllers.size} tracked video players" }
        synchronized(activeControllers) {
            for (controller in activeControllers) {
                runCatching { controller.pause() }
            }
        }
        currentPlayingController = null
    }

    /**
     * Release all video players (e.g. on critical low memory).
     */
    fun releaseAll() {
        logDebug { "releaseAll() invoked, releasing all tracked video players" }
        synchronized(activeControllers) {
            for (controller in activeControllers) {
                runCatching { controller.release() }
            }
            activeControllers.clear()
        }
        currentPlayingController = null
    }

    fun getActiveCount(): Int = activeControllers.size

    fun getCurrentPlaying(): VideoPlayerController? = currentPlayingController?.get()
}
