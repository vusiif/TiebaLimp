package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.ui.page.thread.PostItemData
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun ThreadSearchSection(
    firstPost: Post?,
    loadedPosts: List<PostItemData>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectSearchHit: (hit: ThreadSearchHit, query: String, allHits: List<ThreadSearchHit>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchHits = remember(searchQuery, firstPost, loadedPosts) {
        ThreadSearchUtil.search(searchQuery, firstPost, loadedPosts)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(text = stringResource(id = R.string.hint_search_thread)) },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.tip_search_matches_count, searchHits.size),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = R.string.tip_search_hint_load_more),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (searchHits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_search_no_matches),
                        style = MaterialTheme.typography.body2,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchHits, key = { "${it.floor}_${it.postId}" }) { hit ->
                        SearchHitCard(
                            hit = hit,
                            query = searchQuery,
                            onClick = {
                                onSelectSearchHit(hit, searchQuery, searchHits)
                            }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "输入关键词检索已加载帖内楼层",
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary,
                )
            }
        }
    }
}
