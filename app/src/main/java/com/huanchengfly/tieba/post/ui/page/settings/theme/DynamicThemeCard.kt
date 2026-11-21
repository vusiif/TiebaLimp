package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.dynamicTonalPalette
import com.huanchengfly.tieba.post.utils.ThemeUtil

@Composable
fun DynamicThemeCard(
    isDynamicTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { ThemeUtil.setUseDynamicTheme(true) },
) {
    val context = LocalContext.current
    val tonalPalette = remember { dynamicTonalPalette(context) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        tonalPalette.primary50,
                        tonalPalette.secondary50,
                        tonalPalette.tertiary50,
                        tonalPalette.primary50,
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(all = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = if (isDynamicTheme) {
                    Icons.Rounded.Check
                } else {
                    Icons.Rounded.Colorize
                },
                contentDescription = null,
                tint = ExtendedTheme.colors.windowBackground
            )
            Text(
                text = stringResource(id = R.string.title_dynamic_theme),
                fontWeight = FontWeight.Bold,
                color = ExtendedTheme.colors.windowBackground
            )
        }
    }
}
