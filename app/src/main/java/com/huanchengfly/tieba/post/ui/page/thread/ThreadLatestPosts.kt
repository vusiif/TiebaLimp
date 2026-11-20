package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider

fun LazyListScope.threadLatestPosts(
    latestPosts: List<PostItemData>,
    desc: Boolean,
    highlightedFloor: Int,
    renderPostCard: @Composable (postItem: PostItemData, isHighlighted: Boolean) -> Unit,
) {
    if (latestPosts.isEmpty()) return

    if (!desc) {
        item("LatestPostsTip") {
            Container {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerticalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(id = R.string.below_is_latest_post),
                        color = ExtendedTheme.colors.textSecondary,
                        style = MaterialTheme.typography.caption,
                    )
                    VerticalDivider(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    items(
        items = latestPosts,
        key = { (item) -> "LatestPost_${item.get { id }}" },
        contentType = { (item, blocked) ->
            if (blocked) "BlockedPost"
            else if (item.get { is_fold == 1 || fold_comment_status != 0 }) "FoldedPost"
            else "NormalPost"
        }
    ) { postItem ->
        Container {
            renderPostCard(postItem, highlightedFloor == postItem.post.get { floor })
        }
    }
    if (desc) {
        item("LatestPostsTip") {
            Container {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerticalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(id = R.string.above_is_latest_post),
                        color = ExtendedTheme.colors.textSecondary,
                        style = MaterialTheme.typography.caption,
                    )
                    VerticalDivider(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
