package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageExtra
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFromStoreExtra
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageNoExtra
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPageNavArgsTest {

    @Test
    fun testThreadPageFromConstants() {
        assertEquals("forum", ThreadPageFrom.FROM_FORUM)
        assertEquals("store_thread", ThreadPageFrom.FROM_STORE)
        assertEquals("personalized", ThreadPageFrom.FROM_PERSONALIZED)
        assertEquals("history", ThreadPageFrom.FROM_HISTORY)
    }

    @Test
    fun testThreadPageExtraDataClasses() {
        val extra1 = ThreadPageFromStoreExtra(maxPid = 123456L, maxFloor = 42)
        val extra2 = ThreadPageFromStoreExtra(maxPid = 123456L, maxFloor = 42)
        assertEquals(extra1, extra2)
        assertEquals(123456L, extra1.maxPid)
        assertEquals(42, extra1.maxFloor)
        assertEquals(extra1.hashCode(), extra2.hashCode())

        assertTrue(extra1 is ThreadPageExtra)
        assertTrue(ThreadPageNoExtra is ThreadPageExtra)
    }

    @Test
    fun testThreadPageExtraSerialization() {
        val json = Json { ignoreUnknownKeys = true }

        val noExtraJson = json.encodeToString<ThreadPageExtra>(ThreadPageNoExtra)
        val decodedNoExtra = json.decodeFromString<ThreadPageExtra>(noExtraJson)
        assertEquals(ThreadPageNoExtra, decodedNoExtra)

        val storeExtra: ThreadPageExtra = ThreadPageFromStoreExtra(maxPid = 9999L, maxFloor = 10)
        val storeExtraJson = json.encodeToString(storeExtra)
        val decodedStoreExtra = json.decodeFromString<ThreadPageExtra>(storeExtraJson)
        assertEquals(storeExtra, decodedStoreExtra)
    }
}
