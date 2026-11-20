package com.huanchengfly.tieba.post.ui.page.reply

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun ReplyActionToolbar(
    onEmojiClick: () -> Unit,
    canChooseImage: Boolean,
    selectedImageCount: Int,
    onImageClick: () -> Unit,
    isReplying: Boolean,
    canSend: Boolean,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        IconButton(
            onClick = onEmojiClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEmotions,
                contentDescription = stringResource(id = R.string.insert_emotions),
                modifier = Modifier.size(24.dp)
            )
        }
        if (canChooseImage) {
            IconButton(
                onClick = onImageClick,
                modifier = Modifier.size(24.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (selectedImageCount > 0) {
                            Badge(
                                backgroundColor = ExtendedTheme.colors.accent,
                                contentColor = ExtendedTheme.colors.background,
                            ) {
                                Text(
                                    text = "$selectedImageCount",
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.InsertPhoto,
                        contentDescription = stringResource(id = R.string.insert_photo),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isReplying) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = ExtendedTheme.colors.primary
            )
        } else {
            IconButton(
                onClick = onSendClick,
                enabled = canSend,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(id = R.string.send_reply),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
