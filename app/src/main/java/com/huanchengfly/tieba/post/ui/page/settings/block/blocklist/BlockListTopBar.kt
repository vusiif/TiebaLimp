package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlockListTopBar(
    pagerState: PagerState,
    onBack: () -> Unit,
    onTabSelect: (Int) -> Unit,
    onSimulatorClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TitleCentredToolbar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.title_block_list),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )
        },
        navigationIcon = {
            BackNavigationIcon(onBackPressed = onBack)
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
                    onSimulatorClick()
                }) {
                    Text(text = stringResource(id = R.string.btn_rule_simulator))
                }
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    onExportClick()
                }) {
                    Text(text = stringResource(id = R.string.btn_export_rules))
                }
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    onImportClick()
                }) {
                    Text(text = stringResource(id = R.string.btn_import_rules))
                }
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    onClearClick()
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
                    onClick = { onTabSelect(0) },
                    text = { Text(text = stringResource(id = R.string.title_black_list)) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { onTabSelect(1) },
                    text = { Text(text = stringResource(id = R.string.title_white_list)) },
                )
            }
        }
    )
}
