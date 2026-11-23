package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewHelper {

    fun calculateTextZoom(fontScale: Float): Int {
        return (fontScale * 100).toInt().coerceIn(50, 200)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyDefaultSettings(
        webView: WebView,
        context: Context,
        enableZoom: Boolean = true
    ) {
        // Prevent white screen flash on dark / translucent theme
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val isNight = ThemeUtil.isNightMode()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true

            // Responsive font scale alignment with user app preference
            textZoom = calculateTextZoom(context.appPreferences.fontScale)

            // Zoom controls
            if (enableZoom) {
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            } else {
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }

            // Mixed content & modern cache strategy
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT

            // Dark mode adaptation: algorithmic darkening / force dark
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isAlgorithmicDarkeningAllowed = isNight
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                forceDark = if (isNight) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
            }
        }
    }

    /**
     * Safely tear down WebView to prevent Chromium and View hierarchy memory leaks.
     */
    fun destroyWebView(webView: WebView) {
        runCatching {
            webView.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }
}
