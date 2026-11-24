package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.TiebaDns
import com.huanchengfly.tieba.post.utils.AppNetworkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNetworkManagerTest {

    @Test
    fun testDispatcherConcurrencyConfiguration() {
        val dispatcher = RetrofitTiebaApi.dispatcher
        assertNotNull(dispatcher)
        assertEquals(128, dispatcher.maxRequests)
        assertEquals(24, dispatcher.maxRequestsPerHost)
    }

    @Test
    fun testConnectionPoolConfiguration() {
        val pool = RetrofitTiebaApi.connectionPool
        assertNotNull(pool)

        // Eviction should execute safely on JVM test classpath
        AppNetworkManager.evictIdleConnections()
    }

    @Test
    fun testNullContextSafety() {
        // AppNetworkManager.init must handle null context safely without throwing
        AppNetworkManager.init(null)
    }

    @Test
    fun testCoreHostsCoverage() {
        val hosts = TiebaDns.CORE_HOSTS.toSet()
        assertTrue("Must include protobuf API host", hosts.contains("tiebac.baidu.com"))
        assertTrue("Must include JSON API host", hosts.contains("c.tieba.baidu.com"))
        assertTrue("Must include web API host", hosts.contains("tieba.baidu.com"))
        assertTrue("Must include static CDN host", hosts.contains("tb2.bdstatic.com"))
    }
}
