package com.huanchengfly.tieba.post.ui.page.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.dialogs.PermissionDialog
import com.huanchengfly.tieba.post.models.PermissionBean
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebViewClient
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

open class MyWebViewClient(
    protected val nativeNavigator: DestinationsNavigator? = null,
) : AccompanistWebViewClient() {
    val context: Context
        get() = state.webView?.context ?: App.INSTANCE

    private fun interceptWebViewRequest(
        webView: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val newUri = request.url
        val scheme = newUri.scheme?.lowercase() ?: return false
        val host = newUri.host?.lowercase() ?: return false
        val path = newUri.path?.lowercase() ?: return false
        val isHttp = scheme.startsWith("http")
        val isTieba = host == "wapp.baidu.com" ||
                host.contains("tieba.baidu.com") ||
                host == "tiebac.baidu.com"
        val isInternal = isTieba ||
                host.contains("wappass.baidu.com") ||
                host.contains("ufosdk.baidu.com") ||
                host.contains("m.help.baidu.com")
        return when {
            isHttp && isTieba -> {
                if (path == "/f" || path == "/mo/q/m") {
                    val forumName =
                        newUri.getQueryParameter("kw") ?: newUri.getQueryParameter("word")
                    val threadId = newUri.getQueryParameter("kz")?.toLongOrNull()
                    if (threadId != null) {
                        nativeNavigator?.navigate(
                            ThreadPageDestination(threadId)
                        )
                        true
                    } else if (forumName != null) {
                        nativeNavigator?.navigate(
                            ForumPageDestination(forumName)
                        )
                        true
                    } else false
                } else if (path.startsWith("/p/")) {
                    val threadId = path.substring(3).toLongOrNull()
                    if (threadId != null) {
                        nativeNavigator?.navigate(
                            ThreadPageDestination(threadId)
                        )
                        true
                    } else false
                } else false
            }

            isHttp && !isInternal -> {
                if (context.appPreferences.useWebView)
                    false
                else {
                    if (context.appPreferences.useCustomTabs) {
                        runCatching {
                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setDefaultColorSchemeParams(
                                    CustomTabColorSchemeParams.Builder()
                                        .setToolbarColor(
                                            ThemeUtils.getColorByAttr(
                                                context,
                                                R.attr.colorToolbar
                                            )
                                        )
                                        .setNavigationBarColor(
                                            ThemeUtils.getColorByAttr(
                                                context,
                                                R.attr.colorNavBar
                                            )
                                        )
                                        .setNavigationBarDividerColor(
                                            ThemeUtils.getColorByAttr(
                                                context,
                                                R.attr.colorDivider
                                            )
                                        )
                                        .build()
                                    )
                                .build()
                                .launchUrl(context, newUri)
                        }.onFailure {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    newUri
                                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            )
                        }
                    } else {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                newUri
                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                    }
                    true
                }
            }

            !isHttp -> {
                val currentUri = webView.url?.toUri()
                val currentHost = currentUri?.host
                if (currentHost != null) {
                    launchThirdPartyApp(newUri, currentHost)
                }
                true
            }

            else -> false
        }
    }

    private fun launchThirdPartyApp(
        intent: Intent,
        host: String,
    ) {
        val resolves = context.packageManager.queryIntentActivities(intent, 0)
        if (resolves.isEmpty()) return
        val appName = if (resolves.size == 1) {
            resolves[0].loadLabel(context.packageManager)
        } else {
            context.getString(R.string.name_multi_app)
        }
        val scheme = intent.scheme ?: ""
        PermissionDialog(
            context,
            PermissionBean(
                PermissionDialog.CustomPermission.PERMISSION_START_APP,
                "${host}_${scheme}",
                context.getString(R.string.title_start_app_permission, host, appName),
                R.drawable.ic_round_exit_to_app
            )
        )
            .setOnGrantedCallback { context.startActivity(intent) }
            .show()
    }

    private fun launchThirdPartyApp(
        uri: Uri,
        host: String,
    ) {
        if (uri.scheme.equals("intent", ignoreCase = true)) {
            launchThirdPartyApp(
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    .addCategory(Intent.CATEGORY_BROWSABLE), host
            )
        } else {
            launchThirdPartyApp(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), host
            )
        }
    }

    open fun injectCookies(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val host = uri.host?.lowercase() ?: return
        val isTrustedBaiduHost = host == "baidu.com" || host.endsWith(".baidu.com") || host == "tieba.com" || host.endsWith(".tieba.com")
        if (!isTrustedBaiduHost) return

        val cookieStr = CookieManager.getInstance().getCookie(url) ?: ""
        val cookies = AccountUtil.parseCookie(cookieStr)
        val BDUSS = cookies["BDUSS"]
        val currentAccountBDUSS = AccountUtil.getBduss()
        if (currentAccountBDUSS != null && BDUSS != currentAccountBDUSS) {
            CookieManager.getInstance()
                .setCookie(url, AccountUtil.getBdussCookie(currentAccountBDUSS))
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        injectCookies(url ?: "")
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest?): Boolean {
        if (request == null) return false
        return interceptWebViewRequest(view, request)
    }
}
