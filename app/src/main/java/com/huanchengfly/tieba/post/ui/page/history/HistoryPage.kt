package com.huanchengfly.tieba.post.ui.page.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.draft.DraftsListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.ui.page.history.outbox.OutboxListPage
import com.huanchengfly.tieba.post.ui.page.history.snapshot.ThreadSnapshotListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.DraftHelper
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.ReplyOutboxCoordinator
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://history")
    ]
)
@Composable
fun HistoryPage(
    navigator: DestinationsNavigator
) {
    val pagerState = rememberPagerState { 5 }
    var outboxRefreshSignal by remember { mutableIntStateOf(0) }
    var draftsRefreshSignal by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        scaffoldState = scaffoldState,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_history),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
                actions = {
                    if (pagerState.currentPage != 2) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                when (pagerState.currentPage) {
                                    3 -> {
                                        ReplyOutboxCoordinator.clearAll()
                                        outboxRefreshSignal++
                                    }
                                    4 -> {
                                        DraftHelper.clearAllDrafts()
                                        draftsRefreshSignal++
                                    }
                                    else -> {
                                        HistoryUtil.deleteAll()
                                        emitGlobalEvent(HistoryListUiEvent.DeleteAll)
                                    }
                                }
                                launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.toast_clear_success
                                        )
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(id = R.string.title_history_delete),
                                tint = ExtendedTheme.colors.onTopBar
                            )
                        }
                    }
                },
                content = {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        indicator = { tabPositions ->
                            PagerTabIndicator(
                                pagerState = pagerState,
                                tabPositions = tabPositions
                            )
                        },
                        divider = {},
                        backgroundColor = Color.Transparent,
                        contentColor = ExtendedTheme.colors.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        val tabTitles = listOf(
                            R.string.title_history_thread,
                            R.string.title_history_forum,
                            R.string.title_history_snapshot,
                            R.string.title_history_outbox,
                            R.string.title_history_draft,
                        )
                        tabTitles.forEachIndexed { index, titleRes ->
                            Tab(
                                text = {
                                    Text(
                                        text = stringResource(id = titleRes),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                },
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                selectedContentColor = ExtendedTheme.colors.onTopBar,
                                unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary
                            )
                        }
                    }
                }
            )
        }
    ) {
        ProvideNavigator(navigator = navigator) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true,
            ) {
                when (it) {
                    0 -> HistoryListPage(type = HistoryUtil.TYPE_THREAD)
                    1 -> HistoryListPage(type = HistoryUtil.TYPE_FORUM)
                    2 -> ThreadSnapshotListPage()
                    3 -> OutboxListPage(refreshSignal = outboxRefreshSignal)
                    else -> DraftsListPage(refreshSignal = draftsRefreshSignal)
                }
            }
        }
    }
}