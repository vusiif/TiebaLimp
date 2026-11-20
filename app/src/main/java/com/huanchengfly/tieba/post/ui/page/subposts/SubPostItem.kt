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
    canDelete: (SubPostList) -> Boolean = { false },
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
                            onReplyClick(subPost.get())
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
                if (canDelete(subPost.get()) && onMenuDeleteClick != null) {
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
            onClick = { onReplyClick(subPost.get()) }.takeUnless { context.appPreferences.hideReply }
        ) {
            Card(
                header = {
                    if (author != null) {
                        UserHeader(
                            avatar = {
                                Avatar(
                                    data = StringUtil.getAvatarUrl(author.get { portrait }),
                                    size = Sizes.Small,
                                    contentDescription = null
                                )
                            },
                            name = {
                                UserNameText(
                                    userName = StringUtil.getUsernameAnnotatedString(
                                        LocalContext.current,
                                        author.get { name },
                                        author.get { nameShow }
                                    ),
                                    userLevel = author.get { level_id },
                                    isLz = author.get { id } == threadAuthorId,
                                    bawuType = author.get { bawuType },
                                )
                            },
                            desc = {
                                Text(
                                    text = getDescText(
                                        subPost.get { time }.toLong(),
                                        author.get { ip_address })
                                )
                            },
                            onClick = {
                                onUserClick(author.get())
                            }
                        ) {
                            PostAgreeBtn(
                                hasAgreed = hasAgreed,
                                agreeNum = agreeNum,
                                onClick = { onAgree(subPost.get()) }
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
