package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.FloorBookmark
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun FloorBookmarkEditDialog(
    dialogState: DialogState,
    floor: Int,
    authorName: String,
    contentSnippet: String,
    initialNote: String = "",
    isExisting: Boolean = false,
    onSave: (note: String) -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var note by remember(dialogState.show, initialNote) { mutableStateOf(initialNote) }

    Dialog(
        dialogState = dialogState,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(if (isExisting) R.string.title_edit_floor_bookmark else R.string.title_add_floor_bookmark),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
        },
        buttons = {
            if (isExisting) {
                DialogNegativeButton(
                    text = stringResource(R.string.btn_delete_bookmark),
                    onClick = onDelete
                )
            }
            DialogPositiveButton(
                text = stringResource(R.string.btn_save_bookmark),
                onClick = { onSave(note) }
            )
            DialogNegativeButton(text = stringResource(R.string.button_cancel))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.tip_post_floor, floor) + if (authorName.isNotBlank()) " · $authorName" else "",
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (contentSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ExtendedTheme.colors.floorCard)
                        .padding(10.dp)
                ) {
                    Text(
                        text = contentSnippet,
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.hint_bookmark_note),
                        fontSize = 13.sp,
                        color = ExtendedTheme.colors.textDisabled
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = ExtendedTheme.colors.text,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = ExtendedTheme.colors.divider
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FloorBookmarksDialog(
    dialogState: DialogState,
    bookmarks: List<FloorBookmark>,
    onSelectBookmark: (FloorBookmark) -> Unit,
    onDeleteBookmark: (FloorBookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        dialogState = dialogState,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.title_thread_bookmarks) + if (bookmarks.isNotEmpty()) " (${bookmarks.size})" else "",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
        },
        buttons = {
            DialogNegativeButton(text = stringResource(R.string.btn_close))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (bookmarks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = ExtendedTheme.colors.textDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.tip_empty_bookmarks),
                        style = MaterialTheme.typography.body2,
                        color = ExtendedTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = bookmarks,
                        key = { it.id.takeIf { id -> id != 0L } ?: "${it.threadId}_${it.floor}" }
                    ) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ExtendedTheme.colors.floorCard)
                                .clickable {
                                    dialogState.show = false
                                    onSelectBookmark(item)
                                }
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colors.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.tip_post_floor, item.floor),
                                            style = MaterialTheme.typography.caption.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colors.primary
                                        )
                                    }

                                    if (item.authorName.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.authorName,
                                            style = MaterialTheme.typography.caption,
                                            color = ExtendedTheme.colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    IconButton(
                                        onClick = { onDeleteBookmark(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = stringResource(R.string.btn_delete_bookmark),
                                            tint = ExtendedTheme.colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (item.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.note,
                                        style = MaterialTheme.typography.body2.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        ),
                                        color = ExtendedTheme.colors.text
                                    )
                                }

                                if (item.contentSnippet.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.contentSnippet,
                                        style = MaterialTheme.typography.caption,
                                        color = ExtendedTheme.colors.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
