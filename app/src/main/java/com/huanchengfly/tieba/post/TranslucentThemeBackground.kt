package com.huanchengfly.tieba.post

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.ThemeUtil

@Composable
fun TranslucentThemeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = ExtendedTheme.colors.background,
        modifier = modifier
    ) {
        if (ThemeUtil.isTranslucentTheme(ExtendedTheme.colors.theme)) {
            val backgroundPath by rememberPreferenceAsMutableState(
                key = stringPreferencesKey("translucent_theme_background_path"),
                defaultValue = ""
            )
            val backgroundUri by remember { derivedStateOf { newFileUri(backgroundPath) } }
            AsyncImage(
                imageUri = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}
