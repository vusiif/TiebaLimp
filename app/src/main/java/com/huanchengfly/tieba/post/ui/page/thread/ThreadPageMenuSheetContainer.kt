package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/**
 * Bottom sheet container hosting the ThreadPage menu sheet and main thread reading content.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThreadPageMenuSheetContainer(
    sheetState: ModalBottomSheetState,
    isSeeLz: Boolean,
    isCollected: Boolean,
    isImmersiveMode: Boolean,
    isDesc: Boolean,
    isGalleryMode: Boolean,
    canDelete: Boolean,
    onSeeLzClick: () -> Unit,
    onCollectClick: () -> Unit,
    onImmersiveModeClick: () -> Unit,
    onDescClick: () -> Unit,
    onGalleryModeClick: () -> Unit,
    onJumpPageClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onSaveSnapshotClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        sheetBackgroundColor = ExtendedTheme.colors.windowBackground,
        sheetContent = {
            ThreadPageMenuSheet(
                isSeeLz = isSeeLz,
                isCollected = isCollected,
                isImmersiveMode = isImmersiveMode,
                isDesc = isDesc,
                isGalleryMode = isGalleryMode,
                canDelete = canDelete,
                onSeeLzClick = onSeeLzClick,
                onCollectClick = onCollectClick,
                onImmersiveModeClick = onImmersiveModeClick,
                onDescClick = onDescClick,
                onGalleryModeClick = onGalleryModeClick,
                onJumpPageClick = onJumpPageClick,
                onBookmarksClick = onBookmarksClick,
                onSaveSnapshotClick = onSaveSnapshotClick,
                onShareClick = onShareClick,
                onCopyLinkClick = onCopyLinkClick,
                onReportClick = onReportClick,
                onDeleteClick = onDeleteClick
            )
        },
        scrimColor = Color.Transparent,
        modifier = modifier,
        content = content
    )
}

/**
 * Convenience overload that automatically dispatches standard ViewModel intents and utility actions.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThreadPageMenuSheetContainer(
    sheetState: ModalBottomSheetState,
    viewModel: ThreadViewModel,
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long?,
    forum: ImmutableHolder<SimpleForum>?,
    threadTitle: String,
    isSeeLz: Boolean,
    isCollected: Boolean,
    isImmersiveMode: Boolean,
    curSortType: Int,
    isGalleryMode: Boolean,
    onToggleGalleryMode: () -> Unit,
    canDelete: Boolean,
    antiTbs: String?,
    lastVisibilityPost: Post?,
    threadFirstPostId: Long,
    firstPost: ImmutableHolder<Post>?,
    author: ImmutableHolder<User>?,
    firstPostContentRenders: ImmutableList<PbContentRender>,
    data: List<PostItemData>,
    onJumpPageClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val closeBottomSheet: () -> Unit = {
        coroutineScope.launch {
            sheetState.hide()
        }
    }

    ThreadPageMenuSheetContainer(
        sheetState = sheetState,
        isSeeLz = isSeeLz,
        isCollected = isCollected,
        isImmersiveMode = isImmersiveMode,
        isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
        isGalleryMode = isGalleryMode,
        canDelete = canDelete,
        onSeeLzClick = {
            viewModel.send(
                ThreadUiIntent.LoadFirstPage(
                    threadId,
                    forumId,
                    !isSeeLz,
                    curSortType
                )
            )
            closeBottomSheet()
        },
        onCollectClick = {
            if (isCollected) {
                val fid = forum?.get { id } ?: forumId
                if (fid != null) {
                    viewModel.send(
                        ThreadUiIntent.RemoveFavorite(
                            threadId,
                            fid,
                            antiTbs
                        )
                    )
                }
            } else {
                val readItem = lastVisibilityPost
                if (readItem != null) {
                    viewModel.send(
                        ThreadUiIntent.AddFavorite(
                            threadId,
                            readItem.id,
                            readItem.floor
                        )
                    )
                }
            }
            closeBottomSheet()
        },
        onImmersiveModeClick = {
            if (!isImmersiveMode && !isSeeLz) {
                viewModel.send(
                    ThreadUiIntent.LoadFirstPage(
                        threadId,
                        forumId,
                        true,
                        curSortType
                    )
                )
            }
            viewModel.send(ThreadUiIntent.ToggleImmersiveMode(!isImmersiveMode))
            closeBottomSheet()
        },
        onDescClick = {
            viewModel.send(
                ThreadUiIntent.LoadFirstPage(
                    threadId,
                    forumId,
                    isSeeLz,
                    if (curSortType != ThreadSortType.SORT_TYPE_DESC) ThreadSortType.SORT_TYPE_DESC else ThreadSortType.SORT_TYPE_DEFAULT
                )
            )
            closeBottomSheet()
        },
        onGalleryModeClick = {
            onToggleGalleryMode()
            closeBottomSheet()
        },
        onJumpPageClick = {
            closeBottomSheet()
            onJumpPageClick()
        },
        onBookmarksClick = {
            closeBottomSheet()
            onBookmarksClick()
        },
        onSaveSnapshotClick = {
            closeBottomSheet()
            coroutineScope.launch {
                saveThreadSnapshot(
                    context = context,
                    threadId = threadId,
                    threadTitle = threadTitle,
                    firstPost = firstPost,
                    author = author,
                    forum = forum,
                    firstPostContentRenders = firstPostContentRenders,
                    data = data,
                )
            }
        },
        onShareClick = {
            TiebaUtil.shareText(
                context,
                "https://tieba.baidu.com/p/$threadId",
                threadTitle
            )
        },
        onCopyLinkClick = {
            TiebaUtil.copyText(
                context,
                "https://tieba.baidu.com/p/$threadId?see_lz=${isSeeLz.booleanToString()}"
            )
        },
        onReportClick = {
            val targetFirstPostId = threadFirstPostId.takeIf { it != 0L }
                ?: firstPost?.get { id }
                ?: 0L
            coroutineScope.launch {
                TiebaUtil.reportPost(
                    context,
                    navigator,
                    targetFirstPostId.toString()
                )
            }
        },
        onDeleteClick = {
            onDeleteClick()
        },
        modifier = modifier,
        content = content
    )
}
