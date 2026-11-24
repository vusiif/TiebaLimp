package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ScaffoldState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsSheetPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.ResumeReadingPill
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadGalleryImage
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadGalleryView
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchHit
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/**
 * Main viewport for reading a tieba thread:
 * Hosts gallery mode (ThreadGalleryView) or list mode (LoadMoreLayout + ThreadPostList),
 * pull-to-refresh indicator, resume reading pill, and search floating bar.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThreadReadingLayout(
    lazyListState: LazyListState,
    isGalleryMode: Boolean,
    galleryImages: List<ThreadGalleryImage>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    isLoadMoreError: Boolean = false,
    isRefreshing: Boolean,
    hasPrevious: Boolean,
    loadMoreEnd: Boolean,
    loadMorePreloadCount: Int,
    data: ImmutableList<PostItemData>,
    threadId: Long,
    forumId: Long?,
    curForumId: Long?,
    curSortType: Int,
    currentPageMax: Int,
    totalPage: Int,
    nextPagePostId: Long,
    isSeeLz: Boolean,
    isImmersiveMode: Boolean,
    highlightedFloor: Int,
    thread: ImmutableHolder<ThreadInfo>?,
    firstPost: ImmutableHolder<Post>?,
    firstPostContentRenders: ImmutableList<PbContentRender>,
    author: ImmutableHolder<User>?,
    user: ImmutableHolder<User>,
    forum: ImmutableHolder<SimpleForum>?,
    latestPosts: ImmutableList<PostItemData>,
    onReload: (() -> Unit)?,
    antiTbs: String?,
    viewModel: ThreadViewModel,
    navigator: DestinationsNavigator,
    pullRefreshState: PullRefreshState,
    enablePullRefresh: Boolean,
    readingProgressState: ThreadReadingProgressState,
    activeSearchKeyword: String,
    activeSearchHits: List<ThreadSearchHit>,
    currentHitIndex: Int,
    onHitIndexChange: (Int) -> Unit,
    onJumpToHit: (ThreadSearchHit) -> Unit,
    onCloseSearch: () -> Unit,
    onJumpToResume: () -> Unit,
    onOpenBookmarkEdit: (Post) -> Unit,
    onDeletePost: (ImmutableHolder<Post>?) -> Unit,
    onExitGalleryMode: () -> Unit,
    onPendingScrollToPost: (Long) -> Unit,
    scaffoldState: ScaffoldState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(state = pullRefreshState, enabled = enablePullRefresh)
    ) {
        if (isGalleryMode) {
            ThreadGalleryView(
                images = galleryImages,
                hasMore = hasMore,
                isLoadingMore = isLoadingMore,
                onLoadMore = {
                    if (hasMore) {
                        viewModel.send(
                            ThreadUiIntent.LoadMore(
                                threadId = threadId,
                                page = if (curSortType == ThreadSortType.SORT_TYPE_DESC) totalPage - currentPageMax
                                else currentPageMax + 1,
                                forumId = forumId,
                                postId = nextPagePostId,
                                seeLz = isSeeLz,
                                sortType = curSortType,
                                postIds = data.map { it.post.get { id } }
                            )
                        )
                    }
                },
                onJumpToFloor = { floor, targetPid ->
                    onExitGalleryMode()
                    if (floor == 1L) {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    } else {
                        val indexInData = data.indexOfFirst { it.post.get { id } == targetPid }
                        if (indexInData != -1) {
                            coroutineScope.launch {
                                val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                                lazyListState.animateScrollToItem(baseOffset + indexInData)
                            }
                        } else {
                            onPendingScrollToPost(targetPid)
                            viewModel.send(
                                ThreadUiIntent.Load(
                                    threadId = threadId,
                                    page = 0,
                                    postId = targetPid,
                                    forumId = curForumId,
                                    seeLz = isSeeLz,
                                    sortType = curSortType
                                )
                            )
                        }
                    }
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            String.format(context.getString(R.string.toast_jump_to_floor), floor)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val performLoadMore = remember(hasMore, curSortType, totalPage, currentPageMax, threadId, forumId, nextPagePostId, isSeeLz, data, curForumId, viewModel) {
                {
                    if (hasMore) {
                        viewModel.send(
                            ThreadUiIntent.LoadMore(
                                threadId = threadId,
                                page = if (curSortType == ThreadSortType.SORT_TYPE_DESC) totalPage - currentPageMax
                                else currentPageMax + 1,
                                forumId = forumId,
                                postId = nextPagePostId,
                                seeLz = isSeeLz,
                                sortType = curSortType,
                                postIds = data.map { it.post.get { id } }
                            )
                        )
                    } else if (data.isNotEmpty() && curSortType != ThreadSortType.SORT_TYPE_DESC) {
                        viewModel.send(
                            ThreadUiIntent.LoadLatestPosts(
                                threadId = threadId,
                                curLatestPostId = data.last().post.get { id },
                                forumId = curForumId,
                                seeLz = isSeeLz,
                                sortType = curSortType
                            )
                        )
                    }
                }
            }

            val onToggleSeeLzAction = remember(threadId, forumId, curSortType, viewModel) {
                { seeLz: Boolean ->
                    viewModel.send(
                        ThreadUiIntent.LoadFirstPage(
                            threadId = threadId,
                            forumId = forumId,
                            seeLz = seeLz,
                            sortType = curSortType
                        )
                    )
                }
            }
            val onLoadPreviousAction = remember(threadId, forumId, isSeeLz, curSortType, viewModel) {
                { page: Int, firstPostId: Long, postIds: List<Long> ->
                    viewModel.send(
                        ThreadUiIntent.LoadPrevious(
                            threadId,
                            page,
                            forumId,
                            postId = firstPostId,
                            seeLz = isSeeLz,
                            sortType = curSortType,
                            postIds = postIds
                        )
                    )
                }
            }
            val onAddFavoriteAction = remember(threadId, viewModel) {
                { p: Post ->
                    viewModel.send(
                        ThreadUiIntent.AddFavorite(
                            threadId,
                            p.id,
                            p.floor
                        )
                    )
                }
            }
            val onDeleteFirstPostAction = remember(onDeletePost) {
                { _: Post -> onDeletePost(null) }
            }
            val onUserClickAction = remember(navigator) {
                { id: Long -> navigator.navigate(UserProfilePageDestination(id)) }
            }
            val onAgreePostAction = remember(threadId, viewModel) {
                { post: ImmutableHolder<Post>, agree: Boolean ->
                    viewModel.send(
                        ThreadUiIntent.AgreePost(
                            threadId = threadId,
                            postId = post.get { id },
                            agree = agree
                        )
                    )
                }
            }
            val curForumName = forum?.get { name }.orEmpty()
            val onReplyClickAction = remember(curForumId, curForumName, threadId, navigator) {
                { p: Post ->
                    navigator.navigate(
                        ReplyPageDestination(
                            forumId = curForumId ?: 0,
                            forumName = curForumName,
                            threadId = threadId,
                            postId = p.id,
                            replyUserId = p.author?.id ?: p.author_id,
                            replyUserName = p.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                ?: p.author?.name,
                            replyUserPortrait = p.author?.portrait,
                        )
                    )
                }
            }
            val onSubPostReplyClickAction = remember(curForumId, curForumName, threadId, navigator) {
                { post: Post, subPost: SubPostList ->
                    navigator.navigate(
                        ReplyPageDestination(
                            forumId = curForumId ?: 0,
                            forumName = curForumName,
                            threadId = threadId,
                            postId = post.id,
                            subPostId = subPost.id,
                            replyUserId = subPost.author?.id ?: subPost.author_id,
                            replyUserName = subPost.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                ?: subPost.author?.name,
                            replyUserPortrait = subPost.author?.portrait,
                        )
                    )
                }
            }
            val onOpenSubPostsAction = remember(curForumId, threadId, navigator) {
                { postId: Long, subPostId: Long ->
                    if (curForumId != null) {
                        navigator.navigate(
                            SubPostsSheetPageDestination(
                                forumId = curForumId,
                                threadId = threadId,
                                postId = postId,
                                subPostId = subPostId,
                                loadFromSubPost = false
                            )
                        )
                    }
                }
            }
            val onMenuCopyClickAction = remember(navigator) {
                { text: String ->
                    navigator.navigate(
                        CopyTextDialogPageDestination(text)
                    )
                }
            }
            val onMenuFavoriteClickAction = remember(thread, forum, forumId, threadId, antiTbs, viewModel) {
                { p: Post ->
                    val isPostCollected =
                        p.id == thread?.get { collectMarkPid.toLongOrNull() }
                    val fid = forum?.get { id } ?: forumId
                    if (fid != null) {
                        if (isPostCollected) {
                            viewModel.send(
                                ThreadUiIntent.RemoveFavorite(
                                    threadId = threadId,
                                    forumId = fid,
                                    tbs = antiTbs
                                )
                            )
                        } else {
                            viewModel.send(
                                ThreadUiIntent.AddFavorite(
                                    threadId = threadId,
                                    postId = p.id,
                                    floor = p.floor
                                )
                            )
                        }
                    }
                }
            }
            val onMenuDeleteClickAction = remember(onDeletePost) {
                { p: Post ->
                    onDeletePost(p.wrapImmutable())
                }
            }

            LoadMoreLayout(
                isLoading = isLoadingMore,
                isError = isLoadMoreError,
                onLoadMore = performLoadMore,
                onRetry = performLoadMore,
                loadEnd = loadMoreEnd,
                indicator = { isLoading, end, willLoad ->
                    ThreadLoadMoreIndicator(
                        isLoading = isLoading,
                        loadMoreEnd = end,
                        willLoad = willLoad,
                        hasMore = hasMore,
                        isError = isLoadMoreError,
                        onRetry = performLoadMore
                    )
                },
                lazyListState = lazyListState,
                isEmpty = data.isEmpty(),
                preloadCount = loadMorePreloadCount,
            ) {
                ThreadPostList(
                    lazyListState = lazyListState,
                    threadId = threadId,
                    thread = thread,
                    firstPost = firstPost,
                    firstPostContentRenders = firstPostContentRenders,
                    author = author,
                    user = user,
                    forum = forum,
                    curForumId = curForumId,
                    isImmersiveMode = isImmersiveMode,
                    highlightedFloor = highlightedFloor,
                    isSeeLz = isSeeLz,
                    curSortType = curSortType,
                    currentPageMax = currentPageMax,
                    latestPosts = latestPosts,
                    hasPrevious = hasPrevious,
                    isRefreshing = isRefreshing,
                    data = data,
                    navigator = navigator,
                    onToggleSeeLz = onToggleSeeLzAction,
                    onLoadPrevious = onLoadPreviousAction,
                    onReload = onReload,
                    onAddFavorite = onAddFavoriteAction,
                    onOpenBookmarkEdit = onOpenBookmarkEdit,
                    onDeleteFirstPost = onDeleteFirstPostAction,
                    onUserClick = onUserClickAction,
                    onAgreePost = onAgreePostAction,
                    onReplyClick = onReplyClickAction,
                    onSubPostReplyClick = onSubPostReplyClickAction,
                    onOpenSubPosts = onOpenSubPostsAction,
                    onMenuCopyClick = onMenuCopyClickAction,
                    onMenuFavoriteClick = onMenuFavoriteClickAction,
                    onMenuDeleteClick = onMenuDeleteClickAction
                )
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.primary,
        )

        ResumeReadingPill(
            visible = readingProgressState.showResumePill && !isGalleryMode,
            floor = readingProgressState.readingProgress?.floor.orEmpty(),
            onJump = onJumpToResume,
            onDismiss = { readingProgressState.dismiss() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        ThreadSearchFloatingBar(
            visible = activeSearchKeyword.isNotBlank() && activeSearchHits.isNotEmpty() && !isGalleryMode,
            keyword = activeSearchKeyword,
            currentIndex = currentHitIndex,
            totalHits = activeSearchHits.size,
            onPreviousHit = {
                if (activeSearchHits.isNotEmpty()) {
                    val prevIndex = (currentHitIndex - 1 + activeSearchHits.size) % activeSearchHits.size
                    onHitIndexChange(prevIndex)
                    onJumpToHit(activeSearchHits[prevIndex])
                }
            },
            onNextHit = {
                if (activeSearchHits.isNotEmpty()) {
                    val nextIndex = (currentHitIndex + 1) % activeSearchHits.size
                    onHitIndexChange(nextIndex)
                    onJumpToHit(activeSearchHits[nextIndex])
                }
            },
            onClose = onCloseSearch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
    }
}
