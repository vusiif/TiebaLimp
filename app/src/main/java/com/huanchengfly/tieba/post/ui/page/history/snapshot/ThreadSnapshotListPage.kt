package com.huanchengfly.tieba.post.ui.page.history.snapshot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.ThreadSnapshot
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadSnapshotPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThreadSnapshotUtil
import kotlinx.coroutines.launch

@Composable
fun ThreadSnapshotListPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    var snapshots by remember { mutableStateOf<List<ThreadSnapshot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            snapshots = ThreadSnapshotUtil.getAllSnapshots()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (isLoading && snapshots.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
        }
    } else if (snapshots.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = stringResource(id = R.string.tip_empty_snapshot),
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(snapshots, key = { it.threadId }) { snapshot ->
                val menuState = rememberMenuState()
                LongClickMenu(
                    menuState = menuState,
                    menuContent = {
                        DropdownMenuItem(
                            onClick = {
                                menuState.expanded = false
                                snapshot.threadId.toLongOrNull()?.let { tid ->
                                    navigator.navigate(ThreadSnapshotPageDestination(threadId = tid))
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.menu_view_snapshot))
                        }
                        DropdownMenuItem(
                            onClick = {
                                menuState.expanded = false
                                snapshot.threadId.toLongOrNull()?.let { tid ->
                                    navigator.navigate(ThreadPageDestination(threadId = tid))
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.menu_open_online_thread))
                        }
                        DropdownMenuItem(
                            onClick = {
                                menuState.expanded = false
                                coroutineScope.launch {
                                    snapshot.threadId.toLongOrNull()?.let { tid ->
                                        ThreadSnapshotUtil.deleteSnapshot(context, tid)
                                        context.toastShort(R.string.toast_snapshot_deleted)
                                        loadData()
                                    }
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.menu_delete_snapshot), color = MaterialTheme.colors.error)
                        }
                    }
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = ExtendedTheme.colors.card,
                        elevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                snapshot.threadId.toLongOrNull()?.let { tid ->
                                    navigator.navigate(ThreadSnapshotPageDestination(threadId = tid))
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = snapshot.title.ifBlank { "无标题帖子" },
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = ExtendedTheme.colors.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (snapshot.forumName.isNotBlank()) {
                                    Chip(
                                        text = "${snapshot.forumName}吧",
                                        invertColor = false
                                    )
                                }
                                Chip(
                                    text = "${snapshot.floorCount} 楼",
                                    invertColor = true
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = DateTimeUtils.getRelativeTimeString(context, snapshot.updateTime),
                                    style = MaterialTheme.typography.caption,
                                    color = ExtendedTheme.colors.textSecondary
                                )
                            }
                            if (snapshot.authorName.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Avatar(
                                        data = StringUtil.getAvatarUrl(snapshot.authorAvatar),
                                        size = Sizes.Tiny,
                                        contentDescription = snapshot.authorName
                                    )
                                    Text(
                                        text = snapshot.authorName,
                                        style = MaterialTheme.typography.caption,
                                        color = ExtendedTheme.colors.textSecondary
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
