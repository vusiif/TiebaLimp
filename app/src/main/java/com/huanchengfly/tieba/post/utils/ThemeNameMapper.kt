package com.huanchengfly.tieba.post.utils

import androidx.annotation.StyleRes
import com.huanchengfly.tieba.post.R
import java.util.Locale

object ThemeNameMapper {
    fun isNightMode(theme: String): Boolean {
        return theme.lowercase(Locale.getDefault()).contains("dark") && !theme.contains(
            ThemeUtil.THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isTranslucentTheme(theme: String): Boolean {
        return theme.equals(
            ThemeUtil.THEME_TRANSLUCENT,
            ignoreCase = true
        ) || theme.contains(
            ThemeUtil.THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isDynamicTheme(theme: String): Boolean {
        return theme.endsWith("_dynamic")
    }

    @StyleRes
    fun getThemeByName(themeName: String): Int {
        return when (themeName.lowercase(Locale.getDefault())) {
            ThemeUtil.THEME_TRANSLUCENT, ThemeUtil.THEME_TRANSLUCENT_LIGHT -> R.style.TiebaLite_Translucent_Light
            ThemeUtil.THEME_TRANSLUCENT_DARK -> R.style.TiebaLite_Translucent_Dark
            ThemeUtil.THEME_DEFAULT -> R.style.TiebaLite_Tieba
            ThemeUtil.THEME_BLACK -> R.style.TiebaLite_Black
            ThemeUtil.THEME_PURPLE -> R.style.TiebaLite_Purple
            ThemeUtil.THEME_PINK -> R.style.TiebaLite_Pink
            ThemeUtil.THEME_RED -> R.style.TiebaLite_Red
            ThemeUtil.THEME_BLUE_DARK -> R.style.TiebaLite_Dark_Blue
            ThemeUtil.THEME_GREY_DARK -> R.style.TiebaLite_Dark_Grey
            ThemeUtil.THEME_AMOLED_DARK -> R.style.TiebaLite_Dark_Amoled
            ThemeUtil.THEME_CUSTOM -> R.style.TiebaLite_Custom
            else -> R.style.TiebaLite_Tieba
        }
    }

    fun normalizeThemeName(theme: String?): String {
        if (theme == null) return ThemeUtil.THEME_DEFAULT
        return when (val lower = theme.lowercase(Locale.getDefault())) {
            ThemeUtil.THEME_TRANSLUCENT,
            ThemeUtil.THEME_TRANSLUCENT_LIGHT,
            ThemeUtil.THEME_TRANSLUCENT_DARK,
            ThemeUtil.THEME_CUSTOM,
            ThemeUtil.THEME_DEFAULT,
            ThemeUtil.THEME_BLACK,
            ThemeUtil.THEME_PURPLE,
            ThemeUtil.THEME_PINK,
            ThemeUtil.THEME_RED,
            ThemeUtil.THEME_BLUE_DARK,
            ThemeUtil.THEME_GREY_DARK,
            ThemeUtil.THEME_AMOLED_DARK -> lower
            else -> ThemeUtil.THEME_DEFAULT
        }
    }
}
