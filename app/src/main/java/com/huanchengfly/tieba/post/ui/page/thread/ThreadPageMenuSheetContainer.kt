package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

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
