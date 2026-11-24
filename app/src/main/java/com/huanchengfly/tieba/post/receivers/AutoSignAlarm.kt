package com.huanchengfly.tieba.post.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences

class AutoSignAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            TiebaUtil.startSign(context)
        }.onFailure { e ->
            Log.e(TAG, "Failed to start sign in AutoSignAlarm", e)
        }
        runCatching {
            if (context.appPreferences.autoSign) {
                TiebaUtil.initAutoSign(context)
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to re-init auto sign in AutoSignAlarm", e)
        }
    }

    companion object {
        const val TAG: String = "AutoSignAlarm"
    }
}
