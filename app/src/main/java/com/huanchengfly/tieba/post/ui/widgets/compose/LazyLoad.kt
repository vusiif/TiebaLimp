package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalShouldLoad = compositionLocalOf { true }

@Composable
fun LazyLoad(
    loaded: Boolean,
    onLoad: () -> Unit,
) {
    val shouldLoad = LocalShouldLoad.current
    val curOnLoad by rememberUpdatedState(newValue = onLoad)
    LaunchedEffect(loaded, shouldLoad) {
        if (!loaded && shouldLoad) {
            curOnLoad()
        }
    }
}

@Composable
fun LazyLoad(
    key: Any,
    loaded: Boolean,
    onLoad: () -> Unit,
) {
    val shouldLoad = LocalShouldLoad.current
    val curOnLoad by rememberUpdatedState(newValue = onLoad)
    LaunchedEffect(key, loaded, shouldLoad) {
        if (!loaded && shouldLoad) {
            curOnLoad()
        }
    }
}

@Composable
fun ProvideShouldLoad(
    shouldLoad: Boolean,
    content: @Composable () -> Unit,
) {
    val currentShouldLoad = LocalShouldLoad.current

    val localShouldLoad = remember(currentShouldLoad, shouldLoad) {
        currentShouldLoad && shouldLoad
    }

    CompositionLocalProvider(
        LocalShouldLoad provides localShouldLoad
    ) {
        content()
    }
}

object LazyLoadHelper {
    /**
     * Determines whether a given page index falls within the allowed preload range of the current pager state.
     */
    fun isWithinPreloadRange(
        pageIndex: Int,
        currentPage: Int,
        preloadDistance: Int = 0
    ): Boolean {
        if (preloadDistance < 0) return pageIndex == currentPage
        return kotlin.math.abs(pageIndex - currentPage) <= preloadDistance
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyLoadHorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    preloadDistance: Int = beyondBoundsPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        state,
        Orientation.Horizontal
    ),
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    HorizontalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection
    ) { page ->
        val shouldLoad = remember(page, state.currentPage, preloadDistance) {
            LazyLoadHelper.isWithinPreloadRange(page, state.currentPage, preloadDistance)
        }
        ProvideShouldLoad(
            shouldLoad = shouldLoad
        ) {
            pageContent(page)
        }
    }
}
