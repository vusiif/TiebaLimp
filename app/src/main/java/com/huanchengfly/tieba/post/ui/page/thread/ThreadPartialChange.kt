package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.updateAgreeStatus
import com.huanchengfly.tieba.post.api.models.protos.updateCollectStatus
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.removeAt
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

sealed interface ThreadPartialChange : PartialChange<ThreadUiState> {
    sealed class Init : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            is Success -> oldState.copy(
                isRefreshing = true,
                isError = false,
                error = null,
                title = title,
                author = if (author != null) wrapImmutable(author) else null,
                threadInfo = threadInfo?.wrapImmutable(),
                firstPost = if (threadInfo != null && author != null)
                    wrapImmutable(
                        Post(
                            title = title,
                            author = author,
                            floor = 1,
                            time = threadInfo.createTime
                        )
                    ) else null,
                firstPostContentRenders = firstPostContentRenders.toImmutableList(),
                postId = postId,
                seeLz = seeLz,
                sortType = sortType,
            )

            is Failure -> oldState.copy(
                isError = true,
                error = error.wrapImmutable()
            )
        }

        data class Success(
            val title: String,
            val author: User?,
            val threadInfo: ThreadInfo?,
            val firstPostContentRenders: List<PbContentRender>,
            val postId: Long = 0,
            val seeLz: Boolean = false,
            val sortType: Int = 0,
        ) : Init()

        data class Failure(
            val error: Throwable
        ) : Init()
    }

    sealed class Load : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            is Start -> oldState.copy(isRefreshing = true, isLoadMoreError = false)

            is Success -> oldState.copy(
                isRefreshing = false,
                isError = false,
                isLoadMoreError = false,
                error = null,
                title = title,
                author = wrapImmutable(author),
                user = wrapImmutable(user),
                data = data.toImmutableList(),
                threadInfo = threadInfo.wrapImmutable(),
                firstPost = if (firstPost != null) wrapImmutable(firstPost) else oldState.firstPost,
                forum = wrapImmutable(forum),
                anti = wrapImmutable(anti),
                currentPageMin = currentPage,
                currentPageMax = currentPage,
                totalPage = totalPage,
                hasMore = hasMore,
                nextPagePostId = nextPagePostId,
                hasPrevious = hasPrevious,
                firstPostContentRenders = firstPostContentRenders?.toImmutableList()
                    ?: oldState.firstPostContentRenders,
                latestPosts = persistentListOf(),
                postId = postId,
                seeLz = seeLz,
                sortType = sortType,
            )

            is Failure -> oldState.copy(
                isRefreshing = false,
                isError = true,
                error = error.wrapImmutable()
            )
        }

        data object Start : Load()

        data class Success(
            val title: String,
            val author: User,
            val user: User,
            val firstPost: Post?,
            val data: List<PostItemData>,
            val threadInfo: ThreadInfo,
            val forum: SimpleForum,
            val anti: Anti,
            val currentPage: Int,
            val totalPage: Int,
            val hasMore: Boolean,
            val nextPagePostId: Long,
            val hasPrevious: Boolean,
            val firstPostContentRenders: List<PbContentRender>?,
            val postId: Long = 0,
            val seeLz: Boolean = false,
            val sortType: Int = 0,
        ) : Load()

        data class Failure(
            val error: Throwable,
        ) : Load()
    }

    sealed class LoadFirstPage : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            is Start -> oldState.copy(isRefreshing = true)
            is Success -> oldState.copy(
                isRefreshing = false,
                isError = false,
                error = null,
                title = title,
                author = wrapImmutable(author),
                data = data.toImmutableList(),
                threadInfo = threadInfo.wrapImmutable(),
                currentPageMin = currentPage,
                currentPageMax = currentPage,
                totalPage = totalPage,
                hasMore = hasMore,
                nextPagePostId = nextPagePostId,
                hasPrevious = hasPrevious,
                firstPostContentRenders = firstPostContentRenders.toImmutableList(),
                latestPosts = persistentListOf(),
                postId = postId,
                seeLz = seeLz,
                sortType = sortType,
            )

            is Failure -> oldState.copy(
                isRefreshing = false,
                isError = true,
                error = error.wrapImmutable(),
            )
        }

        data object Start : LoadFirstPage()

        data class Success(
            val title: String,
            val author: User,
            val data: List<PostItemData>,
            val threadInfo: ThreadInfo,
            val currentPage: Int,
            val totalPage: Int,
            val hasMore: Boolean,
            val nextPagePostId: Long,
            val hasPrevious: Boolean,
            val firstPostContentRenders: List<PbContentRender>,
            val postId: Long,
            val seeLz: Boolean,
            val sortType: Int,
        ) : LoadFirstPage()

        data class Failure(
            val error: Throwable
        ) : LoadFirstPage()
    }

    sealed class LoadMore : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            is Start -> oldState.copy(isLoadingMore = true, isLoadMoreError = false)
            is Success -> {
                val uniqueData = data.filterNot { item ->
                    oldState.data.any { it.post.get { id } == item.post.get { id } }
                }
                oldState.copy(
                    isLoadingMore = false,
                    isLoadMoreError = false,
                    author = wrapImmutable(author),
                    data = (oldState.data + uniqueData).toImmutableList(),
                    threadInfo = threadInfo.wrapImmutable(),
                    currentPageMax = currentPage,
                    totalPage = totalPage,
                    hasMore = hasMore,
                    nextPagePostId = nextPagePostId,
                    latestPosts = persistentListOf(),
                )
            }

            is Failure -> oldState.copy(isLoadingMore = false, isLoadMoreError = true)
        }

        data object Start : LoadMore()

        data class Success(
            val author: User,
            val data: List<PostItemData>,
            val threadInfo: ThreadInfo,
            val currentPage: Int,
            val totalPage: Int,
            val hasMore: Boolean,
            val nextPagePostId: Long,
        ) : LoadMore()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String
        ) : LoadMore()
    }

    sealed class LoadPrevious : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            is Start -> oldState.copy(isRefreshing = true)
            is Success -> oldState.copy(
                isRefreshing = false,
                author = wrapImmutable(author),
                data = (data + oldState.data).toImmutableList(),
                threadInfo = threadInfo.wrapImmutable(),
                currentPageMin = currentPage,
                totalPage = totalPage,
                hasPrevious = hasPrevious,
            )

            is Failure -> oldState.copy(isRefreshing = false)
        }

        data object Start : LoadPrevious()

        data class Success(
            val author: User,
            val data: List<PostItemData>,
            val threadInfo: ThreadInfo,
            val currentPage: Int,
            val totalPage: Int,
            val hasPrevious: Boolean,
        ) : LoadPrevious()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String,
        ) : LoadPrevious()
    }

    sealed class LoadLatestPosts : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            Start -> oldState.copy(isLoadingMore = true, isLoadMoreError = false)
            is Success -> {
                val uniqueData = data.filterNot { item ->
                    oldState.data.any { it.post.get { id } == item.post.get { id } }
                }
                oldState.copy(
                    isLoadingMore = false,
                    isLoadMoreError = false,
                    author = wrapImmutable(author),
                    data = (oldState.data + uniqueData).toImmutableList(),
                    threadInfo = threadInfo.wrapImmutable(),
                    currentPageMax = currentPage,
                    totalPage = totalPage,
                    hasMore = hasMore,
                    nextPagePostId = nextPagePostId,
                    latestPosts = persistentListOf(),
                )
            }

            SuccessWithNoNewPost -> oldState.copy(isLoadingMore = false, isLoadMoreError = false)
            is Failure -> oldState.copy(isLoadingMore = false, isLoadMoreError = true)
        }

        data object Start : LoadLatestPosts()

        data class Success(
            val author: User,
            val data: List<PostItemData>,
            val threadInfo: ThreadInfo,
            val currentPage: Int,
            val totalPage: Int,
            val hasMore: Boolean,
            val nextPagePostId: Long,
        ) : LoadLatestPosts()

        data object SuccessWithNoNewPost : LoadLatestPosts()

        data class Failure(
            val error: Throwable,
        ) : LoadLatestPosts()
    }

    sealed class LoadMyLatestReply : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState =
            when (this) {
                Start -> oldState.copy(isLoadingLatestReply = true)
                is Success -> {
                    val continuous = isContinuous || page == oldState.currentPageMax
                    val replacePostIndexes = oldState.data.mapIndexedNotNull { index, item ->
                        val replaceItemIndex =
                            posts.indexOfFirst { it.post.get { id } == item.post.get { id } }
                        if (replaceItemIndex != -1) index to replaceItemIndex else null
                    }
                    val newPost = oldState.data.mapIndexed { index, oldItem ->
                        val replaceIndex = replacePostIndexes.firstOrNull { it.first == index }
                        if (replaceIndex != null) posts[replaceIndex.second] else oldItem
                    }
                    val addPosts = posts.filter {
                        !newPost.any { item -> item.post.get { id } == it.post.get { id } }
                    }
                    when {
                        hasNewPost && continuous && isDesc -> {
                            oldState.copy(
                                isLoadingLatestReply = false,
                                isError = false,
                                error = null,
                                anti = anti.wrapImmutable(),
                                data = (addPosts.reversed() + newPost).toImmutableList(),
                                latestPosts = persistentListOf(),
                            )
                        }

                        hasNewPost && continuous && !isDesc -> {
                            oldState.copy(
                                isLoadingLatestReply = false,
                                isError = false,
                                error = null,
                                anti = anti.wrapImmutable(),
                                data = (newPost + addPosts).toImmutableList(),
                                latestPosts = persistentListOf(),
                            )
                        }

                        hasNewPost -> {
                            oldState.copy(
                                isLoadingLatestReply = false,
                                isError = false,
                                error = null,
                                anti = anti.wrapImmutable(),
                                data = newPost.toImmutableList(),
                                latestPosts = posts.toImmutableList(),
                            )
                        }

                        !hasNewPost -> {
                            oldState.copy(
                                isLoadingLatestReply = false,
                                isError = false,
                                error = null,
                                anti = anti.wrapImmutable(),
                                data = newPost.toImmutableList(),
                                latestPosts = persistentListOf(),
                            )
                        }

                        else -> {
                            oldState.copy(
                                isLoadingLatestReply = false,
                                isError = false,
                                error = null,
                            )
                        }
                    }
                }

                is Failure -> oldState.copy(
                    isLoadingLatestReply = false,
                    isError = true,
                    error = error.wrapImmutable(),
                )
            }

        object Start : LoadMyLatestReply()

        data class Success(
            val anti: Anti,
            val posts: List<PostItemData>,
            val page: Int,
            val isContinuous: Boolean,
            val isDesc: Boolean,
            val hasNewPost: Boolean,
        ) : LoadMyLatestReply()

        data class Failure(
            val error: Throwable,
        ) : LoadMyLatestReply()
    }

    sealed class ToggleImmersiveMode : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            is Success -> oldState.copy(isImmersiveMode = isImmersiveMode)
        }

        data class Success(
            val isImmersiveMode: Boolean
        ) : ToggleImmersiveMode()
    }

    sealed class AddFavorite : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState {
            return when (this) {
                Start -> oldState
                is Success -> oldState.copy(
                    threadInfo = oldState.threadInfo?.getImmutable {
                        updateCollectStatus(
                            newStatus = 1,
                            markPostId = markPostId
                        )
                    }
                )

                is Failure -> oldState
            }
        }

        object Start : AddFavorite()

        data class Success(
            val markPostId: Long,
            val floor: Int
        ) : AddFavorite()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String
        ) : AddFavorite()
    }

    sealed class RemoveFavorite : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState {
            return when (this) {
                Start -> oldState
                Success -> oldState.copy(
                    threadInfo = oldState.threadInfo?.getImmutable {
                        updateCollectStatus(
                            newStatus = 0,
                            markPostId = 0
                        )
                    }
                )

                is Failure -> oldState
            }
        }

        object Start : RemoveFavorite()

        object Success : RemoveFavorite()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String
        ) : RemoveFavorite()
    }

    sealed class AgreeThread : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState {
            return when (this) {
                is Start -> oldState.copy(
                    threadInfo = oldState.threadInfo?.getImmutable {
                        updateAgreeStatus(hasAgree = if (hasAgree) 1 else 0)
                    }
                )

                is Success -> oldState.copy(
                    threadInfo = oldState.threadInfo?.getImmutable {
                        updateAgreeStatus(hasAgree = if (hasAgree) 1 else 0)
                    }
                )

                is Failure -> oldState.copy(
                    threadInfo = oldState.threadInfo?.getImmutable {
                        updateAgreeStatus(hasAgree = if (hasAgree) 1 else 0)
                    }
                )
            }
        }

        data class Start(
            val hasAgree: Boolean
        ) : AgreeThread()

        data class Success(
            val hasAgree: Boolean
        ) : AgreeThread()

        data class Failure(
            val hasAgree: Boolean,
            val errorCode: Int,
            val errorMessage: String
        ) : AgreeThread()
    }

    sealed class AgreePost : ThreadPartialChange {
        private fun List<PostItemData>.updateAgreeStatus(
            postId: Long,
            hasAgree: Int
        ): ImmutableList<PostItemData> {
            return map { item ->
                val (holder) = item
                val (post) = holder
                if (post.id == postId) {
                    item.copy(
                        post = post.updateAgreeStatus(hasAgree).wrapImmutable()
                    )
                } else item
            }.toImmutableList()
        }

        override fun reduce(oldState: ThreadUiState): ThreadUiState {
            return when (this) {
                is Start -> oldState.copy(
                    data = oldState.data.updateAgreeStatus(postId, if (hasAgree) 1 else 0)
                )

                is Success -> oldState.copy(
                    data = oldState.data.updateAgreeStatus(postId, if (hasAgree) 1 else 0)
                )

                is Failure -> oldState.copy(
                    data = oldState.data.updateAgreeStatus(postId, if (hasAgree) 1 else 0)
                )
            }
        }

        data class Start(
            val postId: Long,
            val hasAgree: Boolean
        ) : AgreePost()

        data class Success(
            val postId: Long,
            val hasAgree: Boolean
        ) : AgreePost()

        data class Failure(
            val postId: Long,
            val hasAgree: Boolean,
            val errorCode: Int,
            val errorMessage: String
        ) : AgreePost()
    }

    sealed class DeletePost : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = when (this) {
            is Success -> {
                val deletedPostIndex = oldState.data.indexOfFirst { it.post.get { id } == postId }
                oldState.copy(
                    data = oldState.data.removeAt(deletedPostIndex),
                )
            }

            is Failure -> oldState
        }

        data class Success(
            val postId: Long
        ) : DeletePost()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String
        ) : DeletePost()
    }

    sealed class DeleteThread : ThreadPartialChange {
        override fun reduce(oldState: ThreadUiState): ThreadUiState = oldState

        object Success : DeleteThread()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String
        ) : DeleteThread()
    }
}
