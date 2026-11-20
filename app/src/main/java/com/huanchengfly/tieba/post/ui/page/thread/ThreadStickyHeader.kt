package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider

@Composable
fun ThreadStickyHeader(
    replyCount: Int,
    isSeeLz: Boolean,
    onToggleSeeLz: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Container(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.title_thread_header,
                    "${(replyCount - 1).coerceAtLeast(0)}"
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ExtendedTheme.colors.text,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                Text(
                    text = stringResource(R.string.text_all),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isSeeLz
                        ) {
                            if (isSeeLz) {
                                onToggleSeeLz(false)
                            }
                        },
                    fontSize = 13.sp,
                    fontWeight = if (!isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (!isSeeLz) ExtendedTheme.colors.text else ExtendedTheme.colors.textSecondary,
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.title_see_lz),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isSeeLz
                        ) {
                            if (!isSeeLz) {
                                onToggleSeeLz(true)
                            }
                        },
                    fontSize = 13.sp,
                    fontWeight = if (isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSeeLz) ExtendedTheme.colors.text else ExtendedTheme.colors.textSecondary,
                )
            }
        }
    }
}
