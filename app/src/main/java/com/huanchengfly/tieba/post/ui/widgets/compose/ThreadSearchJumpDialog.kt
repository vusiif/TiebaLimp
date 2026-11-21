package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.ui.page.thread.PostItemData

@Composable
fun ThreadSearchJumpDialog(
    dialogState: DialogState,
    totalFloors: Int,
    currentPage: Int,
    totalPage: Int,
    firstPost: Post?,
    loadedPosts: List<PostItemData>,
    onJumpToFloor: (targetFloor: Int) -> Unit,
    onJumpToPage: (targetPage: Int) -> Unit,
    onSelectSearchHit: (hit: ThreadSearchHit, query: String, allHits: List<ThreadSearchHit>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Dialog(
        dialogState = dialogState,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.title_search_and_jump),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
        },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.primary,
                divider = {},
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(id = R.string.tab_jump_floor),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(id = R.string.tab_search_thread),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    ThreadJumpFloorSection(
                        totalFloors = totalFloors,
                        totalPage = totalPage,
                        onJumpToFloor = { targetFloor ->
                            dialogState.show = false
                            onJumpToFloor(targetFloor)
                        },
                        onJumpToPage = { targetPage ->
                            dialogState.show = false
                            onJumpToPage(targetPage)
                        }
                    )
                }

                1 -> {
                    ThreadSearchSection(
                        firstPost = firstPost,
                        loadedPosts = loadedPosts,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSelectSearchHit = { hit, query, allHits ->
                            dialogState.show = false
                            onSelectSearchHit(hit, query, allHits)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
