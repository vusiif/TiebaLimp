package com.huanchengfly.tieba.post

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations

@Composable
fun BatteryOptimizationDialog() {
    val context = LocalContext.current
    val okSignAlertDialogState = rememberDialogState()

    AlertDialog(
        dialogState = okSignAlertDialogState,
        title = { Text(text = stringResource(id = R.string.title_dialog_oksign_battery_optimization)) },
        content = { Text(text = stringResource(id = R.string.message_dialog_oksign_battery_optimization)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_go_to_ignore_battery_optimization),
                onClick = {
                    context.requestIgnoreBatteryOptimizations()
                }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.button_cancel)
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.button_dont_remind_again),
                onClick = {
                    context.appPreferences.ignoreBatteryOptimizationsDialog = true
                }
            )
        },
    )

    LaunchedEffect(Unit) {
        if (context.appPreferences.autoSign && !context.isIgnoringBatteryOptimizations() && !context.appPreferences.ignoreBatteryOptimizationsDialog) {
            okSignAlertDialogState.show()
        }
    }
}
