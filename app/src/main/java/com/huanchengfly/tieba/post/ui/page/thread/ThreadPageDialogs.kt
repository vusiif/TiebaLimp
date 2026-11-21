package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Composable
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.models.database.FloorBookmark
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchHit
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchJumpDialog

/**
 * Aggregates all interactive dialogs displayed in ThreadPage:
 * - UpdateCollectMarkDialog
 * - ConfirmDeletePostDialog
 * - ThreadBookmarkDialogs
 * - ThreadSearchJumpDialog
 */
@Composable
fun ThreadPageDialogs(
    updateCollectMarkDialogState: DialogState,
    readFloorBeforeBack: Int,
    threadId: Long,
    lastVisibilityPostId: Long,
    onNavUp: () -> Unit,
    confirmDeleteDialogState: DialogState,
    deletePost: ImmutableHolder<Post>?,
    curForumId: Long?,
    onDeleteThread: () -> Unit,
    onDeletePost: (Long) -> Unit,
    bookmarkEditDialogState: DialogState,
    bookmarksDialogState: DialogState,
    bookmarkTargetPost: Post?,
    bookmarkInitialNote: String,
    bookmarkIsExisting: Boolean,
    threadBookmarks: List<FloorBookmark>,
    threadTitle: String,
    onBookmarksChanged: () -> Unit,
    onSelectBookmark: (FloorBookmark) -> Unit,
    searchJumpDialogState: DialogState,
    totalFloors: Int,
    currentPage: Int,
    totalPage: Int,
    firstPost: Post?,
    loadedPosts: List<PostItemData>,
    onJumpToFloor: (Int) -> Unit,
    onJumpToPage: (Int) -> Unit,
    onSelectSearchHit: (ThreadSearchHit, String, List<ThreadSearchHit>) -> Unit,
) {
    UpdateCollectMarkDialog(
        dialogState = updateCollectMarkDialogState,
        floor = readFloorBeforeBack,
        threadId = threadId,
        lastVisibilityPostId = lastVisibilityPostId,
        onNavUp = onNavUp
    )

    ConfirmDeletePostDialog(
        dialogState = confirmDeleteDialogState,
        deletePost = deletePost,
        curForumId = curForumId,
        onDeleteThread = onDeleteThread,
        onDeletePost = onDeletePost
    )

    ThreadBookmarkDialogs(
        editDialogState = bookmarkEditDialogState,
        bookmarksDialogState = bookmarksDialogState,
        targetPost = bookmarkTargetPost,
        initialNote = bookmarkInitialNote,
        isExisting = bookmarkIsExisting,
        threadBookmarks = threadBookmarks,
        threadId = threadId,
        threadTitle = threadTitle,
        onBookmarksChanged = onBookmarksChanged,
        onSelectBookmark = onSelectBookmark
    )

    ThreadSearchJumpDialog(
        dialogState = searchJumpDialogState,
        totalFloors = totalFloors,
        currentPage = currentPage,
        totalPage = totalPage,
        firstPost = firstPost,
        loadedPosts = loadedPosts,
        onJumpToFloor = onJumpToFloor,
        onJumpToPage = onJumpToPage,
        onSelectSearchHit = onSelectSearchHit
    )
}
