package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.rounded.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.thread.PostItemData
import com.huanchengfly.tieba.post.utils.StringUtil

data class ThreadGalleryImage(
    val id: String,
    val picUrl: String,
    val originUrl: String,
    val width: Int,
    val height: Int,
    val floor: Long,
    val postId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val photoViewData: PhotoViewData?,
)

fun extractGalleryImages(
    firstPost: Post?,
    firstPostContentRenders: List<PbContentRender>,
    posts: List<PostItemData>,
): List<ThreadGalleryImage> {
    val list = mutableListOf<ThreadGalleryImage>()

    if (firstPost != null) {
        val authorName = firstPost.author?.nameShow?.ifBlank { firstPost.author?.name }
            ?: firstPost.author?.name
            ?: ""
        val authorAvatar = firstPost.author?.portrait
        firstPostContentRenders.filterIsInstance<PicContentRender>().forEachIndexed { index, render ->
            list.add(
                ThreadGalleryImage(
                    id = "p_${firstPost.id}_$index",
                    picUrl = render.picUrl,
                    originUrl = render.originUrl,
                    width = render.width,
                    height = render.height,
                    floor = 1L,
                    postId = firstPost.id.toLong(),
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    photoViewData = render.photoViewData,
                )
            )
        }
    }

    for (item in posts) {
        if (item.blocked) continue
        val post = item.post.get { this }
        val authorName = post.author?.nameShow?.ifBlank { post.author?.name }
            ?: post.author?.name
            ?: ""
        val authorAvatar = post.author?.portrait
        item.contentRenders.filterIsInstance<PicContentRender>().forEachIndexed { index, render ->
            list.add(
                ThreadGalleryImage(
                    id = "p_${post.id}_$index",
                    picUrl = render.picUrl,
                    originUrl = render.originUrl,
                    width = render.width,
                    height = render.height,
                    floor = post.floor.toLong(),
                    postId = post.id.toLong(),
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    photoViewData = render.photoViewData,
                )
            )
        }
    }

    return list
}

@Composable
fun ThreadGalleryView(
    images: List<ThreadGalleryImage>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onJumpToFloor: (floor: Long, postId: Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val gridState = rememberLazyStaggeredGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = images.size
            if (totalItems == 0) false
            else {
                val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 4
            }
        }
    }

    LaunchedEffect(shouldLoadMore, hasMore, isLoadingMore) {
        if (shouldLoadMore && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    if (images.isEmpty() && !isLoadingMore) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrowseGallery,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = stringResource(id = R.string.tip_empty_gallery),
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            items(images, key = { it.id }) { item ->
                GalleryPhotoCard(item = item, onJumpToFloor = onJumpToFloor)
            }

            if (hasMore || isLoadingMore) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryPhotoCard(
    item: ThreadGalleryImage,
    onJumpToFloor: (floor: Long, postId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = remember(item.width, item.height) {
        if (item.width > 0 && item.height > 0) {
            (item.width.toFloat() / item.height.toFloat()).coerceIn(0.6f, 1.8f)
        } else 1f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        backgroundColor = ExtendedTheme.colors.card,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
            ) {
                NetworkImage(
                    imageUri = item.picUrl,
                    contentDescription = null,
                    photoViewData = item.photoViewData,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#${item.floor} 楼",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Avatar(
                        data = StringUtil.getAvatarUrl(item.authorAvatar),
                        size = 16.dp,
                        contentDescription = item.authorName
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.authorName,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = ExtendedTheme.colors.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                        .clickable { onJumpToFloor(item.floor, item.postId) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colors.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(id = R.string.btn_jump_to_post),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                }
            }
        }
    }
}
