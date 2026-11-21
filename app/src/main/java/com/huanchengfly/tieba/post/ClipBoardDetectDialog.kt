package com.huanchengfly.tieba.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.components.ClipBoardLink
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil

@Composable
fun ClipBoardDetectDialog(
    onOpenLink: (ClipBoardLink) -> Unit,
) {
    val previewInfo by ClipBoardLinkDetector.previewInfoStateFlow.collectAsState()
    val dialogState = rememberDialogState()

    LaunchedEffect(previewInfo) {
        if (previewInfo != null) {
            dialogState.show()
        }
    }

    Dialog(
        dialogState = dialogState,
        title = {
            Text(text = stringResource(id = R.string.title_dialog_clip_board_tieba_url))
        },
        buttons = {
            DialogPositiveButton(text = stringResource(id = R.string.button_open)) {
                previewInfo?.let {
                    onOpenLink(it.clipBoardLink)
                }
            }
            DialogNegativeButton(text = stringResource(id = R.string.btn_close))
        },
    ) {
        previewInfo?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                border = BorderStroke(1.dp, ExtendedTheme.colors.divider),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    it.icon?.let { icon ->
                        if (icon.type == QuickPreviewUtil.Icon.TYPE_DRAWABLE_RES) {
                            AvatarIcon(
                                resId = icon.res,
                                size = Sizes.Medium,
                                contentDescription = null
                            )
                        } else {
                            Avatar(
                                data = icon.url,
                                size = Sizes.Medium,
                                contentDescription = null
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        it.title?.let { title ->
                            Text(text = title, style = MaterialTheme.typography.subtitle1)
                        }
                        it.subtitle?.let { subtitle ->
                            Text(text = subtitle, style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }
        }
    }
}
