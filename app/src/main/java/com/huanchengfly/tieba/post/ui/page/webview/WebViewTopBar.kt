package com.huanchengfly.tieba.post.ui.page.webview

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.TiebaUtil

@Composable
fun WebViewTopBar(
    title: String,
    currentHost: String,
    isExternalHost: Boolean,
    onBack: () -> Unit,
    currentUrl: String,
    onReload: () -> Unit,
) {
    val context = LocalContext.current
    Toolbar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isExternalHost) {
                    Text(
                        text = currentHost,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = ExtendedTheme.colors.onTopBarSecondary,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
        navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
        actions = {
            val menuState = rememberMenuState()
            ClickMenu(
                menuContent = {
                    DropdownMenuItem(
                        onClick = {
                            TiebaUtil.copyText(context, currentUrl)
                            dismiss()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_copy_link))
                    }
                    DropdownMenuItem(
                        onClick = {
                            val uri = currentUrl.toUri()
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    uri
                                )
                            )
                            dismiss()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_open_in_browser))
                    }
                    DropdownMenuItem(
                        onClick = {
                            onReload()
                            dismiss()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_refresh))
                    }
                },
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
        },
    )
}
