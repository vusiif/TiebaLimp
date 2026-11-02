package com.huanchengfly.tieba.post.ui.page.thread.snapshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThreadSnapshotUtil
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://thread_snapshot/{threadId}")
    ]
)
@Composable
fun ThreadSnapshotPage(
    navigator: DestinationsNavigator,
    threadId: Long,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var snapshotData by remember { mutableStateOf<ThreadSnapshotUtil.SnapshotData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(threadId) {
        isLoading = true
        snapshotData = ThreadSnapshotUtil.loadSnapshot(context, threadId)
        isLoading = false
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_thread_snapshots),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
                actions = {
                    IconButton(
                        onClick = {
                            navigator.navigate(ThreadPageDestination(threadId = threadId))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = stringResource(id = R.string.menu_open_online_thread),
                            tint = ExtendedTheme.colors.onTopBar
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.menu_delete_snapshot),
                            tint = ExtendedTheme.colors.onTopBar
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(text = stringResource(id = R.string.menu_delete_snapshot)) },
                text = { Text(text = stringResource(id = R.string.tip_confirm_delete_snapshot)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            coroutineScope.launch {
                                ThreadSnapshotUtil.deleteSnapshot(context, threadId)
                                context.toastShort(R.string.toast_snapshot_deleted)
                                navigator.navigateUp()
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.button_sure_default), color = MaterialTheme.colors.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(text = stringResource(id = R.string.button_cancel))
                    }
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
        } else {
            val data = snapshotData
            if (data == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_empty_snapshot),
                        style = MaterialTheme.typography.body2,
                        color = ExtendedTheme.colors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = ExtendedTheme.colors.card,
                            elevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (data.forumName.isNotBlank()) {
                                    Chip(
                                        text = "${data.forumName}吧",
                                        invertColor = false
                                    )
                                }
                                SelectionContainer {
                                    Text(
                                        text = data.title.ifBlank { "无标题" },
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold,
                                        color = ExtendedTheme.colors.text
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (data.authorAvatar.orEmpty().isNotBlank()) {
                                        Avatar(
                                            data = StringUtil.getAvatarUrl(data.authorAvatar),
                                            size = Sizes.Small,
                                            contentDescription = data.authorName
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = data.authorName.ifBlank { "楼主" },
                                            style = MaterialTheme.typography.subtitle2,
                                            fontWeight = FontWeight.Medium,
                                            color = ExtendedTheme.colors.text
                                        )
                                        Text(
                                            text = stringResource(
                                                id = R.string.tip_snapshot_captured_info,
                                                DateTimeUtils.getRelativeTimeString(context, data.snapshotTime),
                                                data.posts.size
                                            ),
                                            style = MaterialTheme.typography.caption,
                                            color = ExtendedTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(data.posts, key = { it.floor }) { post ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = ExtendedTheme.colors.card,
                            elevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Avatar(
                                        data = StringUtil.getAvatarUrl(post.authorAvatar),
                                        size = Sizes.Tiny,
                                        contentDescription = post.authorName
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = post.authorName,
                                            style = MaterialTheme.typography.body2,
                                            fontWeight = FontWeight.Medium,
                                            color = ExtendedTheme.colors.text
                                        )
                                        if (post.time > 0L) {
                                            Text(
                                                text = DateTimeUtils.getRelativeTimeString(context, post.time),
                                                style = MaterialTheme.typography.caption,
                                                color = ExtendedTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                    Chip(
                                        text = if (post.floor == 1L) "楼主" else "${post.floor} 楼",
                                        invertColor = post.floor == 1L
                                    )
                                }

                                if (post.contentText.isNotBlank()) {
                                    SelectionContainer {
                                        Text(
                                            text = post.contentText,
                                            style = MaterialTheme.typography.body1,
                                            color = ExtendedTheme.colors.text,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                if (post.images.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        post.images.forEachIndexed { idx, img ->
                                            val photoViewData = remember(img) {
                                                PhotoViewData(
                                                    picItems = listOf(
                                                        PicItem(
                                                            picId = "snap_${post.postId}_$idx",
                                                            picIndex = idx,
                                                            url = img.picUrl,
                                                            originUrl = img.originUrl,
                                                            showOriginBtn = false,
                                                            originSize = 0,
                                                            postId = post.postId
                                                        )
                                                    )
                                                )
                                            }
                                            val ratio = if (img.width > 0 && img.height > 0) {
                                                (img.width.toFloat() / img.height.toFloat()).coerceIn(0.5f, 2.5f)
                                            } else 1f

                                            NetworkImage(
                                                imageUri = img.picUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .fillMaxWidth()
                                                    .aspectRatio(ratio),
                                                photoViewData = photoViewData,
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }

                                if (post.subPosts.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ExtendedTheme.colors.bottomBarSurface)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        post.subPosts.forEach { sp ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${sp.authorName}: ",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.caption,
                                                    color = ExtendedTheme.colors.primary
                                                )
                                                Text(
                                                    text = sp.contentText,
                                                    style = MaterialTheme.typography.caption,
                                                    color = ExtendedTheme.colors.text,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.tip_snapshot_footer),
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
