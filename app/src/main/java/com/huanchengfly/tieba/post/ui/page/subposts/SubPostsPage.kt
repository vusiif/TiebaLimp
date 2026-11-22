package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyArgs
import com.huanchengfly.tieba.post.ui.page.reply.ReplyDialog
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyleBottomSheet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Destination
@Composable
fun SubPostsPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@Destination(
    style = DestinationStyleBottomSheet::class
)
@Composable
fun SubPostsSheetPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            isSheet = true,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SubPostsContent(
    viewModel: SubPostsViewModel,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    isSheet: Boolean = false,
    onNavigateUp: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val account = LocalAccount.current

    LazyLoad(key = viewModel, loaded = viewModel.initialized) {
        viewModel.send(
            SubPostsUiIntent.Load(
                forumId,
                threadId,
                postId,
                subPostId.takeIf { loadFromSubPost } ?: 0L
            )
        )
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isRefreshing,
        initial = false
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isLoading,
        initial = false
    )
    val isLoadMoreError by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isLoadMoreError,
        initial = false
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::anti,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::forum,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::thread,
        initial = null
    )
    val post by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::post,
        initial = null
    )
    val postContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::postContentRenders,
        initial = persistentListOf()
    )
    val subPosts by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::subPosts,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::currentPage,
        initial = 1
    )
    val totalCount by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::totalCount,
        initial = 0
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::hasMore,
        initial = true
    )

    val lazyListState = rememberLazyListState()

    viewModel.onEvent<SubPostsUiEvent.ScrollToSubPosts> {
        delay(20)
        lazyListState.scrollToItem(2 + subPosts.indexOfFirst { it.id == subPostId })
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deleteSubPost by remember { mutableStateOf<ImmutableHolder<SubPostList>?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            if (deleteSubPost == null) {
                val isSelfPost = post?.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = postId,
                        deleteMyPost = isSelfPost,
                        tbs = anti?.get { tbs },
                    )
                )
            } else {
                val isSelfSubPost =
                    deleteSubPost!!.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = postId,
                        subPostId = deleteSubPost!!.get { id },
                        deleteMyPost = isSelfSubPost,
                        tbs = anti?.get { tbs },
                    )
                )
            }
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deleteSubPost == null && post != null) stringResource(
                    id = R.string.tip_post_floor,
                    post!!.get { floor })
                else stringResource(id = R.string.this_reply)
            )
        )
    }

    val replyDialogState = rememberDialogState()
    var currentReplyArgs by remember { mutableStateOf<ReplyArgs?>(null) }
    if (currentReplyArgs != null) {
        ReplyDialog(args = currentReplyArgs!!, state = replyDialogState)
    }

//    onGlobalEvent<GlobalEvent.ReplySuccess>(
//        filter = { it.threadId == threadId && it.postId == postId }
//    ) { event ->
//        viewModel.send(
//            SubPostsUiIntent.Load(
//                forumId,
//                threadId,
//                postId,
//                subPostId.takeIf { loadFromSubPost } ?: 0L
//            )
//        )
//    }

    fun showReplyDialog(args: ReplyArgs) {
        currentReplyArgs = args
        replyDialogState.show()
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = subPosts.isEmpty(),
        isError = false,
        isLoading = isRefreshing
    ) {
        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SubPostsToolbar(
                    postFloor = post?.get { floor },
                    isSheet = isSheet,
                    onNavigateUp = onNavigateUp,
                    onOpenOriginThread = {
                        navigator.navigate(
                            ThreadPageDestination(
                                forumId = forumId,
                                threadId = threadId,
                                postId = postId
                            )
                        )
                    }
                )
            },
            bottomBar = {
                SubPostsBottomBar(
                    account = account,
                    onReplyClick = {
                        val fid = forum?.get { id } ?: forumId
                        val forumName = forum?.get { name }
                        if (!forumName.isNullOrEmpty()) {
                            showReplyDialog(
                                ReplyArgs(
                                    forumId = fid,
                                    forumName = forumName,
                                    threadId = threadId,
                                    postId = postId,
                                )
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            val onUserNavigate: (User) -> Unit = remember(navigator) {
                { u: User -> navigator.navigate(UserProfilePageDestination(u.id)) }
            }
            val onCopyNavigate: (String) -> Unit = remember(navigator) {
                { text: String -> navigator.navigate(CopyTextDialogPageDestination(text)) }
            }
            val onPostAgree: () -> Unit = remember(post, viewModel) {
                {
                    val hasAgreed = post?.get { agree?.hasAgree != 0 } == true
                    viewModel.send(
                        SubPostsUiIntent.Agree(
                            forumId,
                            threadId,
                            postId,
                            agree = !hasAgreed
                        )
                    )
                }
            }
            val currentForumName = forum?.get { name }.orEmpty()
            val onPostReplyClick: (Post) -> Unit = remember(currentForumName) {
                { p: Post ->
                    showReplyDialog(
                        ReplyArgs(
                            forumId = forumId,
                            forumName = currentForumName,
                            threadId = threadId,
                            postId = postId,
                            replyUserId = p.author?.id ?: p.author_id,
                            replyUserName = p.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                ?: p.author?.name,
                            replyUserPortrait = p.author?.portrait,
                        )
                    )
                }
            }
            val onPostDeleteClick: (Post) -> Unit = remember {
                { _: Post ->
                    deleteSubPost = null
                    confirmDeleteDialogState.show()
                }
            }

            LoadMoreLayout(
                modifier = Modifier.padding(paddingValues),
                isLoading = isLoading,
                isError = isLoadMoreError,
                onLoadMore = {
                    viewModel.send(
                        SubPostsUiIntent.LoadMore(
                            forumId,
                            threadId,
                            postId,
//                            subPostId,
                            page = currentPage + 1,
                        )
                    )
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = post == null && subPosts.isEmpty(),
            ) {
                MyLazyColumn(state = lazyListState) {
                    item(key = "Post$postId") {
                        post?.let {
                            Column {
                                PostCard(
                                    postHolder = it,
                                    contentRenders = postContentRenders,
                                    canDelete = it.get { author_id } == account?.uid?.toLongOrNull(),
                                    showSubPosts = false,
                                    onUserClick = onUserNavigate,
                                    onAgree = onPostAgree,
                                    onReplyClick = onPostReplyClick,
                                    onMenuCopyClick = onCopyNavigate,
                                    onMenuDeleteClick = onPostDeleteClick,
                                )
                                VerticalDivider(thickness = 2.dp)
                            }
                        }
                    }
                    stickyHeader(key = "SubPostsHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExtendedTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.title_sub_posts_header,
                                    totalCount
                                ),
                                style = MaterialTheme.typography.subtitle1
                            )
                        }
                    }
                    itemsIndexed(
                        items = subPosts,
                        key = { _, subPost -> subPost.id }
                    ) { index, item ->
                        val canDelete = remember(item, account) {
                            item.subPost.get { author_id } == account?.uid?.toLongOrNull()
                        }
                        val onAgree = remember(item, forumId, threadId, postId, viewModel) {
                            { sp: SubPostList ->
                                val hasAgreed = sp.agree?.hasAgree != 0
                                viewModel.send(
                                    SubPostsUiIntent.Agree(
                                        forumId,
                                        threadId,
                                        postId,
                                        subPostId = sp.id,
                                        agree = !hasAgreed
                                    )
                                )
                            }
                        }
                        val forumName = forum?.get { name } ?: ""
                        val onReplyClick = remember(item, forumId, forumName, threadId, postId) {
                            { sp: SubPostList ->
                                showReplyDialog(
                                    ReplyArgs(
                                        forumId = forumId,
                                        forumName = forumName,
                                        threadId = threadId,
                                        postId = postId,
                                        subPostId = sp.id,
                                        replyUserId = sp.author?.id ?: sp.author_id,
                                        replyUserName = sp.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                            ?: sp.author?.name,
                                        replyUserPortrait = sp.author?.portrait,
                                    )
                                )
                            }
                        }
                        val onMenuDeleteClick = remember(item) {
                            { sp: SubPostList ->
                                deleteSubPost = sp.wrapImmutable()
                                confirmDeleteDialogState.show()
                            }
                        }
                        SubPostItem(
                            item = item,
                            canDelete = canDelete,
                            threadAuthorId = thread?.get { author?.id },
                            onUserClick = onUserNavigate,
                            onAgree = onAgree,
                            onReplyClick = onReplyClick,
                            onMenuCopyClick = onCopyNavigate,
                            onMenuDeleteClick = onMenuDeleteClick,
                        )
                    }
                }
            }
        }
    }
}
