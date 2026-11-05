package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.arch.ImmutableHolder

@Composable
fun ThreadTopBar(
    forum: ImmutableHolder<SimpleForum>?,
    onBack: () -> Unit,
    onForumClick: () -> Unit,
    isGalleryMode: Boolean = false,
    onToggleGalleryMode: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TitleCentredToolbar(
        title = {
            forum?.let { nonNullForum ->
                if (nonNullForum.get { name }.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(100))
                            .background(ExtendedTheme.colors.chip)
                            .clickable(onClick = onForumClick)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(
                            data = nonNullForum.get { avatar },
                            contentDescription = nonNullForum.get { name },
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        )

                        Text(
                            text = stringResource(id = R.string.title_forum, nonNullForum.get { name }),
                            fontSize = 14.sp,
                            color = ExtendedTheme.colors.text,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            BackNavigationIcon(onBack)
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(id = R.string.title_search_and_jump),
                    tint = ExtendedTheme.colors.onTopBar
                )
            }
            IconButton(onClick = onToggleGalleryMode) {
                Icon(
                    imageVector = if (isGalleryMode) Icons.Outlined.ViewAgenda else Icons.Outlined.BrowseGallery,
                    contentDescription = stringResource(
                        id = if (isGalleryMode) R.string.title_thread_mode else R.string.title_gallery_mode
                    ),
                    tint = ExtendedTheme.colors.onTopBar
                )
            }
        },
        modifier = modifier
    )
}
