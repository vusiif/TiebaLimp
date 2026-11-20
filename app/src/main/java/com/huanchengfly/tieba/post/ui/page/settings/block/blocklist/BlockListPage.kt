package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
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

    var selectedForumFilter by remember { mutableStateOf<String?>(null) }
    val availableForums by remember(blackList, whiteList) {
        derivedStateOf {
            (blackList + whiteList)
                .mapNotNull { it.forum?.trim()?.takeIf { f -> f.isNotEmpty() } }
                .distinct()
                .sorted()
        }
    }

    RuleSimulatorDialog(dialogState = simulatorDialogState)

    AddBlockRuleDialog(
        dialogState = addDialogState,
        category = addBlockCategory,
        initialForum = selectedForumFilter?.takeIf { it.isNotBlank() },
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
            BlockListTopBar(
                pagerState = pagerState,
                onBack = { navigator.navigateUp() },
                onTabSelect = { page ->
                    coroutineScope.launch { pagerState.animateScrollToPage(page) }
                },
                onSimulatorClick = { simulatorDialogState.show() },
                onExportClick = {
                    val json = BlockManager.exportRulesJson()
                    clipboardManager.setText(AnnotatedString(json))
                    val count = blackList.size + whiteList.size
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            String.format(context.getString(R.string.toast_export_success), count)
                        )
                    }
                },
                onImportClick = { importDialogState.show() },
                onClearClick = { clearConfirmDialogState.show() }
            )
        },
        bottomBar = {
            BlockListBottomBar(
                onAddBlackClick = {
                    addBlockCategory = Block.CATEGORY_BLACK_LIST
                    addDialogState.show()
                },
                onAddWhiteClick = {
                    addBlockCategory = Block.CATEGORY_WHITE_LIST
                    addDialogState.show()
                }
            )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (availableForums.isNotEmpty()) {
                ForumFilterBar(
                    availableForums = availableForums,
                    selectedForum = selectedForumFilter,
                    onSelectForum = { selectedForumFilter = it }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                key = { it },
                verticalAlignment = Alignment.Top
            ) { position ->
                val rawItems by remember {
                    derivedStateOf {
                        if (position == 0) blackList else whiteList
                    }
                }
                val items by remember(rawItems, selectedForumFilter) {
                    derivedStateOf {
                        when (selectedForumFilter) {
                            null -> rawItems
                            "" -> rawItems.filter { it.forum.isNullOrBlank() }
                            else -> rawItems.filter { it.forum?.equals(selectedForumFilter, ignoreCase = true) == true }
                        }
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
}
