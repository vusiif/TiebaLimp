package com.huanchengfly.tieba.post.utils

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.resize.Scale
import com.google.android.material.appbar.AppBarLayout
import com.huanchengfly.tieba.post.App.Companion.translucentBackground
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.interfaces.BackgroundTintable
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import kotlinx.coroutines.launch

object TranslucentThemeViewHelper {
    fun setAppBarFitsSystemWindow(view: View?, appBarFitsSystemWindow: Boolean) {
        if (view == null) return
        if (view is AppBarLayout) {
            view.setFitsSystemWindows(appBarFitsSystemWindow)
            view.clipToPadding = !appBarFitsSystemWindow
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setAppBarFitsSystemWindow(view.getChildAt(i), appBarFitsSystemWindow)
            }
        }
    }

    fun setTranslucentBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!ThemeUtil.isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(Color.TRANSPARENT)
    }

    fun setTranslucentDialogBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!ThemeUtil.isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(
            ThemeUtils.getColorById(
                view.context,
                R.color.theme_color_card_grey_dark
            )
        )
    }

    fun setTranslucentThemeWebViewBackground(webView: WebView?) {
        if (webView == null) {
            return
        }
        if (!ThemeUtil.isTranslucentTheme()) {
            return
        }
        webView.setBackgroundColor(Color.WHITE)
    }

    fun setTranslucentThemeBackground(
        activity: BaseActivity,
        view: View?,
        setFitsSystemWindow: Boolean = true,
        useCache: Boolean = false,
    ) {
        if (view == null) {
            return
        }
        if (!ThemeUtil.isTranslucentTheme()) {
            if (setFitsSystemWindow) {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = false
                (view as ViewGroup).clipToPadding = true
            }
            return
        }
        if (setFitsSystemWindow) {
            if (view is CoordinatorLayout) {
                setAppBarFitsSystemWindow(view, true)
                view.setFitsSystemWindows(false)
                (view as ViewGroup).clipToPadding = true
            } else {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = true
                (view as ViewGroup).clipToPadding = false
            }
        }
        view.backgroundTintList = null
        if (view is BackgroundTintable) {
            (view as BackgroundTintable).setBackgroundTintResId(0)
        }
        val backgroundFilePath = ThemeUtil.dataStore.getString(ThemeUtil.KEY_TRANSLUCENT_THEME_BACKGROUND_PATH)
        if (backgroundFilePath == null) {
            view.setBackgroundColor(Color.BLACK)
            return
        }
        if (useCache && translucentBackground != null &&
            (translucentBackground !is BitmapDrawable || !(translucentBackground as BitmapDrawable).bitmap.isRecycled)
        ) {
            view.background = translucentBackground
            return
        }
        activity.launch {
            val result = DisplayRequest(activity, newFileUri(backgroundFilePath)) {
                resizeScale(Scale.CENTER_CROP)
            }.execute()
            if (result is DisplayResult.Success) {
                if (useCache) {
                    translucentBackground = result.drawable
                }
                view.background = result.drawable
            } else {
                view.setBackgroundColor(Color.BLACK)
            }
        }
    }
}
