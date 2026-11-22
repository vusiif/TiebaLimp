package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlignVerticalTop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.utils.BlockManager.FilterResult
import com.huanchengfly.tieba.post.utils.LazyListImagePreloadEffect
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.max

@Composable
private fun RenderPostCardItem(
    postItem: PostItemData,
    author: ImmutableHolder<User>?,
    user: ImmutableHolder<User>,
    thread: ImmutableHolder<ThreadInfo>?,
    isImmersiveMode: Boolean,
    isSearchHighlighted: Boolean,
    onUserClick: (Long) -> Unit,
    onAgreePost: (post: ImmutableHolder<Post>, agree: Boolean) -> Unit,
    onReplyClick: (Post) -> Unit,
    onSubPostReplyClick: (Post, SubPostList) -> Unit,
    onOpenSubPosts: (postId: Long, subPostId: Long) -> Unit,
    onMenuCopyClick: (String) -> Unit,
    onMenuFavoriteClick: (Post) -> Unit,
    onOpenBookmarkEdit: (Post) -> Unit,
    onMenuDeleteClick: (Post) -> Unit,
) {
    val (item, blocked, renders, subPosts, blockResult) = postItem
    val reason = (blockResult as? FilterResult.Blocked)?.reason
    val canDelete = remember(item, user) { item.get { author_id } == user.get { id } }
    val isCollected = remember(item, thread) {
        item.get { id } == thread?.get { collectMarkPid.toLongOrNull() }
    }
    val onCardUserClick = remember(onUserClick) {
        { u: User -> onUserClick(u.id) }
    }
    val onCardAgree = remember(item, onAgreePost) {
        {
            val postHasAgreed = item.get { agree?.hasAgree == 1 }
            onAgreePost(item, !postHasAgreed)
        }
    }
    val onCardOpenSubPosts = remember(item, onOpenSubPosts) {
        { subPostId: Long -> onOpenSubPosts(item.get { id }, subPostId) }
    }
    PostCard(
        postHolder = item,
        contentRenders = renders,
        subPosts = subPosts,
        threadAuthorId = author?.get { id } ?: 0L,
        blocked = blocked,
        blockReason = reason,
        isSearchHighlighted = isSearchHighlighted,
        canDelete = canDelete,
        immersiveMode = isImmersiveMode,
        isCollected = isCollected,
        onUserClick = onCardUserClick,
        onAgree = onCardAgree,
        onReplyClick = onReplyClick,
        onSubPostReplyClick = onSubPostReplyClick,
        onOpenSubPosts = onCardOpenSubPosts,
        onMenuCopyClick = onMenuCopyClick,
        onMenuFavoriteClick = onMenuFavoriteClick,
        onMenuBookmarkClick = onOpenBookmarkEdit,
        onMenuDeleteClick = onMenuDeleteClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadPostList(
    lazyListState: LazyListState,
    threadId: Long,
    thread: ImmutableHolder<ThreadInfo>?,
    firstPost: ImmutableHolder<Post>?,
    firstPostContentRenders: ImmutableList<PbContentRender>,
    author: ImmutableHolder<User>?,
    user: ImmutableHolder<User>,
    forum: ImmutableHolder<SimpleForum>?,
    curForumId: Long?,
    isImmersiveMode: Boolean,
    highlightedFloor: Int,
    isSeeLz: Boolean,
    curSortType: Int,
    currentPageMax: Int,
    latestPosts: ImmutableList<PostItemData>,
    hasPrevious: Boolean,
    isRefreshing: Boolean,
    data: ImmutableList<PostItemData>,
    navigator: DestinationsNavigator,
    onToggleSeeLz: (Boolean) -> Unit,
    onLoadPrevious: (page: Int, firstPostId: Long, postIds: List<Long>) -> Unit,
    onReload: (() -> Unit)? = null,
    onAddFavorite: (Post) -> Unit,
    onOpenBookmarkEdit: (Post) -> Unit,
    onDeleteFirstPost: (Post) -> Unit,
    onUserClick: (Long) -> Unit,
    onAgreePost: (post: ImmutableHolder<Post>, agree: Boolean) -> Unit,
    onReplyClick: (Post) -> Unit,
    onSubPostReplyClick: (Post, SubPostList) -> Unit,
    onOpenSubPosts: (postId: Long, subPostId: Long) -> Unit,
    onMenuCopyClick: (String) -> Unit,
    onMenuFavoriteClick: (Post) -> Unit,
    onMenuDeleteClick: (Post) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headerOffset = (if (firstPost != null) 1 else 0) + 1 + (if (hasPrevious) 1 else 0)
    LazyListImagePreloadEffect(
        state = lazyListState,
        items = data,
        preloadAhead = 5,
        itemOffset = headerOffset,
        extractUrls = { postItem ->
            postItem.contentRenders.filterIsInstance<PicContentRender>().map { it.picUrl }
        }
    )

    MyLazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxWidth()
    ) {
        item(key = "FirstPost") {
            if (firstPost != null) {
                ThreadFirstPostCard(
                    threadId = threadId,
                    thread = thread,
                    firstPost = firstPost,
                    firstPostContentRenders = firstPostContentRenders,
                    author = author,
                    user = user,
                    forum = forum,
                    curForumId = curForumId,
                    isImmersiveMode = isImmersiveMode,
                    isSearchHighlighted = highlightedFloor == 1,
                    navigator = navigator,
                    onAddFavorite = onAddFavorite,
                    onOpenBookmarkEdit = onOpenBookmarkEdit,
                    onDeleteClick = onDeleteFirstPost
                )
            }
        }

        stickyHeader(key = "ThreadHeader") {
            ThreadStickyHeader(
                replyCount = thread?.get { replyNum } ?: 0,
                isSeeLz = isSeeLz,
                onToggleSeeLz = onToggleSeeLz
            )
        }

        if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
            threadLatestPosts(
                latestPosts = latestPosts,
                desc = true,
                highlightedFloor = highlightedFloor,
            ) { postItem, isHighlighted ->
                RenderPostCardItem(
                    postItem = postItem,
                    author = author,
                    user = user,
                    thread = thread,
                    isImmersiveMode = isImmersiveMode,
                    isSearchHighlighted = isHighlighted,
                    onUserClick = onUserClick,
                    onAgreePost = onAgreePost,
                    onReplyClick = onReplyClick,
                    onSubPostReplyClick = onSubPostReplyClick,
                    onOpenSubPosts = onOpenSubPosts,
                    onMenuCopyClick = onMenuCopyClick,
                    onMenuFavoriteClick = onMenuFavoriteClick,
                    onOpenBookmarkEdit = onOpenBookmarkEdit,
                    onMenuDeleteClick = onMenuDeleteClick
                )
            }
        }

        if (hasPrevious) {
            item(key = "LoadPreviousBtn") {
                Container {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLoadPrevious(
                                    max(currentPageMax - 1, 1),
                                    data.first().post.get { id },
                                    data.map { it.post.get { id } }
                                )
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AlignVerticalTop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(id = R.string.btn_load_previous),
                            color = ExtendedTheme.colors.text,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (!isRefreshing && data.isEmpty()) {
            item(key = "EmptyTip") {
                Container {
                    TipScreen(
                        title = { Text(text = stringResource(id = R.string.title_empty)) },
                        image = {
                            val composition by rememberLottieComposition(
                                LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
                            )
                            LottieAnimation(
                                composition = composition,
                                iterations = LottieConstants.IterateForever,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f)
                            )
                        },
                        actions = {
                            if (onReload != null) {
                                Button(onClick = { onReload() }) {
                                    Text(text = stringResource(id = R.string.btn_refresh))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        scrollable = false
                    )
                }
            }
        } else {
            items(
                items = data,
                key = { (item) -> "Post_${item.get { id }}" },
                contentType = { (item, blocked) ->
                    if (blocked) "BlockedPost"
                    else if (item.get { is_fold == 1 || fold_comment_status != 0 }) "FoldedPost"
                    else "NormalPost"
                }
            ) { postItem ->
                Container {
                    RenderPostCardItem(
                        postItem = postItem,
                        author = author,
                        user = user,
                        thread = thread,
                        isImmersiveMode = isImmersiveMode,
                        isSearchHighlighted = highlightedFloor == postItem.post.get { floor },
                        onUserClick = onUserClick,
                        onAgreePost = onAgreePost,
                        onReplyClick = onReplyClick,
                        onSubPostReplyClick = onSubPostReplyClick,
                        onOpenSubPosts = onOpenSubPosts,
                        onMenuCopyClick = onMenuCopyClick,
                        onMenuFavoriteClick = onMenuFavoriteClick,
                        onOpenBookmarkEdit = onOpenBookmarkEdit,
                        onMenuDeleteClick = onMenuDeleteClick
                    )
                }
            }
        }

        if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
            threadLatestPosts(
                latestPosts = latestPosts,
                desc = false,
                highlightedFloor = highlightedFloor,
            ) { postItem, isHighlighted ->
                RenderPostCardItem(
                    postItem = postItem,
                    author = author,
                    user = user,
                    thread = thread,
                    isImmersiveMode = isImmersiveMode,
                    isSearchHighlighted = isHighlighted,
                    onUserClick = onUserClick,
                    onAgreePost = onAgreePost,
                    onReplyClick = onReplyClick,
                    onSubPostReplyClick = onSubPostReplyClick,
                    onOpenSubPosts = onOpenSubPosts,
                    onMenuCopyClick = onMenuCopyClick,
                    onMenuFavoriteClick = onMenuFavoriteClick,
                    onOpenBookmarkEdit = onOpenBookmarkEdit,
                    onMenuDeleteClick = onMenuDeleteClick
                )
            }
        }
    }
}
