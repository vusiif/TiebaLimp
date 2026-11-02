package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.extension.findFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.litepal.LitePal
import org.litepal.crud.async.FindMultiExecutor
import org.litepal.extension.deleteAll
import org.litepal.extension.find
import org.litepal.extension.findAsync
import org.litepal.extension.findFirstAsync

object HistoryUtil {
    const val PAGE_SIZE = 100
    const val TYPE_FORUM = 1
    const val TYPE_THREAD = 2
    fun deleteAll() {
        LitePal.deleteAll<History>()
    }

    @JvmOverloads
    fun saveHistory(history: History, async: Boolean = true, incrementCount: Boolean = true) {
        if (async) {
            saveOrUpdateAsync(history, incrementCount)
        } else {
            saveOrUpdate(history, incrementCount)
        }
    }

    val all: List<History>
        get() = LitePal.order("timestamp desc, count desc").limit(100).find<History>()

    fun getAll(type: Int): List<History> {
        return LitePal.order("timestamp desc, count desc").where("type = ?", type.toString())
            .limit(PAGE_SIZE)
            .find<History>()
    }

    fun getAllAsync(type: Int): FindMultiExecutor<History> {
        return LitePal.order("timestamp desc, count desc").where("type = ?", type.toString())
            .limit(PAGE_SIZE)
            .findAsync<History>()
    }

    fun getFlow(
        type: Int,
        page: Int
    ): Flow<List<History>> {
        return LitePal.where("type = ?", "$type")
            .order("timestamp desc, count desc")
            .limit(PAGE_SIZE)
            .offset(page * 100)
            .findFlow()
    }

    fun getHistory(data: String, type: Int = TYPE_THREAD): History? {
        return LitePal.where("data = ? and type = ?", data, type.toString()).findFirst(History::class.java)
    }

    fun getThreadProgress(threadId: Long): ThreadHistoryInfoBean? {
        val history = getHistory(threadId.toString(), TYPE_THREAD) ?: return null
        return history.extras?.fromJson<ThreadHistoryInfoBean>()
    }

    fun updateThreadProgress(threadId: Long, extras: String): Boolean = synchronized(this) {
        val historyBean = LitePal.where("data = ? and type = ?", threadId.toString(), TYPE_THREAD.toString())
            .findFirst(History::class.java)
        if (historyBean != null) {
            historyBean.copy(
                timestamp = System.currentTimeMillis(),
                extras = extras
            ).update(historyBean.id) > 0
        } else {
            false
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun update(history: History, incrementCount: Boolean = true): Boolean {
        val historyBean = LitePal.where("data = ? and type = ?", history.data, history.type.toString()).findFirst(
            History::class.java
        )
        if (historyBean != null) {
            val rows = historyBean.copy(
                timestamp = System.currentTimeMillis(),
                title = history.title.ifBlank { historyBean.title },
                extras = history.extras ?: historyBean.extras,
                avatar = history.avatar ?: historyBean.avatar,
                username = history.username ?: historyBean.username,
                count = if (incrementCount) historyBean.count + 1 else historyBean.count
            ).update(historyBean.id)
            return rows > 0
        }
        return false
    }

    @Synchronized
    private fun saveOrUpdate(history: History, incrementCount: Boolean = true) {
        if (update(history, incrementCount)) {
            return
        }
        val saveHistory = history.copy(count = 1, timestamp = System.currentTimeMillis())
        saveHistory.save()
    }

    private fun saveOrUpdateAsync(
        history: History,
        incrementCount: Boolean = true,
        callback: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            val success = synchronized(HistoryUtil) {
                if (update(history, incrementCount)) {
                    true
                } else {
                    history.copy(count = 1, timestamp = System.currentTimeMillis()).save()
                }
            }
            callback?.invoke(success)
        }
    }
}