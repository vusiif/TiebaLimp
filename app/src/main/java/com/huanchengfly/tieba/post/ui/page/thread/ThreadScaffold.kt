package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * Scaffold layout for ThreadPage that wires ThreadTopBar and ThreadBottomBar.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThreadScaffold(
    scaffoldState: ScaffoldState,
    forum: ImmutableHolder<SimpleForum>?,
    onBack: () -> Unit,
    navigator: DestinationsNavigator,
    isGalleryMode: Boolean,
    onToggleGalleryMode: () -> Unit,
    onSearchClick: () -> Unit,
    user: ImmutableHolder<User>,
    curForumId: Long?,
    threadId: Long,
    thread: ImmutableHolder<ThreadInfo>?,
    firstPost: ImmutableHolder<Post>?,
    hasThreadAgreed: Boolean,
    threadAgreeNum: Long,
    bottomSheetState: ModalBottomSheetState,
    openBottomSheet: () -> Unit,
    closeBottomSheet: () -> Unit,
    viewModel: ThreadViewModel,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    MyScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ThreadTopBar(
                forum = forum,
                onBack = onBack,
                onForumClick = {
                    val forumName = forum?.get { name }
                    if (forumName != null) {
                        navigator.navigate(ForumPageDestination(forumName))
                    }
                },
                isGalleryMode = isGalleryMode,
                onToggleGalleryMode = onToggleGalleryMode,
                onSearchClick = onSearchClick
            )
        },
        bottomBar = {
            ThreadBottomBar(
                user = user,
                onClickReply = {
                    navigator.navigate(
                        ReplyPageDestination(
                            forumId = curForumId ?: 0,
                            forumName = forum?.get { name }.orEmpty(),
                            threadId = threadId,
                        )
                    )
                },
                onAgree = {
                    val firstPostId =
                        thread?.get { firstPostId }.takeIf { it != 0L }
                            ?: firstPost?.get { id }
                            ?: 0L
                    if (firstPostId != 0L) {
                        viewModel.send(
                            ThreadUiIntent.AgreeThread(
                                threadId,
                                firstPostId,
                                !hasThreadAgreed
                            )
                        )
                    }
                },
                onClickMore = {
                    if (bottomSheetState.isVisible) {
                        closeBottomSheet()
                    } else {
                        openBottomSheet()
                    }
                },
                hasAgreed = hasThreadAgreed,
                agreeNum = threadAgreeNum,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        },
        modifier = modifier,
        content = content
    )
}
