package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.protos.PollInfo
import java.util.concurrent.ConcurrentHashMap

object PollVoteStore {
    private val cache = ConcurrentHashMap<Long, PollInfo>()

    fun get(threadId: Long, fallback: PollInfo?): PollInfo? {
        if (threadId <= 0L) return fallback
        return cache[threadId] ?: fallback
    }

    fun update(threadId: Long, newPoll: PollInfo) {
        if (threadId > 0L) {
            cache[threadId] = newPoll
        }
    }
}
