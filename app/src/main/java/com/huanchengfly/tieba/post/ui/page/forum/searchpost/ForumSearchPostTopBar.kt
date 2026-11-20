package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarContainer

@Composable
fun ForumSearchPostTopBar(
    forumName: String,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onKeywordSubmit: (String) -> Unit,
    focusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TopAppBarContainer(
        topBar = {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .background(ExtendedTheme.colors.topBar)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SearchBox(
                    keyword = keyword,
                    onKeywordChange = onKeywordChange,
                    modifier = Modifier.fillMaxSize(),
                    onKeywordSubmit = {
                        focusRequester.freeFocus()
                        keyboardController?.hide()
                        onKeywordSubmit(it)
                    },
                    placeholder = {
                        Text(
                            text = stringResource(
                                id = R.string.hint_search_in_ba,
                                forumName
                            ),
                            color = ExtendedTheme.colors.onTopBarSurface.copy(alpha = ContentAlpha.medium)
                        )
                    },
                    prependIcon = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = rememberRipple(bounded = false, 24.dp),
                                    role = Role.Button,
                                    onClick = onNavigateUp
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.button_back)
                            )
                        }
                    },
                    focusRequester = focusRequester,
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }
    )
}
