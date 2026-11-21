package com.huanchengfly.tieba.post.ui.page.user.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

@Composable
fun PageEditProfile(
    viewModel: EditProfileViewModel,
    onBackPressed: () -> Unit,
) {
    val isLoading by viewModel.uiState.collectPartialAsState(
        EditProfileState::isLoading,
        initial = false
    )
    if (!isLoading) {
        val uiState by viewModel.uiState.collectAsState()
        var sex by remember { mutableIntStateOf(uiState.sex) }
        val birthdayTime by remember { mutableLongStateOf(uiState.birthdayTime) }
        val birthdayShowStatus by remember { mutableStateOf(uiState.birthdayShowStatus) }
        var intro by remember { mutableStateOf(uiState.intro) }
        var nickName by remember { mutableStateOf(uiState.nickName) }
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(id = R.string.title_activity_edit_profile),
                    navigationIcon = { BackNavigationIcon { onBackPressed() } },
                    actions = {
                        ActionItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_round_save_24),
                            contentDescription = stringResource(id = R.string.button_save_profile)
                        ) {
                            if (sex != uiState.sex ||
                                birthdayTime != uiState.birthdayTime ||
                                birthdayShowStatus != uiState.birthdayShowStatus ||
                                intro != uiState.intro ||
                                nickName != uiState.nickName
                            ) {
                                if (nickName.isNotEmpty()) {
                                    viewModel.send(
                                        EditProfileIntent.Submit(
                                            sex,
                                            birthdayTime,
                                            birthdayShowStatus,
                                            intro ?: "",
                                            nickName
                                        )
                                    )
                                }
                            } else {
                                viewModel.send(EditProfileIntent.SubmitWithoutChange)
                            }
                        }
                    }
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(id = R.dimen.card_margin))
                    .imePadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val dialogState = rememberDialogState()
                ModifySexDialog(
                    dialogState = dialogState,
                    currentSex = sex,
                    onSexSelected = { sex = it }
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
                EditProfileCard(
                    portrait = uiState.portrait,
                    name = uiState.name,
                    nickName = nickName,
                    sex = sex,
                    intro = intro ?: "",
                    loading = uiState.isLoading,
                    onNickNameChange = { nickName = it },
                    onIntroChange = { intro = it },
                    onUploadPortrait = { viewModel.send(EditProfileIntent.UploadPortraitStart) },
                    onModifySex = { dialogState.show = true }
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
            }
        }
    } else {
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(id = R.string.title_activity_edit_profile),
                    navigationIcon = { BackNavigationIcon { onBackPressed() } }
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(id = R.dimen.card_margin))
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
                EditProfileCard(
                    portrait = "",
                    name = "",
                    nickName = "",
                    sex = 0,
                    intro = "",
                    loading = true
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
            }
        }
    }
}
