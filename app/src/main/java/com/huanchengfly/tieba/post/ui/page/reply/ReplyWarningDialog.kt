package com.huanchengfly.tieba.post.ui.page.reply

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
fun ReplyWarningDialog(
    threadId: Long,
    postId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    fun getDispatchUri(): Uri {
        return if (postId != null) {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=comment_lzl_cut_guide&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2&hightlight_anchor_pid=${postId}&is_anchor_to_comment=1&comment_sort_type=0&fr=bpush&tid=${threadId}")
        } else {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=pb_reply&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2-99999_9&fr=bpush&tid=${threadId}")
        }
    }

    fun launchOfficialApp() {
        val intent = Intent(Intent.ACTION_VIEW).setData(getDispatchUri())
        val resolveInfos =
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ).filter { it.activityInfo.packageName != context.packageName }
        try {
            if (resolveInfos.isNotEmpty()) {
                context.startActivity(intent)
            } else {
                context.toastShort(R.string.toast_official_client_not_install)
            }
        } catch (e: ActivityNotFoundException) {
            context.toastShort(R.string.toast_official_client_not_install)
        }
    }

    val warningDialogState = rememberDialogState()
    Dialog(
        dialogState = warningDialogState,
        title = { Text(text = stringResource(id = R.string.title_dialog_reply_warning)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_official_client_reply),
                onClick = { launchOfficialApp() }
            )
            DialogNegativeButton(text = stringResource(id = R.string.btn_continue_reply))
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_cancel_reply),
                onClick = { onBack() }
            )
        },
    ) {
        Text(
            text = stringResource(id = R.string.message_dialog_reply_warning),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }

    LaunchedEffect(Unit) {
        if (context.appPreferences.postOrReplyWarning) {
            warningDialogState.show()
        }
    }
}
