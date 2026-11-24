package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.models.distinctById
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadItemDataTest {

    @Test
    fun testThreadItemDataBasicProperties() {
        val thread = ThreadInfo(
            id = 1001L,
            title = "Test Thread",
            isTop = 1
        )
        val itemData = ThreadItemData(thread = wrapImmutable(thread))

        assertEquals(1001L, itemData.id)
        assertTrue(itemData.isTop)
        assertEquals(0, itemData.mediaCount)
        assertFalse(itemData.hasVideo)
        assertFalse(itemData.hidden)
    }

    @Test
    fun testThreadItemDataFallbackToThreadId() {
        // When id is 0, it should fallback to threadId
        val thread = ThreadInfo(
            id = 0L,
            threadId = 2002L,
            title = "Fallback Thread",
            isTop = 0
        )
        val itemData = ThreadItemData(thread = wrapImmutable(thread))

        assertEquals(2002L, itemData.id)
        assertFalse(itemData.isTop)
    }

    @Test
    fun testThreadItemDataMediaAndVideo() {
        val thread = ThreadInfo(
            id = 3003L,
            media = listOf(
                Media(bigPic = "https://tiebapic.baidu.com/1.jpg"),
                Media(bigPic = "https://tiebapic.baidu.com/2.jpg")
            ),
            videoInfo = VideoInfo(videoUrl = "https://tieba.baidu.com/video.mp4")
        )
        val itemData = ThreadItemData(thread = wrapImmutable(thread))

        assertEquals(3003L, itemData.id)
        assertEquals(2, itemData.mediaCount)
        assertTrue(itemData.hasVideo)
    }

    @Test
    fun testDistinctById() {
        val item1 = ThreadItemData(thread = wrapImmutable(ThreadInfo(id = 100L, title = "A")))
        val item2 = ThreadItemData(thread = wrapImmutable(ThreadInfo(id = 200L, title = "B")))
        val item3 = ThreadItemData(thread = wrapImmutable(ThreadInfo(id = 100L, title = "A Duplicate")))

        val list = listOf(item1, item2, item3)
        val distinct = list.distinctById()

        assertEquals(2, distinct.size)
        assertEquals(100L, distinct[0].id)
        assertEquals(200L, distinct[1].id)
    }

    @Test
    fun testDestructuringCompatibility() {
        val thread = ThreadInfo(id = 5005L, title = "Destructure Test")
        val itemData = ThreadItemData(thread = wrapImmutable(thread))

        // 2-component destructuring
        val (holder, blocked) = itemData
        assertEquals(5005L, holder.get { id })
        assertFalse(blocked)

        // 4-component destructuring
        val (h2, b2, p2, hid2) = itemData
        assertEquals(5005L, h2.get { id })
        assertFalse(b2)
        assertEquals(null, p2)
        assertFalse(hid2)
    }
}
