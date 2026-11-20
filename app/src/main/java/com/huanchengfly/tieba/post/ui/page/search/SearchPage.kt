package com.huanchengfly.tieba.post.ui.page.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadUiEvent
import com.huanchengfly.tieba.post.ui.page.search.user.SearchUserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarContainer
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://search")
    ]
)
@Composable
fun SearchPage(
    navigator: DestinationsNavigator,
    viewModel: SearchViewModel = pageViewModel<SearchUiIntent, SearchViewModel>(
        listOf(SearchUiIntent.Init)
    ),
) {
    val context = LocalContext.current
    val searchHistories by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::searchHistories,
        initial = persistentListOf()
    )
    val keyword by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::keyword,
        initial = ""
    )

    val isKeywordEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::isKeywordEmpty,
        initial = true
    )
    val suggestions by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::suggestions,
        initial = persistentListOf()
    )

    val showSuggestions by remember {
        derivedStateOf { suggestions.isNotEmpty() }
    }

    var inputKeyword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { inputKeyword }
            .debounce(500)
            .collect {
                viewModel.send(SearchUiIntent.KeywordInputChanged(it))
            }
    }

    LaunchedEffect(keyword) {
        if (keyword.isNotEmpty() && keyword != inputKeyword) {
            inputKeyword = keyword
        }
    }

    MyBackHandler(
        enabled = !isKeywordEmpty,
        currentScreen = SearchPageDestination
    ) {
        viewModel.send(SearchUiIntent.SubmitKeyword(""))
    }

    var expanded by remember { mutableStateOf(false) }

    val initialSortType = remember { SearchThreadSortType.SORT_TYPE_NEWEST }
    var searchThreadSortType by remember { mutableIntStateOf(initialSortType) }
    LaunchedEffect(searchThreadSortType) {
        emitGlobalEvent(SearchThreadUiEvent.SwitchSortType(searchThreadSortType))
    }
    viewModel.onEvent<SearchUiEvent.KeywordChanged> {
        inputKeyword = it.keyword
        if (it.keyword.isNotBlank()) emitGlobalEventSuspend(it)
    }

    val pages by remember {
        derivedStateOf {
            persistentListOf(
                SearchPageItem(
                    id = "forum",
                    text = {
                        Text(
                            text = stringResource(id = R.string.title_search_forum),
                            fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    content = { SearchForumPage(keyword = keyword) }
                ),
                SearchPageItem(
                    id = "thread",
                    text = {
                        Text(
                            text = stringResource(id = R.string.title_search_thread),
                            fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    content = {
                        SearchThreadPage(
                            keyword = keyword,
                            initialSortType = initialSortType
                        )
                    },
                    supportSort = true,
                    sortTypes = persistentMapOf(
                        context.getString(R.string.title_search_order_new) to SearchThreadSortType.SORT_TYPE_NEWEST,
                        context.getString(R.string.title_search_order_old) to SearchThreadSortType.SORT_TYPE_OLDEST,
                        context.getString(R.string.title_search_order_relevant) to SearchThreadSortType.SORT_TYPE_RELATIVE,
                    ),
                    selectedSortType = { searchThreadSortType },
                    onSelectedSortTypeChange = { searchThreadSortType = it }
                ),
                SearchPageItem(
                    id = "user",
                    text = {
                        Text(
                            text = stringResource(id = R.string.title_search_user),
                            fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    content = { SearchUserPage(keyword = keyword) }
                ),
            )
        }
    }
    val pagerState = rememberPagerState { pages.size }
    MyScaffold(
        topBar = {
            TopAppBarContainer(
                topBar = {
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .background(ExtendedTheme.colors.topBar)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        SearchTopBar(
                            keyword = inputKeyword,
                            onKeywordChange = { inputKeyword = it },
                            onKeywordSubmit = {
                                viewModel.send(SearchUiIntent.SubmitKeyword(it))
                            },
                            onBack = {
                                if (isKeywordEmpty) {
                                    navigator.navigateUp()
                                } else {
                                    viewModel.send(SearchUiIntent.SubmitKeyword(""))
                                }
                            }
                        )
                    }
                },
            ) {
                if (!isKeywordEmpty) {
                    SearchTabRow(pagerState = pagerState, pages = pages)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (!isKeywordEmpty) {
                ProvideNavigator(navigator = navigator) {
                    LazyLoadHorizontalPager(
                        state = pagerState,
                        key = { pages[it].id },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        pages[it].content()
                    }
                }
            } else {
                if (showSuggestions) {
                    SearchSuggestionList(
                        suggestions = suggestions,
                        onItemClick = {
                            inputKeyword = it
                            viewModel.send(SearchUiIntent.SubmitKeyword(it))
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Container {
                            SearchHistoryList(
                                searchHistories = searchHistories,
                                onSearchHistoryClick = {
                                    inputKeyword = it.content
                                    viewModel.send(SearchUiIntent.SubmitKeyword(it.content))
                                },
                                expanded = expanded,
                                onToggleExpand = { expanded = !expanded },
                                onDelete = { viewModel.send(SearchUiIntent.DeleteSearchHistory(it.id)) },
                                onClear = { viewModel.send(SearchUiIntent.ClearSearchHistory) }
                            )
                        }
                    }
                }
            }
        }
    }
}
