package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.utils.StringUtil

internal fun getDescText(
    time: Long?,
    floor: Int,
    ipAddress: String?
): String {
    val texts = listOfNotNull(
        time?.let { getRelativeTimeString(App.INSTANCE, it) },
        if (floor > 1) App.INSTANCE.getString(R.string.tip_post_floor, floor) else null,
        if (ipAddress.isNullOrEmpty()) null else App.INSTANCE.getString(
            R.string.text_ip_location,
            ipAddress
        )
    )
    if (texts.isEmpty()) {
        return ""
    }
    return texts.joinToString(" · ")
}

@Composable
fun PostCardHeader(
    author: User,
    floor: Int,
    time: Long?,
    threadAuthorId: Long,
    hasAgreed: Boolean,
    agreeNum: Long,
    onUserClick: () -> Unit,
    onAgree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UserHeader(
        modifier = modifier,
        avatar = {
            Avatar(
                data = StringUtil.getAvatarUrl(author.portrait),
                size = Sizes.Small,
                contentDescription = null
            )
        },
        name = {
            UserNameText(
                userName = StringUtil.getUsernameAnnotatedString(
                    LocalContext.current,
                    author.name,
                    author.nameShow
                ),
                userLevel = author.level_id,
                isLz = author.id == threadAuthorId,
                bawuType = author.bawuType,
            )
        },
        desc = {
            Text(
                text = getDescText(
                    time,
                    floor,
                    author.ip_address
                )
            )
        },
        onClick = onUserClick
    ) {
        if (floor > 1) {
            PostAgreeBtn(
                hasAgreed = hasAgreed,
                agreeNum = agreeNum,
                onClick = onAgree
            )
        }
    }
}
