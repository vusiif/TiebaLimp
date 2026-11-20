package com.huanchengfly.tieba.post.ui.page.user

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.user.edit.EditProfileActivity
import com.huanchengfly.tieba.post.ui.page.user.likeforum.UserLikeForumPage
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostPage
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

@Destination
@Composable
fun UserProfilePage(
    uid: Long,
    navigator: DestinationsNavigator,
    viewModel: UserProfileViewModel = pageViewModel(),
) {
    val account = LocalAccount.current

    val isSelf = remember(account, uid) {
        account?.uid == uid.toString()
    }

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(UserProfileUiIntent.Refresh(uid))
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserProfileUiState::isRefreshing,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserProfileUiState::error,
        initial = null
    )
    val user by viewModel.uiState.collectPartialAsState(
        prop1 = UserProfileUiState::user,
        initial = null
    )
    val disableButton by viewModel.uiState.collectPartialAsState(
        prop1 = UserProfileUiState::disableButton,
        initial = false
    )

    val isError by remember {
        derivedStateOf { error != null }
    }
    val isEmpty by remember {
        derivedStateOf { user == null }
    }

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = isEmpty,
            isError = isError,
            isLoading = isRefreshing,
            onReload = { viewModel.send(UserProfileUiIntent.Refresh(uid)) },
            errorScreen = { ErrorScreen(error = error.getOrNull()) }
        ) {
            user?.let {
                UserProfileContent(
                    user = it,
                    showActionBtn = account != null,
                    disableButton = disableButton,
                    isSelf = isSelf,
                    onBack = { navigator.navigateUp() },
                    onFollow = {
                        viewModel.send(
                            UserProfileUiIntent.Follow(
                                it.get { portrait },
                                account!!.tbs,
                            )
                        )
                    },
                    onUnfollow = {
                        viewModel.send(
                            UserProfileUiIntent.Unfollow(
                                it.get { portrait },
                                account!!.tbs,
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun UserProfileContent(
    user: ImmutableHolder<User>,
    showActionBtn: Boolean,
    disableButton: Boolean,
    isSelf: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
) {
    when (LocalWindowSizeClass.current.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            UserProfileContentExpanded(
                user = user,
                showActionBtn = showActionBtn,
                disableButton = disableButton,
                isSelf = isSelf,
                onBack = onBack,
                onFollow = onFollow,
                onUnfollow = onUnfollow
            )
        }

        else -> {
            UserProfileContentNormal(
                user = user,
                showActionBtn = showActionBtn,
                disableButton = disableButton,
                isSelf = isSelf,
                onBack = onBack,
                onFollow = onFollow,
                onUnfollow = onUnfollow
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserProfileContentNormal(
    user: ImmutableHolder<User>,
    showActionBtn: Boolean,
    disableButton: Boolean,
    isSelf: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var heightOffset by rememberSaveable { mutableFloatStateOf(0f) }
    var headerHeight by rememberSaveable {
        mutableFloatStateOf(
            with(density) {
                (96.dp + 16.dp).toPx()
            }
        )
    }

    val isShowHeaderArea by remember {
        derivedStateOf {
            heightOffset.absoluteValue < headerHeight
        }
    }

    MyScaffold(
        topBar = {
            UserProfileToolbar(
                user = user,
                isSelf = isSelf,
                showTitle = !isShowHeaderArea,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        var isFakeRefreshing by remember { mutableStateOf(false) }

        LaunchedEffect(isFakeRefreshing) {
            if (isFakeRefreshing) {
                delay(1000)
                isFakeRefreshing = false
            }
        }

        PullToRefreshLayout(
            refreshing = isFakeRefreshing,
            onRefresh = {
                coroutineScope.emitGlobalEvent(GlobalEvent.Refresh(key = "user_profile"))
                isFakeRefreshing = true
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

            ProvideContentColor(color = ExtendedTheme.colors.text) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .nestedScroll(headerNestedScrollConnection)
                ) {
                    val pages = remember {
                        listOfNotNull(
                            UserProfilePageData(
                                id = "threads",
                                title = {
                                    stringResource(
                                        id = R.string.title_profile_threads_tab,
                                        it.get { thread_num }.getShortNumString()
                                    )
                                },
                                content = { user, fluid ->
                                    UserPostPage(
                                        uid = user.get { id },
                                        isThread = true,
                                        fluid = fluid,
                                    )
                                }
                            ),
                            UserProfilePageData(
                                id = "posts",
                                title = {
                                    stringResource(
                                        id = R.string.title_profile_posts_tab,
                                        it.get { post_num }.getShortNumString()
                                    )
                                },
                                content = { user, fluid ->
                                    UserPostPage(
                                        uid = user.get { id },
                                        isThread = false,
                                        fluid = fluid,
                                    )
                                }
                            ).takeIf { isSelf },
                            UserProfilePageData(
                                id = "concern_forums",
                                title = {
                                    stringResource(
                                        id = R.string.title_profile_concern_forums_tab,
                                        it.get { my_like_num }.toString()
                                    )
                                },
                                content = { user, fluid ->
                                    UserLikeForumPage(
                                        uid = user.get { id },
                                        fluid = fluid,
                                    )
                                }
                            ),
                        ).toImmutableList()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                with(density) {
                                    (headerHeight + heightOffset).toDp()
                                }
                            )
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
                            UserProfileDetail(
                                user = user,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                showBtn = showActionBtn,
                                isSelf = isSelf,
                                onBtnClick = {
                                    if (disableButton || !showActionBtn) {
                                        return@UserProfileDetail
                                    }
                                    if (isSelf) {
                                        context.goToActivity<EditProfileActivity>()
                                    } else if (user.get { has_concerned } == 0) {
                                        onFollow()
                                    } else {
                                        onUnfollow()
                                    }
                                },
                                onCopyIdClick = {
                                    TiebaUtil.copyText(
                                        context,
                                        user.get { id }.toString()
                                    )
                                }
                            )
                        }
                    }

                    val pagerState = rememberPagerState { pages.size }

                    UserProfileTabRow(
                        user = user,
                        pages = pages,
                        pagerState = pagerState,
                    )

                    LazyLoadHorizontalPager(
                        state = pagerState,
                        key = { pages[it].id },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        pages[it].content(user, false)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserProfileContentExpanded(
    user: ImmutableHolder<User>,
    showActionBtn: Boolean,
    disableButton: Boolean,
    isSelf: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    MyScaffold(
        topBar = {
            UserProfileToolbar(
                user = user,
                isSelf = isSelf,
                showTitle = false,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        ProvideContentColor(color = ExtendedTheme.colors.text) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxHeight()
                        .fillMaxWidth(0.75f)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val pages = remember {
                        listOfNotNull(
                            UserProfilePageData(
                                id = "threads",
                                title = {
                                    stringResource(
                                        id = R.string.title_profile_threads_tab,
                                        it.get { thread_num }.getShortNumString()
                                    )
                                },
                                content = { user, expanded ->
                                    UserPostPage(
                                        uid = user.get { id },
                                        isThread = true,
                                        fluid = expanded,
                                        enablePullRefresh = expanded,
                                    )
                                }
                            ),
                            UserProfilePageData(
                                id = "posts",
                                title = {
                                    stringResource(
                                        id = R.string.title_profile_posts_tab,
                                        it.get { post_num }.getShortNumString()
                                    )
                                },
                                content = { user, expanded ->
                                    UserPostPage(
                                        uid = user.get { id },
                                        isThread = false,
                                        fluid = expanded,
                                        enablePullRefresh = expanded,
                                    )
                                }
                            ).takeIf { isSelf },
                            UserProfilePageData(
                                id = "concern_forums",
                                title = {
                                    stringResource(
                                        id = R.string.title_profile_concern_forums_tab,
                                        it.get { my_like_num }.toString()
                                    )
                                },
                                content = { user, expanded ->
                                    UserLikeForumPage(
                                        uid = user.get { id },
                                        fluid = expanded,
                                        enablePullRefresh = expanded,
                                    )
                                }
                            ),
                        ).toImmutableList()
                    }
                    val pagerState = rememberPagerState { pages.size }

                    UserProfileDetail(
                        user = user,
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.Top),
                        showBtn = showActionBtn,
                        isSelf = isSelf,
                        onBtnClick = {
                            if (disableButton || !showActionBtn) {
                                return@UserProfileDetail
                            }
                            if (isSelf) {
                                context.goToActivity<EditProfileActivity>()
                            } else if (user.get { has_concerned } == 0) {
                                onFollow()
                            } else {
                                onUnfollow()
                            }
                        },
                        onCopyIdClick = {
                            TiebaUtil.copyText(
                                context,
                                user.get { id }.toString()
                            )
                        }
                    )

                    Column(
                        modifier = Modifier.weight(2f)
                    ) {
                        UserProfileTabRow(
                            user = user,
                            pages = pages,
                            pagerState = pagerState,
                        )

                        LazyLoadHorizontalPager(
                            state = pagerState,
                            key = { pages[it].id },
                            modifier = Modifier.weight(1f)
                        ) {
                            pages[it].content(user, true)
                        }
                    }
                }
            }
        }
    }
}
