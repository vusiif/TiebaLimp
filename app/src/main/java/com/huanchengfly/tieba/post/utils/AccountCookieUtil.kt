package com.huanchengfly.tieba.post.utils

/**
 * Utility functions for parsing and formatting Baidu account cookies.
 */
object AccountCookieUtil {
    /**
     * Parses a cookie string separated by ';' into a key-value Map.
     */
    fun parseCookie(cookie: String): Map<String, String> {
        return cookie
            .split(";")
            .map { it.trim().split("=") }
            .filter { it.size > 1 }
            .associate { it.first() to it.drop(1).joinToString("=") }
    }

    /**
     * Formats a Baidu BDUSS token into a standard browser cookie string.
     */
    fun getBdussCookie(bduss: String): String {
        return "BDUSS=$bduss; Path=/; Max-Age=315360000; Domain=.baidu.com; Httponly; Secure"
    }

    /**
     * Extracts BDUSS from a cookie string, case-insensitively.
     */
    fun extractBduss(cookie: String): String? {
        val cookies = parseCookie(cookie).mapKeys { it.key.uppercase() }
        return cookies["BDUSS"]
    }

    /**
     * Extracts STOKEN from a cookie string, case-insensitively.
     */
    fun extractSToken(cookie: String): String? {
        val cookies = parseCookie(cookie).mapKeys { it.key.uppercase() }
        return cookies["STOKEN"]
    }
}
