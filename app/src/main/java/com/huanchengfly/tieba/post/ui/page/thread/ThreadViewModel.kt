package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.repository.EmptyDataException
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

private fun ThreadInfo.getNextPagePostId(
    postIds: List<Long> = emptyList(),
    sortType: Int = ThreadSortType.SORT_TYPE_DEFAULT
): Long {
    val fetchedPostIds = pids.split(",")
        .mapNotNull { it.trim().toLongOrNull() }
    if (sortType == ThreadSortType.SORT_TYPE_DESC) {
        return fetchedPostIds.firstOrNull() ?: 0
    }
    val nextPostIds = fetchedPostIds.filterNot { pid -> postIds.contains(pid) }
    return if (nextPostIds.isNotEmpty()) nextPostIds.last() else 0
}

@Stable
@HiltViewModel
class ThreadViewModel @Inject constructor() :
    BaseViewModel<ThreadUiIntent, ThreadPartialChange, ThreadUiState, ThreadUiEvent>() {
    override fun createInitialState(): ThreadUiState = ThreadUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ThreadUiIntent, ThreadPartialChange, ThreadUiState> =
        ThreadPartialChangeProducer

    override fun dispatchEvent(partialChange: ThreadPartialChange): UiEvent? {
        return when (partialChange) {
            is ThreadPartialChange.Init.Success -> if (partialChange.postId != 0L) ThreadUiEvent.ScrollToFirstReply else null
            ThreadPartialChange.LoadPrevious.Start -> ThreadUiEvent.ScrollToFirstReply
            is ThreadPartialChange.AddFavorite.Success -> ThreadUiEvent.AddFavoriteSuccess(
                partialChange.floor
            )

            ThreadPartialChange.RemoveFavorite.Success -> ThreadUiEvent.RemoveFavoriteSuccess
            is ThreadPartialChange.Load.Success -> ThreadUiEvent.LoadSuccess(partialChange.currentPage)

            is ThreadPartialChange.LoadMyLatestReply.Success -> ThreadUiEvent.ScrollToLatestReply.takeIf {
                partialChange.hasNewPost
            }

            is ThreadPartialChange.DeletePost.Success -> CommonUiEvent.Toast(
                App.INSTANCE.getString(R.string.toast_delete_success)
            )

            is ThreadPartialChange.DeletePost.Failure -> CommonUiEvent.Toast(
                App.INSTANCE.getString(R.string.toast_delete_failure, partialChange.errorMessage)
            )

            is ThreadPartialChange.DeleteThread.Success -> CommonUiEvent.NavigateUp
            is ThreadPartialChange.DeleteThread.Failure -> CommonUiEvent.Toast(
                App.INSTANCE.getString(R.string.toast_delete_failure, partialChange.errorMessage)
            )

            else -> null
        }
    }

    private object ThreadPartialChangeProducer :
        PartialChangeProducer<ThreadUiIntent, ThreadPartialChange, ThreadUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<ThreadUiIntent>): Flow<ThreadPartialChange> =
            merge(
                intentFlow.filterIsInstance<ThreadUiIntent.Init>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.Load>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.LoadMore>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.LoadFirstPage>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.LoadPrevious>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.LoadLatestPosts>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.LoadMyLatestReply>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.ToggleImmersiveMode>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.AddFavorite>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.RemoveFavorite>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.AgreeThread>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.AgreePost>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.DeletePost>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ThreadUiIntent.DeleteThread>()
                    .flatMapConcat { it.producePartialChange() },
            )

        fun ThreadUiIntent.Init.producePartialChange(): Flow<ThreadPartialChange.Init> =
            flowOf<ThreadPartialChange.Init>(
                ThreadPartialChange.Init.Success(
                    threadInfo?.title.orEmpty(),
                    threadInfo?.author,
                    threadInfo,
                    threadInfo?.firstPostContent?.renders ?: emptyList(),
                    postId,
                    seeLz,
                    sortType,
                )
            ).catch { emit(ThreadPartialChange.Init.Failure(it)) }

        fun ThreadUiIntent.Load.producePartialChange(): Flow<ThreadPartialChange.Load> =
            PbPageRepository
                .pbPage(
                    threadId, page, postId, forumId, seeLz, sortType,
                    from = from.takeIf { it == ThreadPageFrom.FROM_STORE }.orEmpty()
                )
                .map<PbPageResponse, ThreadPartialChange.Load> { response ->
                    if (response.data_?.page == null
                        || response.data_.thread?.author == null
                        || response.data_.forum == null
                        || response.data_.anti == null
                    ) throw TiebaUnknownException
                    val postList = response.data_.post_list
                    val firstPost = response.data_.first_floor_post
                    val notFirstPosts = postList.filterNot { it.floor == 1 }
                    val forumName = response.data_.forum.name
                    ThreadPartialChange.Load.Success(
                        response.data_.thread.title,
                        response.data_.thread.author,
                        response.data_.user ?: User(),
                        firstPost,
                        notFirstPosts.map { PostItemData(it.wrapImmutable(), forumName) },
                        response.data_.thread,
                        response.data_.forum,
                        response.data_.anti,
                        response.data_.page.current_page,
                        response.data_.page.new_total_page,
                        response.data_.page.has_more != 0,
                        response.data_.thread.getNextPagePostId(
                            postList.map { it.id },
                            sortType
                        ),
                        response.data_.page.has_prev != 0,
                        firstPost?.contentRenders,
                        postId,
                        seeLz,
                        sortType,
                    )
                }
                .onStart { emit(ThreadPartialChange.Load.Start) }
                .catch { emit(ThreadPartialChange.Load.Failure(it)) }

        fun ThreadUiIntent.LoadFirstPage.producePartialChange(): Flow<ThreadPartialChange.LoadFirstPage> =
            PbPageRepository
                .pbPage(threadId, 0, 0, forumId, seeLz, sortType)
                .map<PbPageResponse, ThreadPartialChange.LoadFirstPage> { response ->
                    if (response.data_?.page == null
                        || response.data_.thread?.author == null
                        || response.data_.forum == null
                        || response.data_.anti == null
                    ) throw TiebaUnknownException
                    val postList = response.data_.post_list
                    val firstPost = response.data_.first_floor_post
                    val notFirstPosts = postList.filterNot { it.floor == 1 }
                    val forumName = response.data_.forum.name
                    ThreadPartialChange.LoadFirstPage.Success(
                        response.data_.thread.title,
                        response.data_.thread.author,
                        notFirstPosts.map { PostItemData(it.wrapImmutable(), forumName) },
                        response.data_.thread,
                        response.data_.page.current_page,
                        response.data_.page.new_total_page,
                        response.data_.page.has_more != 0,
                        response.data_.thread.getNextPagePostId(
                            postList.map { it.id },
                            sortType
                        ),
                        response.data_.page.has_prev != 0,
                        firstPost?.contentRenders ?: emptyList(),
                        postId = 0,
                        seeLz,
                        sortType,
                    )
                }
                .onStart { emit(ThreadPartialChange.LoadFirstPage.Start) }
                .catch { emit(ThreadPartialChange.LoadFirstPage.Failure(it)) }

        fun ThreadUiIntent.LoadMore.producePartialChange(): Flow<ThreadPartialChange.LoadMore> =
            PbPageRepository
                .pbPage(threadId, page, postId, forumId, seeLz, sortType)
                .map<PbPageResponse, ThreadPartialChange.LoadMore> { response ->
                    if (response.data_?.page == null
                        || response.data_.thread?.author == null
                        || response.data_.forum == null
                        || response.data_.anti == null
                    ) throw TiebaUnknownException
                    val postList = response.data_.post_list
                    val posts = postList.filterNot { it.floor == 1 || postIds.contains(it.id) }
                    val forumName = response.data_.forum.name
                    ThreadPartialChange.LoadMore.Success(
                        response.data_.thread.author,
                        posts.map { PostItemData(it.wrapImmutable(), forumName) },
                        response.data_.thread,
                        response.data_.page.current_page,
                        response.data_.page.new_total_page,
                        response.data_.page.has_more != 0,
                        response.data_.thread.getNextPagePostId(
                            postIds + posts.map { it.id },
                            sortType
                        ),
                    )
                }
                .onStart { emit(ThreadPartialChange.LoadMore.Start) }
                .catch {
                    emit(
                        ThreadPartialChange.LoadMore.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        fun ThreadUiIntent.LoadPrevious.producePartialChange(): Flow<ThreadPartialChange.LoadPrevious> =
            PbPageRepository
                .pbPage(threadId, page, postId, forumId, seeLz, sortType, back = true)
                .map<PbPageResponse, ThreadPartialChange.LoadPrevious> { response ->
                    if (response.data_?.page == null
                        || response.data_.thread?.author == null
                        || response.data_.forum == null
                        || response.data_.anti == null
                    ) throw TiebaUnknownException
                    val postList = response.data_.post_list
                    val posts = postList.filterNot { it.floor == 1 || postIds.contains(it.id) }
                    val forumName = response.data_.forum.name
                    ThreadPartialChange.LoadPrevious.Success(
                        response.data_.thread.author,
                        posts.map { PostItemData(it.wrapImmutable(), forumName) },
                        response.data_.thread,
                        response.data_.page.current_page,
                        response.data_.page.new_total_page,
                        response.data_.page.has_prev != 0,
                    )
                }
                .onStart { emit(ThreadPartialChange.LoadPrevious.Start) }
                .catch {
                    emit(
                        ThreadPartialChange.LoadPrevious.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        fun ThreadUiIntent.LoadLatestPosts.producePartialChange(): Flow<ThreadPartialChange.LoadLatestPosts> =
            PbPageRepository
                .pbPage(
                    threadId = threadId,
                    page = 0,
                    postId = curLatestPostId,
                    forumId = forumId,
                    seeLz = seeLz,
                    sortType = sortType,
                    lastPostId = curLatestPostId
                )
                .map { response ->
                    checkNotNull(response.data_)
                    checkNotNull(response.data_.thread)
                    checkNotNull(response.data_.thread.author)
                    checkNotNull(response.data_.page)
                    val postList = response.data_.post_list.filterNot { it.floor == 1 }
                    val forumName = response.data_.thread.forumName
                    if (postList.isEmpty()) {
                        ThreadPartialChange.LoadLatestPosts.SuccessWithNoNewPost
                    } else {
                        ThreadPartialChange.LoadLatestPosts.Success(
                            author = response.data_.thread.author,
                            data = postList.map { PostItemData(it.wrapImmutable(), forumName) },
                            threadInfo = response.data_.thread,
                            currentPage = response.data_.page.current_page,
                            totalPage = response.data_.page.new_total_page,
                            hasMore = response.data_.page.has_more != 0,
                            nextPagePostId = response.data_.thread.getNextPagePostId(
                                postList.map { it.id },
                                sortType
                            ),
                        )
                    }
                }
                .onStart { emit(ThreadPartialChange.LoadLatestPosts.Start) }
                .catch {
                    if (it is EmptyDataException) {
                        emit(ThreadPartialChange.LoadLatestPosts.SuccessWithNoNewPost)
                    } else {
                        emit(ThreadPartialChange.LoadLatestPosts.Failure(it))
                    }
                }

        fun ThreadUiIntent.LoadMyLatestReply.producePartialChange(): Flow<ThreadPartialChange.LoadMyLatestReply> =
            PbPageRepository
                .pbPage(threadId, page = 0, postId = postId, forumId = forumId)
                .map<PbPageResponse, ThreadPartialChange.LoadMyLatestReply> { response ->
                    if (response.data_?.page == null
                        || response.data_.thread?.author == null
                        || response.data_.forum == null
                        || response.data_.anti == null
                    ) throw TiebaUnknownException
                    val firstLatestPost = response.data_.post_list.first()
                    val forumName = response.data_.forum.name
                    ThreadPartialChange.LoadMyLatestReply.Success(
                        anti = response.data_.anti,
                        posts = response.data_.post_list.map { PostItemData(it.wrapImmutable(), forumName) },
                        page = response.data_.page.current_page,
                        isContinuous = firstLatestPost.floor == curLatestPostFloor + 1,
                        isDesc = isDesc,
                        hasNewPost = response.data_.post_list.any { !curPostIds.contains(it.id) },
                    )
                }
                .onStart { emit(ThreadPartialChange.LoadMyLatestReply.Start) }
                .catch { emit(ThreadPartialChange.LoadMyLatestReply.Failure(it)) }

        fun ThreadUiIntent.ToggleImmersiveMode.producePartialChange(): Flow<ThreadPartialChange.ToggleImmersiveMode> =
            flowOf(ThreadPartialChange.ToggleImmersiveMode.Success(isImmersiveMode))

        fun ThreadUiIntent.AddFavorite.producePartialChange(): Flow<ThreadPartialChange.AddFavorite> =
            TiebaApi.getInstance()
                .addStoreFlow(threadId, postId)
                .map { response ->
                    if (response.errorCode == 0) {
                        ThreadPartialChange.AddFavorite.Success(
                            postId, floor
                        )
                    } else ThreadPartialChange.AddFavorite.Failure(
                        response.errorCode,
                        response.errorMsg
                    )
                }
                .onStart { emit(ThreadPartialChange.AddFavorite.Start) }
                .catch {
                    emit(
                        ThreadPartialChange.AddFavorite.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        fun ThreadUiIntent.RemoveFavorite.producePartialChange(): Flow<ThreadPartialChange.RemoveFavorite> =
            TiebaApi.getInstance()
                .removeStoreFlow(threadId, forumId, tbs)
                .map { response ->
                    if (response.errorCode == 0) {
                        ThreadPartialChange.RemoveFavorite.Success
                    } else ThreadPartialChange.RemoveFavorite.Failure(
                        response.errorCode,
                        response.errorMsg
                    )
                }
                .onStart { emit(ThreadPartialChange.RemoveFavorite.Start) }
                .catch {
                    emit(
                        ThreadPartialChange.RemoveFavorite.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        fun ThreadUiIntent.AgreeThread.producePartialChange(): Flow<ThreadPartialChange.AgreeThread> =
            TiebaApi.getInstance()
                .opAgreeFlow(
                    threadId.toString(),
                    postId.toString(),
                    opType = if (agree) 0 else 1,
                    objType = 3
                )
                .map<AgreeBean, ThreadPartialChange.AgreeThread> {
                    ThreadPartialChange.AgreeThread.Success(
                        agree
                    )
                }
                .onStart { emit(ThreadPartialChange.AgreeThread.Start(agree)) }
                .catch {
                    emit(
                        ThreadPartialChange.AgreeThread.Failure(
                            !agree,
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        fun ThreadUiIntent.AgreePost.producePartialChange(): Flow<ThreadPartialChange.AgreePost> =
            TiebaApi.getInstance()
                .opAgreeFlow(
                    threadId.toString(),
                    postId.toString(),
                    if (agree) 0 else 1,
                    objType = 1
                )
                .map<AgreeBean, ThreadPartialChange.AgreePost> {
                    ThreadPartialChange.AgreePost.Success(
                        postId,
                        agree
                    )
                }
                .onStart { emit(ThreadPartialChange.AgreePost.Start(postId, agree)) }
                .catch {
                    emit(
                        ThreadPartialChange.AgreePost.Failure(
                            postId,
                            !agree,
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        fun ThreadUiIntent.DeletePost.producePartialChange(): Flow<ThreadPartialChange.DeletePost> =
            TiebaApi.getInstance()
                .delPostFlow(forumId, forumName, threadId, postId, tbs, false, deleteMyPost)
                .map<CommonResponse, ThreadPartialChange.DeletePost> {
                    ThreadPartialChange.DeletePost.Success(postId)
                }
                .catch {
                    emit(
                        ThreadPartialChange.DeletePost.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        fun ThreadUiIntent.DeleteThread.producePartialChange(): Flow<ThreadPartialChange.DeleteThread> =
            TiebaApi.getInstance()
                .delThreadFlow(forumId, forumName, threadId, tbs, deleteMyThread, false)
                .map<CommonResponse, ThreadPartialChange.DeleteThread> {
                    ThreadPartialChange.DeleteThread.Success
                }
                .catch {
                    emit(
                        ThreadPartialChange.DeleteThread.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }
    }
}

