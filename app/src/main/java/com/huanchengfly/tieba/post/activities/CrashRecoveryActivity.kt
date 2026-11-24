package com.huanchengfly.tieba.post.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.utils.AppCrashManager
import kotlin.system.exitProcess

/**
 * Isolated crash recovery activity running in dedicated ':crash' process.
 * Provides user with graceful crash diagnostics, log export, restart and safe-mode self-healing.
 */
class CrashRecoveryActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_REPORT = "extra_crash_report"
        const val EXTRA_IS_CRASH_LOOP = "extra_is_crash_loop"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = intent.getStringExtra(EXTRA_CRASH_REPORT) ?: "No crash report available"
        val isCrashLoop = intent.getBooleanExtra(EXTRA_IS_CRASH_LOOP, false)

        setContent {
            val isDark = isSystemInDarkTheme()
            val colors = if (isDark) {
                darkColors(
                    primary = Color(0xFFEF5350),
                    primaryVariant = Color(0xFFD32F2F),
                    secondary = Color(0xFF42A5F5),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    onPrimary = Color.White,
                    onBackground = Color(0xFFE0E0E0),
                    onSurface = Color(0xFFE0E0E0)
                )
            } else {
                lightColors(
                    primary = Color(0xFFD32F2F),
                    primaryVariant = Color(0xFFB71C1C),
                    secondary = Color(0xFF1976D2),
                    background = Color(0xFFF7F8FA),
                    surface = Color.White,
                    onPrimary = Color.White,
                    onBackground = Color(0xFF212121),
                    onSurface = Color(0xFF212121)
                )
            }

            MaterialTheme(colors = colors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CrashRecoveryScreen(
                        report = report,
                        isCrashLoop = isCrashLoop,
                        onCopyLog = { copyToClipboard(report) },
                        onRestart = { restartApp(safeReset = false) },
                        onSafeResetAndRestart = { restartApp(safeReset = true) }
                    )
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TiebaLimp Crash Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制崩溃日志到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun restartApp(safeReset: Boolean) {
        if (safeReset) {
            AppCrashManager.performSafeReset(this)
            Toast.makeText(this, "已重置缓存与临时配置", Toast.LENGTH_SHORT).show()
        }
        val intent = Intent(this, MainActivityV2::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finishAffinity()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}

@Composable
fun CrashRecoveryScreen(
    report: String,
    isCrashLoop: Boolean,
    onCopyLog: () -> Unit,
    onRestart: () -> Unit,
    onSafeResetAndRestart: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Crash Icon",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "应用遇到异常并已安全隔离",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "进程已隔离保护，您的数据与登录状态未受损。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Crash Loop Alert Card
            if (isCrashLoop) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚠️ 连续崩溃告警 (Crash Loop)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "检测到应用在短时间内连续发生崩溃，可能由于损坏的缓存或配置冲突引起。强烈建议点击下方「安全模式重置并重启」。",
                            fontSize = 13.sp,
                            color = Color(0xFFBF360C)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Stack Trace Details Card
            Text(
                text = "异常详细信息",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.surface)
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = report,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyLog,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("复制日志", fontSize = 14.sp)
                }

                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = Color.White
                    )
                ) {
                    Text("重启应用", fontSize = 14.sp)
                }
            }

            Button(
                onClick = onSafeResetAndRestart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White
                )
            ) {
                Text("安全模式重置并重启", fontSize = 14.sp)
            }
        }
    }
}
