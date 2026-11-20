package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
fun SubPostsBottomBar(
    account: Account?,
    onReplyClick: () -> Unit,
) {
    val context = LocalContext.current
    if (account != null && !context.appPreferences.hideReply) {
        Column(
            modifier = Modifier.background(ExtendedTheme.colors.threadBottomBar)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Avatar(
                    data = StringUtil.getAvatarUrl(account.portrait),
                    size = Sizes.Tiny,
                    contentDescription = account.name,
                )
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ExtendedTheme.colors.bottomBarSurface)
                        .clickable { onReplyClick() }
                        .padding(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_reply_thread),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.onBottomBarSurface,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .requiredHeightIn(min = if (context.appPreferences.liftUpBottomBar) 16.dp else 0.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }
}
