package com.huanchengfly.tieba.post.utils

object BlockFilterUtil {
    fun isDangerousRegexPattern(pattern: String?): Boolean {
        if (pattern.isNullOrBlank()) return false
        if (pattern.length > 200) return true
        val nestedQuantifiers = Regex("""(\+|\*|\{\d+,?\d*\})\s*(\+|\*|\{\d+,?\d*\})|\([^\)]*(\+|\*)[^\)]*\)\s*(\+|\*|\{\d+,?\d*\})""")
        return nestedQuantifiers.containsMatchIn(pattern)
    }

    fun normalizeIpAddress(ip: String?): String {
        if (ip.isNullOrBlank()) return ""
        return ip
            .removePrefix("IP属地:")
            .removePrefix("IP属地：")
            .removePrefix("IP属地")
            .trim()
    }

    fun matchesForumScope(ruleForum: String?, currentForum: String?): Boolean {
        if (ruleForum.isNullOrBlank()) return true
        if (currentForum.isNullOrBlank()) return false
        return ruleForum.trim().equals(currentForum.trim(), ignoreCase = true)
    }
}
