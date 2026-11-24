package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.ui.widgets.compose.video.MediaPlaybackCoordinator
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerController
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerSource
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class MediaPlaybackCoordinatorTest {

    private class FakeVideoPlayerController(val id: String) : VideoPlayerController {
        var playCount = 0
        var pauseCount = 0
        var releaseCount = 0
        var resetCount = 0

        override fun setSource(source: VideoPlayerSource) {}

        override fun play() {
            playCount++
        }

        override fun pause() {
            pauseCount++
        }

        override fun togglePlaying() {}

        override fun quickSeekForward() {}

        override fun quickSeekRewind() {}

        override fun seekTo(position: Long) {}

        override fun reset() {
            resetCount++
        }

        override fun release() {
            releaseCount++
        }

        override val state: StateFlow<VideoPlayerState> =
            MutableStateFlow(VideoPlayerState())
    }

    @Before
    fun setUp() {
        MediaPlaybackCoordinator.releaseAll()
    }

    @After
    fun tearDown() {
        MediaPlaybackCoordinator.releaseAll()
    }

    @Test
    fun testRegisterAndUnregister() {
        val c1 = FakeVideoPlayerController("c1")
        val c2 = FakeVideoPlayerController("c2")

        MediaPlaybackCoordinator.register(c1)
        MediaPlaybackCoordinator.register(c2)
        assertEquals(2, MediaPlaybackCoordinator.getActiveCount())

        MediaPlaybackCoordinator.unregister(c1)
        assertEquals(1, MediaPlaybackCoordinator.getActiveCount())

        MediaPlaybackCoordinator.unregister(c2)
        assertEquals(0, MediaPlaybackCoordinator.getActiveCount())
    }

    @Test
    fun testRequestPlaybackMutualExclusion() {
        val c1 = FakeVideoPlayerController("c1")
        val c2 = FakeVideoPlayerController("c2")

        MediaPlaybackCoordinator.requestPlayback(c1)
        assertSame(c1, MediaPlaybackCoordinator.getCurrentPlaying())
        assertEquals(0, c1.pauseCount)

        // Requesting playback for c2 must pause c1
        MediaPlaybackCoordinator.requestPlayback(c2)
        assertSame(c2, MediaPlaybackCoordinator.getCurrentPlaying())
        assertEquals(1, c1.pauseCount)
        assertEquals(0, c2.pauseCount)

        // Requesting playback for c2 again should not re-pause c2
        MediaPlaybackCoordinator.requestPlayback(c2)
        assertEquals(0, c2.pauseCount)
    }

    @Test
    fun testPauseAll() {
        val c1 = FakeVideoPlayerController("c1")
        val c2 = FakeVideoPlayerController("c2")

        MediaPlaybackCoordinator.requestPlayback(c1)
        MediaPlaybackCoordinator.register(c2)

        MediaPlaybackCoordinator.pauseAll()

        assertEquals(1, c1.pauseCount)
        assertEquals(1, c2.pauseCount)
        assertNull(MediaPlaybackCoordinator.getCurrentPlaying())
    }

    @Test
    fun testReleaseAll() {
        val c1 = FakeVideoPlayerController("c1")
        val c2 = FakeVideoPlayerController("c2")

        MediaPlaybackCoordinator.requestPlayback(c1)
        MediaPlaybackCoordinator.register(c2)

        MediaPlaybackCoordinator.releaseAll()

        assertEquals(1, c1.releaseCount)
        assertEquals(1, c2.releaseCount)
        assertEquals(0, MediaPlaybackCoordinator.getActiveCount())
        assertNull(MediaPlaybackCoordinator.getCurrentPlaying())
    }

    @Test
    fun testUnregisterCurrentPlayingClearsFocus() {
        val c1 = FakeVideoPlayerController("c1")

        MediaPlaybackCoordinator.requestPlayback(c1)
        assertSame(c1, MediaPlaybackCoordinator.getCurrentPlaying())

        MediaPlaybackCoordinator.unregister(c1)
        assertNull(MediaPlaybackCoordinator.getCurrentPlaying())
        assertEquals(0, MediaPlaybackCoordinator.getActiveCount())
    }
}
