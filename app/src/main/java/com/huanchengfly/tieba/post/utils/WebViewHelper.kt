package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object WebViewHelper {

    private val isWarmedUp = AtomicBoolean(false)

    /**
     * Pre-warms Chromium engine, CookieManager, and class loaders in the background
     * to eliminate the 300-800ms main-thread freeze when user first opens a WebView.
     */
    @JvmStatic
    fun warmupAsync(context: Context) {
        if (isWarmedUp.get()) return
        CoroutineScope(Dispatchers.Main).launch {
            warmup(context)
        }
    }

    @JvmStatic
    fun warmup(context: Context) {
        if (!isWarmedUp.compareAndSet(false, true)) return
        runCatching {
            CookieManager.getInstance()
            WebSettings.getDefaultUserAgent(context)
        }
    }

    fun calculateTextZoom(fontScale: Float): Int {
        return (fontScale * 100).toInt().coerceIn(50, 200)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyDefaultSettings(
        webView: WebView,
        context: Context,
        enableZoom: Boolean = true,
        allowMixedContent: Boolean = false
    ) {
        // Prevent white screen flash on dark / translucent theme
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val isNight = ThemeUtil.isNightMode()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true

            // Security hardening: disallow local file/content access schemes
            allowFileAccess = false
            allowContentAccess = false

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

            // Mixed content & modern offline-resilient cache strategy
            mixedContentMode = if (allowMixedContent) {
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            } else {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            val isConnected = NetworkUtil.isNetworkConnected(context)
            cacheMode = if (isConnected) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK

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
                (parent as? ViewGroup)?.removeView(this)
                stopLoading()
                webChromeClient = null
                webViewClient = android.webkit.WebViewClient()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }
}

