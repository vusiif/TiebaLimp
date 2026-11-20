package com.huanchengfly.tieba.post.ui.page.thread

import android.content.Context
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.utils.ThreadSnapshotUtil

suspend fun saveThreadSnapshot(
    context: Context,
    threadId: Long,
    threadTitle: String,
    firstPost: ImmutableHolder<Post>?,
    author: ImmutableHolder<User>?,
    forum: ImmutableHolder<SimpleForum>?,
    firstPostContentRenders: List<PbContentRender>,
    data: List<PostItemData>,
) {
    val firstPostData = firstPost?.get()
    val authorName = firstPostData?.author?.nameShow?.ifBlank { firstPostData.author?.name }
        ?: firstPostData?.author?.name
        ?: author?.get { nameShow.ifBlank { name } }
        ?: ""
    val authorAvatar = firstPostData?.author?.portrait ?: author?.get { portrait } ?: ""
    val forumTitle = forum?.get { name } ?: ""
    val targetTitle = threadTitle.ifBlank { firstPostData?.title ?: "" }

    val postsList = mutableListOf<ThreadSnapshotUtil.SnapshotPost>()
    if (firstPostData != null) {
        val firstPostImages = firstPostContentRenders.filterIsInstance<PicContentRender>().map {
            ThreadSnapshotUtil.SnapshotImage(
                picUrl = it.picUrl,
                originUrl = it.originUrl,
                width = it.width,
                height = it.height,
            )
        }
        postsList.add(
            ThreadSnapshotUtil.SnapshotPost(
                floor = 1L,
                postId = firstPostData.id,
                authorName = authorName,
                authorAvatar = authorAvatar,
                time = firstPostData.time.toLong(),
                contentText = firstPostData.content.plainText,
                images = firstPostImages,
            )
        )
    }

    for (item in data) {
        if (item.blocked) continue
        val post = item.post.get()
        if (post.floor == 1) continue
        val postAuthor = post.author?.nameShow?.ifBlank { post.author?.name } ?: post.author?.name ?: ""
        val postAvatar = post.author?.portrait
        val postImages = item.contentRenders.filterIsInstance<PicContentRender>().map {
            ThreadSnapshotUtil.SnapshotImage(
                picUrl = it.picUrl,
                originUrl = it.originUrl,
                width = it.width,
                height = it.height,
            )
        }
        val subPosts = item.subPosts.map { sp ->
            val subPost = sp.subPost.get()
            val spAuthor = subPost.author?.nameShow?.ifBlank { subPost.author?.name } ?: subPost.author?.name ?: ""
            ThreadSnapshotUtil.SnapshotSubPost(
                authorName = spAuthor,
                contentText = sp.subPostContent.text,
                time = subPost.time.toLong(),
            )
        }
        postsList.add(
            ThreadSnapshotUtil.SnapshotPost(
                floor = post.floor.toLong(),
                postId = post.id,
                authorName = postAuthor,
                authorAvatar = postAvatar,
                time = post.time.toLong(),
                contentText = post.content.plainText,
                images = postImages,
                subPosts = subPosts,
            )
        )
    }

    val snapshotData = ThreadSnapshotUtil.SnapshotData(
        threadId = threadId,
        title = targetTitle,
        forumName = forumTitle,
        authorName = authorName,
        authorAvatar = authorAvatar,
        postTime = firstPostData?.time?.toLong() ?: System.currentTimeMillis(),
        snapshotTime = System.currentTimeMillis(),
        posts = postsList,
    )

    val success = ThreadSnapshotUtil.saveSnapshot(context, snapshotData)
    if (success) {
        context.toastShort(context.getString(R.string.toast_save_snapshot_success, postsList.size))
    } else {
        context.toastShort(context.getString(R.string.toast_save_snapshot_failed))
    }
}
