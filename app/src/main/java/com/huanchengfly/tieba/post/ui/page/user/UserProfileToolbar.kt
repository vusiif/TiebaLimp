package com.huanchengfly.tieba.post.ui.page.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.utils.BlockManager

@Composable
internal fun UserProfileToolbar(
    user: ImmutableHolder<User>,
    isSelf: Boolean,
    showTitle: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Toolbar(
        title = {
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ToolbarUserTitle(user = user)
            }
        },
        navigationIcon = {
            BackNavigationIcon(onBackPressed = onBack)
        },
        actions = {
            user.takeUnless { isSelf }?.let {
                ClickMenu(
                    menuContent = {
                        DropdownMenuItem(
                            onClick = {
                                BlockManager.addBlockAsync(
                                    Block(
                                        category = Block.CATEGORY_BLACK_LIST,
                                        type = Block.TYPE_USER,
                                        username = it.get { name },
                                        uid = it.get { id }.toString()
                                    )
                                ) {
                                    if (it) context.toastShort(R.string.toast_add_success)
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.menu_add_user_to_black_list))
                        }
                        DropdownMenuItem(
                            onClick = {
                                BlockManager.addBlockAsync(
                                    Block(
                                        category = Block.CATEGORY_WHITE_LIST,
                                        type = Block.TYPE_USER,
                                        username = it.get { name },
                                        uid = it.get { id }.toString()
                                    )
                                ) {
                                    if (it) context.toastShort(R.string.toast_add_success)
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.menu_add_user_to_white_list))
                        }
                    },
                    triggerShape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NoAccounts,
                            contentDescription = stringResource(id = R.string.btn_block)
                        )
                    }
                }
            }
        },
    )
}
