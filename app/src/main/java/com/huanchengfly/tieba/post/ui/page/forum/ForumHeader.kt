package com.huanchengfly.tieba.post.ui.page.forum

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.requestPinShortcut
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.PlaceholderHighlight
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun ForumHeaderPlaceholder(
    forumName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarPlaceholder(size = Sizes.Large)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.title_forum, forumName),
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (LocalAccount.current != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.fade(),
                        )
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(text = stringResource(id = R.string.button_sign_in), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
internal fun ForumHeader(
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>,
    onOpenForumInfo: () -> Unit,
    onBtnClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (forum) = forumInfoImmutableHolder
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = forum.avatar,
                size = Sizes.Large,
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenForumInfo
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.title_forum, forum.name),
                        style = MaterialTheme.typography.h6,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AnimatedVisibility(visible = forum.is_like == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = max(
                                0F,
                                min(
                                    1F,
                                    forum.cur_score * 1.0F / (max(1.0F, forum.levelup_score * 1.0F))
                                )
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .height(8.dp),
                            color = ExtendedTheme.colors.primary,
                            backgroundColor = ExtendedTheme.colors.primary.copy(alpha = 0.25f)
                        )
                        Text(
                            text = stringResource(
                                id = R.string.tip_forum_header_liked,
                                forum.user_level.toString(),
                                forum.level_name
                            ),
                            style = MaterialTheme.typography.caption,
                            color = ExtendedTheme.colors.textSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            val btnEnabled =
                (forum.is_like != 1) || (forum.sign_in_info?.user_info?.is_sign_in != 1)
            if (LocalAccount.current != null) {
                Button(
                    onClick = onBtnClick,
                    elevation = null,
                    shape = RoundedCornerShape(100),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ExtendedTheme.colors.primary,
                        contentColor = ExtendedTheme.colors.onAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    enabled = btnEnabled
                ) {
                    val text = when {
                        forum.is_like != 1 -> stringResource(id = R.string.button_follow)
                        forum.sign_in_info?.user_info?.is_sign_in == 1 -> stringResource(
                            id = R.string.button_signed_in,
                            forum.sign_in_info.user_info.cont_sign_num
                        )

                        else -> stringResource(id = R.string.button_sign_in)
                    }
                    Text(text = text, fontSize = 13.sp)
                }
            }
        }
    }
}

internal fun shareForum(context: Context, forumName: String) {
    TiebaUtil.shareText(
        context,
        "https://tieba.baidu.com/f?kw=$forumName",
        context.getString(R.string.title_forum, forumName)
    )
}

internal suspend fun sendToDesktop(
    context: Context,
    forum: ForumInfo,
    onSuccess: () -> Unit = {},
    onFailure: (String) -> Unit = {}
) {
    requestPinShortcut(
        context,
        "forum_${forum.id}",
        forum.avatar,
        context.getString(R.string.title_forum, forum.name),
        Intent(Intent.ACTION_VIEW).setData(Uri.parse("tblite://forum/${forum.name}")),
        onSuccess = onSuccess,
        onFailure = onFailure
    )
}

@Composable
internal fun RowScope.StatCardItem(
    statNum: Int,
    statText: String
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statNum.getShortNumString(),
            fontSize = 20.sp,
            fontFamily = FontFamily(
                Typeface.createFromAsset(
                    LocalContext.current.assets,
                    "bebas.ttf"
                )
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            fontSize = 12.sp,
            color = ExtendedTheme.colors.textSecondary
        )
    }
}
