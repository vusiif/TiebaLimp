package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.huanchengfly.tieba.post.App

fun isNetworkConnected(context: Context = App.INSTANCE): Boolean {
    return NetworkUtil.isNetworkConnected(context)
}

object NetworkUtil {

    const val NET_TYPE_NONE = 0
    const val NET_TYPE_WIFI = 1
    const val NET_TYPE_MOBILE = 3

    /**
     * Checks if the device has an active network connection with internet capability.
     */
    fun isNetworkConnected(context: Context?): Boolean {
        if (context == null) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork = cm.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            activeNetwork.isConnected
        }
    }

    /**
     * Checks if the device is connected to Wi-Fi or Ethernet.
     */
    fun isWifiConnected(context: Context?): Boolean {
        if (context == null) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val wifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI) ?: return false
            @Suppress("DEPRECATION")
            wifiNetworkInfo.isConnected
        }
    }

    /**
     * Checks if the device is connected to a mobile cellular network.
     */
    fun isMobileConnected(context: Context?): Boolean {
        if (context == null) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val mobileNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) ?: return false
            @Suppress("DEPRECATION")
            mobileNetworkInfo.isConnected
        }
    }

    /**
     * Checks if the active network is metered (e.g. mobile data, metered Wi-Fi hotspot).
     */
    fun isMeteredNetwork(context: Context?): Boolean {
        if (context == null) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return cm.isActiveNetworkMetered
    }

    /**
     * Checks if the connection is an unmetered Wi-Fi or Ethernet network.
     * Prevents unexpected massive data consumption on metered personal hotspots.
     */
    fun isUnmeteredWifi(context: Context?): Boolean {
        return isWifiConnected(context) && !isMeteredNetwork(context)
    }

    /**
     * Checks if the Android system Data Saver (节流模式) is enabled.
     */
    fun isDataSaverEnabled(context: Context?): Boolean {
        if (context == null) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            return cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        }
        return false
    }

    /**
     * Returns Tieba API network type string:
     * "1" for unmetered Wi-Fi / Ethernet, "3" for mobile / metered data.
     */
    fun getNetTypeString(context: Context?): String {
        return if (isUnmeteredWifi(context)) "1" else "3"
    }

    /**
     * Returns Tieba API network type integer:
     * 1 for unmetered Wi-Fi / Ethernet, 3 for mobile / metered data.
     */
    fun getNetTypeInt(context: Context?): Int {
        return if (isUnmeteredWifi(context)) NET_TYPE_WIFI else NET_TYPE_MOBILE
    }
}
