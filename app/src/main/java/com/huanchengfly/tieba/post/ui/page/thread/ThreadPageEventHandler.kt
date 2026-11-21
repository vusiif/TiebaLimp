package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent

/**
 * Handles UI events and global events for ThreadPage.
 */
@Composable
fun ThreadPageEventHandler(
    viewModel: ThreadViewModel,
    lazyListState: LazyListState,
    scaffoldState: ScaffoldState,
    threadId: Long,
    curForumId: Long?,
    curSortType: Int,
    data: List<PostItemData>,
    waitLoadSuccessAndScrollToFirstReply: Boolean,
    onClearWaitLoadSuccess: () -> Unit,
) {
    val context = LocalContext.current

    viewModel.onEvent<ThreadUiEvent.ScrollToFirstReply> {
        lazyListState.animateScrollToItem(1)
    }
    viewModel.onEvent<ThreadUiEvent.ScrollToLatestReply> {
        if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
            lazyListState.animateScrollToItem(2 + data.size)
        } else {
            lazyListState.animateScrollToItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.LoadSuccess> {
        if (it.page > 1 || waitLoadSuccessAndScrollToFirstReply) {
            onClearWaitLoadSuccess()
            lazyListState.animateScrollToItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.AddFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_add_favorite_success, it.floor)
        )
    }
    viewModel.onEvent<ThreadUiEvent.RemoveFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_remove_favorite_success)
        )
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(
        filter = { it.threadId == threadId }
    ) { event ->
        viewModel.send(
            ThreadUiIntent.LoadMyLatestReply(
                threadId = threadId,
                postId = event.newPostId,
                forumId = curForumId,
                isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                curLatestPostFloor = if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                    data.firstOrNull()?.post?.get { floor } ?: 1
                } else {
                    data.lastOrNull()?.post?.get { floor } ?: 1
                },
                curPostIds = data.map { it.post.get { id } },
            )
        )
    }
}
