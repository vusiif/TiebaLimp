package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.models.database.ThreadSnapshot
import com.huanchengfly.tieba.post.utils.ThreadSnapshotUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadSnapshotTest {

    @Test
    fun threadSnapshot_modelDefaultValues() {
        val snapshot = ThreadSnapshot(
            threadId = "12345678",
            title = "测试帖子标题",
            forumName = "显卡",
            authorName = "老哥",
            authorAvatar = "tb.1.test",
            floorCount = 15,
        )

        assertEquals("12345678", snapshot.threadId)
        assertEquals("测试帖子标题", snapshot.title)
        assertEquals("显卡", snapshot.forumName)
        assertEquals("老哥", snapshot.authorName)
        assertEquals(15, snapshot.floorCount)
        assertTrue(snapshot.createTime > 0L)
        assertTrue(snapshot.updateTime > 0L)
    }

    @Test
    fun snapshotData_jsonSerializationContract() {
        val sampleImages = listOf(
            ThreadSnapshotUtil.SnapshotImage(
                picUrl = "https://example.com/pic1.jpg",
                originUrl = "https://example.com/pic1_origin.jpg",
                width = 1080,
                height = 1920,
            )
        )

        val sampleSubPosts = listOf(
            ThreadSnapshotUtil.SnapshotSubPost(
                authorName = "楼中楼用户A",
                contentText = "确实是这样，好帖帮顶！",
                time = 1700001000L,
            )
        )

        val samplePosts = listOf(
            ThreadSnapshotUtil.SnapshotPost(
                floor = 1L,
                postId = 1001L,
                authorName = "楼主老哥",
                authorAvatar = "avatar_lz",
                time = 1700000000L,
                contentText = "这是主楼楼主发表的长文分析...",
                images = sampleImages,
            ),
            ThreadSnapshotUtil.SnapshotPost(
                floor = 2L,
                postId = 1002L,
                authorName = "沙发老哥",
                authorAvatar = "avatar_sofa",
                time = 1700000500L,
                contentText = "前排支持一下！",
                subPosts = sampleSubPosts,
            )
        )

        val original = ThreadSnapshotUtil.SnapshotData(
            threadId = 987654321L,
            title = "深入分析 4090 显卡功耗与架构",
            forumName = "显卡",
            authorName = "楼主老哥",
            authorAvatar = "avatar_lz",
            postTime = 1700000000L,
            snapshotTime = 1700002000L,
            posts = samplePosts,
        )

        val json = original.toJson()
        assertNotNull(json)
        assertTrue(json.contains("987654321"))
        assertTrue(json.contains("深入分析 4090 显卡功耗与架构"))
        assertTrue(json.contains("楼中楼用户A"))

        val restored: ThreadSnapshotUtil.SnapshotData = json.fromJson()
        assertEquals(original.threadId, restored.threadId)
        assertEquals(original.title, restored.title)
        assertEquals(original.forumName, restored.forumName)
        assertEquals(original.authorName, restored.authorName)
        assertEquals(original.posts.size, restored.posts.size)

        // Floor 1
        assertEquals(1L, restored.posts[0].floor)
        assertEquals("楼主老哥", restored.posts[0].authorName)
        assertEquals(1, restored.posts[0].images.size)
        assertEquals(1080, restored.posts[0].images[0].width)

        // Floor 2
        assertEquals(2L, restored.posts[1].floor)
        assertEquals("沙发老哥", restored.posts[1].authorName)
        assertEquals(1, restored.posts[1].subPosts.size)
        assertEquals("确实是这样，好帖帮顶！", restored.posts[1].subPosts[0].contentText)
    }
}
