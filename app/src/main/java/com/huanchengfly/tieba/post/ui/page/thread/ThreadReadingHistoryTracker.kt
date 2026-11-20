package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Stable
class ThreadReadingProgressState {
    var readingProgress by mutableStateOf<ThreadHistoryInfoBean?>(null)
    var showResumePill by mutableStateOf(false)

    fun dismiss() {
        showResumePill = false
    }
}

@Composable
fun rememberThreadReadingProgress(
    threadId: Long,
    postId: Long,
    currentFloor: Int,
): ThreadReadingProgressState {
    val state = remember(threadId) { ThreadReadingProgressState() }

    LaunchedEffect(threadId) {
        if (postId == 0L) {
            val progress = withContext(Dispatchers.IO) {
                runCatching { HistoryUtil.getThreadProgress(threadId) }.getOrNull()
            }
            val floor = progress?.floor?.toIntOrNull() ?: 0
            val pid = progress?.pid?.toLongOrNull() ?: 0L
            if (floor > 1 && pid > 0L) {
                state.readingProgress = progress
                state.showResumePill = true
            }
        }
    }

    LaunchedEffect(state.showResumePill) {
        if (state.showResumePill) {
            delay(8000)
            state.showResumePill = false
        }
    }

    LaunchedEffect(currentFloor) {
        if (state.showResumePill) {
            val targetFloor = state.readingProgress?.floor?.toIntOrNull() ?: 0
            if (currentFloor > 5 || (targetFloor > 0 && currentFloor >= targetFloor)) {
                state.showResumePill = false
            }
        }
    }

    return state
}

@Composable
fun ThreadReadingHistoryTracker(
    threadId: Long,
    threadTitle: String,
    author: ImmutableHolder<User>?,
    forumName: String?,
    currentFloor: Int?,
    lastVisibilityPostId: Long,
    isSeeLz: Boolean,
) {
    var savedHistory by remember(threadId) { mutableStateOf(false) }

    LaunchedEffect(threadId, threadTitle, author, lastVisibilityPostId) {
        if (threadTitle.isBlank()) return@LaunchedEffect

        if (savedHistory && lastVisibilityPostId != 0L) {
            delay(600)
        }

        withContext(Dispatchers.IO) {
            runCatching {
                HistoryUtil.saveHistory(
                    History(
                        title = threadTitle,
                        data = threadId.toString(),
                        type = HistoryUtil.TYPE_THREAD,
                        extras = ThreadHistoryInfoBean(
                            isSeeLz = isSeeLz,
                            pid = lastVisibilityPostId.toString(),
                            forumName = forumName,
                            floor = currentFloor?.toString()
                        ).toJson(),
                        avatar = StringUtil.getAvatarUrl(author?.get { portrait }),
                        username = author?.get { nameShow }
                    ),
                    async = false,
                    incrementCount = !savedHistory
                )
                savedHistory = true
            }
        }
    }
}
