package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.thread.PostItemData

data class ThreadSearchHit(
    val floor: Int,
    val postId: Long,
    val authorName: String,
    val snippet: String,
    val isLz: Boolean = false,
)

object ThreadSearchUtil {
    fun search(
        query: String,
        firstPost: Post?,
        loadedPosts: List<PostItemData>,
    ): List<ThreadSearchHit> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val hits = mutableListOf<ThreadSearchHit>()

        // 1. Check first post (floor 1 / LZ)
        firstPost?.let { p ->
            val text = buildString {
                if (p.title.isNotBlank()) append(p.title).append(" ")
                append(p.content.plainText)
            }
            val author = p.author?.nameShow.takeUnless { it.isNullOrBlank() } ?: p.author?.name.orEmpty()
            val matchInText = text.contains(q, ignoreCase = true)
            val matchInAuthor = author.contains(q, ignoreCase = true)
            if (matchInText || matchInAuthor) {
                val snippet = if (matchInText) extractSnippet(text, q) else "作者匹配: $author"
                hits.add(
                    ThreadSearchHit(
                        floor = 1,
                        postId = p.id,
                        authorName = author,
                        snippet = snippet,
                        isLz = true,
                    )
                )
            }
        }

        // 2. Check loaded floors in data
        for (item in loadedPosts) {
            val p = item.post.get()
            if (p.floor == 1) continue
            val author = p.author?.nameShow.takeUnless { it.isNullOrBlank() } ?: p.author?.name.orEmpty()
            val text = p.content.plainText
            val matchInText = text.contains(q, ignoreCase = true)
            val matchInAuthor = author.contains(q, ignoreCase = true)

            // Also search subposts if any
            val matchedSubPost = item.subPosts.firstOrNull { sub ->
                val sp = sub.subPost.get()
                val subText = sp.content.plainText
                val subAuthor = sp.author?.nameShow.takeUnless { it.isNullOrBlank() } ?: sp.author?.name.orEmpty()
                subText.contains(q, ignoreCase = true) || subAuthor.contains(q, ignoreCase = true)
            }

            if (matchInText || matchInAuthor || matchedSubPost != null) {
                val snippet = when {
                    matchInText -> extractSnippet(text, q)
                    matchedSubPost != null -> {
                        val sp = matchedSubPost.subPost.get()
                        val subText = sp.content.plainText
                        val subAuthor = sp.author?.nameShow.takeUnless { it.isNullOrBlank() } ?: sp.author?.name.orEmpty()
                        if (subText.contains(q, ignoreCase = true)) {
                            "楼中楼 [${subAuthor}]: ${extractSnippet(subText, q)}"
                        } else {
                            "楼中楼作者: $subAuthor"
                        }
                    }
                    else -> "作者匹配: $author"
                }
                hits.add(
                    ThreadSearchHit(
                        floor = p.floor,
                        postId = p.id,
                        authorName = author,
                        snippet = snippet,
                        isLz = false,
                    )
                )
            }
        }

        return hits
    }

    private fun extractSnippet(fullText: String, query: String): String {
        val clean = fullText.replace(Regex("\\s+"), " ").trim()
        val index = clean.indexOf(query, ignoreCase = true)
        if (index == -1) return clean.take(60)
        val start = (index - 20).coerceAtLeast(0)
        val end = (index + query.length + 30).coerceAtMost(clean.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < clean.length) "…" else ""
        return prefix + clean.substring(start, end) + suffix
    }
}

@Composable
fun ThreadSearchJumpDialog(
    dialogState: DialogState,
    totalFloors: Int,
    currentPage: Int,
    totalPage: Int,
    firstPost: Post?,
    loadedPosts: List<PostItemData>,
    onJumpToFloor: (targetFloor: Int) -> Unit,
    onJumpToPage: (targetPage: Int) -> Unit,
    onSelectSearchHit: (hit: ThreadSearchHit, query: String, allHits: List<ThreadSearchHit>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var floorInput by remember { mutableStateOf("") }
    var pageInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val searchHits = remember(searchQuery, firstPost, loadedPosts) {
        ThreadSearchUtil.search(searchQuery, firstPost, loadedPosts)
    }

    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            floorInput = ""
            pageInput = ""
        }
    }

    Dialog(
        dialogState = dialogState,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.title_search_and_jump),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
        },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.primary,
                divider = {},
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(id = R.string.tab_jump_floor),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(id = R.string.tab_search_thread),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Jump to Floor / Page Tab
                    val effectiveMaxFloor = maxOf(totalFloors, 1)
                    val effectiveMaxPage = maxOf(totalPage, 1)

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
                                .clickable {
                                    dialogState.show = false
                                    onJumpToFloor(1)
                                }
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
                                .clickable {
                                    dialogState.show = false
                                    onJumpToFloor(effectiveMaxFloor)
                                }
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
                                        dialogState.show = false
                                        onJumpToFloor(f)
                                    }
                                }
                            )
                        )

                        Button(
                            onClick = {
                                val f = floorInput.toIntOrNull()
                                if (f != null && f in 1..effectiveMaxFloor) {
                                    dialogState.show = false
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
                                        dialogState.show = false
                                        onJumpToPage(p)
                                    }
                                }
                            )
                        )

                        Button(
                            onClick = {
                                val p = pageInput.toIntOrNull()
                                if (p != null && p in 1..effectiveMaxPage) {
                                    dialogState.show = false
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

                1 -> {
                    // Search in Thread Tab
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(text = stringResource(id = R.string.hint_search_thread)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.tip_search_matches_count, searchHits.size),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(id = R.string.tip_search_hint_load_more),
                                style = MaterialTheme.typography.caption,
                                color = ExtendedTheme.colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (searchHits.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tip_search_no_matches),
                                    style = MaterialTheme.typography.body2,
                                    color = ExtendedTheme.colors.textSecondary,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchHits, key = { "${it.floor}_${it.postId}" }) { hit ->
                                    SearchHitCard(
                                        hit = hit,
                                        query = searchQuery,
                                        onClick = {
                                            dialogState.show = false
                                            onSelectSearchHit(hit, searchQuery, searchHits)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "输入关键词检索已加载帖内楼层",
                                style = MaterialTheme.typography.caption,
                                color = ExtendedTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SearchHitCard(
    hit: ThreadSearchHit,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ExtendedTheme.colors.chip)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (hit.floor == 1) "1楼 (楼主)" else "${hit.floor}楼",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = hit.authorName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ExtendedTheme.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.SouthEast,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Highlight matched snippet
            val annotatedSnippet = buildAnnotatedString {
                val text = hit.snippet
                val q = query.trim()
                if (q.isEmpty()) {
                    append(text)
                } else {
                    var startIndex = 0
                    while (startIndex < text.length) {
                        val matchIndex = text.indexOf(q, startIndex, ignoreCase = true)
                        if (matchIndex == -1) {
                            append(text.substring(startIndex))
                            break
                        }
                        if (matchIndex > startIndex) {
                            append(text.substring(startIndex, matchIndex))
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFFF8F00),
                                fontWeight = FontWeight.Bold,
                                background = Color(0xFFFFB300).copy(alpha = 0.25f)
                            )
                        ) {
                            append(text.substring(matchIndex, matchIndex + q.length))
                        }
                        startIndex = matchIndex + q.length
                    }
                }
            }

            Text(
                text = annotatedSnippet,
                fontSize = 13.sp,
                color = ExtendedTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
        }
    }
}
