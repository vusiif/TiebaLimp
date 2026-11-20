package com.huanchengfly.tieba.post.ui.page.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString

@Composable
internal fun ToolbarUserTitle(
    user: ImmutableHolder<User>,
    modifier: Modifier = Modifier,
) {
    UserHeader(
        avatar = {
            Avatar(
                data = StringUtil.getAvatarUrl(user.get { portrait }),
                size = Sizes.Small,
                contentDescription = null
            )
        },
        name = {
            Text(
                text = StringUtil.getUsernameAnnotatedString(
                    LocalContext.current,
                    user.get { name },
                    user.get { nameShow },
                    LocalContentColor.current
                )
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UserProfileDetail(
    user: ImmutableHolder<User>,
    modifier: Modifier = Modifier,
    showBtn: Boolean = true,
    isSelf: Boolean = false,
    onBtnClick: () -> Unit = {},
    onCopyIdClick: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Avatar(
                data = StringUtil.getBigAvatarUrl(user.get { portrait }),
                size = 96.dp,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (showBtn) {
                Button(
                    onClick = onBtnClick,
                    colors = if (user.get { has_concerned } == 0 || isSelf) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    border = if (user.get { has_concerned } == 0 || isSelf) {
                        null
                    } else {
                        ButtonDefaults.outlinedBorder
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSelf) {
                            Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                            Text(text = stringResource(id = R.string.menu_edit_info))
                        } else if (user.get { has_concerned } == 0) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                            Text(text = stringResource(id = R.string.button_follow))
                        } else {
                            Text(text = stringResource(id = R.string.button_unfollow))
                        }
                    }
                }
            }
        }
        Text(
            text = StringUtil.getUsernameAnnotatedString(
                LocalContext.current,
                user.get { name },
                user.get { nameShow },
                LocalContentColor.current
            ),
            style = MaterialTheme.typography.h6,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold
        )
        ProvideTextStyle(value = MaterialTheme.typography.body2) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.text_stat_follow),
                        color = ExtendedTheme.colors.textSecondary
                    )
                    Text(
                        text = user.get { concern_num }.getShortNumString(),
                        color = ExtendedTheme.colors.text,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxHeight())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.text_stat_fans),
                        color = ExtendedTheme.colors.textSecondary
                    )
                    Text(
                        text = user.get { fans_num }.getShortNumString(),
                        color = ExtendedTheme.colors.text,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxHeight())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.text_stat_agrees),
                        color = ExtendedTheme.colors.textSecondary
                    )
                    Text(
                        text = user.get { total_agree_num }.getShortNumString(),
                        color = ExtendedTheme.colors.text,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = user.get { intro }.takeIf { it.isNotEmpty() }
                ?: stringResource(id = R.string.tip_no_intro),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        user.getNullableImmutable { bazhu_grade }?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ExtendedTheme.colors.primary,
                )
                Text(
                    text = it.get { desc },
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.primary,
                )
            }
        } ?: user.getNullableImmutable { new_god_data }
            ?.takeIf { it.get { status } != 0 }
            ?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ExtendedTheme.colors.primary,
                    )
                    Text(
                        text = stringResource(id = R.string.text_god_verify, it.get { field_name }),
                        style = MaterialTheme.typography.body2,
                        color = ExtendedTheme.colors.primary,
                    )
                }
            }
        val sexEmoji = when (user.get { sex }) {
            1 -> "♂"
            2 -> "♀"
            else -> "?"
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip(text = sexEmoji, invertColor = true)
            Chip(
                text = stringResource(
                    id = R.string.text_profile_user_id,
                    user.get { tieba_uid }.toString()
                ),
                appendIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null
                    )
                },
                onClick = onCopyIdClick
            )
            if (user.get { ip_address }.isNotEmpty()) {
                Chip(
                    text = stringResource(
                        id = R.string.text_profile_ip_location,
                        user.get { ip_address }
                    ),
                )
            }
            Chip(
                text = stringResource(
                    id = R.string.text_profile_tb_age,
                    user.get { tb_age }
                )
            )
        }
    }
}

@Composable
internal fun HorizontalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .background(ExtendedTheme.colors.divider)
            .width(1.dp)
            .then(modifier)
    )
}
