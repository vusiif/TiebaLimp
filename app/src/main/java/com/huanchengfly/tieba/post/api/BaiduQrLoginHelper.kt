package com.huanchengfly.tieba.post.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class QrLoginState {
    object Loading : QrLoginState()
    data class WaitingForScan(val imgUrl: String, val sign: String) : QrLoginState()
    data class Scanned(val imgUrl: String) : QrLoginState()
    data class Success(val bduss: String, val sToken: String, val cookieStr: String) : QrLoginState() {
        override fun toString(): String = "Success(bduss=***, sToken=***, cookieStr=***)"
    }
    object Expired : QrLoginState()
    data class Error(val message: String) : QrLoginState()
}

data class QrCodeData(
    val imgUrl: String,
    val sign: String,
)

data class QrTokenResult(
    val bduss: String,
    val sToken: String,
    val cookieStr: String,
    val baiduId: String? = null,
) {
    override fun toString(): String = "QrTokenResult(bduss=***, sToken=***, baiduId=$baiduId)"
}

object BaiduQrLoginHelper {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun fetchQrCode(): Result<QrCodeData> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val url = "https://passport.baidu.com/v2/api/getqrcode?lp=pc&qrloginfrom=pc&apiver=v3&tt=$timestamp"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://passport.baidu.com/v2/?login")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val json = JSONObject(responseBody)
                val errno = json.optInt("errno", -1)
                if (errno == 0) {
                    var rawImgUrl = json.getString("imgurl")
                    if (!rawImgUrl.startsWith("http://") && !rawImgUrl.startsWith("https://")) {
                        rawImgUrl = "https://$rawImgUrl"
                    }
                    val sign = json.getString("sign")
                    QrCodeData(imgUrl = rawImgUrl, sign = sign)
                } else {
                    throw IllegalStateException("Failed to get qrcode, errno: $errno")
                }
            }
        }
    }

    /**
     * Poll unicast endpoint once.
     * @return null if still waiting, "SCANNED" if scanned but waiting confirm, or token string `v` if confirmed.
     */
    suspend fun pollStatusOnce(sign: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val url = "https://passport.baidu.com/channel/unicast?channel_id=$sign&tpl=tb&apiver=v3&tt=$timestamp"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://passport.baidu.com/v2/?login")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (responseBody.isBlank()) return@runCatching null

                val json = JSONObject(responseBody)
                val errno = json.optInt("errno", -1)
                val channelV = json.optString("channel_v", "")

                if (channelV.isNotEmpty()) {
                    val vJson = JSONObject(channelV)
                    val token = vJson.optString("v", "")
                    if (token.isNotEmpty()) {
                        return@runCatching token
                    }
                    val status = vJson.optInt("status", -1)
                    if (status == 1) {
                        return@runCatching "SCANNED"
                    }
                }

                if (errno == 1) {
                    // Timeout / still waiting
                    null
                } else if (errno != 0) {
                    throw IllegalStateException("Unicast returned error errno: $errno")
                } else {
                    null
                }
            }
        }
    }

    suspend fun exchangeTokens(tokenV: String): Result<QrTokenResult> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://passport.baidu.com/v3/login/main/qrbdusslogin?alg=v3&apiver=v3&bduss=$tokenV&loginVersion=v4&qrcode=1&tpl=tb"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://passport.baidu.com/v2/?login")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val setCookies = response.headers("Set-Cookie")
                val cookieMap = mutableMapOf<String, String>()
                for (header in setCookies) {
                    val parts = header.split(";")
                    if (parts.isNotEmpty()) {
                        val kv = parts[0].split("=", limit = 2)
                        if (kv.size == 2) {
                            cookieMap[kv[0].trim().uppercase()] = kv[1].trim()
                        }
                    }
                }

                val bodyString = response.body?.string().orEmpty()
                var bduss = cookieMap["BDUSS"].orEmpty()
                var sToken = cookieMap["STOKEN"].orEmpty()

                if (bodyString.isNotEmpty()) {
                    val json = JSONObject(bodyString)
                    val data = json.optJSONObject("data")
                    val session = data?.optJSONObject("session")
                    if (session != null) {
                        if (bduss.isEmpty()) {
                            bduss = session.optString("bduss", "")
                        }
                        if (sToken.isEmpty()) {
                            sToken = session.optString("stoken", "")
                        }
                    }
                }

                if (bduss.isEmpty()) {
                    throw IllegalStateException("Cannot extract BDUSS from qrbdusslogin response")
                }

                val cookieBuilder = StringBuilder("BDUSS=$bduss;")
                if (sToken.isNotEmpty()) {
                    cookieBuilder.append("STOKEN=$sToken;")
                }
                cookieMap["BAIDUID"]?.let {
                    cookieBuilder.append("BAIDUID=$it;")
                }

                QrTokenResult(
                    bduss = bduss,
                    sToken = sToken,
                    cookieStr = cookieBuilder.toString(),
                    baiduId = cookieMap["BAIDUID"],
                )
            }
        }
    }
}
