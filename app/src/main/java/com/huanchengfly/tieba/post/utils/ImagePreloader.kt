package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.enqueue
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.shouldLoadImage
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Collections
import java.util.LinkedHashSet

val Media.url: String
    get() = ImageUtil.getUrl(
        App.INSTANCE,
        true,
        originPic,
        bigPic,
        dynamicPic,
        srcPic,
    )

val ImmutableHolder<Media>.url: String
    get() = get { url }

object ImagePreloader {
    private const val MAX_PRELOAD_DEDUP_SIZE = 128

    private val preloadedUrls: MutableSet<String> = Collections.synchronizedSet(
        object : LinkedHashSet<String>(MAX_PRELOAD_DEDUP_SIZE, 0.75f) {
            override fun add(element: String): Boolean {
                if (size >= MAX_PRELOAD_DEDUP_SIZE) {
                    val it = iterator()
                    if (it.hasNext()) {
                        it.next()
                        it.remove()
                    }
                }
                return super.add(element)
            }
        }
    )

    fun preload(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        if (!shouldLoadImage(context, false)) return
        if (!preloadedUrls.add(url)) {
            // Already preloaded recently, avoid redundant network/disk dispatch
            return
        }
        runCatching {
            DownloadRequest(context, url).enqueue()
        }
    }

    fun preload(context: Context, urls: Collection<String?>) {
        urls.forEach { preload(context, it) }
    }

    fun isUrlPreloaded(url: String): Boolean = preloadedUrls.contains(url)

    fun getPreloadedCount(): Int = preloadedUrls.size

    fun clearCache() {
        preloadedUrls.clear()
    }
}

/**
 * Automatically preloads image URLs ahead of LazyColumn scrolling to eliminate white image flashes.
 *
 * @param state The [LazyListState] of the list.
 * @param items The backing list of data items.
 * @param preloadAhead Number of items ahead of visible bounds to preload.
 * @param itemOffset Offset index of items if leading headers/decorations exist before [items].
 * @param extractUrls Function that extracts a list of image URLs from an item of type [T].
 */
@Composable
fun <T> LazyListImagePreloadEffect(
    state: LazyListState,
    items: List<T>,
    preloadAhead: Int = 4,
    itemOffset: Int = 0,
    extractUrls: (T) -> List<String>,
) {
    val context = LocalContext.current
    LaunchedEffect(state, items, preloadAhead, itemOffset) {
        var lastVisibleIndex = -1
        snapshotFlow {
            val visible = state.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) -1 to -1
            else visible.first().index to visible.last().index
        }
            .distinctUntilChanged()
            .collect { (firstIndex, lastIndex) ->
                if (firstIndex == -1 || lastIndex == -1) return@collect
                val totalCount = state.layoutInfo.totalItemsCount
                if (totalCount <= 0 || items.isEmpty()) return@collect

                val isScrollingDown = lastIndex >= lastVisibleIndex
                lastVisibleIndex = lastIndex

                if (isScrollingDown) {
                    val start = (lastIndex + 1).coerceAtMost(totalCount - 1)
                    val end = (lastIndex + preloadAhead).coerceAtMost(totalCount - 1)
                    if (start <= end) {
                        for (i in start..end) {
                            val dataIndex = i - itemOffset
                            if (dataIndex in items.indices) {
                                val urls = extractUrls(items[dataIndex])
                                ImagePreloader.preload(context, urls)
                            }
                        }
                    }
                } else {
                    val start = (firstIndex - preloadAhead).coerceAtLeast(0)
                    val end = (firstIndex - 1).coerceAtLeast(0)
                    if (start <= end) {
                        for (i in start..end) {
                            val dataIndex = i - itemOffset
                            if (dataIndex in items.indices) {
                                val urls = extractUrls(items[dataIndex])
                                ImagePreloader.preload(context, urls)
                            }
                        }
                    }
                }
            }
    }
}
