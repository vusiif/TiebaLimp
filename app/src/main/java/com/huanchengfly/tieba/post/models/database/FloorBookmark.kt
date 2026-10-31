package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import org.litepal.crud.LitePalSupport

@Immutable
data class FloorBookmark(
    val threadId: String = "",
    val threadTitle: String = "",
    val postId: String = "",
    val floor: Int = 0,
    val authorName: String = "",
    val contentSnippet: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
) : LitePalSupport() {
    val id: Long = 0L
}
