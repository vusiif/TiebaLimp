package com.huanchengfly.tieba.post.ui.widgets.compose

import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.plainText
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
