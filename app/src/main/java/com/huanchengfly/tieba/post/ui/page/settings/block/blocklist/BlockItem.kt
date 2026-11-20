package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.utils.GsonUtil

@Composable
fun BlockItemPlaceholder(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            modifier = Modifier.placeholder(visible = true)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.title_block_settings),
            modifier = Modifier.placeholder(visible = true)
        )
    }
}

@Composable
fun BlockItem(
    item: Block,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, typeName) = when (item.type) {
            Block.TYPE_USER -> Icons.Outlined.AccountCircle to stringResource(R.string.rule_type_user)
            Block.TYPE_REGEX -> Icons.Outlined.Edit to stringResource(R.string.rule_type_regex)
            Block.TYPE_IP_REGION -> Icons.Outlined.Explore to stringResource(R.string.rule_type_ip)
            Block.TYPE_USER_LEVEL -> Icons.Rounded.Star to stringResource(R.string.rule_type_level)
            else -> ImageVector.vectorResource(id = R.drawable.ic_comment_new) to stringResource(R.string.rule_type_keyword)
        }

        Icon(
            imageVector = icon,
            contentDescription = typeName,
            tint = MaterialTheme.colors.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.secondary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                }

                val forumBadge = if (!item.forum.isNullOrBlank()) "${item.forum}吧" else stringResource(R.string.rule_scope_all)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (!item.forum.isNullOrBlank()) MaterialTheme.colors.primary.copy(alpha = 0.12f)
                            else ExtendedTheme.colors.text.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = forumBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!item.forum.isNullOrBlank()) MaterialTheme.colors.primary else ExtendedTheme.colors.textSecondary
                    )
                }

                when (item.type) {
                    Block.TYPE_USER -> {
                        Text(
                            text = item.username ?: "UID: ${item.uid}",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Block.TYPE_REGEX -> {
                        Text(
                            text = item.pattern.orEmpty(),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Block.TYPE_IP_REGION -> {
                        Text(
                            text = item.ipRegion.orEmpty(),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Block.TYPE_USER_LEVEL -> {
                        Text(
                            text = "低于 Lv.${item.minLevel} 级",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    else -> {
                        val keywordsList: List<String> = runCatching {
                            GsonUtil.getGson()
                                .fromJson<List<String>>(
                                    item.keywords ?: "[]",
                                    object : TypeToken<List<String>>() {}.type
                                )
                        }.getOrDefault(emptyList())
                        Text(
                            text = keywordsList.joinToString(" "),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (item.type == Block.TYPE_USER && !item.uid.isNullOrBlank() && !item.username.isNullOrBlank()) {
                Text(
                    text = "UID: ${item.uid}",
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
    }
}
