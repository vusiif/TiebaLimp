package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun AppThemePage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val themeValues = stringArrayResource(id = R.array.theme_values)
    val themeNames = stringArrayResource(id = R.array.themeNames)
    val currentTheme by remember { ThemeUtil.themeState }
    val isDynamicTheme by rememberPreferenceAsState(
        key = booleanPreferencesKey("useDynamicColorTheme"),
        defaultValue = false
    )
    val customPrimaryColorDialogState = rememberDialogState()
    var customPrimaryColor by remember {
        mutableStateOf(
            Color(
                App.ThemeDelegate.getColorByAttr(
                    context,
                    R.attr.colorPrimary,
                    ThemeUtil.THEME_CUSTOM
                )
            )
        )
    }
    var customToolbarPrimaryColor by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(ThemeUtil.KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR),
        defaultValue = false
    )
    var customStatusBarFontDark by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(ThemeUtil.KEY_CUSTOM_STATUS_BAR_FONT_DARK),
        defaultValue = false
    )

    CustomPrimaryColorDialog(
        dialogState = customPrimaryColorDialogState,
        initialColor = customPrimaryColor,
        initialToolbarPrimaryColor = customToolbarPrimaryColor,
        initialStatusBarFontDark = customStatusBarFontDark,
        onColorSaved = { customPrimaryColor = it },
        onToolbarPrimaryColorChange = { customToolbarPrimaryColor = it },
        onStatusBarFontDarkChange = { customStatusBarFontDark = it }
    )

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_theme),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item {
                        DynamicThemeCard(isDynamicTheme = isDynamicTheme)
                    }
                }
                item {
                    CustomThemeEntranceRow(
                        customPrimaryColor = customPrimaryColor,
                        isDynamicTheme = isDynamicTheme,
                        currentTheme = currentTheme,
                        onCustomColorClick = { customPrimaryColorDialogState.show() }
                    )
                }
                itemsIndexed(
                    items = themeValues.toList(),
                    key = { _, item -> item }
                ) { index, item ->
                    val name = themeNames[index]
                    val backgroundColor = remember {
                        Color(
                            App.ThemeDelegate.getColorByAttr(
                                context,
                                R.attr.colorBackground,
                                item
                            )
                        )
                    }
                    val primaryColor = remember {
                        Color(
                            App.ThemeDelegate.getColorByAttr(
                                context,
                                R.attr.colorNewPrimary,
                                item
                            )
                        )
                    }
                    val accentColor = remember {
                        Color(
                            App.ThemeDelegate.getColorByAttr(
                                context,
                                R.attr.colorAccent,
                                item
                            )
                        )
                    }
                    val onAccentColor = remember {
                        Color(
                            App.ThemeDelegate.getColorByAttr(
                                context,
                                R.attr.colorOnAccent,
                                item
                            )
                        )
                    }
                    val onBackgroundColor = remember {
                        Color(
                            App.ThemeDelegate.getColorByAttr(
                                context,
                                R.attr.colorText,
                                item
                            )
                        )
                    }
                    if (index == 0) {
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                    val onItemClick = remember(item) {
                        {
                            ThemeUtil.switchTheme(item)
                            ThemeUtil.setUseDynamicTheme(false)
                        }
                    }
                    if (ThemeUtil.isNightMode(item)) {
                        ThemeItem(
                            themeName = name,
                            themeValue = item,
                            primaryColor = backgroundColor,
                            accentColor = backgroundColor,
                            contentColor = onBackgroundColor,
                            selected = !isDynamicTheme && currentTheme == item,
                            onClick = onItemClick
                        )
                    } else {
                        ThemeItem(
                            themeName = name,
                            themeValue = item,
                            primaryColor = primaryColor,
                            accentColor = accentColor,
                            contentColor = onAccentColor,
                            selected = !isDynamicTheme && currentTheme == item,
                            onClick = onItemClick
                        )
                    }
                }
            }
        }
    }
}
