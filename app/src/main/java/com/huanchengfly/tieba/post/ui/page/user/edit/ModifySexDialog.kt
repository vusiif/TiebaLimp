package com.huanchengfly.tieba.post.ui.page.user.edit

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ModifySexDialog(
    dialogState: DialogState,
    currentSex: Int,
    onSexSelected: (Int) -> Unit,
) {
    Dialog(
        dialogState = dialogState,
        title = { Text(text = stringResource(id = R.string.title_modify_sex)) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        ListSinglePicker(
            itemTitles = persistentListOf(
                stringResource(id = R.string.profile_sex_male),
                stringResource(id = R.string.profile_sex_female)
            ),
            itemValues = persistentListOf(1, 2),
            selectedPosition = if (currentSex == 1) 0 else if (currentSex == 2) 1 else -1,
            onItemSelected = { _, _, value, _ ->
                onSexSelected(value)
                dismiss()
            }
        )
    }
}
