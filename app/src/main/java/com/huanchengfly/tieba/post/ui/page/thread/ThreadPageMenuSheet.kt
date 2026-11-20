package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThreadPageMenuSheet(
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
) {
    ThreadMenu(
        isSeeLz = isSeeLz,
        isCollected = isCollected,
        isImmersiveMode = isImmersiveMode,
        isDesc = isDesc,
        isGalleryMode = isGalleryMode,
        canDelete = { canDelete },
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
        onDeleteClick = onDeleteClick,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 16.dp)
            .defaultMinSize(minHeight = 1.dp)
    )
}
