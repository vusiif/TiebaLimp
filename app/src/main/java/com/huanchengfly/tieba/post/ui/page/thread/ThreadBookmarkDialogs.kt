package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.models.database.FloorBookmark
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.FloorBookmarkEditDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.FloorBookmarksDialog
import com.huanchengfly.tieba.post.utils.FloorBookmarkUtil
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
fun ThreadBookmarkDialogs(
    editDialogState: DialogState,
    bookmarksDialogState: DialogState,
    targetPost: Post?,
    initialNote: String,
    isExisting: Boolean,
    threadBookmarks: List<FloorBookmark>,
    threadId: Long,
    threadTitle: String,
    onBookmarksChanged: () -> Unit,
    onSelectBookmark: (FloorBookmark) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    FloorBookmarkEditDialog(
        dialogState = editDialogState,
        floor = targetPost?.floor ?: 0,
        authorName = targetPost?.author?.nameShow.takeIf { !it.isNullOrEmpty() }
            ?: targetPost?.author?.name.orEmpty(),
        contentSnippet = targetPost?.content?.plainText.orEmpty(),
        initialNote = initialNote,
        isExisting = isExisting,
        onSave = { note ->
            val target = targetPost ?: return@FloorBookmarkEditDialog
            coroutineScope.launch {
                FloorBookmarkUtil.addOrUpdateBookmark(
                    threadId = threadId,
                    threadTitle = threadTitle,
                    postId = target.id,
                    floor = target.floor,
                    authorName = target.author?.nameShow.takeIf { !it.isNullOrEmpty() }
                        ?: target.author?.name.orEmpty(),
                    contentSnippet = target.content.plainText,
                    note = note
                )
                context.toastShort(R.string.toast_bookmark_saved)
                onBookmarksChanged()
            }
        },
        onDelete = {
            val target = targetPost ?: return@FloorBookmarkEditDialog
            coroutineScope.launch {
                FloorBookmarkUtil.deleteBookmark(threadId, target.id)
                context.toastShort(R.string.toast_bookmark_deleted)
                onBookmarksChanged()
            }
        }
    )

    val immutableBookmarks = remember(threadBookmarks) { threadBookmarks.toImmutableList() }
    FloorBookmarksDialog(
        dialogState = bookmarksDialogState,
        bookmarks = immutableBookmarks,
        onSelectBookmark = onSelectBookmark,
        onDeleteBookmark = { bookmark ->
            coroutineScope.launch {
                FloorBookmarkUtil.deleteBookmarkById(bookmark.id)
                context.toastShort(R.string.toast_bookmark_deleted)
                onBookmarksChanged()
            }
        }
    )
}
