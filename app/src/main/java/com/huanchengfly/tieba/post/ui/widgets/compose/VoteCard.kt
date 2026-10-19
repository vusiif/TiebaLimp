package com.huanchengfly.tieba.post.ui.widgets.compose

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.PollInfo
import com.huanchengfly.tieba.post.api.models.protos.PollOption
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun VoteCard(
    pollInfo: PollInfo,
    threadId: Long,
    forumId: Long,
    modifier: Modifier = Modifier,
    onVoteSuccess: ((newPollInfo: PollInfo) -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPollInfo by remember(pollInfo) { mutableStateOf(pollInfo) }
    var selectedOptionIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val hasVoted = currentPollInfo.is_polled == 1
    val isMulti = currentPollInfo.is_multi == 1
    val myVotedOptionIds = remember(currentPollInfo.polled_value) {
        currentPollInfo.polled_value
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }
    val totalVotes = max(1L, currentPollInfo.total_poll)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ExtendedTheme.colors.floorCard,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = stringResource(R.string.title_vote),
                    tint = ExtendedTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = currentPollInfo.title.ifBlank { stringResource(R.string.title_vote) },
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                    color = ExtendedTheme.colors.text,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tag & Status Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(
                    text = stringResource(if (isMulti) R.string.chip_vote_multi else R.string.chip_vote_single),
                    invertColor = false
                )
                if (hasVoted) {
                    Chip(
                        text = stringResource(R.string.chip_vote_polled),
                        invertColor = true,
                        prefixIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        R.string.tip_vote_count,
                        currentPollInfo.total_num,
                        currentPollInfo.total_poll
                    ),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary
                )
            }

            // Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentPollInfo.options.forEach { option ->
                    if (hasVoted) {
                        VotedOptionRow(
                            option = option,
                            totalVotes = totalVotes,
                            isMyChoice = myVotedOptionIds.contains(option.id)
                        )
                    } else {
                        SelectableOptionRow(
                            option = option,
                            isSelected = selectedOptionIds.contains(option.id),
                            isMulti = isMulti,
                            onClick = {
                                if (isMulti) {
                                    selectedOptionIds = if (selectedOptionIds.contains(option.id)) {
                                        selectedOptionIds - option.id
                                    } else {
                                        selectedOptionIds + option.id
                                    }
                                } else {
                                    selectedOptionIds = setOf(option.id)
                                }
                            }
                        )
                    }
                }
            }

            // Submit Button
            if (!hasVoted) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (!AccountUtil.isLoggedIn()) {
                            Toast.makeText(context, R.string.tip_login, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val optionsStr = selectedOptionIds.joinToString(",")
                        isSubmitting = true
                        coroutineScope.launch {
                            TiebaApi.getInstance().addPollPostFlow(
                                threadId = threadId.toString(),
                                forumId = forumId.toString(),
                                options = optionsStr
                            ).catch { e ->
                                isSubmitting = false
                                Toast.makeText(context, e.getErrorMessage(), Toast.LENGTH_SHORT).show()
                            }.collect {
                                isSubmitting = false
                                Toast.makeText(context, R.string.toast_vote_success, Toast.LENGTH_SHORT).show()
                                val updatedOptions = currentPollInfo.options.map { o ->
                                    if (selectedOptionIds.contains(o.id)) {
                                        o.copy(num = o.num + 1)
                                    } else o
                                }
                                val newPoll = currentPollInfo.copy(
                                    is_polled = 1,
                                    polled_value = optionsStr,
                                    total_num = currentPollInfo.total_num + 1,
                                    total_poll = currentPollInfo.total_poll + selectedOptionIds.size,
                                    options = updatedOptions
                                )
                                currentPollInfo = newPoll
                                onVoteSuccess?.invoke(newPoll)
                            }
                        }
                    },
                    enabled = selectedOptionIds.isNotEmpty() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(100)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ExtendedTheme.colors.onAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.btn_voting))
                    } else {
                        Text(text = stringResource(R.string.btn_submit_vote))
                    }
                }
            }
        }
    }
}

@Composable
private fun VotedOptionRow(
    option: PollOption,
    totalVotes: Long,
    isMyChoice: Boolean,
    modifier: Modifier = Modifier,
) {
    val ratio = if (totalVotes > 0) (option.num.toFloat() / totalVotes).coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "voteRatio")
    val percentage = (ratio * 100).toInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(ExtendedTheme.colors.card)
            .border(
                BorderStroke(
                    1.dp,
                    if (isMyChoice) ExtendedTheme.colors.primary.copy(alpha = 0.5f)
                    else ExtendedTheme.colors.divider.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // Percentage Progress Bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedRatio)
                .background(
                    if (isMyChoice) ExtendedTheme.colors.primary.copy(alpha = 0.28f)
                    else ExtendedTheme.colors.primary.copy(alpha = 0.12f)
                )
        )

        // Content Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isMyChoice) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.chip_vote_my_choice),
                    tint = ExtendedTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = option.text,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = if (isMyChoice) FontWeight.Bold else FontWeight.Normal
                ),
                color = ExtendedTheme.colors.text,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${option.num} 票 (${percentage}%)",
                style = MaterialTheme.typography.caption.copy(
                    fontWeight = if (isMyChoice) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isMyChoice) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun SelectableOptionRow(
    option: PollOption,
    isSelected: Boolean,
    isMulti: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) ExtendedTheme.colors.primary.copy(alpha = 0.10f)
                else ExtendedTheme.colors.card
            )
            .border(
                BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) ExtendedTheme.colors.primary
                    else ExtendedTheme.colors.divider.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isMulti) {
                    if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank
                } else {
                    if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = if (isSelected) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = option.text,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                ),
                color = if (isSelected) ExtendedTheme.colors.primary else ExtendedTheme.colors.text,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
