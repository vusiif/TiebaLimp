package com.huanchengfly.tieba.post.ui.page.reply

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BaseDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

data class ReplyArgs(
    val forumId: Long,
    val forumName: String,
    val threadId: Long,
    val postId: Long? = null,
    val subPostId: Long? = null,
    val replyUserId: Long? = null,
    val replyUserName: String? = null,
    val replyUserPortrait: String? = null,
    val tbs: String? = null,
)

@Composable
fun ReplyDialog(
    args: ReplyArgs,
    state: DialogState = rememberDialogState(),
) {
    BaseDialog(
        dialogState = state,
        imePadding = false,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = ExtendedTheme.colors.windowBackground,
            elevation = 0.dp,
        ) {
            ReplyPageContent(
                viewModel = pageViewModel(),
                onBack = { dismiss() },
                forumId = args.forumId,
                forumName = args.forumName,
                threadId = args.threadId,
                postId = args.postId,
                subPostId = args.subPostId,
                replyUserId = args.replyUserId,
                replyUserName = args.replyUserName,
                replyUserPortrait = args.replyUserPortrait,
                tbs = args.tbs,
                isDialog = true
            )
        }
    }
}
