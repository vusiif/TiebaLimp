package com.huanchengfly.tieba.post.utils

import android.content.Context
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.database.ThreadSnapshot
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import java.io.File

object ThreadSnapshotUtil {
    data class SnapshotData(
        val threadId: Long,
        val title: String,
        val forumName: String,
        val authorName: String,
        val authorAvatar: String?,
        val postTime: Long,
        val snapshotTime: Long,
        val posts: List<SnapshotPost>,
    )

    data class SnapshotPost(
        val floor: Long,
        val postId: Long,
        val authorName: String,
        val authorAvatar: String?,
        val time: Long,
        val contentText: String,
        val images: List<SnapshotImage> = emptyList(),
        val subPosts: List<SnapshotSubPost> = emptyList(),
    )

    data class SnapshotImage(
        val picUrl: String,
        val originUrl: String,
        val width: Int,
        val height: Int,
    )

    data class SnapshotSubPost(
        val authorName: String,
        val contentText: String,
        val time: Long,
    )

    private fun getSnapshotDir(context: Context): File {
        val dir = File(context.filesDir, "snapshots")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getSnapshotFile(context: Context, threadId: Long): File {
        return File(getSnapshotDir(context), "thread_$threadId.json")
    }

    suspend fun saveSnapshot(
        context: Context,
        data: SnapshotData,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = getSnapshotFile(context, data.threadId)
            val json = data.toJson()
            file.writeText(json, Charsets.UTF_8)

            val existing = LitePal.where("threadId = ?", data.threadId.toString())
                .findFirst(ThreadSnapshot::class.java)

            if (existing != null) {
                existing.title = data.title
                existing.forumName = data.forumName
                existing.authorName = data.authorName
                existing.authorAvatar = data.authorAvatar.orEmpty()
                existing.floorCount = data.posts.size
                existing.updateTime = System.currentTimeMillis()
                existing.save()
            } else {
                ThreadSnapshot(
                    threadId = data.threadId.toString(),
                    title = data.title,
                    forumName = data.forumName,
                    authorName = data.authorName,
                    authorAvatar = data.authorAvatar.orEmpty(),
                    floorCount = data.posts.size,
                    createTime = System.currentTimeMillis(),
                    updateTime = System.currentTimeMillis(),
                ).save()
            }
            true
        }.getOrDefault(false)
    }

    suspend fun loadSnapshot(
        context: Context,
        threadId: Long,
    ): SnapshotData? = withContext(Dispatchers.IO) {
        runCatching {
            val file = getSnapshotFile(context, threadId)
            if (!file.exists()) return@withContext null
            val json = file.readText(Charsets.UTF_8)
            json.fromJson<SnapshotData>()
        }.getOrNull()
    }

    suspend fun getAllSnapshots(): List<ThreadSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            LitePal.order("updateTime desc")
                .find(ThreadSnapshot::class.java)
        }.getOrElse { emptyList() }
    }

    suspend fun deleteSnapshot(context: Context, threadId: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = getSnapshotFile(context, threadId)
            if (file.exists()) {
                file.delete()
            }
            LitePal.deleteAll(ThreadSnapshot::class.java, "threadId = ?", threadId.toString()) > 0
        }.getOrDefault(false)
    }

    suspend fun isSnapshotSaved(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            LitePal.where("threadId = ?", threadId.toString())
                .count(ThreadSnapshot::class.java) > 0
        }.getOrDefault(false)
    }
}
