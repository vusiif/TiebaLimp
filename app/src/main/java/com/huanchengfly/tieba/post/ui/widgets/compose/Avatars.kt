package com.huanchengfly.tieba.post.ui.widgets.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.resize.Precision
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.utils.ImageUtil

object Sizes {
    val Tiny = 24.dp
    val Small = 36.dp
    val Medium = 48.dp
    val Large = 56.dp
}

@Composable
fun AvatarIcon(
    icon: ImageVector,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarIcon(
    @DrawableRes
    resId: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = ImageVector.vectorResource(id = resId),
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarPlaceholder(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Avatar(
        data = ImageUtil.getPlaceHolder(LocalContext.current, 0),
        size = size,
        contentDescription = null,
        modifier = modifier.placeholder(
            visible = true,
            highlight = PlaceholderHighlight.fade(),
            shape = CircleShape
        )
    )
}

@Composable
fun Avatar(
    data: String?,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Avatar(
        data = data,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        shape = shape,
        targetSize = size,
    )
}

@Composable
fun Avatar(
    data: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    targetSize: Dp? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val targetPx = remember(targetSize, density) {
        if (targetSize != null && targetSize > 0.dp) {
            with(density) { (targetSize * 1.5f).roundToPx().coerceIn(48, 384) }
        } else {
            with(density) { 64.dp.roundToPx() }
        }
    }

    val request = remember(data, context, targetPx) {
        DisplayRequest(context, data) {
            placeholder(ImageUtil.getPlaceHolder(context, 0))
            crossfade()
            resize(targetPx, targetPx, Precision.LESS_PIXELS)
        }
    }

    AsyncImage(
        request = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
    )
}

@Composable
fun Avatar(
    data: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val request = remember(data, context) {
        DisplayRequest(context, newResourceUri(data)) {
            placeholder(ImageUtil.getPlaceHolder(context, 0))
            crossfade()
        }
    }

    AsyncImage(
        request = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

@Composable
fun Avatar(
    data: Drawable,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberDrawablePainter(drawable = data),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}