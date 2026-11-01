package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.main.BottomNavigationDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.GsonUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Destination
@Composable
fun BlockListPage(
    viewModel: BlockListViewModel = pageViewModel<BlockListUiIntent, BlockListViewModel>(
        listOf(BlockListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    var addBlockCategory by remember { mutableIntStateOf(Block.CATEGORY_BLACK_LIST) }
    val addDialogState = rememberDialogState()
    val importDialogState = rememberDialogState()
    val clearConfirmDialogState = rememberDialogState()
    val simulatorDialogState = rememberDialogState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    val blackList by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::blackList,
        initial = emptyList()
    )
    val whiteList by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::whiteList,
        initial = emptyList()
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::isLoading,
        initial = false
    )

    RuleSimulatorDialog(dialogState = simulatorDialogState)

    AddBlockRuleDialog(
        dialogState = addDialogState,
        category = addBlockCategory,
        onConfirm = { type, keywords, username, uid, pattern, minLevel, ipRegion, forum ->
            viewModel.send(
                BlockListUiIntent.Add(
                    category = addBlockCategory,
                    type = type,
                    keywords = keywords,
                    username = username,
                    uid = uid,
                    pattern = pattern,
                    minLevel = minLevel,
                    ipRegion = ipRegion,
                    forum = forum,
                )
            )
        }
    )

    ImportRulesDialog(
        dialogState = importDialogState,
        onConfirm = { json ->
            viewModel.send(BlockListUiIntent.ImportRules(json))
        }
    )

    ConfirmDialog(
        dialogState = clearConfirmDialogState,
        onConfirm = {
            val cat = if (pagerState.currentPage == 0) Block.CATEGORY_BLACK_LIST else Block.CATEGORY_WHITE_LIST
            viewModel.send(BlockListUiIntent.Clear(cat))
        },
        title = { Text(text = stringResource(id = R.string.btn_clear_rules)) },
        content = {
            Text(
                text = stringResource(
                    id = if (pagerState.currentPage == 0) R.string.tip_confirm_clear_blacklist
                    else R.string.tip_confirm_clear_whitelist
                )
            )
        }
    )

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_block_list),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            menuExpanded = false
                            simulatorDialogState.show()
                        }) {
                            Text(text = stringResource(id = R.string.btn_rule_simulator))
                        }
                        DropdownMenuItem(onClick = {
                            menuExpanded = false
                            val json = BlockManager.exportRulesJson()
                            clipboardManager.setText(AnnotatedString(json))
                            val count = blackList.size + whiteList.size
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    String.format(context.getString(R.string.toast_export_success), count)
                                )
                            }
                        }) {
                            Text(text = stringResource(id = R.string.btn_export_rules))
                        }
                        DropdownMenuItem(onClick = {
                            menuExpanded = false
                            importDialogState.show()
                        }) {
                            Text(text = stringResource(id = R.string.btn_import_rules))
                        }
                        DropdownMenuItem(onClick = {
                            menuExpanded = false
                            clearConfirmDialogState.show()
                        }) {
                            Text(text = stringResource(id = R.string.btn_clear_rules))
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
                        contentColor = ExtendedTheme.colors.onTopBar,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(84.dp * 2),
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                            text = { Text(text = stringResource(id = R.string.title_black_list)) },
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                            text = { Text(text = stringResource(id = R.string.title_white_list)) },
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                BottomNavigationDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                addBlockCategory = Block.CATEGORY_BLACK_LIST
                                addDialogState.show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(id = R.string.title_add_black),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                addBlockCategory = Block.CATEGORY_WHITE_LIST
                                addDialogState.show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(id = R.string.title_add_white),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        viewModel.onEvent<BlockListUiEvent.Success> { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    is BlockListUiEvent.Success.Add -> context.getString(R.string.toast_add_success)
                    is BlockListUiEvent.Success.Delete -> context.getString(R.string.toast_delete_success)
                    is BlockListUiEvent.Success.Import -> String.format(
                        context.getString(R.string.toast_import_success),
                        event.count
                    )
                    is BlockListUiEvent.Success.Clear -> context.getString(R.string.toast_clear_success)
                }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it },
            contentPadding = paddingValues,
            verticalAlignment = Alignment.Top
        ) { position ->
            val items by remember {
                derivedStateOf {
                    if (position == 0) blackList else whiteList
                }
            }
            StateScreen(
                isEmpty = items.isEmpty(),
                isError = false,
                isLoading = isLoading,
                loadingScreen = {
                    MyLazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(4) {
                            BlockItemPlaceholder()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                MyLazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { item ->
                        LongClickMenu(
                            menuContent = {
                                DropdownMenuItem(onClick = {
                                    val copyText = when (item.type) {
                                        Block.TYPE_USER -> item.username ?: item.uid.orEmpty()
                                        Block.TYPE_REGEX -> item.pattern.orEmpty()
                                        Block.TYPE_IP_REGION -> item.ipRegion.orEmpty()
                                        Block.TYPE_USER_LEVEL -> "Lv.${item.minLevel}"
                                        else -> {
                                            runCatching {
                                                GsonUtil.getGson().fromJson<List<String>>(
                                                    item.keywords ?: "[]",
                                                    object : TypeToken<List<String>>() {}.type
                                                ).joinToString(" ")
                                            }.getOrDefault("")
                                        }
                                    }
                                    clipboardManager.setText(AnnotatedString(copyText))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.toast_copy_success))
                                    }
                                }) {
                                    Text(text = stringResource(id = R.string.title_copy))
                                }
                                DropdownMenuItem(onClick = {
                                    viewModel.send(BlockListUiIntent.Delete(item.id))
                                }) {
                                    Text(text = stringResource(id = R.string.title_delete))
                                }
                            }
                        ) {
                            BlockItem(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockItemPlaceholder() {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            modifier = Modifier.placeholder(visible = true)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.title_block_settings),
            modifier = Modifier.placeholder(visible = true)
        )
    }
}

@Composable
private fun BlockItem(
    item: Block,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, typeName) = when (item.type) {
            Block.TYPE_USER -> Icons.Outlined.AccountCircle to stringResource(R.string.rule_type_user)
            Block.TYPE_REGEX -> Icons.Outlined.Edit to stringResource(R.string.rule_type_regex)
            Block.TYPE_IP_REGION -> Icons.Outlined.Explore to stringResource(R.string.rule_type_ip)
            Block.TYPE_USER_LEVEL -> Icons.Rounded.Star to stringResource(R.string.rule_type_level)
            else -> ImageVector.vectorResource(id = R.drawable.ic_comment_new) to stringResource(R.string.rule_type_keyword)
        }

        Icon(
            imageVector = icon,
            contentDescription = typeName,
            tint = MaterialTheme.colors.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.secondary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                }

                val forumBadge = if (!item.forum.isNullOrBlank()) "${item.forum}吧" else stringResource(R.string.rule_scope_all)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (!item.forum.isNullOrBlank()) MaterialTheme.colors.primary.copy(alpha = 0.12f)
                            else ExtendedTheme.colors.text.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = forumBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!item.forum.isNullOrBlank()) MaterialTheme.colors.primary else ExtendedTheme.colors.textSecondary
                    )
                }

                when (item.type) {
                    Block.TYPE_USER -> {
                        Text(
                            text = item.username ?: "UID: ${item.uid}",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Block.TYPE_REGEX -> {
                        Text(
                            text = item.pattern.orEmpty(),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Block.TYPE_IP_REGION -> {
                        Text(
                            text = item.ipRegion.orEmpty(),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Block.TYPE_USER_LEVEL -> {
                        Text(
                            text = "低于 Lv.${item.minLevel} 级",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    else -> {
                        val keywordsList: List<String> = runCatching {
                            GsonUtil.getGson()
                                .fromJson<List<String>>(
                                    item.keywords ?: "[]",
                                    object : TypeToken<List<String>>() {}.type
                                )
                        }.getOrDefault(emptyList())
                        Text(
                            text = keywordsList.joinToString(" "),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (item.type == Block.TYPE_USER && !item.uid.isNullOrBlank() && !item.username.isNullOrBlank()) {
                Text(
                    text = "UID: ${item.uid}",
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
    }
}
