package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.thread.PostAgreeBtn
import com.huanchengfly.tieba.post.ui.page.thread.UserNameText
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.launch

fun getDescText(
    time: Long?,
    ipAddress: String?
): String {
    val texts = listOfNotNull(
        time?.let { DateTimeUtils.getRelativeTimeString(App.INSTANCE, it) },
        ipAddress?.let { App.INSTANCE.getString(R.string.text_ip_location, it) }
    )
    if (texts.isEmpty()) return ""
    return texts.joinToString(" ")
}

@Composable
fun SubPostItem(
    item: SubPostItemData,
    threadAuthorId: Long? = null,
    canDelete: Boolean = false,
    onUserClick: (User) -> Unit = {},
    onAgree: (SubPostList) -> Unit = {},
    onReplyClick: (SubPostList) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostList) -> Unit)? = null,
) {
    val (subPost, contentRenders, blocked) = item
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val author = remember(subPost) { subPost.get { author }?.wrapImmutable() }
    val hasAgreed = remember(subPost) {
        subPost.get { agree?.hasAgree == 1 }
    }
    val agreeNum = remember(subPost) {
        subPost.get { agree?.diffAgreeNum ?: 0L }
    }
    val avatarUrl = remember(author) { author?.get { StringUtil.getAvatarUrl(portrait) } }
    val isLz = remember(author, threadAuthorId) {
        author?.get { id } == threadAuthorId
    }
    val userLevel = remember(author) { author?.get { level_id } ?: 0 }
    val bawuType = remember(author) { author?.get { bawuType } }
    val userName = remember(author, context) {
        author?.let {
            StringUtil.getUsernameAnnotatedString(
                context,
                it.get { name },
                it.get { nameShow }
            )
        }
    }
    val descText = remember(subPost, author) {
        getDescText(
            subPost.get { time }.toLong(),
            author?.get { ip_address }
        )
    }
    val onUserClickItem = remember(author, onUserClick) {
        { author?.let { onUserClick(it.get()) } ?: Unit }
    }
    val onAgreeItem = remember(subPost, onAgree) {
        { onAgree(subPost.get()) }
    }
    val onReplyItem = remember(subPost, onReplyClick) {
        { onReplyClick(subPost.get()) }
    }
    val menuState = rememberMenuState()
    BlockableContent(
        blocked = blocked,
        blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_sub_post)) }) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        LongClickMenu(
            menuState = menuState,
            indication = null,
            menuContent = {
                if (!context.appPreferences.hideReply) {
                    DropdownMenuItem(
                        onClick = {
                            onReplyItem()
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.btn_reply))
                    }
                }
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuCopyClick(contentRenders.joinToString("\n") { it.toString() })
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.menu_copy))
                    }
                }
                DropdownMenuItem(
                    onClick = {
                        coroutineScope.launch {
                            TiebaUtil.reportPost(context, navigator, subPost.get { id }.toString())
                        }
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.title_report))
                }
                if (canDelete && onMenuDeleteClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuDeleteClick(subPost.get())
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_delete))
                    }
                }
            },
            onClick = onReplyItem.takeUnless { context.appPreferences.hideReply }
        ) {
            Card(
                header = {
                    if (author != null) {
                        UserHeader(
                            avatar = {
                                Avatar(
                                    data = avatarUrl,
                                    size = Sizes.Small,
                                    contentDescription = null
                                )
                            },
                            name = {
                                userName?.let {
                                    UserNameText(
                                        userName = it,
                                        userLevel = userLevel,
                                        isLz = isLz,
                                        bawuType = bawuType,
                                    )
                                }
                            },
                            desc = {
                                Text(text = descText)
                            },
                            onClick = onUserClickItem
                        ) {
                            PostAgreeBtn(
                                hasAgreed = hasAgreed,
                                agreeNum = agreeNum,
                                onClick = onAgreeItem
                            )
                        }
                    }
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(start = Sizes.Small + 8.dp)
                            .fillMaxWidth()
                    ) {
                        contentRenders.fastForEach { it.Render() }
                    }
                }
            )
        }
    }
}

@Deprecated(
    message = "Use Boolean canDelete to prevent allocating lambdas during recomposition",
    replaceWith = ReplaceWith("SubPostItem(item, threadAuthorId, canDelete(item.subPost.get()), onUserClick, onAgree, onReplyClick, onMenuCopyClick, onMenuDeleteClick)")
)
@Composable
fun SubPostItem(
    item: SubPostItemData,
    threadAuthorId: Long? = null,
    canDelete: (SubPostList) -> Boolean,
    onUserClick: (User) -> Unit = {},
    onAgree: (SubPostList) -> Unit = {},
    onReplyClick: (SubPostList) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostList) -> Unit)? = null,
) {
    SubPostItem(
        item = item,
        threadAuthorId = threadAuthorId,
        canDelete = remember(item, canDelete) { canDelete(item.subPost.get()) },
        onUserClick = onUserClick,
        onAgree = onAgree,
        onReplyClick = onReplyClick,
        onMenuCopyClick = onMenuCopyClick,
        onMenuDeleteClick = onMenuDeleteClick,
    )
}
