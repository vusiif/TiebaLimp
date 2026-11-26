package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.models.DraftModel
import com.huanchengfly.tieba.post.utils.OutboxStore
import com.huanchengfly.tieba.post.utils.PendingReplyItem
import com.huanchengfly.tieba.post.utils.ReplyOutboxManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DraftAndOutboxTest {

    private class InMemoryOutboxStore : OutboxStore {
        private val map = linkedMapOf<String, PendingReplyItem>()

        override fun getAll(): List<PendingReplyItem> = map.values.toList()

        override fun put(item: PendingReplyItem) {
            map[item.id] = item
        }

        override fun remove(id: String) {
            map.remove(id)
        }
    }

    @Test
    fun testDraftModelHasSameContentIgnoresTimestamp() {
        val d1 = DraftModel(
            text = "hello",
            imageUris = listOf("content://img/1"),
            isOriginImage = true,
            updateTimestamp = 1000L
        )
        val d2 = DraftModel(
            text = "hello",
            imageUris = listOf("content://img/1"),
            isOriginImage = true,
            updateTimestamp = 9999L
        )
        val d3 = DraftModel(
            text = "hello world",
            imageUris = listOf("content://img/1"),
            isOriginImage = true,
            updateTimestamp = 9999L
        )

        assertTrue(d1.hasSameContent(d2))
        assertFalse(d1.hasSameContent(d3))
        assertFalse(d1.hasSameContent(d1.copy(isOriginImage = false)))
    }

    @Test
    fun testDraftModelSerializeAndDeserialize() {
        val original = DraftModel(
            text = "multi line\ndraft",
            imageUris = listOf("file:///a.jpg"),
            isOriginImage = true,
            updateTimestamp = 123456L,
            forumId = 88L,
            forumName = "android",
            threadId = 999L,
            postId = 1001L
        )
        val restored = DraftModel.deserialize(original.serialize())
        assertTrue(original.hasSameContent(restored))
        assertEquals(123456L, restored.updateTimestamp)
        assertEquals(88L, restored.forumId)
        assertEquals("android", restored.forumName)
        assertEquals(999L, restored.threadId)
        assertEquals(1001L, restored.postId)

        val legacy = DraftModel.deserialize("legacy plain text")
        assertEquals("legacy plain text", legacy.text)
        assertTrue(legacy.imageUris.isEmpty())
    }

    @Test
    fun testTransientNetworkErrorClassification() {
        assertTrue(ReplyOutboxManager.isTransientNetworkError(IOException("broken pipe")))
        assertTrue(ReplyOutboxManager.isTransientNetworkError(SocketTimeoutException("timeout")))
        assertTrue(ReplyOutboxManager.isTransientNetworkError(UnknownHostException("offline")))
        assertTrue(
            ReplyOutboxManager.isTransientNetworkError(
                RuntimeException("wrapped", IOException("reset"))
            )
        )
        assertFalse(ReplyOutboxManager.isTransientNetworkError(IllegalArgumentException("bad param")))
    }

    @Test
    fun testOutboxEnqueueDeduplicationAndFlushDelivery() = runBlocking {
        val store = InMemoryOutboxStore()
        val deliveredIds = mutableListOf<String>()
        var currentTime = 1_000_000L

        val manager = ReplyOutboxManager(
            store = store,
            sender = { item -> deliveredIds.add(item.id) },
            onDelivered = { },
            timeProvider = { currentTime }
        )

        val item = PendingReplyItem(
            id = "req-1",
            accountUid = "1001",
            content = "test reply",
            forumId = 10L,
            forumName = "test",
            threadId = 20L,
            tbs = "tbs-xyz",
            createdAt = currentTime
        )

        assertTrue(manager.enqueue(item))
        // Duplicate same target + same content should be ignored
        assertFalse(manager.enqueue(item.copy(id = "req-2")))
        assertEquals(1, manager.pendingCount())

        // Offline flush does nothing
        assertEquals(0, manager.flushPendingReplies(isNetworkAvailable = false))
        assertEquals(1, manager.pendingCount())

        // Online flush delivers and removes from store
        assertEquals(1, manager.flushPendingReplies(isNetworkAvailable = true))
        assertEquals(listOf("req-1"), deliveredIds)
        assertEquals(0, manager.pendingCount())
    }

    @Test
    fun testOutboxRetryAndTtlExpiration() = runBlocking {
        val store = InMemoryOutboxStore()
        var currentTime = 1_000_000L
        var shouldFailTransient = true

        val manager = ReplyOutboxManager(
            store = store,
            sender = {
                if (shouldFailTransient) throw SocketTimeoutException("timeout")
            },
            onDelivered = { },
            timeProvider = { currentTime }
        )

        val item = PendingReplyItem(
            id = "req-retry",
            accountUid = "1001",
            content = "retry me",
            forumId = 10L,
            forumName = "test",
            threadId = 30L,
            tbs = "tbs",
            createdAt = currentTime
        )
        manager.enqueue(item)

        // First transient failure increments retryCount to 1
        assertEquals(0, manager.flushPendingReplies(true))
        assertEquals(1, store.getAll().first().retryCount)

        // Second transient failure increments retryCount to 2
        assertEquals(0, manager.flushPendingReplies(true))
        assertEquals(2, store.getAll().first().retryCount)

        // Recovery succeeds on 3rd attempt
        shouldFailTransient = false
        assertEquals(1, manager.flushPendingReplies(true))
        assertEquals(0, manager.pendingCount())

        // Test TTL expiration
        val expiredItem = item.copy(id = "req-expired", createdAt = currentTime)
        manager.enqueue(expiredItem)
        currentTime += ReplyOutboxManager.OUTBOX_TTL_MS + 1000L
        assertEquals(0, manager.flushPendingReplies(true))
        assertEquals(0, manager.pendingCount())
    }

    @Test
    fun testOutboxManualInspectionRetryAndClear() = runBlocking {
        val store = InMemoryOutboxStore()
        var failSend = true
        val manager = ReplyOutboxManager(
            store = store,
            sender = { if (failSend) throw IOException("offline") },
            onDelivered = { }
        )

        val first = PendingReplyItem(
            id = "item-1",
            accountUid = "1001",
            content = "first",
            forumId = 1L,
            forumName = "f1",
            threadId = 10L,
            tbs = "t1",
            createdAt = 1000L
        )
        val second = PendingReplyItem(
            id = "item-2",
            accountUid = "1001",
            content = "second",
            forumId = 1L,
            forumName = "f1",
            threadId = 20L,
            tbs = "t2",
            createdAt = 2000L
        )
        manager.enqueue(first)
        manager.enqueue(second)

        val ordered = manager.getPendingItems()
        assertEquals(listOf("item-2", "item-1"), ordered.map { it.id })

        // Manual retry fails transiently -> increments retryCount
        assertFalse(manager.retryItem("item-2", isNetworkAvailable = true))
        assertEquals(1, manager.getPendingItems().first { it.id == "item-2" }.retryCount)

        // Manual retry succeeds -> removes item-2
        failSend = false
        assertTrue(manager.retryItem("item-2", isNetworkAvailable = true))
        assertEquals(listOf("item-1"), manager.getPendingItems().map { it.id })

        // Clear all removes remaining
        manager.clearAll()
        assertEquals(0, manager.pendingCount())
    }
}
