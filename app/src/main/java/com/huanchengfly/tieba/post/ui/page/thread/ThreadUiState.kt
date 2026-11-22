package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ThreadUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadMoreError: Boolean = false,
    val isLoadingLatestReply: Boolean = false,
    val isError: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,

    val hasMore: Boolean = true,
    val nextPagePostId: Long = 0,
    val hasPrevious: Boolean = false,
    val currentPageMin: Int = 0,
    val currentPageMax: Int = 0,
    val totalPage: Int = 0,

    val seeLz: Boolean = false,
    val sortType: Int = ThreadSortType.SORT_TYPE_DEFAULT,
    val postId: Long = 0,

    val title: String = "",
    val author: ImmutableHolder<User>? = null,
    val user: ImmutableHolder<User> = wrapImmutable(User()),
    val threadInfo: ImmutableHolder<ThreadInfo>? = null,
    val firstPost: ImmutableHolder<Post>? = null,
    val forum: ImmutableHolder<SimpleForum>? = null,
    val anti: ImmutableHolder<Anti>? = null,

    val firstPostContentRenders: ImmutableList<PbContentRender> = persistentListOf(),
    val data: ImmutableList<PostItemData> = persistentListOf(),
    val latestPosts: ImmutableList<PostItemData> = persistentListOf(),

    val isImmersiveMode: Boolean = false,
) : UiState

sealed interface ThreadUiEvent : UiEvent {
    data object ScrollToFirstReply : ThreadUiEvent

    data object ScrollToLatestReply : ThreadUiEvent

    data class LoadSuccess(
        val page: Int
    ) : ThreadUiEvent

    data class AddFavoriteSuccess(val floor: Int) : ThreadUiEvent

    data object RemoveFavoriteSuccess : ThreadUiEvent
}

object ThreadSortType {
    const val SORT_TYPE_ASC = 0
    const val SORT_TYPE_DESC = 1
    const val SORT_TYPE_HOT = 2
    const val SORT_TYPE_DEFAULT = SORT_TYPE_ASC
}
