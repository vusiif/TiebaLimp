package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.models.database.FloorBookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal

object FloorBookmarkUtil {
    suspend fun getBookmarks(threadId: Long): List<FloorBookmark> = withContext(Dispatchers.IO) {
        runCatching {
            LitePal.where("threadId = ?", threadId.toString())
                .order("floor asc")
                .find(FloorBookmark::class.java)
        }.getOrElse { emptyList() }
    }

    suspend fun findBookmark(threadId: Long, postId: Long): FloorBookmark? = withContext(Dispatchers.IO) {
        runCatching {
            LitePal.where("threadId = ? and postId = ?", threadId.toString(), postId.toString())
                .findFirst(FloorBookmark::class.java)
        }.getOrNull()
    }

    suspend fun addOrUpdateBookmark(
        threadId: Long,
        threadTitle: String,
        postId: Long,
        floor: Int,
        authorName: String,
        contentSnippet: String,
        note: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            LitePal.deleteAll(
                FloorBookmark::class.java,
                "threadId = ? and postId = ?",
                threadId.toString(),
                postId.toString()
            )
            FloorBookmark(
                threadId = threadId.toString(),
                threadTitle = threadTitle,
                postId = postId.toString(),
                floor = floor,
                authorName = authorName,
                contentSnippet = contentSnippet.take(120),
                note = note.trim(),
                timestamp = System.currentTimeMillis()
            ).save()
        }.getOrDefault(false)
    }

    suspend fun deleteBookmark(threadId: Long, postId: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            LitePal.deleteAll(
                FloorBookmark::class.java,
                "threadId = ? and postId = ?",
                threadId.toString(),
                postId.toString()
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun deleteBookmarkById(id: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            LitePal.delete(FloorBookmark::class.java, id) > 0
        }.getOrDefault(false)
    }
}
