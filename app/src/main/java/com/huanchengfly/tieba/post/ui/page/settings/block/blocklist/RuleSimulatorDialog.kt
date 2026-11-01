package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.utils.BlockManager
import kotlin.math.roundToInt

@Composable
fun RuleSimulatorDialog(
    dialogState: DialogState,
    modifier: Modifier = Modifier,
) {
    var contentInput by remember { mutableStateOf("") }
    var userNameInput by remember { mutableStateOf("") }
    var uidInput by remember { mutableStateOf("") }
    var ipInput by remember { mutableStateOf("") }
    var forumInput by remember { mutableStateOf("") }
    var levelThreshold by remember { mutableFloatStateOf(4f) }

    val simResult = remember(
        contentInput,
        userNameInput,
        uidInput,
        ipInput,
        forumInput,
        levelThreshold,
        BlockManager.blackList.size,
        BlockManager.whiteList.size,
    ) {
        BlockManager.simulateCheck(
            content = contentInput,
            userId = uidInput.toLongOrNull() ?: 0L,
            userName = userNameInput,
            userLevel = levelThreshold.roundToInt(),
            userIp = ipInput,
            forumName = forumInput,
        )
    }

    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        title = {
            Text(
                text = stringResource(id = R.string.title_rule_simulator),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
        },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_sure_default),
                onClick = { dismiss() },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val (bannerColor, bannerTitle) = when {
                simResult.isBlocked -> Color(0xFFE53935) to stringResource(R.string.sim_result_blocked)
                simResult.isWhitelisted -> Color(0xFFF57C00) to stringResource(R.string.sim_result_whitelisted)
                else -> Color(0xFF43A047) to stringResource(R.string.sim_result_allowed)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bannerColor.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = bannerTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerColor,
                    )
                    Text(
                        text = simResult.reason,
                        fontSize = 12.sp,
                        color = ExtendedTheme.colors.text,
                    )
                }
            }

            OutlinedTextField(
                value = contentInput,
                onValueChange = { contentInput = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                label = { Text(text = stringResource(id = R.string.sim_input_content)) },
                placeholder = { Text(text = "输入待匹配的贴子或回复文本") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(text = stringResource(id = R.string.sim_input_user)) },
                )
                OutlinedTextField(
                    value = uidInput,
                    onValueChange = { uidInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(text = stringResource(id = R.string.sim_input_uid)) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = forumInput,
                    onValueChange = { forumInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(text = stringResource(id = R.string.sim_input_forum)) },
                    placeholder = { Text(text = "如：孙笑川") },
                )
                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(text = stringResource(id = R.string.sim_input_ip)) },
                    placeholder = { Text(text = "如：北京") },
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                val currentLevel = levelThreshold.roundToInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.sim_input_level, currentLevel),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Lv.1 ~ Lv.18",
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                }
                Slider(
                    value = levelThreshold,
                    onValueChange = { levelThreshold = it },
                    valueRange = 1f..18f,
                    steps = 16,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
