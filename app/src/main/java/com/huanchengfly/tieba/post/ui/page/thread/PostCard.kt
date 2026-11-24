package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.FoldedTip
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val RoundedCornerShape8 = RoundedCornerShape(8.dp)

@Composable
fun PostCard(
    postHolder: ImmutableHolder<Post>,
    contentRenders: ImmutableList<PbContentRender>,
    subPosts: ImmutableList<SubPostItemData> = persistentListOf(),
    threadAuthorId: Long = 0L,
    blocked: Boolean = false,
    blockReason: String? = null,
    isSearchHighlighted: Boolean = false,
    canDelete: Boolean = false,
    immersiveMode: Boolean = false,
    isCollected: Boolean = false,
    showSubPosts: Boolean = true,
    onUserClick: (User) -> Unit = {},
    onAgree: () -> Unit = {},
    onReplyClick: (Post) -> Unit = {},
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuFavoriteClick: ((Post) -> Unit)? = null,
    onMenuBookmarkClick: ((Post) -> Unit)? = null,
    onMenuDeleteClick: ((Post) -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentOnUserClick by rememberUpdatedState(onUserClick)
    val currentOnAgree by rememberUpdatedState(onAgree)
    val currentOnReplyClick by rememberUpdatedState(onReplyClick)
    val currentOnSubPostReplyClick by rememberUpdatedState(onSubPostReplyClick)
    val currentOnOpenSubPosts by rememberUpdatedState(onOpenSubPosts)
    val currentOnMenuCopyClick by rememberUpdatedState(onMenuCopyClick)
    val currentOnMenuFavoriteClick by rememberUpdatedState(onMenuFavoriteClick)
    val currentOnMenuBookmarkClick by rememberUpdatedState(onMenuBookmarkClick)
    val currentOnMenuDeleteClick by rememberUpdatedState(onMenuDeleteClick)

    val post = remember(postHolder) { postHolder.get() }
    val isFold = remember(postHolder) {
        post.is_fold == 1 || post.fold_comment_status != 0
    }
    var isFoldExpanded by rememberSaveable(post.id) {
        mutableStateOf(false)
    }
    var isManuallyRevealed by rememberSaveable(post.id) {
        mutableStateOf(false)
    }
    val hasPadding = remember(key1 = postHolder, key2 = immersiveMode) {
        postHolder.get { floor > 1 } && !immersiveMode
    }
    val paddingModifier = remember(hasPadding) {
        Modifier.padding(start = if (hasPadding) Sizes.Small + 8.dp else 0.dp)
    }
    val author = remember(postHolder) { postHolder.get { author ?: User() } }
    val showTitle = remember(postHolder) {
        post.title.isNotBlank() && post.floor <= 1 && post.is_ntitle != 1
    }
    val hasAgreed = remember(postHolder) {
        post.agree?.hasAgree == 1
    }
    val agreeNum = remember(postHolder) {
        post.agree?.diffAgreeNum ?: 0L
    }
    val menuState = rememberMenuState()
    BlockableContent(
        blocked = blocked && !isManuallyRevealed,
        blockedTip = {
            BlockTip(
                onClick = { isManuallyRevealed = true }
            ) {
                val tip = if (!blockReason.isNullOrBlank()) {
                    stringResource(id = R.string.tip_blocked_reason_format, postHolder.get { floor }, blockReason)
                } else {
                    stringResource(id = R.string.tip_blocked_post, postHolder.get { floor })
                }
                Text(
                    text = "$tip · ${stringResource(id = R.string.tip_click_to_unhide)}"
                )
            }
        },
        hideBlockedContent = context.appPreferences.hideBlockedContent || immersiveMode,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        if (isFold && !isFoldExpanded) {
            val foldTipText = remember(postHolder) {
                if (post.fold_tip.isNotBlank()) {
                    context.getString(R.string.tip_folded_post, post.floor, post.fold_tip)
                } else {
                    context.getString(R.string.tip_folded_post_default, post.floor)
                }
            }
            FoldedTip(
                text = foldTipText,
                onClick = { isFoldExpanded = true }
            )
        } else {
            val onHeaderUserClick = remember(author) {
                { currentOnUserClick(author) }
            }
            val accentColor = ExtendedTheme.colors.accent
            val cardModifier = remember(isSearchHighlighted, accentColor) {
                if (isSearchHighlighted) {
                    Modifier
                        .border(2.dp, accentColor, RoundedCornerShape8)
                        .padding(2.dp)
                } else Modifier
            }
            LongClickMenu(
                menuState = menuState,
                indication = null,
                onClick = {
                    currentOnReplyClick(post)
                }.takeIf { !context.appPreferences.hideReply },
                menuContent = {
                    PostCardMenuContent(
                        post = post,
                        isCollected = isCollected,
                        canDelete = canDelete,
                        isFold = isFold,
                        onReplyClick = { currentOnReplyClick(post) },
                        onCopyClick = currentOnMenuCopyClick,
                        onFavoriteClick = currentOnMenuFavoriteClick,
                        onBookmarkClick = currentOnMenuBookmarkClick,
                        onDeleteClick = currentOnMenuDeleteClick,
                        onToggleFold = { isFoldExpanded = false },
                        onDismissMenu = { menuState.expanded = false }
                    )
                }
            ) {
                Card(
                    modifier = cardModifier,
                    header = {
                        if (!immersiveMode) {
                            PostCardHeader(
                                author = author,
                                floor = post.floor,
                                time = post.time.toLong(),
                                threadAuthorId = threadAuthorId,
                                hasAgreed = hasAgreed,
                                agreeNum = agreeNum,
                                onUserClick = onHeaderUserClick,
                                onAgree = currentOnAgree
                            )
                        }
                    },
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = paddingModifier
                                .fillMaxWidth()
                        ) {
                            if (showTitle) {
                                Text(
                                    text = post.title,
                                    style = MaterialTheme.typography.subtitle1,
                                    fontSize = 15.sp
                                )
                            }

                            if (isCollected) {
                                Chip(
                                    text = stringResource(id = R.string.title_collected_floor),
                                    invertColor = true,
                                    prefixIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }

                            if (isSearchHighlighted) {
                                Chip(
                                    text = stringResource(id = R.string.tip_search_highlight_badge),
                                    invertColor = true,
                                    prefixIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }

                            if (isFold) {
                                Chip(
                                    text = if (post.fold_tip.isNotBlank()) post.fold_tip else stringResource(id = R.string.chip_folded_post),
                                    invertColor = false
                                )
                            }

                            contentRenders.fastForEach { it.Render() }
                        }

                        if (showSubPosts && post.sub_post_number > 0 && subPosts.isNotEmpty() && !immersiveMode) {
                            PostSubPostsPreview(
                                post = post,
                                subPosts = subPosts,
                                paddingModifier = paddingModifier,
                                onSubPostReplyClick = currentOnSubPostReplyClick,
                                onOpenSubPosts = currentOnOpenSubPosts,
                                onMenuCopyClick = currentOnMenuCopyClick
                            )
                        }
                    }
                )
            }
        }
    }
}

@Deprecated(
    message = "Use Boolean parameter for Compose recomposition optimization",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun PostCard(
    postHolder: ImmutableHolder<Post>,
    contentRenders: ImmutableList<PbContentRender>,
    subPosts: ImmutableList<SubPostItemData> = persistentListOf(),
    threadAuthorId: Long = 0L,
    blocked: Boolean = false,
    blockReason: String? = null,
    isSearchHighlighted: Boolean = false,
    canDelete: (Post) -> Boolean,
    immersiveMode: Boolean = false,
    isCollected: (Post) -> Boolean = { false },
    showSubPosts: Boolean = true,
    onUserClick: (User) -> Unit = {},
    onAgree: () -> Unit = {},
    onReplyClick: (Post) -> Unit = {},
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuFavoriteClick: ((Post) -> Unit)? = null,
    onMenuBookmarkClick: ((Post) -> Unit)? = null,
    onMenuDeleteClick: ((Post) -> Unit)? = null,
) {
    val post = remember(postHolder) { postHolder.get() }
    PostCard(
        postHolder = postHolder,
        contentRenders = contentRenders,
        subPosts = subPosts,
        threadAuthorId = threadAuthorId,
        blocked = blocked,
        blockReason = blockReason,
        isSearchHighlighted = isSearchHighlighted,
        canDelete = canDelete(post),
        immersiveMode = immersiveMode,
        isCollected = isCollected(post),
        showSubPosts = showSubPosts,
        onUserClick = onUserClick,
        onAgree = onAgree,
        onReplyClick = onReplyClick,
        onSubPostReplyClick = onSubPostReplyClick,
        onOpenSubPosts = onOpenSubPosts,
        onMenuCopyClick = onMenuCopyClick,
        onMenuFavoriteClick = onMenuFavoriteClick,
        onMenuBookmarkClick = onMenuBookmarkClick,
        onMenuDeleteClick = onMenuDeleteClick,
    )
}
