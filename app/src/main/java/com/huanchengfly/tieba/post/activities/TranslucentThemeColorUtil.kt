package com.huanchengfly.tieba.post.activities

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.theme.TintMaterialButton
import com.huanchengfly.tieba.post.utils.ThemeUtil.TRANSLUCENT_THEME_DARK
import com.huanchengfly.tieba.post.utils.ThemeUtil.TRANSLUCENT_THEME_LIGHT

object TranslucentThemeColorUtil {
    fun argbToHexString(alpha: Int, red: Int, green: Int, blue: Int): String {
        val hr = Integer.toHexString(red)
        val hg = Integer.toHexString(green)
        val hb = Integer.toHexString(blue)
        val ha = Integer.toHexString(alpha)
        return "#" + fixHexString(ha) + fixHexString(hr) + fixHexString(hg) + fixHexString(hb)
    }

    private fun fixHexString(string: String): String {
        var hexStr = string
        if (hexStr.isEmpty()) {
            hexStr = "00"
        }
        if (hexStr.length == 1) {
            hexStr = "0$hexStr"
        }
        if (hexStr.length > 2) {
            hexStr = hexStr.substring(0, 2)
        }
        return hexStr
    }

    fun colorToHexString(@ColorInt color: Int): String {
        return argbToHexString(
            Color.alpha(color),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    fun applyThemeModeButtons(
        context: Context,
        darkColorBtn: TintMaterialButton,
        lightColorBtn: TintMaterialButton,
        themeMode: Int
    ) {
        when (themeMode) {
            TRANSLUCENT_THEME_DARK -> {
                darkColorBtn.setBackgroundTintResId(R.color.default_color_accent)
                darkColorBtn.setTextColorResId(R.color.white)
                darkColorBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle),
                    null,
                    null,
                    null
                )
                lightColorBtn.setBackgroundTintResId(R.color.color_divider)
                lightColorBtn.setTextColorResId(R.color.color_text_secondary)
                lightColorBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    null,
                    null
                )
            }
            TRANSLUCENT_THEME_LIGHT -> {
                darkColorBtn.setBackgroundTintResId(R.color.color_divider)
                darkColorBtn.setTextColorResId(R.color.color_text_secondary)
                darkColorBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
                lightColorBtn.setBackgroundTintResId(R.color.default_color_accent)
                lightColorBtn.setTextColorResId(R.color.white)
                lightColorBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle),
                    null,
                    null,
                    null
                )
            }
        }
    }
}
