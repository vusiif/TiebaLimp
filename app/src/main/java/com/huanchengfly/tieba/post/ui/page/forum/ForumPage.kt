package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.ForumHistoryExtra
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListPage
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.ScrollableTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://forum/{forumName}")
    ]
)
@Composable
fun ForumPage(
    forumName: String,
    viewModel: ForumViewModel = pageViewModel(),
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ForumUiIntent.Load(forumName, getSortType(context, forumName)))
        viewModel.initialized = true
    }

    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = scaffoldState.snackbarHostState
    viewModel.onEvent<ForumUiEvent.SignIn.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_sign_success,
                "${it.signBonusPoint}",
                "${it.userSignRank}"
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.SignIn.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_sign_failed,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_like_success,
                it.memberSum,
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_like_failed,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_unlike_success
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_unlike_failed,
                it.errorMsg
            )
        )
    }

    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::isLoading,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::isError,
        initial = false
    )
    val forumInfo by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::forum,
        initial = null
    )
    val tbs by viewModel.uiState.collectPartialAsState(prop1 = ForumUiState::tbs, initial = null)

    val account = LocalAccount.current
    val pagerState = rememberPagerState { 2 }

    val currentPage by remember {
        derivedStateOf {
            pagerState.currentPage
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current

    var heightOffset by rememberSaveable { mutableFloatStateOf(0f) }
    var headerHeight by rememberSaveable {
        mutableFloatStateOf(
            with(density) {
                (Sizes.Large + 16.dp * 2).toPx()
            }
        )
    }

    val isShowTopBarArea by remember {
        derivedStateOf {
            heightOffset.absoluteValue < headerHeight
        }
    }

    val unlikeDialogState = rememberDialogState()

    LaunchedEffect(forumInfo) {
        if (forumInfo != null) {
            val (forum) = forumInfo as ImmutableHolder<ForumInfo>
            HistoryUtil.saveHistory(
                History(
                    title = context.getString(R.string.title_forum, forum.name),
                    timestamp = System.currentTimeMillis(),
                    avatar = forum.avatar,
                    type = HistoryUtil.TYPE_FORUM,
                    data = forum.name,
                    extras = Json.encodeToString(ForumHistoryExtra(forum.id))
                ),
                true
            )
        }
    }

    if (account != null && forumInfo != null) {
        ConfirmDialog(
            dialogState = unlikeDialogState,
            onConfirm = {
                viewModel.send(
                    ForumUiIntent.Unlike(forumInfo!!.get { id }, forumName, tbs ?: account.tbs)
                )
            },
            title = {
                Text(
                    text = stringResource(
                        id = R.string.title_dialog_unfollow_forum,
                        forumName
                    )
                )
            }
        )
    }

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = forumInfo == null,
            isError = isError,
            isLoading = isLoading,
            onReload = {
                viewModel.send(
                    ForumUiIntent.Load(
                        forumName,
                        getSortType(context, forumName)
                    )
                )
            },
            loadingScreen = {
                LoadingPlaceholder(forumName)
            }
        ) {
            MyScaffold(
                scaffoldState = scaffoldState,
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    ForumToolbar(
                        forumName = forumName,
                        showTitle = !isShowTopBarArea,
                        menuContent = {
                            DropdownMenuItem(
                                onClick = {
                                    shareForum(context, forumName)
                                    dismiss()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.title_share))
                            }
                            DropdownMenuItem(
                                onClick = {
                                    if (forumInfo != null) {
                                        val (forum) = forumInfo!!
                                        coroutineScope.launch {
                                            sendToDesktop(
                                                context,
                                                forum,
                                                onSuccess = {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = context.getString(
                                                                R.string.toast_send_to_desktop_success
                                                            )
                                                        )
                                                    }
                                                },
                                                onFailure = {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = context.getString(
                                                                R.string.toast_send_to_desktop_failed,
                                                                it
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    dismiss()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.title_send_to_desktop))
                            }
                            DropdownMenuItem(
                                onClick = {
                                    unlikeDialogState.show()
                                    dismiss()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.title_unfollow))
                            }
                        },
                        forumId = forumInfo?.get { id }
                    )
                },
                floatingActionButton = {
                    val fabFunction = context.appPreferences.forumFabFunction
                    if (fabFunction != "hide") {
                        val onFabClick: () -> Unit = remember(fabFunction, currentPage, forumName, context) {
                            {
                                when (fabFunction) {
                                    "refresh" -> {
                                        coroutineScope.launch {
                                            emitGlobalEventSuspend(
                                                ForumThreadListUiEvent.BackToTop(
                                                    currentPage == 1
                                                )
                                            )
                                            emitGlobalEventSuspend(
                                                ForumThreadListUiEvent.Refresh(
                                                    currentPage == 1,
                                                    getSortType(
                                                        context,
                                                        forumName
                                                    )
                                                )
                                            )
                                        }
                                    }

                                    "back_to_top" -> {
                                        coroutineScope.launch {
                                            emitGlobalEvent(
                                                ForumThreadListUiEvent.BackToTop(
                                                    currentPage == 1
                                                )
                                            )
                                        }
                                    }

                                    else -> {
                                        context.toastShort(R.string.toast_feature_unavailable)
                                    }
                                }
                                Unit
                            }
                        }
                        FloatingActionButton(
                            onClick = onFabClick,
                            backgroundColor = ExtendedTheme.colors.windowBackground,
                            contentColor = ExtendedTheme.colors.primary,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            Icon(
                                imageVector = when (fabFunction) {
                                    "refresh" -> Icons.Rounded.Refresh
                                    "back_to_top" -> Icons.Rounded.VerticalAlignTop
                                    else -> Icons.Rounded.Add
                                },
                                contentDescription = null
                            )
                        }
                    }
                }
            ) { contentPadding ->
                var isFakeLoading by remember { mutableStateOf(false) }

                LaunchedEffect(isFakeLoading) {
                    if (isFakeLoading) {
                        delay(1000)
                        isFakeLoading = false
                    }
                }

                PullToRefreshLayout(
                    refreshing = isFakeLoading,
                    onRefresh = {
                        coroutineScope.emitGlobalEvent(
                            ForumThreadListUiEvent.Refresh(
                                currentPage == 1,
                                getSortType(
                                    context,
                                    forumName
                                )
                            )
                        )
                        isFakeLoading = true
                    }
                ) {
                    val headerNestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y < 0) {
                                    val prevHeightOffset = heightOffset
                                    heightOffset = max(heightOffset + available.y, -headerHeight)
                                    if (prevHeightOffset != heightOffset) {
                                        return available.copy(x = 0f)
                                    }
                                }

                                return Offset.Zero
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y > 0f) {
                                    val prevHeightOffset = heightOffset
                                    heightOffset = min(heightOffset + available.y, 0f)
                                    if (prevHeightOffset != heightOffset) {
                                        return available.copy(x = 0f)
                                    }
                                }

                                return Offset.Zero
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .nestedScroll(headerNestedScrollConnection)
                    ) {
                        Column {
                            val containerHeight by remember {
                                derivedStateOf {
                                    with(density) {
                                        (headerHeight + heightOffset).toDp()
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(containerHeight)
                                    .clipToBounds()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentHeight(
                                            align = Alignment.Bottom,
                                            unbounded = true
                                        )
                                        .onSizeChanged {
                                            headerHeight = it.height.toFloat()
                                        }
                                ) {
                                    val onOpenForumInfo = remember(forumInfo, navigator) {
                                        {
                                            forumInfo?.let { holder ->
                                                navigator.navigate(
                                                    ForumDetailPageDestination(
                                                        forumId = holder.get { this.id }
                                                    )
                                                )
                                            }
                                            Unit
                                        }
                                    }
                                    val onBtnClick = remember(forumInfo, viewModel, tbs, account) {
                                        {
                                            forumInfo?.let { holder ->
                                                val (forum) = holder
                                                when {
                                                    forum.is_like != 1 -> viewModel.send(
                                                        ForumUiIntent.Like(
                                                            forum.id,
                                                            forum.name,
                                                            tbs ?: account?.tbs.orEmpty()
                                                        )
                                                    )

                                                    forum.sign_in_info?.user_info?.is_sign_in != 1 -> {
                                                        viewModel.send(
                                                            ForumUiIntent.SignIn(
                                                                forum.id,
                                                                forum.name,
                                                                tbs ?: account?.tbs.orEmpty()
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            Unit
                                        }
                                    }
                                    forumInfo?.let { holder ->
                                        ForumHeader(
                                            forumInfoImmutableHolder = holder,
                                            onOpenForumInfo = onOpenForumInfo,
                                            onBtnClick = onBtnClick,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }

                            val tabTextStyle = remember {
                                TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.sp
                                )
                            }
                            val onTab0Click = remember(pagerState) {
                                {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                    Unit
                                }
                            }
                            val onTab1Click = remember(pagerState) {
                                {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                    Unit
                                }
                            }

                            ScrollableTabRow(
                                selectedTabIndex = currentPage,
                                indicator = { tabPositions ->
                                    PagerTabIndicator(
                                        pagerState = pagerState,
                                        tabPositions = tabPositions
                                    )
                                },
                                divider = {},
                                backgroundColor = Color.Transparent,
                                contentColor = ExtendedTheme.colors.primary,
                                edgePadding = 0.dp,
                                modifier = Modifier
                                    .wrapContentWidth(align = Alignment.Start)
                                    .align(Alignment.Start)
                            ) {
                                var currentSortType by remember {
                                    mutableIntStateOf(
                                        getSortType(
                                            context,
                                            forumName
                                        )
                                    )
                                }
                                TabClickMenu(
                                    selected = currentPage == 0,
                                    onClick = onTab0Click,
                                    text = {
                                        Text(
                                            text = stringResource(id = R.string.tab_forum_latest),
                                            style = tabTextStyle
                                        )
                                    },
                                    menuContent = {
                                        ListSinglePicker(
                                            itemTitles = persistentListOf(
                                                stringResource(id = R.string.title_sort_by_reply),
                                                stringResource(id = R.string.title_sort_by_send)
                                            ),
                                            itemValues = persistentListOf(0, 1),
                                            selectedPosition = currentSortType,
                                            onItemSelected = { _, _, value, changed ->
                                                if (changed) {
                                                    currentSortType = value
                                                    coroutineScope.launch {
                                                        setSortType(context, forumName, value)
                                                        emitGlobalEvent(
                                                            ForumThreadListUiEvent.Refresh(
                                                                currentPage == 1,
                                                                value
                                                            )
                                                        )
                                                    }
                                                }
                                                dismiss()
                                            }
                                        )
                                    },
                                    selectedContentColor = ExtendedTheme.colors.primary,
                                    unselectedContentColor = ExtendedTheme.colors.textSecondary
                                )
                                Tab(
                                    selected = currentPage == 1,
                                    onClick = onTab1Click,
                                    selectedContentColor = ExtendedTheme.colors.primary,
                                    unselectedContentColor = ExtendedTheme.colors.textSecondary
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.tab_forum_good),
                                            style = tabTextStyle
                                        )
                                    }
                                }
                            }

                            if (forumInfo != null) {
                                val currentForumId = forumInfo!!.get { id }
                                val currentForumName = forumInfo!!.get { name }
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    key = { it },
                                    verticalAlignment = Alignment.Top,
                                    userScrollEnabled = true,
                                ) { pageIndex ->
                                    ForumThreadListPage(
                                        forumId = currentForumId,
                                        forumName = currentForumName,
                                        isGood = pageIndex == 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
