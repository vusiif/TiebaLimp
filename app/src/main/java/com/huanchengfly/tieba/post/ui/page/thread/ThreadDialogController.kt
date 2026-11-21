package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.models.database.FloorBookmark
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadSearchHit
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.FloorBookmarkUtil
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Controller managing interactive dialog states and operations in ThreadPage.
 */
@Stable
class ThreadDialogController(
    val threadId: Long,
    val curForumId: Long?,
    val curForumName: String?,
    val curTbs: String?,
    val author: ImmutableHolder<User>?,
    val user: ImmutableHolder<User>,
    val viewModel: ThreadViewModel,
    val navigator: DestinationsNavigator,
    val coroutineScope: CoroutineScope,
    val jumpController: ThreadJumpController,
    val updateCollectMarkDialogState: DialogState,
    val confirmDeleteDialogState: DialogState,
    val bookmarkEditDialogState: DialogState,
    val bookmarksDialogState: DialogState,
    val searchJumpDialogState: DialogState,
) {
    var readFloorBeforeBack by mutableIntStateOf(1)
    var deletePost by mutableStateOf<ImmutableHolder<Post>?>(null)

    var bookmarkTargetPost by mutableStateOf<Post?>(null)
    var bookmarkInitialNote by mutableStateOf("")
    var bookmarkIsExisting by mutableStateOf(false)
    var threadBookmarks by mutableStateOf<List<FloorBookmark>>(emptyList())

    fun openCollectMarkDialog(floor: Int) {
        readFloorBeforeBack = floor
        updateCollectMarkDialogState.show()
    }

    fun openDeleteThread() {
        deletePost = null
        confirmDeleteDialogState.show()
    }

    fun openDeletePost(post: ImmutableHolder<Post>?) {
        deletePost = post
        confirmDeleteDialogState.show()
    }

    fun openBookmarkEdit(post: Post) {
        bookmarkTargetPost = post
        coroutineScope.launch {
            val existing = FloorBookmarkUtil.findBookmark(threadId, post.id)
            bookmarkIsExisting = existing != null
            bookmarkInitialNote = existing?.note.orEmpty()
            bookmarkEditDialogState.show()
        }
    }

    fun openBookmarks() {
        coroutineScope.launch {
            threadBookmarks = FloorBookmarkUtil.getBookmarks(threadId)
            bookmarksDialogState.show()
        }
    }

    fun openSearchJumpDialog() {
        searchJumpDialogState.show()
    }

    fun refreshBookmarks() {
        coroutineScope.launch {
            threadBookmarks = FloorBookmarkUtil.getBookmarks(threadId)
        }
    }

    fun onDeleteThread() {
        val forumId = curForumId ?: return
        val isSelfThread = author?.get { id } == user.get { id }
        viewModel.send(
            ThreadUiIntent.DeleteThread(
                forumId = forumId,
                forumName = curForumName.orEmpty(),
                threadId = threadId,
                deleteMyThread = isSelfThread,
                tbs = curTbs
            )
        )
    }

    fun onDeletePost(postId: Long) {
        val forumId = curForumId ?: return
        val isSelfPost = deletePost?.get { author_id } == user.get { id }
        viewModel.send(
            ThreadUiIntent.DeletePost(
                forumId = forumId,
                forumName = curForumName.orEmpty(),
                threadId = threadId,
                postId = postId,
                deleteMyPost = isSelfPost,
                tbs = curTbs
            )
        )
    }

    fun onJumpToPage(targetPage: Int, forumId: Long?, isSeeLz: Boolean, curSortType: Int) {
        viewModel.send(
            ThreadUiIntent.Load(
                threadId = threadId,
                forumId = forumId,
                page = targetPage,
                seeLz = isSeeLz,
                sortType = curSortType
            )
        )
    }

    fun onSelectSearchHit(hit: ThreadSearchHit, query: String, hits: List<ThreadSearchHit>) {
        jumpController.activeSearchKeyword = query
        jumpController.activeSearchHits = hits
        jumpController.currentHitIndex = hits.indexOf(hit).coerceAtLeast(0)
        jumpController.onJumpToHit(hit)
    }
}

@Composable
fun rememberThreadDialogController(
    threadId: Long,
    curForumId: Long?,
    curForumName: String?,
    curTbs: String?,
    author: ImmutableHolder<User>?,
    user: ImmutableHolder<User>,
    viewModel: ThreadViewModel,
    navigator: DestinationsNavigator,
    jumpController: ThreadJumpController,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): ThreadDialogController {
    val updateCollectMarkDialogState = rememberDialogState()
    val confirmDeleteDialogState = rememberDialogState()
    val bookmarkEditDialogState = rememberDialogState()
    val bookmarksDialogState = rememberDialogState()
    val searchJumpDialogState = rememberDialogState()

    return remember(
        threadId,
        curForumId,
        curForumName,
        curTbs,
        author,
        user,
        viewModel,
        navigator,
        jumpController
    ) {
        ThreadDialogController(
            threadId = threadId,
            curForumId = curForumId,
            curForumName = curForumName,
            curTbs = curTbs,
            author = author,
            user = user,
            viewModel = viewModel,
            navigator = navigator,
            coroutineScope = coroutineScope,
            jumpController = jumpController,
            updateCollectMarkDialogState = updateCollectMarkDialogState,
            confirmDeleteDialogState = confirmDeleteDialogState,
            bookmarkEditDialogState = bookmarkEditDialogState,
            bookmarksDialogState = bookmarksDialogState,
            searchJumpDialogState = searchJumpDialogState,
        )
    }
}

/**
 * Container composable rendering all interactive dialogs managed by [ThreadDialogController].
 */
@Composable
fun ThreadPageDialogContainer(
    dialogController: ThreadDialogController,
    lastVisibilityPostId: Long,
    threadTitle: String,
    totalFloors: Int,
    currentPage: Int,
    totalPage: Int,
    firstPost: Post?,
    loadedPosts: List<PostItemData>,
    forumId: Long?,
    isSeeLz: Boolean,
    curSortType: Int,
) {
    ThreadPageDialogs(
        updateCollectMarkDialogState = dialogController.updateCollectMarkDialogState,
        readFloorBeforeBack = dialogController.readFloorBeforeBack,
        threadId = dialogController.threadId,
        lastVisibilityPostId = lastVisibilityPostId,
        onNavUp = { dialogController.navigator.navigateUp() },
        confirmDeleteDialogState = dialogController.confirmDeleteDialogState,
        deletePost = dialogController.deletePost,
        curForumId = dialogController.curForumId,
        onDeleteThread = dialogController::onDeleteThread,
        onDeletePost = dialogController::onDeletePost,
        bookmarkEditDialogState = dialogController.bookmarkEditDialogState,
        bookmarksDialogState = dialogController.bookmarksDialogState,
        bookmarkTargetPost = dialogController.bookmarkTargetPost,
        bookmarkInitialNote = dialogController.bookmarkInitialNote,
        bookmarkIsExisting = dialogController.bookmarkIsExisting,
        threadBookmarks = dialogController.threadBookmarks,
        threadTitle = threadTitle,
        onBookmarksChanged = dialogController::refreshBookmarks,
        onSelectBookmark = { bookmark ->
            dialogController.jumpController.onSelectBookmark(bookmark, firstPost?.id ?: 0L)
        },
        searchJumpDialogState = dialogController.searchJumpDialogState,
        totalFloors = totalFloors,
        currentPage = currentPage,
        totalPage = totalPage,
        firstPost = firstPost,
        loadedPosts = loadedPosts,
        onJumpToFloor = dialogController.jumpController::onJumpToFloor,
        onJumpToPage = { targetPage ->
            dialogController.onJumpToPage(targetPage, forumId, isSeeLz, curSortType)
        },
        onSelectSearchHit = dialogController::onSelectSearchHit
    )
}
