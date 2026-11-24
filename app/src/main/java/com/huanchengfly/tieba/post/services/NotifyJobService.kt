package com.huanchengfly.tieba.post.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.MsgBean
import com.huanchengfly.tieba.post.pendingIntentFlagImmutable
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.AccountUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotifyJobService : JobService() {
    private var notificationManager: NotificationManager? = null
    private var activeCall: Call<MsgBean>? = null

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = notificationManager ?: return
            if (channelsInitialized) return
            runCatching {
                val channelGroup = NotificationChannelGroup(CHANNEL_GROUP, CHANNEL_GROUP_NAME).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        description = "贴吧的各种消息通知"
                    }
                }
                nm.createNotificationChannelGroup(channelGroup)
                createChannel(nm, CHANNEL_REPLY, CHANNEL_REPLY_NAME)
                createChannel(nm, CHANNEL_AT, CHANNEL_AT_NAME)
                channelsInitialized = true
            }
        }
    }

    private fun createChannel(nm: NotificationManager, id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(id) == null) {
                val channel = NotificationChannel(
                    id,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    group = CHANNEL_GROUP
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Log.i(TAG, "onStartJob")
        if (AccountUtil.getLoginInfo() == null) {
            Log.d(TAG, "User not logged in, skip fetching messages")
            jobFinished(params, false)
            return false
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ensureChannels()

        val call = TiebaApi.getInstance().msg()
        activeCall = call
        call.enqueue(object : Callback<MsgBean> {
            override fun onFailure(call: Call<MsgBean>, t: Throwable) {
                Log.w(TAG, "Failed to fetch messages: ${t.message}")
                activeCall = null
                // Do not aggressively reschedule periodically to save battery
                jobFinished(params, false)
            }

            override fun onResponse(call: Call<MsgBean>, response: Response<MsgBean>) {
                activeCall = null
                runCatching {
                    val msgBean = response.body() ?: run {
                        jobFinished(params, false)
                        return
                    }
                    val canNotify = hasNotificationPermission() && notificationManager != null
                    var total = 0

                    val replyMe = msgBean.message?.replyMe
                    if (!replyMe.isNullOrEmpty() && replyMe != "0") {
                        val replyCount = replyMe.toIntOrNull() ?: 0
                        total += replyCount
                        sendBroadcast(
                            Intent(ACTION_NEW_MESSAGE)
                                .putExtra("channel", CHANNEL_REPLY)
                                .putExtra("count", replyCount)
                        )
                        if (canNotify) {
                            updateNotification(
                                getString(R.string.tips_message_reply, replyMe),
                                ID_REPLY,
                                CHANNEL_REPLY,
                                CHANNEL_REPLY_NAME,
                                Intent(ACTION_VIEW, Uri.parse("tblite://notifications/0"))
                            )
                        }
                    }

                    val atMe = msgBean.message?.atMe
                    if (!atMe.isNullOrEmpty() && atMe != "0") {
                        val atCount = atMe.toIntOrNull() ?: 0
                        total += atCount
                        sendBroadcast(
                            Intent(ACTION_NEW_MESSAGE)
                                .putExtra("channel", CHANNEL_AT)
                                .putExtra("count", atMe)
                        )
                        if (canNotify) {
                            updateNotification(
                                getString(R.string.tips_message_at, atMe),
                                ID_AT,
                                CHANNEL_AT,
                                CHANNEL_AT_NAME,
                                Intent(ACTION_VIEW, Uri.parse("tblite://notifications/1"))
                            )
                        }
                    }

                    sendBroadcast(
                        Intent(ACTION_NEW_MESSAGE)
                            .putExtra("channel", CHANNEL_TOTAL)
                            .putExtra("count", total)
                    )
                }.onFailure { e ->
                    Log.e(TAG, "Error processing msg response", e)
                }
                jobFinished(params, false)
            }
        })
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        activeCall?.cancel()
        activeCall = null
        return true
    }

    private fun updateNotification(
        text: String,
        id: Int,
        channel: String,
        channelName: String,
        intent: Intent
    ) {
        val nm = notificationManager ?: return
        runCatching {
            val color = runCatching {
                ThemeUtils.getColorByAttr(this, R.attr.colorPrimary)
            }.getOrElse {
                ContextCompat.getColor(this, R.color.default_color_primary)
            }

            val notification = NotificationCompat.Builder(this, channel)
                .setSubText(channelName)
                .setContentText(getString(R.string.tip_touch_to_view))
                .setContentTitle(text)
                .setSmallIcon(R.drawable.ic_round_drafts)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        pendingIntentFlagImmutable()
                    )
                )
                .setColor(color)
                .build()
            nm.notify(id, notification)
        }.onFailure { e ->
            Log.e(TAG, "Failed to post notification $id", e)
        }
    }

    companion object {
        val TAG: String = NotifyJobService::class.java.simpleName
        private var channelsInitialized = false

        const val ACTION_NEW_MESSAGE = "com.huanchengfly.tieba.post.action.NEW_MESSAGE"
        const val CHANNEL_GROUP = "20"
        const val CHANNEL_AT = "3"
        const val CHANNEL_AT_NAME = "提到我的"
        const val CHANNEL_TOTAL = "total"
        const val ID_REPLY = 20
        const val ID_AT = 21
        const val CHANNEL_GROUP_NAME = "消息通知"
        const val CHANNEL_REPLY = "2"
        const val CHANNEL_REPLY_NAME = "回复我的"
    }
}
