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
        readingProgressState.readingProgress?.let { progress ->
            val targetPid = progress.pid?.toLongOrNull() ?: 0L
            val targetFloor = progress.floor.orEmpty()
            readingProgressState.dismiss()
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
        },
        searchJumpDialogState = searchJumpDialogState,
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
                        highlightedFloor = highlightedFloor,
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
                        activeSearchKeyword = activeSearchKeyword,
                        activeSearchHits = activeSearchHits,
                        currentHitIndex = currentHitIndex,
                        onHitIndexChange = { currentHitIndex = it },
                        onJumpToHit = onJumpToHit,
                        onCloseSearch = {
                            activeSearchKeyword = ""
                            activeSearchHits = emptyList()
                            highlightedFloor = -1
                        },
                        onJumpToResume = onJumpToResume,
                        onOpenBookmarkEdit = openBookmarkEdit,
                        onDeletePost = { post ->
                            deletePost = post
                            confirmDeleteDialogState.show()
                        },
                        onExitGalleryMode = { isGalleryMode = false },
                        onPendingScrollToPost = { pendingScrollToPostId = it },
                        scaffoldState = scaffoldState,
                    )
                }
            }
        }
        }
    }
}
