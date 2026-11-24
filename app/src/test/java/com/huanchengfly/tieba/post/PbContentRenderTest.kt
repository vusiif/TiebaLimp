package com.huanchengfly.tieba.post

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.ui.common.VideoContentRender
import com.huanchengfly.tieba.post.ui.common.VoiceContentRender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PbContentRenderTest {

    @Test
    fun testPicContentRenderAspectRatio() {
        val normalPic = PicContentRender(
            picUrl = "https://img.test/pic.jpg",
            originUrl = "https://img.test/pic.jpg",
            showOriginBtn = false,
            originSize = 100,
            width = 1920,
            height = 1080,
            picId = "p1"
        )
        assertEquals(1920f / 1080f, normalPic.aspectRatio, 0.001f)
        assertEquals("[图片]", normalPic.toString())

        val ultraWidePic = PicContentRender(
            picUrl = "https://img.test/wide.jpg",
            originUrl = "https://img.test/wide.jpg",
            showOriginBtn = false,
            originSize = 100,
            width = 20000,
            height = 100,
            picId = "p2"
        )
        assertEquals(10f, ultraWidePic.aspectRatio, 0.001f)

        val ultraTallPic = PicContentRender(
            picUrl = "https://img.test/tall.jpg",
            originUrl = "https://img.test/tall.jpg",
            showOriginBtn = false,
            originSize = 100,
            width = 100,
            height = 20000,
            picId = "p3"
        )
        assertEquals(0.1f, ultraTallPic.aspectRatio, 0.001f)

        val zeroPic = PicContentRender(
            picUrl = "https://img.test/zero.jpg",
            originUrl = "https://img.test/zero.jpg",
            showOriginBtn = false,
            originSize = 100,
            width = 0,
            height = 0,
            picId = "p4"
        )
        assertEquals(1f, zeroPic.aspectRatio, 0.001f)
    }

    @Test
    fun testVideoContentRenderAspectRatioAndToString() {
        val video = VideoContentRender(
            videoUrl = "https://video.test/v.mp4",
            picUrl = "https://video.test/thumb.jpg",
            webUrl = "https://video.test/page",
            width = 1280,
            height = 720
        )
        assertEquals(1280f / 720f, video.aspectRatio, 0.001f)
        assertEquals("[视频]", video.toString())

        val zeroHeightVideo = VideoContentRender(
            videoUrl = "https://video.test/v2.mp4",
            picUrl = "https://video.test/thumb2.jpg",
            webUrl = "https://video.test/page2",
            width = 1280,
            height = 0
        )
        assertEquals(1f, zeroHeightVideo.aspectRatio, 0.001f)

        val negativeVideo = VideoContentRender(
            videoUrl = "https://video.test/v3.mp4",
            picUrl = "https://video.test/thumb3.jpg",
            webUrl = "https://video.test/page3",
            width = -10,
            height = -20
        )
        assertEquals(1f, negativeVideo.aspectRatio, 0.001f)
    }

    @Test
    fun testVoiceContentRenderUrlAndToString() {
        val voice = VoiceContentRender(
            voiceMd5 = "abc123def456",
            duration = 15
        )
        assertEquals("[语音]", voice.toString())
        assertTrue(voice.voiceUrl.contains("voice_md5=abc123def456"))
        assertEquals(15, voice.duration)
    }

    @Test
    fun testTextContentRenderAnnotationsAndOperators() {
        val plainText = TextContentRender("Hello world")
        assertFalse(plainText.hasClickableAnnotations)
        assertEquals("Hello world", plainText.toString())

        val annotatedWithUrl = TextContentRender(
            buildAnnotatedString {
                append("Check this ")
                pushStringAnnotation("url", "https://tieba.baidu.com")
                append("link")
                pop()
            }
        )
        assertTrue(annotatedWithUrl.hasClickableAnnotations)

        val annotatedWithUser = TextContentRender(
            buildAnnotatedString {
                append("Hello ")
                pushStringAnnotation("user", "12345")
                append("@tester")
                pop()
            }
        )
        assertTrue(annotatedWithUser.hasClickableAnnotations)

        val combined = plainText + " again"
        assertEquals("Hello world again", combined.toString())

        val combinedAnnotated = plainText + AnnotatedString(" + extra")
        assertEquals("Hello world + extra", combinedAnnotated.toString())
    }
}
