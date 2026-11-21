package com.huanchengfly.tieba.post

import android.content.Intent
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.ramcosta.composedestinations.spec.Direction

object MainActivityIntentDispatcher {
    fun isTiebaDispatchIntent(intent: Intent): Boolean {
        val data = intent.data ?: return false
        return data.scheme == "com.baidu.tieba" && data.host == "unidispatch"
    }

    fun resolveDirection(intent: Intent): Direction? {
        val data = intent.data ?: return null
        if (!isTiebaDispatchIntent(intent)) return null
        return when (data.path.orEmpty().lowercase()) {
            "/frs" -> {
                val forumName = data.getQueryParameter("kw") ?: return null
                ForumPageDestination(forumName)
            }

            "/pb" -> {
                val threadId = data.getQueryParameter("tid")?.toLongOrNull() ?: return null
                ThreadPageDestination(threadId)
            }

            else -> null
        }
    }
}
