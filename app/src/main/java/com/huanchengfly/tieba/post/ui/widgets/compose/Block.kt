package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
fun BlockTip(
    text: @Composable () -> Unit = { Text(text = stringResource(id = R.string.tip_blocked_content)) },
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ExtendedTheme.colors.textSecondary.copy(alpha = 0.1f))
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.caption) {
            ProvideContentColor(color = ExtendedTheme.colors.textSecondary) {
                text()
            }
        }
    }
}

@Composable
fun BlockableContent(
    blocked: Boolean,
    modifier: Modifier = Modifier,
    blockedTip: @Composable () -> Unit = { BlockTip() },
    hideBlockedContent: Boolean = LocalContext.current.appPreferences.hideBlockedContent,
    content: @Composable () -> Unit,
) {
    if (!blocked) {
        content()
    } else if (!hideBlockedContent) {
        Column(modifier = modifier) {
            blockedTip()
        }
    }
}

@Composable
fun FoldedTip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ExtendedTheme.colors.textSecondary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}