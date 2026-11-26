package com.huanchengfly.tieba.post.ui.page.history.outbox

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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
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
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.PendingReplyItem
import com.huanchengfly.tieba.post.utils.ReplyOutboxCoordinator
import com.huanchengfly.tieba.post.utils.ReplyOutboxManager
import kotlinx.coroutines.launch

@Composable
fun OutboxListPage(
    refreshSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<PendingReplyItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRetrying by remember { mutableStateOf(false) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            items = ReplyOutboxCoordinator.getPendingItems()
            isLoading = false
        }
    }

    LaunchedEffect(refreshSignal) {
        loadData()
    }

    if (isLoading && items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
        }
    } else if (items.isEmpty()) {
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
                    imageVector = Icons.Outlined.Send,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = stringResource(id = R.string.tip_empty_outbox),
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
            item(key = "outbox_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        enabled = !isRetrying,
                        onClick = {
                            coroutineScope.launch {
                                isRetrying = true
                                val delivered = ReplyOutboxCoordinator.flushAll()
                                items = ReplyOutboxCoordinator.getPendingItems()
                                isRetrying = false
                                if (delivered > 0) {
                                    context.toastShort(R.string.toast_outbox_retry_success, delivered)
                                } else {
                                    context.toastShort(R.string.toast_outbox_retry_none)
                                }
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.button_retry_all))
                    }
                }
            }
            items(items, key = { it.id }) { item ->
                OutboxReplyCard(
                    item = item,
                    enabled = !isRetrying,
                    onClick = {
                        navigator.navigate(
                            ThreadPageDestination(
                                threadId = item.threadId,
                                forumId = item.forumId,
                                postId = item.postId ?: 0L
                            )
                        )
                    },
                    onRetry = {
                        coroutineScope.launch {
                            isRetrying = true
                            val ok = ReplyOutboxCoordinator.retryItem(item.id)
                            items = ReplyOutboxCoordinator.getPendingItems()
                            isRetrying = false
                            if (ok) {
                                context.toastShort(R.string.toast_outbox_retry_success, 1)
                            } else {
                                context.toastShort(R.string.toast_outbox_retry_none)
                            }
                        }
                    },
                    onCancel = {
                        coroutineScope.launch {
                            ReplyOutboxCoordinator.removeById(item.id)
                            items = ReplyOutboxCoordinator.getPendingItems()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OutboxReplyCard(
    item: PendingReplyItem,
    enabled: Boolean,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = ExtendedTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
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
                Chip(
                    text = stringResource(
                        id = R.string.text_outbox_forum_format,
                        item.forumName,
                        item.threadId
                    ),
                    invertColor = false,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = DateTimeUtils.getRelativeTimeString(context, item.createdAt),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
            Text(
                text = item.content,
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
                color = ExtendedTheme.colors.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.lastError.isNullOrBlank()) {
                Text(
                    text = item.lastError,
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        id = R.string.text_outbox_retry_count,
                        item.retryCount,
                        ReplyOutboxManager.MAX_RETRIES
                    ),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    enabled = enabled,
                    onClick = onCancel
                ) {
                    Text(text = stringResource(id = R.string.button_delete_outbox))
                }
                TextButton(
                    enabled = enabled,
                    onClick = onRetry
                ) {
                    Text(text = stringResource(id = R.string.button_retry_send))
                }
            }
        }
    }
}
