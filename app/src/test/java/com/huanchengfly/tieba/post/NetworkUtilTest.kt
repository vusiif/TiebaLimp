package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.NetworkUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkUtilTest {

    @Test
    fun testNetTypeConstants() {
        assertEquals(0, NetworkUtil.NET_TYPE_NONE)
        assertEquals(1, NetworkUtil.NET_TYPE_WIFI)
        assertEquals(3, NetworkUtil.NET_TYPE_MOBILE)
    }

    @Test
    fun testNullContext_safeHandling() {
        assertFalse(NetworkUtil.isNetworkConnected(null))
        assertFalse(NetworkUtil.isWifiConnected(null))
        assertFalse(NetworkUtil.isMobileConnected(null))
        assertFalse(NetworkUtil.isMeteredNetwork(null))
        assertFalse(NetworkUtil.isUnmeteredWifi(null))
        assertFalse(NetworkUtil.isDataSaverEnabled(null))
    }

    @Test
    fun testNetTypeString_nullContext_defaultsToMobile() {
        // When context is null, network cannot be confirmed as unmetered Wi-Fi,
        // so it defaults to mobile "3" to protect bandwidth.
        assertEquals("3", NetworkUtil.getNetTypeString(null))
    }

    @Test
    fun testNetTypeInt_nullContext_defaultsToMobile() {
        assertEquals(NetworkUtil.NET_TYPE_MOBILE, NetworkUtil.getNetTypeInt(null))
    }
}
