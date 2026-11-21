package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.fetch.newResourceUri
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
fun CustomThemeEntranceRow(
    customPrimaryColor: Color,
    isDynamicTheme: Boolean,
    currentTheme: String,
    onCustomColorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    ProvideContentColor(color = ExtendedTheme.colors.windowBackground) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color = customPrimaryColor)
                    .clickable(onClick = onCustomColorClick)
                    .padding(all = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = if (!isDynamicTheme && currentTheme == ThemeUtil.THEME_CUSTOM) {
                            Icons.Rounded.Check
                        } else {
                            Icons.Rounded.ColorLens
                        },
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(id = R.string.title_custom_color),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        context.goToActivity<TranslucentThemeActivity>()
                    },
                contentAlignment = Alignment.Center
            ) {
                val previewImageUri =
                    if (context.appPreferences.translucentThemeBackgroundPath != null) {
                        newFileUri(context.appPreferences.translucentThemeBackgroundPath!!)
                    } else {
                        newResourceUri(R.drawable.user_header)
                    }
                AsyncImage(
                    imageUri = previewImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = if (ThemeUtil.isTranslucentTheme(currentTheme)) {
                                Icons.Rounded.Check
                            } else {
                                Icons.Rounded.PhotoSizeSelectActual
                            },
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = R.string.title_theme_translucent),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
