package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Snapshot of a thread list's scroll position using both item-key (postId)
 * and fallback index+pixel offset for accurate restoration across recompositions
 * or list prepends.
 */
data class ThreadScrollAnchor(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val anchorPostId: Long? = null,
    val anchorPostScrollOffset: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class ThreadAnchorKey(
    val threadId: Long,
    val seeLz: Boolean,
    val sortType: Int,
)

/**
 * Pure calculation helper for resolving a [ThreadScrollAnchor] back to a concrete
 * (index, scrollOffset) pair for a given [data] list and [baseOffset].
 */
object ThreadScrollAnchorResolver {
    fun resolveTargetPosition(
        anchor: ThreadScrollAnchor,
        data: List<PostItemData>,
        baseOffset: Int,
        totalItemsCount: Int,
    ): Pair<Int, Int>? {
        if (totalItemsCount <= 0) return null
        val anchorId = anchor.anchorPostId
        if (anchorId != null && anchorId > 0L) {
            val idxInData = data.indexOfFirst { it.id == anchorId }
            if (idxInData != -1) {
                val targetIndex = (baseOffset + idxInData).coerceIn(0, totalItemsCount - 1)
                return targetIndex to anchor.anchorPostScrollOffset.coerceAtLeast(0)
            }
        }
        val clampedIndex = anchor.firstVisibleItemIndex.coerceIn(0, totalItemsCount - 1)
        return clampedIndex to anchor.firstVisibleItemScrollOffset.coerceAtLeast(0)
    }
}

/**
 * Thread-safe bounded LRU registry retaining recent thread scroll anchors across
 * navigation, back-stack recreation, and transient list reloads.
 */
class ThreadScrollAnchorStore(
    private val maxEntries: Int = 64,
) {
    private val cache = object : LinkedHashMap<ThreadAnchorKey, ThreadScrollAnchor>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ThreadAnchorKey, ThreadScrollAnchor>?): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun save(key: ThreadAnchorKey, anchor: ThreadScrollAnchor) {
        if (anchor.firstVisibleItemIndex == 0 && anchor.firstVisibleItemScrollOffset == 0 && anchor.anchorPostId == null) {
            cache.remove(key)
            return
        }
        cache[key] = anchor
    }

    @Synchronized
    fun get(key: ThreadAnchorKey): ThreadScrollAnchor? = cache[key]

    @Synchronized
    fun remove(key: ThreadAnchorKey) {
        cache.remove(key)
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    @Synchronized
    fun size(): Int = cache.size
}

object ThreadScrollAnchorRegistry {
    private val store = ThreadScrollAnchorStore()

    fun save(key: ThreadAnchorKey, anchor: ThreadScrollAnchor) = store.save(key, anchor)
    fun get(key: ThreadAnchorKey): ThreadScrollAnchor? = store.get(key)
    fun remove(key: ThreadAnchorKey) = store.remove(key)
    fun clear() = store.clear()
}

private fun captureAnchor(lazyListState: LazyListState): ThreadScrollAnchor {
    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
    val firstPostItem = visibleItems.firstOrNull { it.key is Long }
    val anchorPostId = firstPostItem?.key as? Long
    val anchorPostOffset = if (firstPostItem != null) (-firstPostItem.offset).coerceAtLeast(0) else 0
    return ThreadScrollAnchor(
        firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset,
        anchorPostId = anchorPostId,
        anchorPostScrollOffset = anchorPostOffset,
    )
}

/**
 * Automatically saves and restores scroll anchor (postId + pixel offset) for [ThreadPostList].
 */
@Composable
fun ThreadScrollAnchorEffect(
    lazyListState: LazyListState,
    data: List<PostItemData>,
    hasPrevious: Boolean,
    threadId: Long,
    isSeeLz: Boolean,
    sortType: Int,
    disableAutoRestore: Boolean,
) {
    val anchorKey = remember(threadId, isSeeLz, sortType) {
        ThreadAnchorKey(threadId, isSeeLz, sortType)
    }
    var restoredForKey by remember(anchorKey) { mutableStateOf(false) }
    val currentData by rememberUpdatedState(data)
    val currentHasPrevious by rememberUpdatedState(hasPrevious)

    LaunchedEffect(anchorKey, data.isNotEmpty(), disableAutoRestore) {
        if (restoredForKey || disableAutoRestore || data.isEmpty()) return@LaunchedEffect
        val saved = ThreadScrollAnchorRegistry.get(anchorKey)
        restoredForKey = true
        if (saved != null) {
            val baseOffset = 2 + (if (currentHasPrevious) 1 else 0)
            val totalItems = baseOffset + data.size + 1
            val target = ThreadScrollAnchorResolver.resolveTargetPosition(
                anchor = saved,
                data = data,
                baseOffset = baseOffset,
                totalItemsCount = totalItems
            )
            if (target != null && (target.first > 0 || target.second > 0)) {
                lazyListState.scrollToItem(target.first, target.second)
            }
        }
    }

    LaunchedEffect(anchorKey, lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                if (currentData.isNotEmpty()) {
                    ThreadScrollAnchorRegistry.save(anchorKey, captureAnchor(lazyListState))
                }
            }
    }

    DisposableEffect(anchorKey) {
        onDispose {
            if (currentData.isNotEmpty()) {
                ThreadScrollAnchorRegistry.save(anchorKey, captureAnchor(lazyListState))
            }
        }
    }
}
