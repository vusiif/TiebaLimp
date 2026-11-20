package com.huanchengfly.tieba.post.ui.page.login

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.page.webview.MyWebViewClient
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.parseCookie
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

fun saveAccountAndFinish(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    navigator: DestinationsNavigator?,
    bduss: String,
    sToken: String,
    cookieStr: String,
    baiduId: String? = null,
    onSuccess: (() -> Unit)? = null,
    onFailure: (() -> Unit)? = null,
) {
    coroutineScope.launch {
        if (!baiduId.isNullOrEmpty() && ClientUtils.baiduId.isNullOrEmpty()) {
            ClientUtils.saveBaiduId(context, baiduId)
        }
        snackbarHostState.showSnackbar(
            context.getString(R.string.text_please_wait),
            duration = SnackbarDuration.Indefinite
        )
        AccountUtil.fetchAccountFlow(bduss, sToken, cookieStr)
            .catch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    context.getString(
                        R.string.text_login_failed,
                        it.getErrorMessage()
                    ), duration = SnackbarDuration.Short
                )
                onFailure?.invoke()
            }
            .flowOn(Dispatchers.Main)
            .collect { account ->
                AccountUtil.newAccount(account.uid, account) { success ->
                    if (success) {
                        AccountUtil.switchAccount(context, account.id)
                        onSuccess?.invoke()
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.text_login_success),
                                duration = SnackbarDuration.Short
                            )
                            delay(1500L)
                            navigator?.navigateUp()
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.text_login_failed_default),
                                duration = SnackbarDuration.Short
                            )
                        }
                        onFailure?.invoke()
                    }
                }
            }
    }
}

class LoginWebViewClient(
    nativeNavigator: DestinationsNavigator? = null,
    val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
) : MyWebViewClient(nativeNavigator) {
    private var isLoadingAccount = false

    override fun injectCookies(url: String) {}

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url == null || isLoadingAccount) {
            return
        }
        val cookieStr = CookieManager.getInstance().getCookie(url) ?: return
        val cookies = parseCookie(cookieStr).mapKeys { it.key.uppercase() }
        val bduss = cookies["BDUSS"]
        val sToken = cookies["STOKEN"]
        val baiduId = cookies["BAIDUID"]
        if (url.startsWith("https://tieba.baidu.com/index/tbwise/") || url.startsWith("https://tiebac.baidu.com/index/tbwise/")) {
            if (bduss == null || sToken == null) {
                return
            }
            isLoadingAccount = true
            saveAccountAndFinish(
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                navigator = nativeNavigator,
                bduss = bduss,
                sToken = sToken,
                cookieStr = cookieStr,
                baiduId = baiduId,
                onFailure = {
                    navigator.loadUrl(LOGIN_URL)
                    isLoadingAccount = false
                }
            )
        }
    }
}
