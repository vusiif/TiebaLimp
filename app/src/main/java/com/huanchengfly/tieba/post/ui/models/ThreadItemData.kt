package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.personalized.ThreadPersonalized
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class ThreadItemData(
    val thread: ImmutableHolder<ThreadInfo>,
    val blocked: Boolean = thread.get { shouldBlock() },
    val personalized: ImmutableHolder<ThreadPersonalized>? = null,
    val hidden: Boolean = blocked && runCatching { App.INSTANCE.appPreferences.hideBlockedContent }.getOrDefault(false),
    val id: Long = if (thread.item.threadId != 0L) thread.item.threadId else thread.item.id,
    val isTop: Boolean = thread.item.isTop == 1,
    val mediaCount: Int = thread.item.media.size,
    val hasVideo: Boolean = thread.item.videoInfo != null,
)

fun List<ThreadItemData>.distinctById(): ImmutableList<ThreadItemData> {
    return distinctBy { it.id }.toImmutableList()
}