package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun ForumFilterBar(
    availableForums: List<String>,
    selectedForum: String?,
    onSelectForum: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            ForumFilterChip(
                label = stringResource(R.string.filter_all_rules),
                selected = selectedForum == null,
                onClick = { onSelectForum(null) }
            )
        }
        item {
            ForumFilterChip(
                label = stringResource(R.string.filter_global_rules),
                selected = selectedForum == "",
                onClick = { onSelectForum("") }
            )
        }
        items(availableForums, key = { it }) { forum ->
            ForumFilterChip(
                label = "${forum}吧",
                selected = selectedForum.equals(forum, ignoreCase = true),
                onClick = { onSelectForum(forum) }
            )
        }
    }
}

@Composable
fun ForumFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) MaterialTheme.colors.primary else ExtendedTheme.colors.chip
    val textColor = if (selected) Color.White else ExtendedTheme.colors.text
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
