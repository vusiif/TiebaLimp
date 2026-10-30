package com.huanchengfly.tieba.post.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.huanchengfly.tieba.post.App
import kotlinx.collections.immutable.persistentListOf


object LauncherIcons {
    const val NEW_ICON = "com.huanchengfly.tieba.post.MainActivityIconDefault"
    const val NEW_ICON_THEMED = "com.huanchengfly.tieba.post.MainActivityIconThemed"
    const val NEW_ICON_INVERT = "com.huanchengfly.tieba.post.MainActivityIconInvert"
    const val OLD_ICON = "com.huanchengfly.tieba.post.MainActivityIconOld"
    const val LEGACY_MAIN_ACTIVITY_V2 = "com.huanchengfly.tieba.post.MainActivityV2"

    const val DEFAULT_ICON = NEW_ICON

    val ICONS = persistentListOf(NEW_ICON, NEW_ICON_THEMED, NEW_ICON_INVERT, OLD_ICON)

    val SUPPORT_THEMED_ICON = persistentListOf(NEW_ICON)
    val THEMED_ICON_MAPPING = mapOf(
        NEW_ICON to NEW_ICON_THEMED,
    )

    const val OLD_LAUNCHER_ICON = "com.huanchengfly.tieba.post.activities.MainActivity"
}

object AppIconUtil {
    const val PREF_KEY_APP_ICON = "app_icon"

    private val context: Context
        get() = App.INSTANCE

    private val appPreferences: AppPreferencesUtils
        get() = context.appPreferences

    fun setIcon(
        icon: String = appPreferences.appIcon ?: LauncherIcons.NEW_ICON,
        isThemed: Boolean = appPreferences.useThemedIcon,
    ) {
        val normalizedIcon = if (icon == LauncherIcons.LEGACY_MAIN_ACTIVITY_V2) {
            LauncherIcons.NEW_ICON
        } else icon
        val useThemedIcon = isThemed && LauncherIcons.SUPPORT_THEMED_ICON.contains(normalizedIcon)
        var newIcon = if (LauncherIcons.ICONS.contains(normalizedIcon)) {
            normalizedIcon
        } else LauncherIcons.DEFAULT_ICON
        if (useThemedIcon) {
            newIcon = LauncherIcons.THEMED_ICON_MAPPING[newIcon] ?: newIcon
        }

        // MainActivityV2 must always remain enabled as the base activity for aliases and deep links
        context.packageManager.enableComponent(ComponentName(context, LauncherIcons.LEGACY_MAIN_ACTIVITY_V2))

        val targetComponent = ComponentName(context, newIcon)
        val isTargetEnabled = context.packageManager.getComponentEnabledSetting(targetComponent) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val areOthersDisabled = LauncherIcons.ICONS.filter { it != newIcon }.all {
            context.packageManager.getComponentEnabledSetting(ComponentName(context, it)) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (isTargetEnabled && areOthersDisabled) {
            return
        }

        LauncherIcons.ICONS.forEach {
            if (it == newIcon) {
                context.packageManager.enableComponent(ComponentName(context, it))
            } else {
                context.packageManager.disableComponent(ComponentName(context, it))
            }
        }
        context.packageManager.disableComponent(
            ComponentName(
                context,
                LauncherIcons.OLD_LAUNCHER_ICON
            )
        )
    }

    /**
     * 启用组件
     *
     * @param componentName 组件名
     */
    fun PackageManager.enableComponent(componentName: ComponentName) {
        if (getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    /**
     * 禁用组件
     *
     * @param componentName 组件名
     */
    fun PackageManager.disableComponent(componentName: ComponentName) {
        if (getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}