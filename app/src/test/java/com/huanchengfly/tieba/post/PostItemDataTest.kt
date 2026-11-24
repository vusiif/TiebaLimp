package com.huanchengfly.tieba.post

import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.thread.PostItemData
import com.huanchengfly.tieba.post.ui.page.thread.SubPostItemData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostItemDataTest {

    @Test
    fun testPostItemDataBasicProperties() {
        val post = Post(
            id = 8888L,
            floor = 3,
            author_id = 123L,
            is_fold = 0,
            fold_comment_status = 0,
            sub_post_number = 5
        )
        val postItem = PostItemData(
            post = wrapImmutable(post),
            blocked = false,
            contentRenders = persistentListOf(),
            subPosts = persistentListOf()
        )

        assertEquals(8888L, postItem.id)
        assertEquals(3, postItem.floor)
        assertEquals(123L, postItem.authorId)
        assertFalse(postItem.isFolded)
        assertEquals(5, postItem.subPostCount)
    }

    @Test
    fun testPostItemDataFoldedConditions() {
        val post1 = Post(id = 8889L, is_fold = 1)
        val postItem1 = PostItemData(
            post = wrapImmutable(post1),
            blocked = false,
            contentRenders = persistentListOf(),
            subPosts = persistentListOf()
        )
        assertTrue(postItem1.isFolded)

        val post2 = Post(id = 8890L, fold_comment_status = 2)
        val postItem2 = PostItemData(
            post = wrapImmutable(post2),
            blocked = false,
            contentRenders = persistentListOf(),
            subPosts = persistentListOf()
        )
        assertTrue(postItem2.isFolded)
    }

    @Test
    fun testPostItemDataDestructuringCompatibility() {
        val post = Post(id = 7777L, floor = 10)
        val postItem = PostItemData(
            post = wrapImmutable(post),
            blocked = true,
            contentRenders = persistentListOf(),
            subPosts = persistentListOf()
        )

        val (holder, blocked, renders, subPosts, blockResult) = postItem
        assertEquals(7777L, holder.get { id })
        assertTrue(blocked)
        assertTrue(renders.isEmpty())
        assertTrue(subPosts.isEmpty())
        assertEquals(7777L, postItem.id)
        assertEquals(10, postItem.floor)
    }

    @Test
    fun testSubPostItemDataProperties() {
        val subPost = SubPostList(
            id = 9999L,
            author = User(id = 456L, name = "tester")
        )
        val subPostItem = SubPostItemData(
            subPost = wrapImmutable(subPost),
            subPostContent = AnnotatedString("hello world"),
            blocked = false
        )

        assertEquals(9999L, subPostItem.id)
        assertEquals(456L, subPostItem.authorId)
        assertFalse(subPostItem.blocked)
        assertEquals("hello world", subPostItem.subPostContent.text)
    }

    @Test
    fun testPostItemDataPicRendersAndUrls() {
        val picRender1 = com.huanchengfly.tieba.post.ui.common.PicContentRender(
            picUrl = "https://img.test/pic1.jpg",
            originUrl = "https://img.test/origin1.jpg",
            showOriginBtn = false,
            originSize = 100,
            width = 800,
            height = 600,
            picId = "pic1"
        )
        val picRender2 = com.huanchengfly.tieba.post.ui.common.PicContentRender(
            picUrl = "https://img.test/pic2.jpg",
            originUrl = "https://img.test/origin2.jpg",
            showOriginBtn = true,
            originSize = 200,
            width = 1200,
            height = 800,
            picId = "pic2"
        )
        val textRender = com.huanchengfly.tieba.post.ui.common.TextContentRender("sample text")

        val post = Post(id = 5555L, floor = 1)
        val postItem = PostItemData(
            post = wrapImmutable(post),
            blocked = false,
            contentRenders = persistentListOf(textRender, picRender1, picRender2),
            subPosts = persistentListOf()
        )

        assertEquals(2, postItem.picRenders.size)
        assertEquals("pic1", postItem.picRenders[0].picId)
        assertEquals("pic2", postItem.picRenders[1].picId)

        assertEquals(2, postItem.picUrls.size)
        assertEquals("https://img.test/pic1.jpg", postItem.picUrls[0])
        assertEquals("https://img.test/pic2.jpg", postItem.picUrls[1])
    }
}
