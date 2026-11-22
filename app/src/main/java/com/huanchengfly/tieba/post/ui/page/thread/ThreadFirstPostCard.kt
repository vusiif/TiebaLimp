package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.OriginThreadCard
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.VoteCard
import com.huanchengfly.tieba.post.utils.PollVoteStore
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ThreadFirstPostCard(
    threadId: Long,
    thread: ImmutableHolder<ThreadInfo>?,
    firstPost: ImmutableHolder<Post>,
    firstPostContentRenders: ImmutableList<PbContentRender>,
    author: ImmutableHolder<User>?,
    user: ImmutableHolder<User>,
    forum: ImmutableHolder<SimpleForum>?,
    curForumId: Long?,
    isImmersiveMode: Boolean,
    isSearchHighlighted: Boolean,
    navigator: DestinationsNavigator,
    onAddFavorite: (Post) -> Unit,
    onOpenBookmarkEdit: (Post) -> Unit,
    onDeleteClick: (Post) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canDelete = remember(firstPost, user) {
        firstPost.get { author_id } == user.get { id }
    }
    val isCollected = remember(firstPost, thread) {
        firstPost.get { id } == thread?.get { collectMarkPid }?.toLongOrNull()
    }
    Container(modifier = modifier) {
        Column {
            PostCard(
                postHolder = firstPost,
                contentRenders = firstPostContentRenders,
                threadAuthorId = author?.get { id } ?: 0L,
                canDelete = canDelete,
                immersiveMode = isImmersiveMode,
                isSearchHighlighted = isSearchHighlighted,
                isCollected = isCollected,
                showSubPosts = false,
                onUserClick = {
                    navigator.navigate(
                        UserProfilePageDestination(it.id)
                    )
                },
                onReplyClick = {
                    navigator.navigate(
                        ReplyPageDestination(
                            forumId = curForumId ?: 0,
                            forumName = forum?.get { name }.orEmpty(),
                            threadId = threadId,
                        )
                    )
                },
                onMenuCopyClick = {
                    navigator.navigate(
                        CopyTextDialogPageDestination(it)
                    )
                },
                onMenuFavoriteClick = {
                    onAddFavorite(it)
                },
                onMenuBookmarkClick = {
                    onOpenBookmarkEdit(it)
                },
            ) {
                onDeleteClick(it)
            }

            thread?.getNullableImmutable { origin_thread_info }
                .takeIf { thread?.get { is_share_thread } == 1 }
                ?.let {
                    OriginThreadCard(
                        originThreadInfo = it,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .clickable {
                                navigator.navigate(
                                    ThreadPageDestination(
                                        threadId = it.get { tid.toLong() },
                                        forumId = it.get { fid },
                                    )
                                )
                            }
                            .padding(16.dp)
                    )
                }

            val effectiveForumId = (curForumId?.takeIf { it > 0 } ?: thread?.get { forumId }) ?: 0L
            var localPollInfo by remember(thread, threadId) {
                mutableStateOf(PollVoteStore.get(threadId, thread?.get { poll_info }))
            }
            localPollInfo?.takeIf { it.options.isNotEmpty() }?.let { poll ->
                VoteCard(
                    pollInfo = poll,
                    threadId = threadId,
                    forumId = effectiveForumId,
                    onVoteSuccess = { newPoll -> localPollInfo = newPoll },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                thickness = 2.dp
            )
        }
    }
}
