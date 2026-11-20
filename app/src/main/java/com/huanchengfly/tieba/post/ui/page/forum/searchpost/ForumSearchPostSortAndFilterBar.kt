package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ForumSearchPostSortAndFilterBar(
    currentSortType: Int,
    currentFilterType: Int,
    sortTypeMapping: Map<Int, String>,
    filterTypeMapping: Map<Int, String>,
    onSortTypeChange: (Int) -> Unit,
    onFilterTypeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(ExtendedTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        val menuState = rememberMenuState()

        val rotate by animateFloatAsState(
            targetValue = if (menuState.expanded) 180f else 0f,
            label = "ArrowIndicatorRotate"
        )

        ClickMenu(
            menuContent = {
                ListSinglePicker(
                    itemTitles = sortTypeMapping.values.toImmutableList(),
                    itemValues = sortTypeMapping.keys.toImmutableList(),
                    selectedPosition = sortTypeMapping.keys.indexOf(
                        currentSortType
                    ),
                    onItemSelected = { _, _, newSortType, changed ->
                        if (changed) {
                            onSortTypeChange(newSortType)
                        }
                        dismiss()
                    }
                )
            },
            menuState = menuState,
            indication = null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sortTypeMapping[currentSortType].orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotate)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            filterTypeMapping.keys.map<Int, @Composable () -> Unit> { type ->
                {
                    Text(
                        text = filterTypeMapping[type].orEmpty(),
                        fontSize = 13.sp,
                        fontWeight = if (type == currentFilterType) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.RadioButton,
                            onClick = {
                                if (type != currentFilterType) {
                                    onFilterTypeChange(type)
                                }
                            }
                        )
                    )
                }
            }.forEachIndexed { index, composable ->
                composable()
                if (index < filterTypeMapping.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = 8.dp
                        )
                    )
                }
            }
        }
    }
}
