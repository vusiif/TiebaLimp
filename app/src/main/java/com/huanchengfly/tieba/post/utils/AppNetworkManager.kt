package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.TiebaDns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppNetworkManager {
    private const val TAG = "AppNetworkManager"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var isRegistered = false

    @Volatile
    var isConnected: Boolean = true
        private set

    @JvmStatic
    fun init(context: Context?) {
        if (context == null || isRegistered) return
        synchronized(this) {
            if (isRegistered) return
            runCatching {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return
                isConnected = NetworkUtil.isNetworkConnected(context)

                // Initial warmup on app launch
                if (isConnected) {
                    RetrofitTiebaApi.warmupConnections()
                }

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "Network became available: $network")
                        isConnected = true
                        scope.launch {
                            // Prewarm DNS and HTTP/2 sockets over the newly available network
                            TiebaDns.prewarm()
                            RetrofitTiebaApi.warmupConnections()
                            ReplyOutboxCoordinator.flushPendingRepliesAsync()
                        }
                    }

                    override fun onLost(network: Network) {
                        Log.d(TAG, "Network lost: $network")
                        isConnected = false
                        scope.launch {
                            // Purge stale DNS and broken TCP sockets
                            TiebaDns.clear()
                            RetrofitTiebaApi.connectionPool.evictAll()
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm.registerDefaultNetworkCallback(callback)
                }
                networkCallback = callback
                isRegistered = true
                Log.d(TAG, "AppNetworkManager initialized and network callback registered.")
            }.onFailure { e ->
                Log.e(TAG, "Failed to register network callback", e)
            }
        }
    }

    @JvmStatic
    fun evictIdleConnections() {
        runCatching {
            RetrofitTiebaApi.connectionPool.evictAll()
        }
    }
}
