package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import org.litepal.crud.LitePalSupport

@Immutable
data class ThreadSnapshot(
    var threadId: String = "",
    var title: String = "",
    var forumName: String = "",
    var authorName: String = "",
    var authorAvatar: String = "",
    var floorCount: Int = 0,
    var createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = System.currentTimeMillis(),
) : LitePalSupport() {
    val id: Long = 0L
}
