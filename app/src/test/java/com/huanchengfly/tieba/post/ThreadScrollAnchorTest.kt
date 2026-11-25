package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.thread.PostItemData
import com.huanchengfly.tieba.post.ui.page.thread.ThreadAnchorKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadScrollAnchor
import com.huanchengfly.tieba.post.ui.page.thread.ThreadScrollAnchorResolver
import com.huanchengfly.tieba.post.ui.page.thread.ThreadScrollAnchorStore
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadScrollAnchorTest {

    private fun fakePostItem(id: Long, floor: Int): PostItemData {
        return PostItemData(
            post = wrapImmutable(Post(id = id, floor = floor)),
            blocked = false,
            contentRenders = persistentListOf(),
            subPosts = persistentListOf()
        )
    }

    @Test
    fun testResolveTargetPositionByAnchorPostIdEvenAfterPrepend() {
        val dataAfterPrepend = listOf(
            fakePostItem(1001L, 2),
            fakePostItem(1002L, 3),
            fakePostItem(1003L, 4),
            fakePostItem(1004L, 5),
        )
        // Suppose previously postId 1003 was at index 2 with 45px scroll offset
        val savedAnchor = ThreadScrollAnchor(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffset = 45,
            anchorPostId = 1003L,
            anchorPostScrollOffset = 60
        )

        // With baseOffset = 3 (header + loadPrevious), post 1003 is at index 3 + 2 = 5
        val resolved = ThreadScrollAnchorResolver.resolveTargetPosition(
            anchor = savedAnchor,
            data = dataAfterPrepend,
            baseOffset = 3,
            totalItemsCount = 8
        )
        assertEquals(5 to 60, resolved)
    }

    @Test
    fun testResolveFallbackIndexAndClampingWhenAnchorPostMissing() {
        val data = listOf(
            fakePostItem(2001L, 10),
            fakePostItem(2002L, 11),
        )
        val savedAnchor = ThreadScrollAnchor(
            firstVisibleItemIndex = 20,
            firstVisibleItemScrollOffset = 120,
            anchorPostId = 9999L,
            anchorPostScrollOffset = 30
        )

        val resolved = ThreadScrollAnchorResolver.resolveTargetPosition(
            anchor = savedAnchor,
            data = data,
            baseOffset = 2,
            totalItemsCount = 5
        )
        assertEquals(4 to 120, resolved)
    }

    @Test
    fun testLruStoreEvictionAndTopPositionCleanup() {
        val store = ThreadScrollAnchorStore(maxEntries = 2)
        val k1 = ThreadAnchorKey(1L, false, 0)
        val k2 = ThreadAnchorKey(2L, false, 0)
        val k3 = ThreadAnchorKey(3L, false, 0)

        store.save(k1, ThreadScrollAnchor(3, 50, 101L, 50))
        store.save(k2, ThreadScrollAnchor(4, 20, 102L, 20))
        assertEquals(2, store.size())

        // Touch k1 so k2 becomes least recently used
        store.get(k1)
        store.save(k3, ThreadScrollAnchor(5, 10, 103L, 10))

        assertEquals(2, store.size())
        assertNull(store.get(k2))
        assertEquals(3, store.get(k1)?.firstVisibleItemIndex)

        // Saving top (0, 0, null) removes entry
        store.save(k1, ThreadScrollAnchor(0, 0, null, 0))
        assertEquals(1, store.size())
        assertNull(store.get(k1))
    }
}
