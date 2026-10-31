package com.huanchengfly.tieba.post.ui.widgets.compose

import android.annotation.SuppressLint
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.LocalDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApi")
@Composable
fun MyBackHandler(
    enabled: Boolean,
    currentScreen: DestinationSpec<*>? = null,
    onBack: () -> Unit,
) {
    val currentDestination = LocalDestination.current

    val shouldEnable =
        enabled && (currentScreen == null || currentDestination?.baseRoute == currentScreen.baseRoute)

    BackHandler(enabled = shouldEnable, onBack = onBack)
}

@SuppressLint("RestrictedApi")
@Composable
fun MyPredictiveBackHandler(
    enabled: Boolean = true,
    currentScreen: DestinationSpec<*>? = null,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
) {
    val currentDestination = LocalDestination.current

    val shouldEnable =
        enabled && (currentScreen == null || currentDestination?.baseRoute == currentScreen.baseRoute)

    PredictiveBackHandler(enabled = shouldEnable, onBack = onBack)
}

@Composable
fun PredictiveBackBox(
    enabled: Boolean = true,
    currentScreen: DestinationSpec<*>? = null,
    onBackConfirm: (reset: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val cornerRadius = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    val resetToIdle: () -> Unit = {
        coroutineScope.launch {
            scale.snapTo(1f)
            cornerRadius.snapTo(0f)
            offsetX.snapTo(0f)
            alpha.snapTo(1f)
        }
    }

    MyPredictiveBackHandler(
        enabled = enabled,
        currentScreen = currentScreen
    ) { progress ->
        try {
            progress.collect { backEvent ->
                val interpolated = FastOutSlowInEasing.transform(backEvent.progress)
                val targetScale = 1.0f - (interpolated * 0.08f)
                val targetRadius = interpolated * 24f
                val isLeftEdge = backEvent.swipeEdge == BackEventCompat.EDGE_LEFT
                val direction = if (isLeftEdge) 1f else -1f
                val targetOffset = direction * (interpolated * 36f)

                scale.snapTo(targetScale)
                cornerRadius.snapTo(targetRadius)
                offsetX.snapTo(targetOffset)
            }

            coroutineScope {
                launch {
                    scale.animateTo(
                        targetValue = 0.88f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
                launch {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(120)
                    )
                }
            }
            onBackConfirm(resetToIdle)
        } catch (e: CancellationException) {
            coroutineScope {
                launch {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    cornerRadius.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                }
                launch {
                    offsetX.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    alpha.animateTo(1f)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offsetX.value.dp.toPx()
                this.alpha = alpha.value
                val radiusPx = cornerRadius.value.dp.toPx()
                if (radiusPx > 0.5f) {
                    shape = RoundedCornerShape(radiusPx)
                    clip = true
                    shadowElevation = (cornerRadius.value * 0.6f).dp.toPx()
                }
            }
    ) {
        content()
    }
}
