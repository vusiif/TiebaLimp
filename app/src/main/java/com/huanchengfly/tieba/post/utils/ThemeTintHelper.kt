package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.widgets.theme.TintSwipeRefreshLayout

object ThemeTintHelper {
    fun getTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorText)
    }

    fun getSecondaryTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorTextSecondary)
    }

    fun setChipThemeByLevel(level: String?, parent: View, vararg textViews: TextView) {
        setChipTheme(Util.getIconColorByLevel(level), parent, *textViews)
    }

    fun setChipTheme(@ColorInt color: Int, parent: View, vararg textViews: TextView) {
        parent.backgroundTintList = ColorStateList.valueOf(color)
        for (textView in textViews) {
            textView.setTextColor(ThemeUtils.getColorByAttr(parent.context, R.attr.colorOnAccent))
        }
    }

    fun setThemeForSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        if (swipeRefreshLayout is TintSwipeRefreshLayout) {
            swipeRefreshLayout.tint()
            return
        }
        val context = swipeRefreshLayout.context
        val resources = context.resources
        if (resources != null) {
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(resources.getColor(R.color.color_swipe_refresh_bg))
            swipeRefreshLayout.setColorSchemeColors(
                ThemeUtils.getColorByAttr(
                    context,
                    R.attr.colorAccent
                )
            )
        }
    }
}
