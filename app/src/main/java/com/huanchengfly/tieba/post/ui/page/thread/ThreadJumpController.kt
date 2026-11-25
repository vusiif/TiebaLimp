package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.FloorBookmark
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchHit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Controller managing floor/post jumping, search navigation, and reading progress resumption.
 */
@Stable
class ThreadJumpController(
    val lazyListState: LazyListState,
    val coroutineScope: CoroutineScope,
    val getPostsData: () -> List<PostItemData>,
    val hasPrevious: () -> Boolean,
    val threadId: Long,
    val curForumId: () -> Long?,
    val isSeeLz: () -> Boolean,
    val curSortType: () -> Int,
    val viewModel: ThreadViewModel,
    val readingProgressState: ThreadReadingProgressState,
    val onToast: (String) -> Unit,
) {
    var pendingScrollToPostId by mutableStateOf<Long?>(null)
    var pendingScrollToFloor by mutableStateOf<Int?>(null)
    var activeSearchKeyword by mutableStateOf("")
    var activeSearchHits by mutableStateOf<List<ThreadSearchHit>>(emptyList())
    var currentHitIndex by mutableIntStateOf(0)
    var highlightedFloor by mutableIntStateOf(-1)

    private fun scrollToDataIndex(indexInData: Int) {
        coroutineScope.launch {
            val baseOffset = 2 + (if (hasPrevious()) 1 else 0)
            lazyListState.animateScrollToItem(baseOffset + indexInData)
        }
    }

    fun onJumpToFloor(targetFloor: Int) {
        highlightedFloor = targetFloor
        if (targetFloor <= 1) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        } else {
            val data = getPostsData()
            val indexInData = data.indexOfFirst { it.floor == targetFloor }
            if (indexInData != -1) {
                scrollToDataIndex(indexInData)
            } else {
                pendingScrollToFloor = targetFloor
                val targetPage = maxOf((targetFloor - 1) / 30 + 1, 1)
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId = threadId,
                        forumId = curForumId(),
                        page = targetPage,
                        seeLz = isSeeLz(),
                        sortType = curSortType()
                    )
                )
            }
        }
    }

    fun onJumpToHit(hit: ThreadSearchHit) {
        highlightedFloor = hit.floor
        if (hit.floor <= 1) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        } else {
            val data = getPostsData()
            val indexInData = data.indexOfFirst { it.floor == hit.floor || it.id == hit.postId }
            if (indexInData != -1) {
                scrollToDataIndex(indexInData)
            } else {
                pendingScrollToPostId = hit.postId
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId = threadId,
                        page = 0,
                        postId = hit.postId,
                        forumId = curForumId(),
                        seeLz = isSeeLz(),
                        sortType = curSortType()
                    )
                )
            }
        }
    }

    fun onJumpToResume() {
        readingProgressState.readingProgress?.let { progress ->
            val targetPid = progress.pid?.toLongOrNull() ?: 0L
            val targetFloor = progress.floor.orEmpty()
            readingProgressState.dismiss()
            if (targetPid > 0L) {
                val data = getPostsData()
                val indexInData = data.indexOfFirst { it.id == targetPid }
                if (indexInData != -1) {
                    scrollToDataIndex(indexInData)
                } else {
                    pendingScrollToPostId = targetPid
                    viewModel.send(
                        ThreadUiIntent.Load(
                            threadId = threadId,
                            page = 0,
                            postId = targetPid,
                            forumId = curForumId(),
                            seeLz = isSeeLz(),
                            sortType = curSortType()
                        )
                    )
                }
                onToast(targetFloor)
            }
        }
    }

    fun onSelectBookmark(bookmark: FloorBookmark, firstPostId: Long) {
        val targetPid = bookmark.postId.toLongOrNull() ?: 0L
        val targetFloor = bookmark.floor
        if (targetFloor == 1 || (targetPid > 0L && firstPostId == targetPid)) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
            onToast("1")
        } else if (targetPid > 0L) {
            val data = getPostsData()
            val indexInData = data.indexOfFirst { it.id == targetPid }
            if (indexInData != -1) {
                scrollToDataIndex(indexInData)
            } else {
                pendingScrollToPostId = targetPid
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId = threadId,
                        page = 0,
                        postId = targetPid,
                        forumId = curForumId(),
                        seeLz = isSeeLz(),
                        sortType = curSortType()
                    )
                )
            }
            onToast(targetFloor.toString())
        }
    }

    fun closeSearch() {
        activeSearchKeyword = ""
        activeSearchHits = emptyList()
        highlightedFloor = -1
    }
}

@Composable
fun rememberThreadJumpController(
    lazyListState: LazyListState,
    data: List<PostItemData>,
    hasPrevious: Boolean,
    threadId: Long,
    curForumId: Long?,
    isSeeLz: Boolean,
    curSortType: Int,
    viewModel: ThreadViewModel,
    readingProgressState: ThreadReadingProgressState,
): ThreadJumpController {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val controller = remember(threadId) {
        ThreadJumpController(
            lazyListState = lazyListState,
            coroutineScope = coroutineScope,
            getPostsData = { data },
            hasPrevious = { hasPrevious },
            threadId = threadId,
            curForumId = { curForumId },
            isSeeLz = { isSeeLz },
            curSortType = { curSortType },
            viewModel = viewModel,
            readingProgressState = readingProgressState,
            onToast = { floor ->
                context.toastShort(context.getString(R.string.toast_jump_to_reading_history, floor))
            },
        )
    }

    LaunchedEffect(data, controller.pendingScrollToPostId) {
        val targetId = controller.pendingScrollToPostId ?: return@LaunchedEffect
        val indexInData = data.indexOfFirst { it.id == targetId }
        if (indexInData != -1) {
            controller.pendingScrollToPostId = null
            val baseOffset = 2 + (if (hasPrevious) 1 else 0)
            lazyListState.animateScrollToItem(baseOffset + indexInData)
        }
    }

    LaunchedEffect(data, controller.pendingScrollToFloor) {
        val targetFloor = controller.pendingScrollToFloor ?: return@LaunchedEffect
        if (targetFloor <= 1) {
            controller.pendingScrollToFloor = null
            lazyListState.animateScrollToItem(0)
        } else {
            val indexInData = data.indexOfFirst { it.floor == targetFloor }
            if (indexInData != -1) {
                controller.pendingScrollToFloor = null
                val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                lazyListState.animateScrollToItem(baseOffset + indexInData)
            } else if (data.isNotEmpty()) {
                val closestIndex = data.indices.minByOrNull { kotlin.math.abs(data[it].floor - targetFloor) }
                if (closestIndex != null) {
                    controller.pendingScrollToFloor = null
                    val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                    lazyListState.animateScrollToItem(baseOffset + closestIndex)
                }
            }
        }
    }

    return controller
}
