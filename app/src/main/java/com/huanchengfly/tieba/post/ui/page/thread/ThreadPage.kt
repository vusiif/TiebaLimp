package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.R
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
import com.huanchengfly.tieba.post.models.database.FloorBookmark
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PredictiveBackBox
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchHit
import com.huanchengfly.tieba.post.ui.widgets.compose.extractGalleryImages
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.FloorBookmarkUtil
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

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

    val lastVisibilityFloor by remember {
        derivedStateOf { lastVisibilityPost?.get { floor } ?: 0 }
    }
    val readingProgressState = rememberThreadReadingProgress(
        threadId = threadId,
        postId = postId,
        currentFloor = lastVisibilityFloor
    )
    val galleryImages by remember(firstPost, firstPostContentRenders, data) {
        derivedStateOf {
            extractGalleryImages(
                firstPost = firstPost?.get { this },
                firstPostContentRenders = firstPostContentRenders,
                posts = data
            )
        }
    }

    val jumpController = rememberThreadJumpController(
        lazyListState = lazyListState,
        data = data,
        hasPrevious = hasPrevious,
        threadId = threadId,
        curForumId = curForumId,
        isSeeLz = isSeeLz,
        curSortType = curSortType,
        viewModel = viewModel,
        readingProgressState = readingProgressState,
    )

    val openBottomSheet: () -> Unit = {
        coroutineScope.launch {
            bottomSheetState.show()
        }
    }
    val closeBottomSheet: () -> Unit = {
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

    ThreadPageEventHandler(
        viewModel = viewModel,
        lazyListState = lazyListState,
        scaffoldState = scaffoldState,
        threadId = threadId,
        curForumId = curForumId,
        curSortType = curSortType,
        data = data,
        waitLoadSuccessAndScrollToFirstReply = waitLoadSuccessAndScrollToFirstReply,
        onClearWaitLoadSuccess = { waitLoadSuccessAndScrollToFirstReply = false }
    )

    val updateCollectMarkDialogState = rememberDialogState()
    var readFloorBeforeBack by remember {
        mutableIntStateOf(1)
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deletePost by remember { mutableStateOf<ImmutableHolder<Post>?>(null) }

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

    val searchJumpDialogState = rememberDialogState()
    val totalFloorCount = thread?.get { replyNum } ?: (data.lastOrNull()?.post?.get { floor } ?: 1)

    ThreadPageDialogs(
        updateCollectMarkDialogState = updateCollectMarkDialogState,
        readFloorBeforeBack = readFloorBeforeBack,
        threadId = threadId,
        lastVisibilityPostId = lastVisibilityPostId,
        onNavUp = { navigator.navigateUp() },
        confirmDeleteDialogState = confirmDeleteDialogState,
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
        },
        bookmarkEditDialogState = bookmarkEditDialogState,
        bookmarksDialogState = bookmarksDialogState,
        bookmarkTargetPost = bookmarkTargetPost,
        bookmarkInitialNote = bookmarkInitialNote,
        bookmarkIsExisting = bookmarkIsExisting,
        threadBookmarks = threadBookmarks,
        threadTitle = threadTitle,
        onBookmarksChanged = {
            coroutineScope.launch {
                threadBookmarks = FloorBookmarkUtil.getBookmarks(threadId)
            }
        },
        onSelectBookmark = { bookmark ->
            jumpController.onSelectBookmark(bookmark, firstPost?.get { id } ?: 0L)
        },
        searchJumpDialogState = searchJumpDialogState,
        totalFloors = totalFloorCount,
        currentPage = currentPageMax,
        totalPage = totalPage,
        firstPost = firstPost?.get(),
        loadedPosts = data,
        onJumpToFloor = jumpController::onJumpToFloor,
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
            jumpController.activeSearchKeyword = query
            jumpController.activeSearchHits = hits
            jumpController.currentHitIndex = hits.indexOf(hit).coerceAtLeast(0)
            jumpController.onJumpToHit(hit)
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

    ThreadReadingHistoryTracker(
        threadId = threadId,
        threadTitle = threadTitle,
        author = author,
        forumName = forum?.get { name },
        currentFloor = lastVisibilityFloor,
        lastVisibilityPostId = lastVisibilityPostId,
        isSeeLz = isSeeLz,
    )

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
            ThreadScaffold(
                scaffoldState = scaffoldState,
                forum = forum,
                onBack = handleBack,
                navigator = navigator,
                isGalleryMode = isGalleryMode,
                onToggleGalleryMode = { isGalleryMode = !isGalleryMode },
                onSearchClick = { searchJumpDialogState.show() },
                user = user,
                curForumId = curForumId,
                threadId = threadId,
                thread = thread,
                firstPost = firstPost,
                hasThreadAgreed = hasThreadAgreed,
                threadAgreeNum = threadAgreeNum,
                bottomSheetState = bottomSheetState,
                openBottomSheet = openBottomSheet,
                closeBottomSheet = closeBottomSheet,
                viewModel = viewModel,
            ) { paddingValues ->
                ThreadPageMenuSheetContainer(
                    sheetState = bottomSheetState,
                    viewModel = viewModel,
                    navigator = navigator,
                    threadId = threadId,
                    forumId = forumId,
                    forum = forum,
                    threadTitle = threadTitle,
                    isSeeLz = isSeeLz,
                    isCollected = isCollected,
                    isImmersiveMode = isImmersiveMode,
                    curSortType = curSortType,
                    isGalleryMode = isGalleryMode,
                    onToggleGalleryMode = { isGalleryMode = !isGalleryMode },
                    canDelete = author?.get { id } == user.get { id },
                    antiTbs = curTbs,
                    lastVisibilityPost = lastVisibilityPost?.get(),
                    threadFirstPostId = thread?.get { firstPostId } ?: 0L,
                    firstPost = firstPost,
                    author = author,
                    firstPostContentRenders = firstPostContentRenders,
                    data = data,
                    onJumpPageClick = { searchJumpDialogState.show() },
                    onBookmarksClick = {
                        coroutineScope.launch {
                            threadBookmarks = FloorBookmarkUtil.getBookmarks(threadId)
                            bookmarksDialogState.show()
                        }
                    },
                    onDeleteClick = {
                        deletePost = null
                        confirmDeleteDialogState.show()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    ThreadReadingLayout(
                        lazyListState = lazyListState,
                        isGalleryMode = isGalleryMode,
                        galleryImages = galleryImages,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        isRefreshing = isRefreshing,
                        hasPrevious = hasPrevious,
                        loadMoreEnd = loadMoreEnd,
                        loadMorePreloadCount = loadMorePreloadCount,
                        data = data,
                        threadId = threadId,
                        forumId = forumId,
                        curForumId = curForumId,
                        curSortType = curSortType,
                        currentPageMax = currentPageMax,
                        totalPage = totalPage,
                        nextPagePostId = nextPagePostId,
                        isSeeLz = isSeeLz,
                        isImmersiveMode = isImmersiveMode,
                        highlightedFloor = jumpController.highlightedFloor,
                        thread = thread,
                        firstPost = firstPost,
                        firstPostContentRenders = firstPostContentRenders,
                        author = author,
                        user = user,
                        forum = forum,
                        latestPosts = latestPosts,
                        onReload = if (canReload) { { reload() } } else null,
                        antiTbs = curTbs,
                        viewModel = viewModel,
                        navigator = navigator,
                        pullRefreshState = pullRefreshState,
                        enablePullRefresh = enablePullRefresh,
                        readingProgressState = readingProgressState,
                        activeSearchKeyword = jumpController.activeSearchKeyword,
                        activeSearchHits = jumpController.activeSearchHits,
                        currentHitIndex = jumpController.currentHitIndex,
                        onHitIndexChange = { jumpController.currentHitIndex = it },
                        onJumpToHit = jumpController::onJumpToHit,
                        onCloseSearch = jumpController::closeSearch,
                        onJumpToResume = jumpController::onJumpToResume,
                        onOpenBookmarkEdit = openBookmarkEdit,
                        onDeletePost = { post ->
                            deletePost = post
                            confirmDeleteDialogState.show()
                        },
                        onExitGalleryMode = { isGalleryMode = false },
                        onPendingScrollToPost = { jumpController.pendingScrollToPostId = it },
                        scaffoldState = scaffoldState,
                    )
                }
            }
        }
        }
    }
}
