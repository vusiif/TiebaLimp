package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun ThreadJumpFloorSection(
    totalFloors: Int,
    totalPage: Int,
    onJumpToFloor: (targetFloor: Int) -> Unit,
    onJumpToPage: (targetPage: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveMaxFloor = maxOf(totalFloors, 1)
    val effectiveMaxPage = maxOf(totalPage, 1)
    var floorInput by remember { mutableStateOf("") }
    var pageInput by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.tip_total_thread_meta, effectiveMaxFloor, effectiveMaxPage),
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Jump Action Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ExtendedTheme.colors.chip)
                    .clickable { onJumpToFloor(1) }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.btn_jump_lz),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ExtendedTheme.colors.text,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ExtendedTheme.colors.chip)
                    .clickable { onJumpToFloor(effectiveMaxFloor) }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.btn_jump_latest),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ExtendedTheme.colors.text,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Jump to specific floor input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = floorInput,
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() }) floorInput = newVal
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(text = "${stringResource(id = R.string.label_jump_floor)} (1-$effectiveMaxFloor)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val f = floorInput.toIntOrNull()
                        if (f != null && f in 1..effectiveMaxFloor) {
                            onJumpToFloor(f)
                        }
                    }
                )
            )

            Button(
                onClick = {
                    val f = floorInput.toIntOrNull()
                    if (f != null && f in 1..effectiveMaxFloor) {
                        onJumpToFloor(f)
                    }
                },
                enabled = floorInput.toIntOrNull()?.let { it in 1..effectiveMaxFloor } == true,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(id = R.string.btn_jump_to_floor))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Jump to specific page input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = pageInput,
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() }) pageInput = newVal
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(text = "${stringResource(id = R.string.label_jump_page)} (1-$effectiveMaxPage)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val p = pageInput.toIntOrNull()
                        if (p != null && p in 1..effectiveMaxPage) {
                            onJumpToPage(p)
                        }
                    }
                )
            )

            Button(
                onClick = {
                    val p = pageInput.toIntOrNull()
                    if (p != null && p in 1..effectiveMaxPage) {
                        onJumpToPage(p)
                    }
                },
                enabled = pageInput.toIntOrNull()?.let { it in 1..effectiveMaxPage } == true,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(id = R.string.btn_jump_to_page))
            }
        }
    }
}
