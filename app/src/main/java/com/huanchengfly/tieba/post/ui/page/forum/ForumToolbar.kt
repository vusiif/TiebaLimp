package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import androidx.compose.material.DropdownMenuItem
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumSearchPostPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PlaceholderHighlight
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LoadingPlaceholder(
    forumName: String
) {
    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            ForumToolbar(
                forumName = forumName,
                showTitle = false,
                menuContent = {
                    DropdownMenuItem(
                        onClick = {
                            shareForum(context, forumName)
                            dismiss()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_share))
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            ForumHeaderPlaceholder(
                forumName = forumName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Row(modifier = Modifier.height(48.dp)) {
                persistentListOf(
                    stringResource(id = R.string.tab_forum_latest),
                    stringResource(id = R.string.tab_forum_good),
                ).fastForEach {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.fade(),
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            style = MaterialTheme.typography.button,
                        )
                    }
                }
            }
            repeat(4) {
                FeedCardPlaceholder()
            }
        }
    }
}

@Composable
internal fun BackNavigationIconPlaceholder() {
    IconButton(
        onClick = {},
        enabled = false,
        modifier = Modifier.alpha(0f)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null
        )
    }
}

@Composable
internal fun ForumToolbar(
    forumName: String,
    showTitle: Boolean,
    menuContent: @Composable (MenuScope.() -> Unit)? = null,
    forumId: Long? = null,
) {
    val navigator = LocalNavigator.current
    val onBackPressed: () -> Unit = remember(navigator) {
        {
            navigator.navigateUp()
            Unit
        }
    }
    val onSearchClick = remember(navigator, forumName, forumId) {
        {
            if (forumId != null) {
                navigator.navigate(ForumSearchPostPageDestination(forumName, forumId))
            }
        }
    }
    Toolbar(
        title = {
            if (showTitle) Text(
                text = stringResource(
                    id = R.string.title_forum,
                    forumName
                )
            )
        },
        navigationIcon = { BackNavigationIcon(onBackPressed = onBackPressed) },
        actions = {
            if (forumId != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.btn_search_in_forum)
                    )
                }
            }
            Box {
                if (menuContent != null) {
                    val menuState = rememberMenuState()
                    ClickMenu(
                        menuContent = menuContent,
                        menuState = menuState,
                        triggerShape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(id = R.string.btn_more)
                            )
                        }
                    }
                }
            }
        }
    )
}
