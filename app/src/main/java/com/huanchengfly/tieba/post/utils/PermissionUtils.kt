package com.huanchengfly.tieba.post.utils

import android.content.Context
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort

object PermissionUtils {
    const val READ_CALENDAR = "android.permission.READ_CALENDAR"
    const val WRITE_CALENDAR = "android.permission.WRITE_CALENDAR"

    const val CAMERA = "android.permission.CAMERA"

    const val READ_CONTACTS = "android.permission.READ_CONTACTS"
    const val WRITE_CONTACTS = "android.permission.WRITE_CONTACTS"
    const val GET_ACCOUNTS = "android.permission.GET_ACCOUNTS"

    const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    const val ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"

    const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"

    const val READ_PHONE_STATE = "android.permission.READ_PHONE_STATE"
    const val CALL_PHONE = "android.permission.CALL_PHONE"
    const val USE_SIP = "android.permission.USE_SIP"
    const val READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS"
    const val ANSWER_PHONE_CALLS = "android.permission.ANSWER_PHONE_CALLS"
    const val ADD_VOICEMAIL = "com.android.voicemail.permission.ADD_VOICEMAIL"

    const val READ_CALL_LOG = "android.permission.READ_CALL_LOG"
    const val WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG"
    const val PROCESS_OUTGOING_CALLS = "android.permission.PROCESS_OUTGOING_CALLS"

    const val BODY_SENSORS = "android.permission.BODY_SENSORS"
    const val ACTIVITY_RECOGNITION = "android.permission.ACTIVITY_RECOGNITION"

    const val SEND_SMS = "android.permission.SEND_SMS"
    const val RECEIVE_SMS = "android.permission.RECEIVE_SMS"
    const val READ_SMS = "android.permission.READ_SMS"
    const val RECEIVE_WAP_PUSH = "android.permission.RECEIVE_WAP_PUSH"
    const val RECEIVE_MMS = "android.permission.RECEIVE_MMS"

    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    const val WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"

    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    /**
     * Turn permissions into text.
     */
    fun transformText(context: Context, permissions: List<String>): List<String> =
        PermissionTextTransformer.transformText(context, permissions)

    fun askPermission(context: Context, permissionData: PermissionData, onGranted: () -> Unit) {
        askPermission(context, permissionData, R.string.tip_no_permission, onGranted)
    }

    fun askPermission(
        context: Context,
        permissionData: PermissionData,
        deniedToast: Int,
        onGranted: () -> Unit
    ) {
        askPermission(context, permissionData, context.getString(deniedToast), onGranted, null)
    }

    fun askPermission(
        context: Context,
        permissionData: PermissionData,
        deniedToast: Int,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)?
    ) {
        askPermission(context, permissionData, context.getString(deniedToast), onGranted, onDenied)
    }

    @JvmOverloads
    fun askPermission(
        context: Context,
        permissionData: PermissionData,
        deniedToast: String,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        if (XXPermissions.isGranted(context, permissionData.permissions)) {
            onGranted()
        } else {
            XXPermissions.with(context)
                .permission(permissionData.permissions)
                .interceptor(ShowPermissionTipInterceptor(permissionData))
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (all) {
                            onGranted()
                        } else {
                            context.toastShort(deniedToast)
                            onDenied?.invoke()
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        context.toastShort(deniedToast)
                        onDenied?.invoke()
                    }
                })
        }
    }

    data class PermissionData(val permissions: List<String>, val desc: String)
}
