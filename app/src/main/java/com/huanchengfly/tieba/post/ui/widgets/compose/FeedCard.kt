package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.thread.UserNameText
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.PollVoteStore
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
private fun UserHeader(
    userProvider: () -> ImmutableHolder<User>,
    timeProvider: () -> Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val user = remember(userProvider) { userProvider() }
    val time = remember(timeProvider) { timeProvider() }
    val bawuType = remember(user) { user.get { bawuType } }
    val ipAddress = remember(user) { user.get { ip_address } }
    UserHeader(
        avatar = {
            Avatar(
                data = user.get { StringUtil.getAvatarUrl(portrait) },
                size = Sizes.Small,
                contentDescription = null
            )
        },
        name = {
            UserNameText(
                userName = StringUtil.getUsernameAnnotatedString(
                    context = LocalContext.current,
                    username = user.get { name },
                    nickname = user.get { nameShow },
                    color = LocalContentColor.current
                ),
                userLevel = user.get { level_id },
                bawuType = bawuType
            )
        },
        onClick = onClick,
        desc = {
            val relativeTime = DateTimeUtils.getRelativeTimeString(
                context,
                time.toString()
            )
            val descText = if (ipAddress.isNotBlank()) {
                "$relativeTime  ${stringResource(R.string.text_ip_location, ipAddress)}"
            } else {
                relativeTime
            }
            Text(text = descText)
        },
        content = content,
        modifier = modifier
    )
}

@Composable
fun UserHeader(
    nameProvider: () -> String,
    nameShowProvider: () -> String,
    portraitProvider: () -> String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    timeProvider: (() -> Int)? = null,
    ipProvider: (() -> String)? = null,
    content: @Composable RowScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val name = remember(nameProvider) { nameProvider() }
    val nameShow = remember(nameShowProvider) { nameShowProvider() }
    val portrait = remember(portraitProvider) { portraitProvider() }
    val time = remember(timeProvider) { timeProvider?.invoke() }
    val ip = remember(ipProvider) { ipProvider?.invoke().orEmpty() }
    UserHeader(
        avatar = {
            Avatar(
                data = StringUtil.getAvatarUrl(portrait),
                size = Sizes.Small,
                contentDescription = null
            )
        },
        name = {
            Text(
                text = StringUtil.getUsernameAnnotatedString(
                    context = LocalContext.current,
                    username = name,
                    nickname = nameShow,
                    color = LocalContentColor.current
                ),
                color = ExtendedTheme.colors.text
            )
        },
        onClick = onClick,
        desc = (@Composable {
            val relativeTime = time?.let {
                DateTimeUtils.getRelativeTimeString(
                    context,
                    it.toString()
                )
            }.orEmpty()
            val descText = if (ip.isNotBlank()) {
                if (relativeTime.isNotEmpty()) "$relativeTime  ${stringResource(R.string.text_ip_location, ip)}"
                else stringResource(R.string.text_ip_location, ip)
            } else {
                relativeTime
            }
            Text(text = descText)
        }).takeIf { time != null || ip.isNotBlank() },
        content = content,
        modifier = modifier
    )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
    action: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    val cardModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    val paddingModifier = if (action != null) Modifier.padding(top = 16.dp)
    else Modifier.padding(vertical = 16.dp)

    Column(
        modifier = cardModifier
            .then(modifier)
            .then(paddingModifier)
            .padding(contentPadding)
    ) {
        header()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            content()
        }
        action?.invoke(this)
    }
}

@Composable
fun ThreadContent(
    modifier: Modifier = Modifier,
    title: String = "",
    abstractText: String = "",
    tabName: String = "",
    showTitle: Boolean = true,
    showAbstract: Boolean = true,
    isGood: Boolean = false,
    maxLines: Int = 5,
    highlightKeywords: ImmutableList<String> = persistentListOf(),
) {
    val content = buildAnnotatedString {
        if (showTitle) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                if (isGood) {
                    withStyle(style = SpanStyle(color = ExtendedTheme.colors.accent)) {
                        append(stringResource(id = R.string.tip_good))
                    }
                    append(" ")
                }

                if (tabName.isNotBlank()) {
                    append(tabName)
                    append(" | ")
                }

                append(title)
            }
        }
        if (showTitle && showAbstract) {
            append('\n')
        }
        if (showAbstract) {
            append(abstractText.emoticonString)
        }
    }

    HighlightText(
        text = content,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        fontSize = 15.sp,
        lineSpacing = 0.8.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        style = MaterialTheme.typography.body1,
        highlightKeywords = highlightKeywords
    )
}

@Composable
fun FeedCardPlaceholder() {
    Card(
        header = { UserHeaderPlaceholder(avatarSize = Sizes.Small) },
        content = {
            Text(
                text = "TitlePlaceholder",
                style = MaterialTheme.typography.subtitle1,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.fade(),
                    )
            )

            Text(
                text = "Text",
                style = MaterialTheme.typography.body1,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.fade(),
                    )
            )
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    ActionBtnPlaceholder(
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@Composable
fun ForumInfoChip(
    imageUriProvider: () -> String?,
    nameProvider: () -> String,
    onClick: () -> Unit,
) {
    val imageUri = imageUriProvider()
    val name = nameProvider()
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(4.dp))
            .background(color = ExtendedTheme.colors.chip)
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageUri?.let {
            Avatar(
                data = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(4.dp)
            )
        }
        Text(
            text = stringResource(id = R.string.title_forum_name, name),
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.onChip,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ThreadForumInfo(
    item: ImmutableHolder<ThreadInfo>,
    onClick: (SimpleForum) -> Unit,
) {
    val hasForumInfo = remember(item) { item.isNotNull { forumInfo } }
    if (hasForumInfo) {
        val forumInfo = remember(item) { item.getImmutable { forumInfo!! } }
        ThreadForumInfo(
            forumName = forumInfo.get { name },
            forumAvatar = forumInfo.get { avatar },
            onClick = { onClick(forumInfo.get()) }
        )
    }
}

@Composable
private fun ThreadForumInfo(
    forumName: String,
    forumAvatar: String?,
    onClick: () -> Unit,
) {
    val hasForum = remember(forumName) { forumName.isNotBlank() }
    if (hasForum) {
        ForumInfoChip(
            imageUriProvider = { forumAvatar },
            nameProvider = { forumName },
            onClick = onClick
        )
    }
}

@Composable
fun FeedCard(
    item: ImmutableHolder<ThreadInfo>,
    onClick: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadInfo) -> Unit = {},
    onClickUser: (User) -> Unit = {},
    onClickForum: (SimpleForum) -> Unit = {},
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
    dislikeAction: @Composable () -> Unit = {},
) {
    Card(
        header = {
            val author = remember(item) { item.getNullableImmutable { author } }
            author?.let {
                UserHeader(
                    userProvider = { it },
                    timeProvider = { item.get { lastTimeInt } },
                    onClick = {
                        onClickUser(it.get())
                    },
                ) { dislikeAction() }
            }
        },
        content = {
            ThreadContent(
                title = item.get { title },
                abstractText = item.get { abstractText },
                tabName = item.get { tabName },
                showTitle = item.get { isNoTitle != 1 && title.isNotBlank() },
                showAbstract = item.get { abstractText.isNotBlank() },
                isGood = item.get { isGood == 1 },
            )

            ThreadMedia(
                item = item,
            )

            item.getNullableImmutable { origin_thread_info }
                .takeIf { item.get { is_share_thread } == 1 }?.let {
                    OriginThreadCard(
                        originThreadInfo = it,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .clickable {
                                onClickOriginThread(it.get())
                            }
                            .padding(16.dp)
                    )
                }

            val cardThreadId = item.get { if (threadId != 0L) threadId else id }
            var localPollInfo by remember(item) {
                mutableStateOf(PollVoteStore.get(cardThreadId, item.get { poll_info }))
            }
            localPollInfo?.takeIf { it.options.isNotEmpty() }?.let { poll ->
                VoteCard(
                    pollInfo = poll,
                    threadId = cardThreadId,
                    forumId = item.get { forumId },
                    onVoteSuccess = { newPoll -> localPollInfo = newPoll },
                )
            }

            ThreadForumInfo(item = item, onClick = onClickForum)
        },
        action = {
            val context = LocalContext.current
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = item.get { shareNum },
                    onClick = {
                        val threadId = item.get { if (threadId != 0L) threadId else id }
                        val title = item.get { title }.ifBlank { null }
                        TiebaUtil.shareText(context, "https://tieba.baidu.com/p/$threadId", title)
                    },
                    modifier = Modifier.weight(1f)
                )

                ThreadReplyBtn(
                    replyNum = item.get { replyNum },
                    onClick = { onClickReply(item.get()) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = item.get { agree?.hasAgree == 1 },
                    agreeNum = item.get { agreeNum },
                    onClick = { onAgree(item.get()) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item.get()) },
        modifier = modifier,
    )
}

@Composable
fun FeedCard(
    item: ImmutableHolder<PostInfoList>,
    onClick: (PostInfoList) -> Unit,
    onAgree: (PostInfoList) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (PostInfoList) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
) {
    Card(
        header = {
            UserHeader(
                nameProvider = { item.get { user_name } },
                nameShowProvider = { item.get { name_show } },
                portraitProvider = { item.get { user_portrait } },
                timeProvider = { item.get { create_time } },
                ipProvider = { item.get { ip } },
                onClick = {
                    onClickUser(item.get { user_id })
                },
            )
        },
        content = {
            ThreadContent(
                title = item.get { title },
                abstractText = item.get { abstractText },
                showTitle = item.get { is_ntitle != 1 && title.isNotBlank() },
                showAbstract = item.get { abstractText.isNotBlank() },
            )

            ThreadMedia(
                forumId = item.get { forum_id },
                forumName = item.get { forum_name },
                threadId = item.get { thread_id },
                medias = item.getImmutableList { media },
                videoInfo = item.getNullableImmutable { video_info }
            )

            item.getNullableImmutable { origin_thread_info }
                .takeIf { item.get { is_share_thread } == 1 }?.let {
                    OriginThreadCard(
                        originThreadInfo = it,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .clickable {
                                onClickOriginThread(it.get())
                            }
                            .padding(16.dp)
                    )
                }

            val pfcThreadId = item.get { thread_id }
            var localPollInfo by remember(item) {
                mutableStateOf(PollVoteStore.get(pfcThreadId, item.get { poll_info }))
            }
            localPollInfo?.takeIf { it.options.isNotEmpty() }?.let { poll ->
                VoteCard(
                    pollInfo = poll,
                    threadId = pfcThreadId,
                    forumId = item.get { forum_id },
                    onVoteSuccess = { newPoll -> localPollInfo = newPoll },
                )
            }

            ThreadForumInfo(
                forumName = item.get { forum_name },
                forumAvatar = null,
                onClick = { onClickForum(item.get { forum_name }) }
            )
        },
        action = {
            val context = LocalContext.current
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = item.get { share_num }.toLong(),
                    onClick = {
                        val threadId = item.get { thread_id }
                        val title = item.get { title }.ifBlank { null }
                        TiebaUtil.shareText(context, "https://tieba.baidu.com/p/$threadId", title)
                    },
                    modifier = Modifier.weight(1f)
                )

                ThreadReplyBtn(
                    replyNum = item.get { reply_num },
                    onClick = { onClickReply(item.get()) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = item.get { agree?.hasAgree == 1 },
                    agreeNum = item.get { agree_num },
                    onClick = { onAgree(item.get()) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item.get()) },
        modifier = modifier,
    )
}

@Preview("FeedCardPreview")
@Composable
fun FeedCardPreview() {
    FeedCard(
        item = wrapImmutable(
            ThreadInfo(
                title = "预览",
                author = User(),
                lastTimeInt = (System.currentTimeMillis() / 1000).toInt()
            )
        ),
        onClick = {},
        onAgree = {},
        modifier = Modifier.background(ExtendedTheme.colors.card)
    )
}
