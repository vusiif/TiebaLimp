package com.huanchengfly.tieba.post.ui.page.webview

import android.content.Context
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebView
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.dialogs.PermissionDialog
import com.huanchengfly.tieba.post.models.PermissionBean
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebChromeClient
import com.huanchengfly.tieba.post.utils.DialogUtil
import com.huanchengfly.tieba.post.utils.compose.launchActivityForResult
import kotlinx.coroutines.CoroutineScope
import java.lang.ref.WeakReference
import java.util.UUID

class MyWebChromeClient(
    context: Context,
    coroutineScope: CoroutineScope,
) : AccompanistWebChromeClient() {
    private val contextWeakReference = WeakReference(context)

    val context: Context
        get() = state.webView?.context ?: contextWeakReference.get() ?: App.INSTANCE

    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    val id: String = UUID.randomUUID().toString()

    init {
        coroutineScope.onGlobalEvent<GlobalEvent.ActivityResult>(
            filter = { it.requesterId == id },
        ) {
            uploadMessage?.onReceiveValue(FileChooserParams.parseResult(it.resultCode, it.intent))
            uploadMessage = null
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        callback?.invoke(origin, false, false)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (webView == null || filePathCallback == null || fileChooserParams == null) return false
        uploadMessage?.onReceiveValue(null)
        uploadMessage = filePathCallback
        launchActivityForResult(id, fileChooserParams.createIntent())
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
        DialogUtil.build(view?.context ?: context)
            .setMessage(message)
            .setPositiveButton(R.string.button_sure_default) { _, _ ->
                result?.confirm()
            }
            .setCancelable(false)
            .create()
            .show()
        return true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String?,
        message: String?,
        result: JsResult,
    ): Boolean {
        val context = view.context ?: context
        if ("ClipboardGuardCopyRequest".equals(message, ignoreCase = true)) {
            val uri = Uri.parse(url)
            if (uri != null && uri.host != null) {
                PermissionDialog(
                    context,
                    PermissionBean(
                        PermissionDialog.CustomPermission.PERMISSION_CLIPBOARD_COPY,
                        uri.host,
                        context.getString(
                            R.string.title_ask_permission_clipboard_copy,
                            uri.host
                        ),
                        R.drawable.ic_round_file_copy
                    )
                )
                    .setOnGrantedCallback { result.confirm() }
                    .setOnDeniedCallback { result.cancel() }
                    .show()
            }
        } else {
            DialogUtil.build(view.context)
                .setTitle("Confirm")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    result.confirm()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    result.cancel()
                }
                .create()
                .show()
        }
        return true
    }
}
