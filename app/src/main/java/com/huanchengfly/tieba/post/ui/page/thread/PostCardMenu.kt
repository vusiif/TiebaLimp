package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.launch

@Composable
fun PostCardMenuContent(
    post: Post,
    isCollected: Boolean,
    canDelete: Boolean,
    isFold: Boolean,
    onReplyClick: () -> Unit,
    onCopyClick: ((String) -> Unit)?,
    onFavoriteClick: ((Post) -> Unit)?,
    onBookmarkClick: ((Post) -> Unit)?,
    onDeleteClick: ((Post) -> Unit)?,
    onToggleFold: () -> Unit,
    onDismissMenu: () -> Unit,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()

    if (!context.appPreferences.hideReply) {
        DropdownMenuItem(
            onClick = {
                onReplyClick()
                onDismissMenu()
            }
        ) {
            Text(text = stringResource(id = R.string.btn_reply))
        }
    }
    if (onCopyClick != null) {
        DropdownMenuItem(
            onClick = {
                onCopyClick(post.content.plainText)
                onDismissMenu()
            }
        ) {
            Text(text = stringResource(id = R.string.menu_copy))
        }
    }
    DropdownMenuItem(
        onClick = {
            coroutineScope.launch {
                TiebaUtil.reportPost(context, navigator, post.id.toString())
            }
            onDismissMenu()
        }
    ) {
        Text(text = stringResource(id = R.string.title_report))
    }
    if (onFavoriteClick != null) {
        DropdownMenuItem(
            onClick = {
                onFavoriteClick(post)
                onDismissMenu()
            }
        ) {
            if (isCollected) {
                Text(text = stringResource(id = R.string.title_collect_on))
            } else {
                Text(text = stringResource(id = R.string.title_collect_floor))
            }
        }
    }
    if (onBookmarkClick != null) {
        DropdownMenuItem(
            onClick = {
                onBookmarkClick(post)
                onDismissMenu()
            }
        ) {
            Text(text = stringResource(id = R.string.title_add_floor_bookmark))
        }
    }
    if (canDelete && onDeleteClick != null) {
        DropdownMenuItem(
            onClick = {
                onDeleteClick(post)
                onDismissMenu()
            }
        ) {
            Text(text = stringResource(id = R.string.title_delete))
        }
    }
    if (isFold) {
        DropdownMenuItem(
            onClick = {
                onToggleFold()
                onDismissMenu()
            }
        ) {
            Text(text = stringResource(id = R.string.btn_fold_floor))
        }
    }
}
