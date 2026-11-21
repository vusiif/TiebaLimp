package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.OnPermissionInterceptor
import com.hjq.permissions.XXPermissions
import com.huanchengfly.tieba.post.components.dialogs.RequestPermissionTipDialog

class ShowPermissionTipInterceptor(val permissions: List<String>, val description: String) :
    OnPermissionInterceptor {
    companion object {
        @JvmStatic
        val HANDLER: Handler = Handler(Looper.getMainLooper())
    }

    constructor(permissionData: PermissionUtils.PermissionData) : this(
        permissionData.permissions,
        permissionData.desc
    )

    private var mRequestFlag = false

    private var tipDialog: RequestPermissionTipDialog? = null

    override fun launchPermissionRequest(
        activity: Activity,
        allPermissions: MutableList<String>,
        callback: OnPermissionCallback?
    ) {
        mRequestFlag = true
        super.launchPermissionRequest(activity, allPermissions, callback)
        HANDLER.postDelayed({
            if (!mRequestFlag) {
                return@postDelayed
            }
            if (activity.isFinishing || activity.isDestroyed) {
                return@postDelayed
            }
            tipDialog = RequestPermissionTipDialog(
                activity,
                PermissionUtils.PermissionData(permissions, description)
            ).apply { show() }
            tipDialog?.show()
        }, 300)
    }

    override fun finishPermissionRequest(
        activity: Activity,
        allPermissions: MutableList<String>,
        skipRequest: Boolean,
        callback: OnPermissionCallback?
    ) {
        mRequestFlag = false
        runCatching { tipDialog?.dismiss() }
    }
}

class PermissionRequester(val context: Context) {
    var permissions: List<String> = emptyList()
    var description: String = ""
    var unchecked: Boolean = false
    var onGranted: (() -> Unit)? = null
    var onDenied: (() -> Unit)? = null

    fun start() {
        if (XXPermissions.isGranted(context, permissions)) {
            onGranted?.invoke()
        } else {
            XXPermissions.with(context)
                .permission(permissions)
                .interceptor(ShowPermissionTipInterceptor(permissions, description))
                .apply {
                    if (unchecked) {
                        unchecked()
                    }
                }
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (all) {
                            onGranted?.invoke()
                        } else {
                            onDenied?.invoke()
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        onDenied?.invoke()
                    }
                })
        }
    }
}

fun Context.requestPermission(
    builder: PermissionRequester.() -> Unit
) {
    PermissionRequester(this).apply(builder).start()
}
