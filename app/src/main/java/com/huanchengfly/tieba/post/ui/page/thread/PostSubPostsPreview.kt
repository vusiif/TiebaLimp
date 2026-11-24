package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.buildChipInlineContent
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.BlockManager.FilterResult
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun PostSubPostsPreview(
    post: Post,
    subPosts: ImmutableList<SubPostItemData>,
    paddingModifier: Modifier = Modifier,
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val currentOnSubPostReplyClick by rememberUpdatedState(onSubPostReplyClick)
    val currentOnOpenSubPosts by rememberUpdatedState(onOpenSubPosts)
    val currentOnMenuCopyClick by rememberUpdatedState(onMenuCopyClick)
    val onOpenAllClick = remember { { currentOnOpenSubPosts(0) } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(paddingModifier)
            .clip(RoundedCornerShape(6.dp))
            .background(ExtendedTheme.colors.floorCard)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        subPosts.fastForEach { item ->
            PreviewSubPostRow(
                item = item,
                post = post,
                onSubPostReplyClick = currentOnSubPostReplyClick,
                onOpenSubPosts = currentOnOpenSubPosts,
                onMenuCopyClick = currentOnMenuCopyClick,
            )
        }

        if (post.sub_post_number > subPosts.size) {
            Text(
                text = stringResource(
                    id = R.string.open_all_sub_posts,
                    post.sub_post_number
                ),
                style = MaterialTheme.typography.caption,
                fontSize = 13.sp,
                color = ExtendedTheme.colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clickable(onClick = onOpenAllClick)
                    .padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun PreviewSubPostRow(
    item: SubPostItemData,
    post: Post,
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)?,
    onOpenSubPosts: (subPostId: Long) -> Unit,
    onMenuCopyClick: ((String) -> Unit)?,
) {
    var subPostRevealed by rememberSaveable(item.id) {
        mutableStateOf(false)
    }
    val currentSubReplyClick by rememberUpdatedState(onSubPostReplyClick)
    val currentSubOpenSubPosts by rememberUpdatedState(onOpenSubPosts)
    val currentSubCopyClick by rememberUpdatedState(onMenuCopyClick)

    val subReason = (item.blockResult as? FilterResult.Blocked)?.reason
    BlockableContent(
        blocked = item.blocked && !subPostRevealed,
        blockedTip = {
            val subTip = if (!subReason.isNullOrBlank()) {
                stringResource(id = R.string.tip_blocked_sub_reason_format, subReason)
            } else {
                stringResource(id = R.string.tip_blocked_sub_post)
            }
            Text(
                text = "$subTip · ${stringResource(id = R.string.tip_click_to_unhide)}",
                style = MaterialTheme.typography.body2.copy(
                    color = ExtendedTheme.colors.textDisabled,
                    fontSize = 13.sp
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable { subPostRevealed = true }
            )
        },
    ) {
        val onReplyClick: ((SubPostList) -> Unit)? = remember(item.id, post.id) {
            val replyClick = currentSubReplyClick ?: return@remember null
            { it: SubPostList -> replyClick(post, it) }
        }
        val onCopyClick: ((SubPostList) -> Unit)? = remember(item.id) {
            val copyClick = currentSubCopyClick ?: return@remember null
            { it: SubPostList -> copyClick(it.content.plainText) }
        }
        SubPostItem(
            subPostList = item.subPost,
            subPostContent = item.subPostContent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            onReplyClick = onReplyClick,
            onOpenSubPosts = currentSubOpenSubPosts,
            onMenuCopyClick = onCopyClick
        )
    }
}

@Composable
private fun SubPostItem(
    subPostList: ImmutableHolder<SubPostList>,
    subPostContent: AnnotatedString,
    modifier: Modifier = Modifier,
    onReplyClick: ((SubPostList) -> Unit)?,
    onOpenSubPosts: (Long) -> Unit,
    onMenuCopyClick: ((SubPostList) -> Unit)?,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = rememberMenuState()
    LongClickMenu(
        menuState = menuState,
        menuContent = {
            if (!context.appPreferences.hideReply) {
                DropdownMenuItem(
                    onClick = {
                        onReplyClick?.invoke(subPostList.get())
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.title_reply))
                }
            }
            if (onMenuCopyClick != null) {
                DropdownMenuItem(
                    onClick = {
                        onMenuCopyClick(subPostList.get())
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.menu_copy))
                }
            }
            DropdownMenuItem(
                onClick = {
                    coroutineScope.launch {
                        TiebaUtil.reportPost(context, navigator, subPostList.get { id }.toString())
                    }
                    menuState.expanded = false
                }
            ) {
                Text(text = stringResource(id = R.string.title_report))
            }
        },
        shape = RoundedCornerShape(0),
        onClick = {
            onOpenSubPosts(subPostList.get { id })
        }
    ) {
        val lzChip = buildChipInlineContent(
            stringResource(id = R.string.tip_lz),
            backgroundColor = ExtendedTheme.colors.textSecondary.copy(alpha = 0.1f),
            color = ExtendedTheme.colors.textSecondary
        )
        val bawuChip = buildChipInlineContent(
            "吧务",
            color = ExtendedTheme.colors.primary,
            backgroundColor = ExtendedTheme.colors.primary.copy(alpha = 0.1f)
        )
        val inlineContent = remember(lzChip, bawuChip) {
            mapOf(
                "Lz" to lzChip,
                "Bawu" to bawuChip,
            )
        }
        ProvideTextStyle(value = MaterialTheme.typography.body2.copy(color = ExtendedTheme.colors.text)) {
            PbContentText(
                text = subPostContent,
                modifier = modifier,
                fontSize = 13.sp,
                emoticonSize = 0.9f,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                lineSpacing = 0.4.sp,
                inlineContent = inlineContent
            )
        }
    }
}
