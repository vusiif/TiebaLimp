package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogScope
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import kotlin.math.roundToInt

@Composable
fun AddBlockRuleDialog(
    dialogState: DialogState,
    category: Int,
    onConfirm: (
        type: Int,
        keywords: List<String>,
        username: String?,
        uid: String?,
        pattern: String?,
        minLevel: Int,
        ipRegion: String?,
        forum: String?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableIntStateOf(Block.TYPE_KEYWORD) }
    var keywordText by remember { mutableStateOf("") }
    var regexText by remember { mutableStateOf("") }
    var ipRegionText by remember { mutableStateOf("") }
    var levelThreshold by remember { mutableFloatStateOf(4f) }
    var usernameText by remember { mutableStateOf("") }
    var uidText by remember { mutableStateOf("") }
    var forumText by remember { mutableStateOf("") }

    val regexError = remember(regexText) {
        if (regexText.isBlank()) null
        else runCatching { Regex(regexText) }.exceptionOrNull()?.message
    }

    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            keywordText = ""
            regexText = ""
            ipRegionText = ""
            levelThreshold = 4f
            usernameText = ""
            uidText = ""
            forumText = ""
            selectedType = Block.TYPE_KEYWORD
        }
    }

    val isInputValid = when (selectedType) {
        Block.TYPE_KEYWORD -> keywordText.isNotBlank()
        Block.TYPE_REGEX -> regexText.isNotBlank() && regexError == null
        Block.TYPE_IP_REGION -> ipRegionText.isNotBlank()
        Block.TYPE_USER_LEVEL -> levelThreshold.roundToInt() in 1..18
        Block.TYPE_USER -> usernameText.isNotBlank() || uidText.isNotBlank()
        else -> false
    }

    val titleText = if (category == Block.CATEGORY_WHITE_LIST) {
        stringResource(id = R.string.title_add_white)
    } else {
        stringResource(id = R.string.title_add_black)
    }

    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
        },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_sure_default),
                onClick = {
                    if (isInputValid) {
                        onConfirm(
                            selectedType,
                            if (selectedType == Block.TYPE_KEYWORD) {
                                keywordText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                            } else emptyList(),
                            if (selectedType == Block.TYPE_USER) usernameText.trim().ifEmpty { null } else null,
                            if (selectedType == Block.TYPE_USER) uidText.trim().ifEmpty { null } else null,
                            if (selectedType == Block.TYPE_REGEX) regexText.trim() else null,
                            if (selectedType == Block.TYPE_USER_LEVEL) levelThreshold.roundToInt() else 0,
                            if (selectedType == Block.TYPE_IP_REGION) ipRegionText.trim() else null,
                            forumText.trim().ifEmpty { null },
                        )
                    }
                }
            )
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            val typeList = listOf(
                Block.TYPE_KEYWORD to stringResource(id = R.string.rule_type_keyword),
                Block.TYPE_REGEX to stringResource(id = R.string.rule_type_regex),
                Block.TYPE_IP_REGION to stringResource(id = R.string.rule_type_ip),
                Block.TYPE_USER_LEVEL to stringResource(id = R.string.rule_type_level),
                Block.TYPE_USER to stringResource(id = R.string.rule_type_user),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeList.forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colors.secondary
                                else ExtendedTheme.colors.text.copy(alpha = 0.08f)
                            )
                            .clickable { selectedType = type }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colors.onSecondary else ExtendedTheme.colors.text,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedType) {
                Block.TYPE_KEYWORD -> {
                    Text(
                        text = stringResource(id = R.string.tip_add_block),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keywordText,
                        onValueChange = { keywordText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        placeholder = { Text(text = "如：广告 兼职 刷单") },
                    )
                }

                Block.TYPE_REGEX -> {
                    Text(
                        text = stringResource(id = R.string.tip_regex_desc),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = regexText,
                        onValueChange = { regexText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(text = "如：.*(兼职|刷单|威信).*") },
                        isError = regexError != null,
                    )
                    if (regexError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(id = R.string.tip_regex_invalid)}: $regexError",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.error,
                        )
                    }
                }

                Block.TYPE_IP_REGION -> {
                    Text(
                        text = stringResource(id = R.string.tip_ip_region_desc),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ipRegionText,
                        onValueChange = { ipRegionText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(text = "如：北京 或 广东 或 海外") },
                    )
                }

                Block.TYPE_USER_LEVEL -> {
                    val currentVal = levelThreshold.roundToInt()
                    Text(
                        text = stringResource(id = R.string.tip_level_threshold_desc),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "屏蔽低于 Lv.$currentVal 的发言",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Lv.1 ~ Lv.18",
                            style = MaterialTheme.typography.caption,
                            color = ExtendedTheme.colors.textSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = levelThreshold,
                        onValueChange = { levelThreshold = it },
                        valueRange = 1f..18f,
                        steps = 16,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Block.TYPE_USER -> {
                    Text(
                        text = "精准屏蔽或放行指定贴吧用户（用户名与 UID 至少填一项）",
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = usernameText,
                        onValueChange = { usernameText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(text = "用户名") },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uidText,
                        onValueChange = { uidText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(text = "UID（选填）") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = forumText,
                onValueChange = { forumText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(text = stringResource(id = R.string.label_rule_forum)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ImportRulesDialog(
    dialogState: DialogState,
    onConfirm: (json: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var jsonText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            jsonText = ""
        }
    }

    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        title = {
            Text(
                text = stringResource(id = R.string.title_import_rules),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
        },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_sure_default),
                onClick = {
                    if (jsonText.isNotBlank()) {
                        onConfirm(jsonText.trim())
                    }
                }
            )
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.hint_import_rules),
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                singleLine = false,
                maxLines = 8,
                placeholder = { Text(text = "[{\"category\":10,\"type\":0,...}]") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val clip = clipboardManager.getText()?.text
                    if (!clip.isNullOrBlank()) {
                        jsonText = clip
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(id = R.string.btn_paste_from_clipboard))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
