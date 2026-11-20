package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun UpdateCollectMarkDialog(
    dialogState: DialogState,
    floor: Int,
    threadId: Long,
    lastVisibilityPostId: Long,
    onNavUp: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    ConfirmDialog(
        dialogState = dialogState,
        onConfirm = {
            coroutineScope.launch {
                onNavUp()
                if (lastVisibilityPostId != 0L) {
                    TiebaApi.getInstance()
                        .addStoreFlow(threadId, lastVisibilityPostId)
                        .catch {
                            context.toastShort(
                                R.string.message_update_collect_mark_failed,
                                it.getErrorMessage()
                            )
                        }
                        .collect {
                            context.toastShort(R.string.message_update_collect_mark_success)
                        }
                }
            }
        },
        onCancel = onNavUp
    ) {
        Text(text = stringResource(R.string.message_update_collect_mark, floor))
    }
}

@Composable
fun ConfirmDeletePostDialog(
    dialogState: DialogState,
    deletePost: ImmutableHolder<Post>?,
    curForumId: Long?,
    onDeleteThread: () -> Unit,
    onDeletePost: (postId: Long) -> Unit,
) {
    ConfirmDialog(
        dialogState = dialogState,
        onConfirm = {
            curForumId ?: return@ConfirmDialog
            if (deletePost == null) {
                onDeleteThread()
            } else {
                onDeletePost(deletePost.get { id })
            }
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deletePost == null) stringResource(id = R.string.this_thread)
                else stringResource(id = R.string.tip_post_floor, deletePost.get { floor })
            )
        )
    }
}
