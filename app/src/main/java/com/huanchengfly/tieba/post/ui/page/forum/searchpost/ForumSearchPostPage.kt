package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadList
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Destination
@Composable
fun ForumSearchPostPage(
    forumName: String,
    forumId: Long,
    navigator: DestinationsNavigator,
    viewModel: ForumSearchPostViewModel = pageViewModel<ForumSearchPostUiIntent, ForumSearchPostViewModel>(
        listOf(ForumSearchPostUiIntent.Init)
    ),
) {
    val context = LocalContext.current
    val currentKeyword by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::keyword,
        initial = ""
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::error,
        initial = null
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::data,
        initial = persistentListOf()
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isLoadingMore,
        initial = false
    )
    val currentSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::sortType,
        initial = ForumSearchPostSortType.NEWEST
    )
    val currentFilterType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::filterType,
        initial = ForumSearchPostFilterType.ALL
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::hasMore,
        initial = true
    )
    val searchHistories by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::searchHistories,
        initial = persistentListOf()
    )

    val isEmpty by remember {
        derivedStateOf { data.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }
    val isKeywordEmpty by remember {
        derivedStateOf { currentKeyword.isEmpty() }
    }
    var inputKeyword by remember { mutableStateOf("") }

    LaunchedEffect(currentKeyword) {
        if (currentKeyword.isNotEmpty() && currentKeyword != inputKeyword) {
            inputKeyword = currentKeyword
        }
    }

    fun refresh() {
        viewModel.send(
            ForumSearchPostUiIntent.Refresh(
                currentKeyword,
                forumName,
                forumId,
                currentSortType,
                currentFilterType
            )
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = ::refresh
    )
    val lazyListState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val sortTypeMapping = remember {
        mapOf(
            ForumSearchPostSortType.NEWEST to context.getString(R.string.title_search_post_sort_by_time),
            ForumSearchPostSortType.RELATIVE to context.getString(R.string.title_search_post_sort_by_relevant),
        )
    }
    val filterTypeMapping = remember {
        mapOf(
            ForumSearchPostFilterType.ALL to context.getString(R.string.title_search_filter_all),
            ForumSearchPostFilterType.ONLY_THREAD to context.getString(R.string.title_search_filter_only_thread),
        )
    }

    MyScaffold(
        topBar = {
            ForumSearchPostTopBar(
                forumName = forumName,
                keyword = inputKeyword,
                onKeywordChange = { inputKeyword = it },
                onKeywordSubmit = {
                    viewModel.send(
                        ForumSearchPostUiIntent.Refresh(
                            it,
                            forumName,
                            forumId,
                            currentSortType,
                            currentFilterType
                        )
                    )
                },
                focusRequester = focusRequester,
                onNavigateUp = { navigator.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ProvideNavigator(navigator = navigator) {
                if (!isKeywordEmpty) {
                    StateScreen(
                        modifier = Modifier.fillMaxSize(),
                        isEmpty = isEmpty,
                        isError = isError,
                        isLoading = isRefreshing,
                        onReload = ::refresh,
                        errorScreen = {
                            error?.item?.let {
                                ErrorScreen(error = it)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pullRefresh(pullRefreshState)
                        ) {
                            LoadMoreLayout(
                                isLoading = isLoadingMore,
                                onLoadMore = {
                                    viewModel.send(
                                        ForumSearchPostUiIntent.LoadMore(
                                            currentKeyword,
                                            forumName,
                                            forumId,
                                            currentPage,
                                            currentSortType,
                                            currentFilterType
                                        )
                                    )
                                },
                                loadEnd = !hasMore,
                                lazyListState = lazyListState,
                            ) {
                                SearchThreadList(
                                    data = data,
                                    lazyListState = lazyListState,
                                    onItemClick = {
                                        if (it.postInfo != null) {
                                            navigator.navigate(
                                                SubPostsPageDestination(
                                                    threadId = it.tid.toLong(),
                                                    subPostId = it.cid.toLong(),
                                                    loadFromSubPost = true
                                                )
                                            )
                                        } else if (it.mainPost != null) {
                                            navigator.navigate(
                                                ThreadPageDestination(
                                                    threadId = it.tid.toLong(),
                                                    postId = it.pid.toLong(),
                                                    scrollToReply = true,
                                                )
                                            )
                                        } else {
                                            navigator.navigate(
                                                ThreadPageDestination(
                                                    threadId = it.tid.toLong()
                                                )
                                            )
                                        }
                                    },
                                    onItemUserClick = {
                                        navigator.navigate(UserProfilePageDestination(it.userId.toLong()))
                                    },
                                    onItemForumClick = {
                                        navigator.navigate(
                                            ForumPageDestination(
                                                it.forumName
                                            )
                                        )
                                    },
                                    onQuotePostClick = {
                                        navigator.navigate(
                                            ThreadPageDestination(
                                                threadId = it.tid,
                                                postId = it.pid,
                                                scrollToReply = true
                                            )
                                        )
                                    },
                                    onMainPostClick = {
                                        navigator.navigate(
                                            ThreadPageDestination(
                                                threadId = it.tid,
                                                scrollToReply = true
                                            )
                                        )
                                    },
                                    hideForum = true,
                                    searchKeyword = currentKeyword,
                                ) {
                                    stickyHeader(key = "Sort&Filter") {
                                        ForumSearchPostSortAndFilterBar(
                                            currentSortType = currentSortType,
                                            currentFilterType = currentFilterType,
                                            sortTypeMapping = sortTypeMapping,
                                            filterTypeMapping = filterTypeMapping,
                                            onSortTypeChange = { newSortType ->
                                                viewModel.send(
                                                    ForumSearchPostUiIntent.Refresh(
                                                        currentKeyword,
                                                        forumName,
                                                        forumId,
                                                        newSortType,
                                                        currentFilterType
                                                    )
                                                )
                                            },
                                            onFilterTypeChange = { newFilterType ->
                                                viewModel.send(
                                                    ForumSearchPostUiIntent.Refresh(
                                                        currentKeyword,
                                                        forumName,
                                                        forumId,
                                                        currentSortType,
                                                        newFilterType
                                                    )
                                                )
                                            }
                                        )
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
                        }
                    }
                } else {
                    SearchHistoryList(
                        searchHistories = searchHistories,
                        onSearchHistoryClick = {
                            viewModel.send(
                                ForumSearchPostUiIntent.Refresh(
                                    it.content,
                                    forumName,
                                    forumId,
                                    currentSortType,
                                    currentFilterType
                                )
                            )
                        },
                        onDelete = {
                            viewModel.send(ForumSearchPostUiIntent.DeleteHistory(it.id))
                        },
                        onClear = {
                            viewModel.send(ForumSearchPostUiIntent.ClearHistory)
                        }
                    )
                }
            }
        }
    }
}
