package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils

object ThemeUtil {
    val themeState: MutableState<String> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableStateOf(
            if (App.isInitialized) dataStore.getString(KEY_THEME, THEME_DEFAULT)
            else THEME_DEFAULT
        )
    }

    const val TAG = "ThemeUtil"

    const val KEY_THEME = "theme"
    const val KEY_DARK_THEME = "dark_theme"
    const val KEY_OLD_THEME = "old_theme"
    const val KEY_USE_DYNAMIC_THEME = "useDynamicColorTheme"
    const val KEY_SWITCH_REASON = "switch_reason"
    const val KEY_TRANSLUCENT_PRIMARY_COLOR = "translucent_primary_color"
    const val KEY_CUSTOM_STATUS_BAR_FONT_DARK = "custom_status_bar_font_dark"
    const val KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR = "custom_toolbar_primary_color"
    const val KEY_TRANSLUCENT_THEME_BACKGROUND_PATH = "translucent_theme_background_path"
    const val KEY_USE_DYNAMIC_COLOR_THEME = "useDynamicColorTheme"

    const val THEME_TRANSLUCENT = "translucent"
    const val THEME_TRANSLUCENT_LIGHT = "translucent_light"
    const val THEME_TRANSLUCENT_DARK = "translucent_dark"
    const val THEME_CUSTOM = "custom"
    const val THEME_DEFAULT = "tieba"
    const val THEME_BLACK = "black"
    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"
    const val THEME_RED = "red"
    const val THEME_BLUE_DARK = "blue_dark"
    const val THEME_GREY_DARK = "grey_dark"
    const val THEME_AMOLED_DARK = "amoled_dark"

    const val TRANSLUCENT_THEME_LIGHT = 0
    const val TRANSLUCENT_THEME_DARK = 1

    private val context: Context
        get() = INSTANCE

    val dataStore: DataStore<Preferences>
        get() = INSTANCE.dataStore

    fun getTextColor(context: Context?): Int = ThemeTintHelper.getTextColor(context)

    @JvmStatic
    fun getSecondaryTextColor(context: Context?): Int = ThemeTintHelper.getSecondaryTextColor(context)

    fun switchToNightMode(context: Activity) {
        switchToNightMode(context, true)
    }

    private fun refreshUI(activity: Activity?) {
        if (activity is BaseActivity) {
            activity.refreshUIIfNeed()
            return
        }
        ThemeUtils.refreshUI(activity)
    }

    private fun getOldTheme(): String {
        val oldTheme =
            dataStore.getString(KEY_OLD_THEME, THEME_DEFAULT).takeUnless { isNightMode(it) }

        return oldTheme ?: THEME_DEFAULT
    }

    fun switchTheme(newTheme: String, recordOldTheme: Boolean = true) {
        if (recordOldTheme) {
            val oldTheme = getRawTheme()
            if (!isNightMode(oldTheme)) {
                dataStore.putString(KEY_OLD_THEME, oldTheme)
            }
        }
        dataStore.putString(KEY_THEME, newTheme)
        themeState.value = newTheme
    }

    fun switchDynamicTheme() {
        val currentUseDynamicTheme = dataStore.getBoolean(KEY_USE_DYNAMIC_THEME, false)
        dataStore.putBoolean(KEY_USE_DYNAMIC_THEME, !currentUseDynamicTheme)
    }

    fun setUseDynamicTheme(useDynamicTheme: Boolean) {
        dataStore.putBoolean(KEY_USE_DYNAMIC_THEME, useDynamicTheme)
    }

    fun isUsingDynamicTheme(): Boolean {
        return context.appPreferences.useDynamicColorTheme
    }

    fun switchNightMode() {
        if (isNightMode()) {
            switchTheme(getOldTheme(), recordOldTheme = false)
        } else {
            switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        }
    }

    fun switchToNightMode(context: Activity, recreate: Boolean) {
        switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmOverloads
    fun switchFromNightMode(context: Activity, recreate: Boolean = true) {
        switchTheme(getOldTheme(), recordOldTheme = false)
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmStatic
    fun setChipThemeByLevel(level: String?, parent: View, vararg textViews: TextView) =
        ThemeTintHelper.setChipThemeByLevel(level, parent, *textViews)

    fun setChipTheme(@ColorInt color: Int, parent: View, vararg textViews: TextView) =
        ThemeTintHelper.setChipTheme(color, parent, *textViews)

    @JvmStatic
    fun setThemeForSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) =
        ThemeTintHelper.setThemeForSwipeRefreshLayout(swipeRefreshLayout)

    @JvmStatic
    fun isNightMode(): Boolean = isNightMode(getRawTheme())

    @JvmStatic
    fun isNightMode(theme: String): Boolean = ThemeNameMapper.isNightMode(theme)

    fun isTranslucentTheme(): Boolean = isTranslucentTheme(getRawTheme())

    @JvmStatic
    fun isTranslucentTheme(theme: String): Boolean = ThemeNameMapper.isTranslucentTheme(theme)

    fun isDynamicTheme(theme: String): Boolean = ThemeNameMapper.isDynamicTheme(theme)

    fun isStatusBarFontDark(): Boolean {
        val theme = getRawTheme()
        val isToolbarPrimaryColor: Boolean = INSTANCE.appPreferences.toolbarPrimaryColor
        return if (theme == THEME_CUSTOM) {
            INSTANCE.appPreferences.customStatusBarFontDark
        } else if (isTranslucentTheme(theme)) {
            theme.contains("dark", ignoreCase = true)
        } else if (!isToolbarPrimaryColor) {
            !isNightMode(theme)
        } else {
            false
        }
    }

    fun isNavigationBarFontDark(): Boolean = !isNightMode()

    fun setTheme(context: Activity) {
        val nowTheme = getCurrentTheme()
        context.setTheme(getThemeByName(nowTheme))
    }

    @JvmOverloads
    fun getCurrentTheme(
        theme: String = getRawTheme(),
        checkDynamic: Boolean = false,
    ): String {
        var nowTheme = theme
        if (isTranslucentTheme(nowTheme)) {
            val colorTheme =
                dataStore.getInt("translucent_background_theme", TRANSLUCENT_THEME_LIGHT)
            nowTheme = if (colorTheme == TRANSLUCENT_THEME_DARK) {
                THEME_TRANSLUCENT_DARK
            } else {
                THEME_TRANSLUCENT_LIGHT
            }
        } else if (checkDynamic && isUsingDynamicTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nowTheme = "${nowTheme}_dynamic"
        }
        return nowTheme
    }

    @JvmStatic
    fun setTranslucentThemeWebViewBackground(webView: WebView?) =
        TranslucentThemeViewHelper.setTranslucentThemeWebViewBackground(webView)

    fun setTranslucentBackground(view: View?) =
        TranslucentThemeViewHelper.setTranslucentBackground(view)

    @JvmStatic
    fun setTranslucentDialogBackground(view: View?) =
        TranslucentThemeViewHelper.setTranslucentDialogBackground(view)

    fun setTranslucentThemeBackground(
        activity: BaseActivity,
        view: View?,
        setFitsSystemWindow: Boolean = true,
        useCache: Boolean = false,
    ) = TranslucentThemeViewHelper.setTranslucentThemeBackground(activity, view, setFitsSystemWindow, useCache)

    @StyleRes
    private fun getThemeByName(themeName: String): Int = ThemeNameMapper.getThemeByName(themeName)

    @JvmStatic
    fun getRawTheme(): String = ThemeNameMapper.normalizeThemeName(themeState.value)
}
