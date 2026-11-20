package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar

@Composable
fun SubPostsToolbar(
    postFloor: Int?,
    isSheet: Boolean,
    onNavigateUp: () -> Unit,
    onOpenOriginThread: () -> Unit,
) {
    TitleCentredToolbar(
        title = {
            Text(
                text = postFloor?.let {
                    stringResource(
                        id = R.string.title_sub_posts,
                        it
                    )
                } ?: stringResource(id = R.string.title_sub_posts_default),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = if (isSheet) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.btn_close)
                )
            }
        },
        actions = {
            if (!isSheet) {
                IconButton(onClick = onOpenOriginThread) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInBrowser,
                        contentDescription = stringResource(id = R.string.btn_open_origin_thread)
                    )
                }
            }
        }
    )
}
