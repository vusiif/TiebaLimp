package com.huanchengfly.tieba.post.ui.page.login

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.request.DisplayRequest
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.BaiduQrLoginHelper
import com.huanchengfly.tieba.post.api.QrLoginState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun QrLoginContent(
    modifier: Modifier = Modifier,
    refreshTrigger: Int,
    onSuccess: (bduss: String, sToken: String, cookieStr: String, baiduId: String?) -> Unit,
) {
    val context = LocalContext.current
    var qrState by remember { mutableStateOf<QrLoginState>(QrLoginState.Loading) }
    var localRefreshCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger, localRefreshCount) {
        qrState = QrLoginState.Loading
        val fetchRes = BaiduQrLoginHelper.fetchQrCode()
        val qrData = fetchRes.getOrNull()
        if (qrData == null) {
            qrState = QrLoginState.Error(fetchRes.exceptionOrNull()?.message ?: context.getString(R.string.title_api_error))
            return@LaunchedEffect
        }
        qrState = QrLoginState.WaitingForScan(qrData.imgUrl, qrData.sign)

        val startTime = System.currentTimeMillis()
        val timeoutMs = 5 * 60 * 1000L // 5 minutes
        var consecutiveErrors = 0
        while (isActive && System.currentTimeMillis() - startTime < timeoutMs) {
            val pollRes = BaiduQrLoginHelper.pollStatusOnce(qrData.sign)
            if (pollRes.isFailure) {
                consecutiveErrors++
                if (consecutiveErrors >= 5) {
                    qrState = QrLoginState.Error(context.getString(R.string.tip_qrcode_network_error))
                    break
                }
                delay(2000L)
                continue
            }
            consecutiveErrors = 0
            val pollResult = pollRes.getOrNull()
            if (pollResult == "SCANNED") {
                qrState = QrLoginState.Scanned(qrData.imgUrl)
            } else if (!pollResult.isNullOrEmpty()) {
                val exchangeRes = BaiduQrLoginHelper.exchangeTokens(pollResult)
                val tokenResult = exchangeRes.getOrNull()
                if (tokenResult != null) {
                    qrState = QrLoginState.Success(tokenResult.bduss, tokenResult.sToken, tokenResult.cookieStr)
                    onSuccess(tokenResult.bduss, tokenResult.sToken, tokenResult.cookieStr, tokenResult.baiduId)
                    break
                } else {
                    qrState = QrLoginState.Error(exchangeRes.exceptionOrNull()?.message ?: context.getString(R.string.title_api_error))
                    break
                }
            }
            delay(1500L)
        }
        if (qrState is QrLoginState.WaitingForScan || qrState is QrLoginState.Scanned) {
            qrState = QrLoginState.Expired
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.92f),
            elevation = 2.dp,
            shape = RoundedCornerShape(16.dp),
            color = ExtendedTheme.colors.card
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (val state = qrState) {
                    is QrLoginState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = ExtendedTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.tip_qrcode_loading),
                            style = MaterialTheme.typography.body2,
                            color = ExtendedTheme.colors.textSecondary
                        )
                    }

                    is QrLoginState.WaitingForScan, is QrLoginState.Scanned -> {
                        val imgUrl = if (state is QrLoginState.WaitingForScan) state.imgUrl else (state as QrLoginState.Scanned).imgUrl
                        val isScanned = state is QrLoginState.Scanned

                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                request = DisplayRequest(context, imgUrl) {
                                    crossfade()
                                },
                                contentDescription = "Baidu QR Code",
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isScanned) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.tip_qrcode_scanned_short),
                                            color = Color.White,
                                            style = MaterialTheme.typography.subtitle1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isScanned) {
                                stringResource(R.string.tip_qrcode_scanned)
                            } else {
                                stringResource(R.string.tip_qrcode_waiting)
                            },
                            style = MaterialTheme.typography.body2,
                            color = if (isScanned) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                ImageUtil.download(context, imgUrl, false, null)
                                val pm = context.packageManager
                                val launchIntent = pm.getLaunchIntentForPackage("com.baidu.tieba")
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                    Toast.makeText(
                                        context,
                                        R.string.toast_qrcode_saved,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        R.string.toast_tieba_not_installed,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.btn_save_and_open_tieba))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { localRefreshCount++ }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.btn_refresh_qrcode))
                        }
                    }

                    is QrLoginState.Expired, is QrLoginState.Error -> {
                        val errorText = (state as? QrLoginState.Error)?.message
                            ?: stringResource(R.string.tip_qrcode_expired)

                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = ExtendedTheme.colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.body2,
                            color = ExtendedTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { localRefreshCount++ }
                        ) {
                            Text(text = stringResource(R.string.btn_refresh_qrcode))
                        }
                    }

                    is QrLoginState.Success -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = ExtendedTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.text_login_success),
                            style = MaterialTheme.typography.body2,
                            color = ExtendedTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}
