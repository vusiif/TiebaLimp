package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun SearchHitCard(
    hit: ThreadSearchHit,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ExtendedTheme.colors.chip)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (hit.floor == 1) "1楼 (楼主)" else "${hit.floor}楼",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = hit.authorName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ExtendedTheme.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.SouthEast,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Highlight matched snippet
            val annotatedSnippet = buildAnnotatedString {
                val text = hit.snippet
                val q = query.trim()
                if (q.isEmpty()) {
                    append(text)
                } else {
                    var startIndex = 0
                    while (startIndex < text.length) {
                        val matchIndex = text.indexOf(q, startIndex, ignoreCase = true)
                        if (matchIndex == -1) {
                            append(text.substring(startIndex))
                            break
                        }
                        if (matchIndex > startIndex) {
                            append(text.substring(startIndex, matchIndex))
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFFF8F00),
                                fontWeight = FontWeight.Bold,
                                background = Color(0xFFFFB300).copy(alpha = 0.25f)
                            )
                        ) {
                            append(text.substring(matchIndex, matchIndex + q.length))
                        }
                        startIndex = matchIndex + q.length
                    }
                }
            }

            Text(
                text = annotatedSnippet,
                fontSize = 13.sp,
                color = ExtendedTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
        }
    }
}
