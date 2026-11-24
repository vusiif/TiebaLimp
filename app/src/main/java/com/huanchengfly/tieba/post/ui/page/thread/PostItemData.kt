package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.getContentText
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.utils.BlockManager.FilterResult
import com.huanchengfly.tieba.post.utils.BlockManager.checkBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class PostItemData(
    val post: ImmutableHolder<Post>,
    val blocked: Boolean,
    val contentRenders: ImmutableList<PbContentRender>,
    val subPosts: ImmutableList<SubPostItemData>,
    val blockResult: FilterResult = FilterResult.Allowed,
    val id: Long = post.item.id,
    val floor: Int = post.item.floor,
    val authorId: Long = post.item.author_id,
    val isFolded: Boolean = post.item.is_fold == 1 || post.item.fold_comment_status != 0,
    val subPostCount: Int = post.item.sub_post_number,
    val picRenders: ImmutableList<PicContentRender> = contentRenders.filterIsInstance<PicContentRender>().toImmutableList(),
    val picUrls: ImmutableList<String> = picRenders.map { it.picUrl }.toImmutableList(),
) {
    constructor(
        post: ImmutableHolder<Post>,
        forumName: String? = null,
    ) : this(
        post = post,
        renders = post.get { this.contentRenders },
        subPosts = post.get {
            sub_post_list?.sub_post_list?.map {
                SubPostItemData(
                    it.wrapImmutable(),
                    it.getContentText(origin_thread_info?.author?.id),
                    forumName
                )
            }?.toImmutableList() ?: persistentListOf()
        },
        result = try {
            post.get { checkBlock(forumName) }
        } catch (_: Throwable) {
            FilterResult.Allowed
        }
    )

    private constructor(
        post: ImmutableHolder<Post>,
        renders: ImmutableList<PbContentRender>,
        subPosts: ImmutableList<SubPostItemData>,
        result: FilterResult,
    ) : this(
        post = post,
        blocked = result is FilterResult.Blocked,
        contentRenders = renders,
        subPosts = subPosts,
        blockResult = result,
    )
}

@Immutable
data class SubPostItemData(
    val subPost: ImmutableHolder<SubPostList>,
    val subPostContent: AnnotatedString,
    val blocked: Boolean,
    val blockResult: FilterResult = FilterResult.Allowed,
    val id: Long = subPost.item.id,
    val authorId: Long = subPost.item.author?.id ?: 0L,
) {
    constructor(
        subPost: ImmutableHolder<SubPostList>,
        subPostContent: AnnotatedString,
        forumName: String? = null,
    ) : this(
        subPost = subPost,
        subPostContent = subPostContent,
        result = try {
            subPost.get { checkBlock(forumName) }
        } catch (_: Throwable) {
            FilterResult.Allowed
        }
    )

    private constructor(
        subPost: ImmutableHolder<SubPostList>,
        subPostContent: AnnotatedString,
        result: FilterResult,
    ) : this(
        subPost = subPost,
        subPostContent = subPostContent,
        blocked = result is FilterResult.Blocked,
        blockResult = result,
    )

    val author: ImmutableHolder<User>?
        get() = subPost.get { author }?.wrapImmutable()
}
