package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.loadMoreIndicator

@Composable
internal fun ThreadLoadMoreIndicator(
    isLoading: Boolean,
    loadMoreEnd: Boolean,
    willLoad: Boolean,
    hasMore: Boolean,
) {
    Surface(
        elevation = 8.dp,
        shape = RoundedCornerShape(100),
        color = ExtendedTheme.colors.loadMoreIndicator,
        contentColor = ExtendedTheme.colors.text
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(10.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2.copy(fontSize = 13.sp)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = ExtendedTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.text_loading),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    loadMoreEnd -> {
                        Text(
                            text = stringResource(id = R.string.no_more),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    hasMore -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load) else stringResource(
                                id = R.string.pull_to_load
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load_latest_posts) else stringResource(
                                id = R.string.pull_to_load_latest_posts
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
