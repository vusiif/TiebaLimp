package com.huanchengfly.tieba.post.activities

import android.content.Context
import com.huanchengfly.tieba.post.api.LiteApi
import com.huanchengfly.tieba.post.api.retrofit.doIfSuccess
import com.huanchengfly.tieba.post.utils.CacheUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

object TranslucentWallpaperHelper {
    private const val CACHE_KEY = "recommend_wallpapers"

    @Suppress("UNCHECKED_CAST")
    fun getCachedWallpapers(context: Context): List<String>? {
        return CacheUtil.getCache(context, CACHE_KEY, List::class.java) as? List<String>
    }

    fun fetchWallpapers(
        context: Context,
        scope: CoroutineScope,
        onLoaded: (List<String>) -> Unit
    ) {
        scope.launch(IO) {
            LiteApi.instance
                .wallpapersAsync()
                .doIfSuccess {
                    CacheUtil.putCache(context, CACHE_KEY, it)
                    onLoaded(it)
                }
        }
    }
}
