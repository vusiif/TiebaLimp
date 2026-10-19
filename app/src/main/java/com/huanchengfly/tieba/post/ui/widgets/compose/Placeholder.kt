package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

sealed interface PlaceholderHighlight {
    val animationSpec: InfiniteRepeatableSpec<Float>

    fun brush(progress: Float, size: Size): Brush

    fun alpha(progress: Float): Float = 1.0f

    companion object {
        fun fade(
            animationSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            highlightColor: Color? = null,
        ): PlaceholderHighlight = FadeHighlight(animationSpec, highlightColor)

        fun shimmer(
            animationSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            highlightColor: Color? = null,
        ): PlaceholderHighlight = ShimmerHighlight(animationSpec, highlightColor)
    }
}

private class FadeHighlight(
    override val animationSpec: InfiniteRepeatableSpec<Float>,
    private val highlightColor: Color?,
) : PlaceholderHighlight {
    override fun brush(progress: Float, size: Size): Brush {
        val color = (highlightColor ?: Color.White).copy(alpha = progress * 0.4f)
        return Brush.linearGradient(
            colors = listOf(color, color),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    }

    override fun alpha(progress: Float): Float = 0.5f + (progress * 0.5f)
}

private class ShimmerHighlight(
    override val animationSpec: InfiniteRepeatableSpec<Float>,
    private val highlightColor: Color?,
) : PlaceholderHighlight {
    override fun brush(progress: Float, size: Size): Brush {
        val width = size.width
        val dx = progress * (width * 2) - width
        val shimmerColor = highlightColor ?: Color.White
        return Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                shimmerColor.copy(alpha = 0.35f),
                Color.Transparent
            ),
            start = Offset(dx, 0f),
            end = Offset(dx + width * 0.7f, size.height)
        )
    }
}

/**
 * Modern lightweight native Jetpack Compose replacement for deprecated Accompanist placeholder.
 */
fun Modifier.placeholder(
    visible: Boolean,
    color: Color = Color.Unspecified,
    shape: Shape? = null,
    highlight: PlaceholderHighlight? = null,
): Modifier = composed {
    if (!visible) return@composed this

    val resolvedColor = if (color != Color.Unspecified) {
        color
    } else {
        ExtendedTheme.colors.chip.takeIf { it != Color.Unspecified }
            ?: MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    }
    val resolvedShape = shape ?: RoundedCornerShape(4.dp)

    if (highlight != null) {
        val transition = rememberInfiniteTransition(label = "placeholder")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = highlight.animationSpec,
            label = "placeholder_progress"
        )

        this.drawWithContent {
            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            val baseAlpha = highlight.alpha(progress)
            drawOutline(outline = outline, color = resolvedColor, alpha = baseAlpha)
            val highlightBrush = highlight.brush(progress, size)
            drawOutline(outline = outline, brush = highlightBrush)
        }
    } else {
        this.drawWithContent {
            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            drawOutline(outline = outline, color = resolvedColor)
        }
    }
}