package com.huanchengfly.tieba.post.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences

class AutoSignAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            TiebaUtil.startSign(context)
        }
        runCatching {
            if (context.appPreferences.autoSign) {
                TiebaUtil.initAutoSign(context)
            }
        }
    }

    companion object {
        val TAG: String = "AutoSignAlarm"
    }
}