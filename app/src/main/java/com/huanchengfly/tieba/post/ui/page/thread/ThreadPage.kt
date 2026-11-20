package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AlignVerticalTop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.FloorBookmark
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsSheetPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.BlockManager.FilterResult
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.FloorBookmarkUtil
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchHit
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchJumpDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.PredictiveBackBox
import com.huanchengfly.tieba.post.ui.widgets.compose.ResumeReadingPill
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadGalleryView
import com.huanchengfly.tieba.post.ui.widgets.compose.extractGalleryImages
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.math.max

object ThreadPageFrom {
    const val FROM_FORUM = "forum"

    // 鏀惰棌
    const val FROM_STORE = "store_thread"
    const val FROM_PERSONALIZED = "personalized"
    const val FROM_HISTORY = "history"
}

@Serializable
sealed interface ThreadPageExtra

@Serializable
data object ThreadPageNoExtra : ThreadPageExtra

@Serializable
data class ThreadPageFromStoreExtra(
    val maxPid: Long,
    val maxFloor: Int,
) : ThreadPageExtra

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://thread/{threadId}"),
    ]
)
@Composable
fun ThreadPage(
    threadId: Long,
    navigator: DestinationsNavigator,
    forumId: Long? = null,
    postId: Long = 0,
    seeLz: Boolean = false,
    sortType: Int = 0,
    from: String = "",
    extra: ThreadPageExtra? = null,
    threadInfo: ThreadInfo? = null,
    scrollToReply: Boolean = false,
    viewModel: ThreadViewModel = pageViewModel(),
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(
            ThreadUiIntent.Init(
                threadId,
                forumId,
                postId,
                threadInfo,
                seeLz,
                sortType
            )
        )
        viewModel.send(
            ThreadUiIntent.Load(
                threadId,
                page = 0,
                postId = postId,
                forumId = forumId,
                seeLz = seeLz,
                sortType = sortType,
                from = from
            )
        )
        viewModel.initialized = true
    }
    val scaffoldState = rememberScaffoldState()
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::data,
        initial = persistentListOf()
    )
    val author by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::author,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::threadInfo,
        initial = null
    )
    val firstPost by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPost,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::forum,
        initial = null
    )
    val user by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::user,
        initial = wrapImmutable(User())
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::anti,
        initial = null
    )
    val firstPostContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPostContentRenders,
        initial = persistentListOf()
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isLoadingMore,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isError,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::error,
        initial = null
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasMore,
        initial = true
    )
    val nextPagePostId by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::nextPagePostId,
        initial = 0L
    )
    val hasPrevious by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasPrevious,
        initial = true
    )
    val currentPageMax by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::currentPageMax,
        initial = 0
    )
    val totalPage by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::totalPage,
        initial = 0
    )
    val isSeeLz by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::seeLz,
        initial = seeLz
    )
    val curSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::sortType,
        initial = sortType
    )
    val isImmersiveMode by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isImmersiveMode,
        initial = false
    )
    val latestPosts by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::latestPosts,
        initial = persistentListOf()
    )

    var isGalleryMode by rememberSaveable { mutableStateOf(false) }
    val isEmpty by remember {
        derivedStateOf { data.isEmpty() && firstPost == null }
    }
    val enablePullRefresh by remember(isGalleryMode, hasPrevious, curSortType) {
        derivedStateOf {
            !isGalleryMode && (hasPrevious || curSortType == ThreadSortType.SORT_TYPE_DESC)
        }
    }
    val loadMoreEnd by remember {
        derivedStateOf {
            !hasMore && curSortType == ThreadSortType.SORT_TYPE_DESC
        }
    }
    val loadMorePreloadCount by remember {
        derivedStateOf {
            if (hasMore) {
                1
            } else {
                0
            }
        }
    }
    val isCollected = remember(thread) {
        thread?.get { collectStatus != 0 } == true
    }
    val hasThreadAgreed = remember(thread) {
        thread?.get { agree?.hasAgree == 1 } == true
    }
    val threadAgreeNum = remember(thread) {
        thread?.get { agree?.diffAgreeNum } ?: 0L
    }
    val threadTitle = remember(thread) {
        thread?.get { title } ?: ""
    }
    val curForumId = remember(forumId, forum) {
        forumId ?: forum?.get { id }
    }
    val curForumName = remember(forum) { forum?.get { name } }
    val curTbs = remember(anti) { anti?.get { tbs } }
    var waitLoadSuccessAndScrollToFirstReply by remember { mutableStateOf(scrollToReply) }

    val lazyListState = rememberLazyListState()
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val lastVisibilityPost by remember {
        derivedStateOf {
            data.firstOrNull { (post) ->
                val lastPostKey = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull { info ->
                    info.key is String && (info.key as String).startsWith("Post_")
                }?.key as String?
                lastPostKey?.endsWith(post.get { id }.toString()) == true
            }?.post ?: firstPost
        }
    }
    val lastVisibilityPostId by remember {
        derivedStateOf { lastVisibilityPost?.get { id } ?: 0L }
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var readingProgress by remember { mutableStateOf<ThreadHistoryInfoBean?>(null) }
    var showResumePill by remember { mutableStateOf(false) }
    var pendingScrollToPostId by remember { mutableStateOf<Long?>(null) }
    var pendingScrollToFloor by remember { mutableStateOf<Int?>(null) }
    var activeSearchKeyword by remember { mutableStateOf("") }
    var activeSearchHits by remember { mutableStateOf<List<ThreadSearchHit>>(emptyList()) }
    var currentHitIndex by remember { mutableIntStateOf(0) }
    var highlightedFloor by remember { mutableIntStateOf(-1) }
    val galleryImages by remember(firstPost, firstPostContentRenders, data) {
        derivedStateOf {
            extractGalleryImages(
                firstPost = firstPost?.get { this },
                firstPostContentRenders = firstPostContentRenders,
                posts = data
            )
        }
    }

    LaunchedEffect(threadId) {
        if (postId == 0L) {
            val progress = withContext(Dispatchers.IO) {
                runCatching { HistoryUtil.getThreadProgress(threadId) }.getOrNull()
            }
            val floor = progress?.floor?.toIntOrNull() ?: 0
            val pid = progress?.pid?.toLongOrNull() ?: 0L
            if (floor > 1 && pid > 0L) {
                readingProgress = progress
                showResumePill = true
            }
        }
    }

    LaunchedEffect(showResumePill) {
        if (showResumePill) {
            delay(8000)
            showResumePill = false
        }
    }

    LaunchedEffect(lastVisibilityPostId) {
        if (showResumePill) {
            val currentFloor = lastVisibilityPost?.get { floor } ?: 0
            val targetFloor = readingProgress?.floor?.toIntOrNull() ?: 0
            if (currentFloor > 5 || (targetFloor > 0 && currentFloor >= targetFloor)) {
                showResumePill = false
            }
        }
    }

    LaunchedEffect(data, pendingScrollToPostId) {
        val targetId = pendingScrollToPostId ?: return@LaunchedEffect
        val indexInData = data.indexOfFirst { it.post.get { id } == targetId }
        if (indexInData != -1) {
            pendingScrollToPostId = null
            val baseOffset = 2 + (if (hasPrevious) 1 else 0)
            lazyListState.animateScrollToItem(baseOffset + indexInData)
        }
    }

    LaunchedEffect(data, pendingScrollToFloor) {
        val targetFloor = pendingScrollToFloor ?: return@LaunchedEffect
        if (targetFloor <= 1) {
            pendingScrollToFloor = null
            lazyListState.animateScrollToItem(0)
        } else {
            val indexInData = data.indexOfFirst { it.post.get { floor } == targetFloor }
            if (indexInData != -1) {
                pendingScrollToFloor = null
                val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                lazyListState.animateScrollToItem(baseOffset + indexInData)
            } else if (data.isNotEmpty()) {
                val closestIndex = data.indices.minByOrNull { kotlin.math.abs(data[it].post.get { floor } - targetFloor) }
                if (closestIndex != null) {
                    pendingScrollToFloor = null
                    val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                    lazyListState.animateScrollToItem(baseOffset + closestIndex)
                }
            }
        }
    }

    val onJumpToFloor: (Int) -> Unit = { targetFloor ->
        highlightedFloor = targetFloor
        if (targetFloor <= 1) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        } else {
            val indexInData = data.indexOfFirst { it.post.get { floor } == targetFloor }
            if (indexInData != -1) {
                coroutineScope.launch {
                    val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                    lazyListState.animateScrollToItem(baseOffset + indexInData)
                }
            } else {
                pendingScrollToFloor = targetFloor
                val targetPage = maxOf((targetFloor - 1) / 30 + 1, 1)
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId = threadId,
                        forumId = curForumId,
                        page = targetPage,
                        seeLz = isSeeLz,
                        sortType = curSortType
                    )
                )
            }
        }
    }

    val onJumpToHit: (ThreadSearchHit) -> Unit = { hit ->
        highlightedFloor = hit.floor
        if (hit.floor <= 1) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        } else {
            val indexInData = data.indexOfFirst { it.post.get { floor } == hit.floor || it.post.get { id } == hit.postId }
            if (indexInData != -1) {
                coroutineScope.launch {
                    val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                    lazyListState.animateScrollToItem(baseOffset + indexInData)
                }
            } else {
                pendingScrollToPostId = hit.postId
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId = threadId,
                        page = 0,
                        postId = hit.postId,
                        forumId = curForumId,
                        seeLz = isSeeLz,
                        sortType = curSortType
                    )
                )
            }
        }
    }

    val onJumpToResume: () -> Unit = {
        readingProgress?.let { progress ->
            val targetPid = progress.pid?.toLongOrNull() ?: 0L
            val targetFloor = progress.floor.orEmpty()
            showResumePill = false
            if (targetPid > 0L) {
                val indexInData = data.indexOfFirst { it.post.get { id } == targetPid }
                if (indexInData != -1) {
                    coroutineScope.launch {
                        val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                        lazyListState.animateScrollToItem(baseOffset + indexInData)
                    }
                } else {
                    pendingScrollToPostId = targetPid
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
                context.toastShort(context.getString(R.string.toast_jump_to_reading_history, targetFloor))
            }
        }
    }

    val openBottomSheet = {
        coroutineScope.launch {
            bottomSheetState.show()
        }
    }
    val closeBottomSheet = {
        coroutineScope.launch {
            bottomSheetState.hide()
        }
    }

    MyBackHandler(
        enabled = bottomSheetState.isVisible,
        currentScreen = ThreadPageDestination
    ) {
        closeBottomSheet()
    }

    viewModel.onEvent<ThreadUiEvent.ScrollToFirstReply> {
        lazyListState.animateScrollToItem(1)
    }
    viewModel.onEvent<ThreadUiEvent.ScrollToLatestReply> {
        if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
            lazyListState.animateScrollToItem(2 + data.size)
        } else {
            lazyListState.animateScrollToItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.LoadSuccess> {
        if (it.page > 1 || waitLoadSuccessAndScrollToFirstReply) {
            waitLoadSuccessAndScrollToFirstReply = false
            lazyListState.animateScrollToItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.AddFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_add_favorite_success, it.floor)
        )
    }
    viewModel.onEvent<ThreadUiEvent.RemoveFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_remove_favorite_success)
        )
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(
        filter = { it.threadId == threadId }
    ) { event ->
        viewModel.send(
            ThreadUiIntent.LoadMyLatestReply(
                threadId = threadId,
                postId = event.newPostId,
                forumId = curForumId,
                isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                curLatestPostFloor = if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                    data.firstOrNull()?.post?.get { floor } ?: 1
                } else {
                    data.lastOrNull()?.post?.get { floor } ?: 1
                },
                curPostIds = data.map { it.post.get { id } },
            )
        )
    }

    val updateCollectMarkDialogState = rememberDialogState()
    var readFloorBeforeBack by remember {
        mutableIntStateOf(1)
    }
    UpdateCollectMarkDialog(
        dialogState = updateCollectMarkDialogState,
        floor = readFloorBeforeBack,
        threadId = threadId,
        lastVisibilityPostId = lastVisibilityPostId,
        onNavUp = { navigator.navigateUp() }
    )

    val confirmDeleteDialogState = rememberDialogState()
    var deletePost by remember { mutableStateOf<ImmutableHolder<Post>?>(null) }
    ConfirmDeletePostDialog(
        dialogState = confirmDeleteDialogState,
        deletePost = deletePost,
        curForumId = curForumId,
        onDeleteThread = {
            val isSelfThread = author?.get { id } == user.get { id }
            viewModel.send(
                ThreadUiIntent.DeleteThread(
                    forumId = curForumId!!,
                    forumName = curForumName.orEmpty(),
                    threadId = threadId,
                    deleteMyThread = isSelfThread,
                    tbs = curTbs
                )
            )
        },
        onDeletePost = { postId ->
            val isSelfPost = deletePost!!.get { author_id } == user.get { id }
            viewModel.send(
                ThreadUiIntent.DeletePost(
                    forumId = curForumId!!,
                    forumName = curForumName.orEmpty(),
                    threadId = threadId,
                    postId = postId,
                    deleteMyPost = isSelfPost,
                    tbs = curTbs
                )
            )
        }
    )

    var bookmarkTargetPost by remember { mutableStateOf<Post?>(null) }
    var bookmarkInitialNote by remember { mutableStateOf("") }
    var bookmarkIsExisting by remember { mutableStateOf(false) }
    val bookmarkEditDialogState = rememberDialogState()

    var threadBookmarks by remember { mutableStateOf<List<FloorBookmark>>(emptyList()) }
    val bookmarksDialogState = rememberDialogState()

    val openBookmarkEdit: (Post) -> Unit = { post ->
        bookmarkTargetPost = post
        coroutineScope.launch {
            val existing = FloorBookmarkUtil.findBookmark(threadId, post.id)
            bookmarkIsExisting = existing != null
            bookmarkInitialNote = existing?.note.orEmpty()
            bookmarkEditDialogState.show()
        }
    }

    ThreadBookmarkDialogs(
        editDialogState = bookmarkEditDialogState,
        bookmarksDialogState = bookmarksDialogState,
        targetPost = bookmarkTargetPost,
        initialNote = bookmarkInitialNote,
        isExisting = bookmarkIsExisting,
        threadBookmarks = threadBookmarks,
        threadId = threadId,
        threadTitle = threadTitle,
        onBookmarksChanged = {
            coroutineScope.launch {
                threadBookmarks = FloorBookmarkUtil.getBookmarks(threadId)
            }
        },
        onSelectBookmark = { bookmark ->
            val targetPid = bookmark.postId.toLongOrNull() ?: 0L
            val targetFloor = bookmark.floor
            if (targetFloor == 1 || (targetPid > 0L && firstPost?.get { id } == targetPid)) {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
                context.toastShort(context.getString(R.string.toast_jump_to_reading_history, "1"))
            } else if (targetPid > 0L) {
                val indexInData = data.indexOfFirst { it.post.get { id } == targetPid }
                if (indexInData != -1) {
                    coroutineScope.launch {
                        val baseOffset = 2 + (if (hasPrevious) 1 else 0)
                        lazyListState.animateScrollToItem(baseOffset + indexInData)
                    }
                } else {
                    pendingScrollToPostId = targetPid
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
                context.toastShort(context.getString(R.string.toast_jump_to_reading_history, targetFloor.toString()))
            }
        }
    )

    val searchJumpDialogState = rememberDialogState()
    val totalFloorCount = thread?.get { replyNum } ?: (data.lastOrNull()?.post?.get { floor } ?: 1)
    ThreadSearchJumpDialog(
        dialogState = searchJumpDialogState,
        totalFloors = totalFloorCount,
        currentPage = currentPageMax,
        totalPage = totalPage,
        firstPost = firstPost?.get(),
        loadedPosts = data,
        onJumpToFloor = onJumpToFloor,
        onJumpToPage = { targetPage ->
            viewModel.send(
                ThreadUiIntent.Load(
                    threadId = threadId,
                    forumId = forum?.get { id } ?: forumId,
                    page = targetPage,
                    seeLz = isSeeLz,
                    sortType = curSortType
                )
            )
        },
        onSelectSearchHit = { hit, query, hits ->
            activeSearchKeyword = query
            activeSearchHits = hits
            currentHitIndex = hits.indexOf(hit).coerceAtLeast(0)
            onJumpToHit(hit)
        }
    )

    LaunchedEffect(Unit) {
        if (from == ThreadPageFrom.FROM_STORE && extra is ThreadPageFromStoreExtra && extra.maxPid != postId) {
            val result = scaffoldState.snackbarHostState.showSnackbar(
                context.getString(R.string.message_store_thread_update, extra.maxFloor),
                context.getString(R.string.button_load_new),
                SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId,
                        page = 0,
                        postId = extra.maxPid,
                        forumId = forumId,
                        seeLz = seeLz,
                        sortType = sortType
                    )
                )
            }
        }
    }

    var savedHistory by remember(threadId) { mutableStateOf(false) }
    LaunchedEffect(threadId, threadTitle, author, lastVisibilityPostId) {
        if (threadTitle.isBlank()) return@LaunchedEffect

        if (savedHistory && lastVisibilityPostId != 0L) {
            delay(600)
        }

        withContext(Dispatchers.IO) {
            runCatching {
                HistoryUtil.saveHistory(
                    History(
                        title = threadTitle,
                        data = threadId.toString(),
                        type = HistoryUtil.TYPE_THREAD,
                        extras = ThreadHistoryInfoBean(
                            isSeeLz = isSeeLz,
                            pid = lastVisibilityPostId.toString(),
                            forumName = forum?.get { name },
                            floor = lastVisibilityPost?.get { floor }?.toString()
                        ).toJson(),
                        avatar = StringUtil.getAvatarUrl(author?.get { portrait }),
                        username = author?.get { nameShow }
                    ),
                    async = false,
                    incrementCount = !savedHistory
                )
                savedHistory = true
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.send(
                ThreadUiIntent.LoadFirstPage(
                    threadId,
                    forumId,
                    isSeeLz,
                    curSortType
                )
            )
        }
    )

    @Composable
    fun PostCard(
        item: ImmutableHolder<Post>,
        contentRenders: ImmutableList<PbContentRender>,
        subPosts: ImmutableList<SubPostItemData>,
        blocked: Boolean,
        blockReason: String? = null,
        isSearchHighlighted: Boolean = false,
    ) {
        PostCard(
            postHolder = item,
            contentRenders = contentRenders,
            subPosts = subPosts,
            threadAuthorId = author?.get { id } ?: 0L,
            blocked = blocked,
            blockReason = blockReason,
            isSearchHighlighted = isSearchHighlighted,
            canDelete = { it.author_id == user.get { id } },
            immersiveMode = isImmersiveMode,
            isCollected = { it.id == thread?.get { collectMarkPid.toLongOrNull() } },
            onUserClick = {
                navigator.navigate(UserProfilePageDestination(it.id))
            },
            onAgree = {
                val postHasAgreed =
                    item.get { agree?.hasAgree == 1 }
                viewModel.send(
                    ThreadUiIntent.AgreePost(
                        threadId = threadId,
                        postId = item.get { id },
                        agree = !postHasAgreed
                    )
                )
            },
            onReplyClick = {
                navigator.navigate(
                    ReplyPageDestination(
                        forumId = curForumId ?: 0,
                        forumName = forum?.get { name } ?: "",
                        threadId = threadId,
                        postId = it.id,
                        replyUserId = it.author?.id ?: it.author_id,
                        replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                            ?: it.author?.name,
                        replyUserPortrait = it.author?.portrait,
                    )
                )
            },
            onSubPostReplyClick = { post, subPost ->
                navigator.navigate(
                    ReplyPageDestination(
                        forumId = curForumId ?: 0,
                        forumName = forum?.get { name } ?: "",
                        threadId = threadId,
                        postId = post.id,
                        subPostId = subPost.id,
                        replyUserId = subPost.author?.id ?: subPost.author_id,
                        replyUserName = subPost.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                            ?: subPost.author?.name,
                        replyUserPortrait = subPost.author?.portrait,
                    )
                )
            },
            onOpenSubPosts = {
                if (curForumId != null) {
                    navigator.navigate(
                        SubPostsSheetPageDestination(
                            forumId = curForumId,
                            threadId = threadId,
                            postId = item.get { id },
                            subPostId = it,
                            loadFromSubPost = false
                        )
                    )
                }
            },
            onMenuCopyClick = {
                navigator.navigate(
                    CopyTextDialogPageDestination(it)
                )
            },
            onMenuFavoriteClick = {
                val isPostCollected =
                    it.id == thread?.get { collectMarkPid.toLongOrNull() }
                val fid = forum?.get { id } ?: forumId
                val tbs = anti?.get { tbs }
                if (fid != null) {
                    if (isPostCollected) {
                        viewModel.send(
                            ThreadUiIntent.RemoveFavorite(
                                threadId = threadId,
                                forumId = fid,
                                tbs = tbs
                            )
                        )
                    } else {
                        viewModel.send(
                            ThreadUiIntent.AddFavorite(
                                threadId = threadId,
                                postId = it.id,
                                floor = it.floor
                            )
                        )
                    }
                }
            },
            onMenuBookmarkClick = {
                openBookmarkEdit(it)
            },
            onMenuDeleteClick = {
                deletePost = it.wrapImmutable()
                confirmDeleteDialogState.show()
            },
        )
    }

    val handleBack: () -> Unit = {
        if (isGalleryMode) {
            isGalleryMode = false
        } else if (isCollected) {
            readFloorBeforeBack = lastVisibilityPost?.get { floor } ?: 0
            if (readFloorBeforeBack != 0) {
                updateCollectMarkDialogState.show()
            } else {
                navigator.navigateUp()
            }
        } else {
            navigator.navigateUp()
        }
    }

    ProvideNavigator(navigator = navigator) {
        PredictiveBackBox(
            enabled = !bottomSheetState.isVisible && !isGalleryMode,
            currentScreen = ThreadPageDestination,
            onBackConfirm = { reset ->
                if (isCollected) {
                    readFloorBeforeBack = lastVisibilityPost?.get { floor } ?: 0
                    if (readFloorBeforeBack != 0) {
                        reset()
                        updateCollectMarkDialogState.show()
                    } else {
                        navigator.navigateUp()
                    }
                } else {
                    navigator.navigateUp()
                }
            }
        ) {
            MyBackHandler(enabled = isGalleryMode) {
                isGalleryMode = false
            }
            StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = isEmpty,
            isError = isError,
            isLoading = isRefreshing,
            errorScreen = {
                error?.let {
                    val (e) = it
                    ErrorScreen(error = e)
                }
            },
            onReload = {
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId,
                        page = 0,
                        postId = postId,
                        forumId = forumId,
                        seeLz = seeLz,
                        sortType = sortType
                    )
                )
            }
        ) {
            MyScaffold(
                scaffoldState = scaffoldState,
                topBar = {
                    ThreadTopBar(
                        forum = forum,
                        onBack = handleBack,
                        onForumClick = {
                            val forumName = forum?.get { name }
                            if (forumName != null) navigator.navigate(
                                ForumPageDestination(
                                    forumName
                                )
                            )
                        },
                        isGalleryMode = isGalleryMode,
                        onToggleGalleryMode = { isGalleryMode = !isGalleryMode },
                        onSearchClick = { searchJumpDialogState.show() }
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
                            if (firstPostId != 0L) viewModel.send(
                                ThreadUiIntent.AgreeThread(
                                    threadId,
                                    firstPostId,
                                    !hasThreadAgreed
                                )
                            )
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
            ) { paddingValues ->
                ModalBottomSheetLayout(
                    sheetState = bottomSheetState,
                    sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    sheetBackgroundColor = ExtendedTheme.colors.windowBackground,
                    sheetContent = {
                        ThreadMenu(
                            isSeeLz = isSeeLz,
                            isCollected = isCollected,
                            isImmersiveMode = isImmersiveMode,
                            isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                            isGalleryMode = isGalleryMode,
                            canDelete = { author?.get { id } == user.get { id } },
                            onSeeLzClick = {
                                viewModel.send(
                                    ThreadUiIntent.LoadFirstPage(
                                        threadId,
                                        forumId,
                                        !isSeeLz,
                                        curSortType
                                    )
                                )
                                closeBottomSheet()
                            },
                            onCollectClick = {
                                if (isCollected) {
                                    val fid = forum?.get { id } ?: forumId
                                    val tbs = anti?.get { tbs }
                                    if (fid != null) {
                                        viewModel.send(
                                            ThreadUiIntent.RemoveFavorite(
                                                threadId,
                                                fid,
                                                tbs
                                            )
                                        )
                                    }
                                } else {
                                    val readItem = lastVisibilityPost
                                    if (readItem != null) {
                                        viewModel.send(
                                            ThreadUiIntent.AddFavorite(
                                                threadId,
                                                readItem.get { id },
                                                readItem.get { floor }
                                            )
                                        )
                                    }
                                }
                                closeBottomSheet()
                            },
                            onImmersiveModeClick = {
                                if (!isImmersiveMode && !isSeeLz) {
                                    viewModel.send(
                                        ThreadUiIntent.LoadFirstPage(
                                            threadId,
                                            forumId,
                                            true,
                                            curSortType
                                        )
                                    )
                                }
                                viewModel.send(ThreadUiIntent.ToggleImmersiveMode(!isImmersiveMode))
                                closeBottomSheet()
                            },
                            onDescClick = {
                                viewModel.send(
                                    ThreadUiIntent.LoadFirstPage(
                                        threadId,
                                        forumId,
                                        isSeeLz,
                                        if (curSortType != ThreadSortType.SORT_TYPE_DESC) ThreadSortType.SORT_TYPE_DESC else ThreadSortType.SORT_TYPE_DEFAULT
                                    )
                                )
                                closeBottomSheet()
                            },
                            onGalleryModeClick = {
                                isGalleryMode = !isGalleryMode
                                closeBottomSheet()
                            },
                            onJumpPageClick = {
                                closeBottomSheet()
                                searchJumpDialogState.show()
                            },
                            onBookmarksClick = {
                                closeBottomSheet()
                                coroutineScope.launch {
                                    threadBookmarks = FloorBookmarkUtil.getBookmarks(threadId)
                                    bookmarksDialogState.show()
                                }
                            },
                            onSaveSnapshotClick = {
                                closeBottomSheet()
                                coroutineScope.launch {
                                    saveThreadSnapshot(
                                        context = context,
                                        threadId = threadId,
                                        threadTitle = threadTitle,
                                        firstPost = firstPost,
                                        author = author,
                                        forum = forum,
                                        firstPostContentRenders = firstPostContentRenders,
                                        data = data,
                                    )
                                }
                            },
                            onShareClick = {
                                TiebaUtil.shareText(
                                    context,
                                    "https://tieba.baidu.com/p/$threadId",
                                    threadTitle
                                )
                            },
                            onCopyLinkClick = {
                                TiebaUtil.copyText(
                                    context,
                                    "https://tieba.baidu.com/p/$threadId?see_lz=${isSeeLz.booleanToString()}"
                                )
                            },
                            onReportClick = {
                                val firstPostId =
                                    thread?.get { firstPostId }.takeIf { it != 0L }
                                        ?: firstPost?.get { id }
                                        ?: 0L
                                coroutineScope.launch {
                                    TiebaUtil.reportPost(
                                        context,
                                        navigator,
                                        firstPostId.toString()
                                    )
                                }
                            },
                            onDeleteClick = {
                                deletePost = null
                                confirmDeleteDialogState.show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(vertical = 16.dp)
                                .defaultMinSize(minHeight = 1.dp)
                        )
                    },
                    scrimColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Box(
                        modifier = Modifier
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
                                    isGalleryMode = false
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
                                            pendingScrollToPostId = targetPid
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
                            LoadMoreLayout(
                            isLoading = isLoadingMore,
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
                            },
                            loadEnd = loadMoreEnd,
                            indicator = { isLoading, loadMoreEnd, willLoad ->
                                ThreadLoadMoreIndicator(
                                    isLoading,
                                    loadMoreEnd,
                                    willLoad,
                                    hasMore
                                )
                            },
                            lazyListState = lazyListState,
                            isEmpty = data.isEmpty(),
                            preloadCount = loadMorePreloadCount,
                        ) {
                            MyLazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item(key = "FirstPost") {
                                    if (firstPost != null) {
                                        ThreadFirstPostCard(
                                            threadId = threadId,
                                            thread = thread,
                                            firstPost = firstPost!!,
                                            firstPostContentRenders = firstPostContentRenders,
                                            author = author,
                                            user = user,
                                            forum = forum,
                                            curForumId = curForumId,
                                            isImmersiveMode = isImmersiveMode,
                                            isSearchHighlighted = highlightedFloor == 1,
                                            navigator = navigator,
                                            onAddFavorite = {
                                                viewModel.send(
                                                    ThreadUiIntent.AddFavorite(
                                                        threadId,
                                                        it.id,
                                                        it.floor
                                                    )
                                                )
                                            },
                                            onOpenBookmarkEdit = { openBookmarkEdit(it) },
                                            onDeleteClick = {
                                                deletePost = null
                                                confirmDeleteDialogState.show()
                                            }
                                        )
                                    }
                                }
                                stickyHeader(key = "ThreadHeader") {
                                    ThreadStickyHeader(
                                        replyCount = thread?.get { replyNum } ?: 0,
                                        isSeeLz = isSeeLz,
                                        onToggleSeeLz = { seeLz ->
                                            viewModel.send(
                                                ThreadUiIntent.LoadFirstPage(
                                                    threadId = threadId,
                                                    forumId = forumId,
                                                    seeLz = seeLz,
                                                    sortType = curSortType
                                                )
                                            )
                                        }
                                    )
                                }
                                if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                                    threadLatestPosts(
                                        latestPosts = latestPosts,
                                        desc = true,
                                        highlightedFloor = highlightedFloor,
                                    ) { postItem, isHighlighted ->
                                        val (item, blocked, renders, subPosts, blockResult) = postItem
                                        val reason = (blockResult as? FilterResult.Blocked)?.reason
                                        PostCard(
                                            item,
                                            renders,
                                            subPosts,
                                            blocked,
                                            reason,
                                            isSearchHighlighted = isHighlighted
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
                                                        viewModel.send(
                                                            ThreadUiIntent.LoadPrevious(
                                                                threadId,
                                                                max(currentPageMax - 1, 1),
                                                                forumId,
                                                                postId = data
                                                                    .first()
                                                                    .post
                                                                    .get { id },
                                                                seeLz = isSeeLz,
                                                                sortType = curSortType,
                                                                postIds = data.map { it.post.get { id } }
                                                            )
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
                                                    if (canReload) {
                                                        Button(onClick = { reload() }) {
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
                                        val (item, blocked, renders, subPosts, blockResult) = postItem
                                        val reason = (blockResult as? FilterResult.Blocked)?.reason
                                        Container {
                                            PostCard(
                                                item,
                                                renders,
                                                subPosts,
                                                blocked,
                                                reason,
                                                isSearchHighlighted = highlightedFloor == item.get { floor }
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
                                        val (item, blocked, renders, subPosts, blockResult) = postItem
                                        val reason = (blockResult as? FilterResult.Blocked)?.reason
                                        PostCard(
                                            item,
                                            renders,
                                            subPosts,
                                            blocked,
                                            reason,
                                            isSearchHighlighted = isHighlighted
                                        )
                                    }
                                }
                            }
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
                            visible = showResumePill && !isGalleryMode,
                            floor = readingProgress?.floor.orEmpty(),
                            onJump = onJumpToResume,
                            onDismiss = { showResumePill = false },
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
                                    currentHitIndex = prevIndex
                                    onJumpToHit(activeSearchHits[prevIndex])
                                }
                            },
                            onNextHit = {
                                if (activeSearchHits.isNotEmpty()) {
                                    val nextIndex = (currentHitIndex + 1) % activeSearchHits.size
                                    currentHitIndex = nextIndex
                                    onJumpToHit(activeSearchHits[nextIndex])
                                }
                            },
                            onClose = {
                                activeSearchKeyword = ""
                                activeSearchHits = emptyList()
                                highlightedFloor = -1
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        }
    }
}
