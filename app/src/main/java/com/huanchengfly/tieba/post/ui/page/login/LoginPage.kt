package com.huanchengfly.tieba.post.ui.page.login

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.webview.MyWebChromeClient
import com.huanchengfly.tieba.post.ui.page.webview.isInternalHost
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingState
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.WebView
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSaveableWebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberWebViewNavigator
import com.huanchengfly.tieba.post.utils.WebViewHelper
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

const val LOGIN_URL =
    "https://wappass.baidu.com/passport?login&u=https%3A%2F%2Ftieba.baidu.com%2Findex%2Ftbwise%2Fmine"

enum class LoginMode {
    QR_CODE,
    WEB,
}

@SuppressLint("SetJavaScriptEnabled")
@Destination
@Composable
fun LoginPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMode by rememberSaveable {
        mutableStateOf(LoginMode.QR_CODE)
    }
    var qrRefreshTrigger by remember {
        mutableIntStateOf(0)
    }

    val webViewState = rememberSaveableWebViewState()
    val webViewNavigator = rememberWebViewNavigator()
    var loaded by rememberSaveable {
        mutableStateOf(false)
    }
    var pageTitle by rememberSaveable {
        mutableStateOf("")
    }
    val displayPageTitle by remember {
        derivedStateOf {
            pageTitle.ifEmpty {
                context.getString(R.string.title_default)
            }
        }
    }
    val currentHost by remember {
        derivedStateOf {
            webViewState.lastLoadedUrl?.toUri()?.host.orEmpty().lowercase()
        }
    }
    val isExternalHost by remember {
        derivedStateOf {
            currentHost.isNotEmpty() && !isInternalHost(currentHost)
        }
    }

    DisposableEffect(Unit) {
        val job = coroutineScope.launch {
            snapshotFlow { webViewState.pageTitle }
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .cancellable()
                .collect {
                    pageTitle = it
                }
        }
        onDispose {
            job.cancel()
        }
    }

    val isLoading by remember {
        derivedStateOf {
            webViewState.loadingState is LoadingState.Loading
        }
    }

    val progress by remember {
        derivedStateOf {
            webViewState.loadingState.let {
                if (it is LoadingState.Loading) {
                    it.progress
                } else {
                    0f
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    MyScaffold(
        topBar = {
            Column {
                Toolbar(
                    title = {
                        if (selectedMode == LoginMode.QR_CODE) {
                            Text(
                                text = stringResource(R.string.title_login_account),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Column {
                                Text(
                                    text = displayPageTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isExternalHost) {
                                    Text(
                                        text = currentHost,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = ExtendedTheme.colors.onTopBarSecondary,
                                        style = MaterialTheme.typography.caption
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
                    actions = {
                        if (selectedMode == LoginMode.WEB) {
                            val menuState = rememberMenuState()
                            ClickMenu(
                                menuContent = {
                                    DropdownMenuItem(
                                        onClick = {
                                            webViewNavigator.reload()
                                            dismiss()
                                        }
                                    ) {
                                        Text(text = stringResource(id = R.string.title_refresh))
                                    }
                                },
                                menuState = menuState,
                                triggerShape = CircleShape
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = stringResource(id = R.string.btn_more)
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { qrRefreshTrigger++ }) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.btn_refresh_qrcode),
                                    tint = ExtendedTheme.colors.onTopBar
                                )
                            }
                        }
                    },
                )
                TabRow(
                    selectedTabIndex = selectedMode.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedMode == LoginMode.QR_CODE,
                        onClick = { selectedMode = LoginMode.QR_CODE },
                        text = {
                            Text(
                                text = stringResource(R.string.title_login_qrcode),
                                color = if (selectedMode == LoginMode.QR_CODE) ExtendedTheme.colors.primary else ExtendedTheme.colors.onTopBarSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedMode == LoginMode.WEB,
                        onClick = { selectedMode = LoginMode.WEB },
                        text = {
                            Text(
                                text = stringResource(R.string.title_login_web),
                                color = if (selectedMode == LoginMode.WEB) ExtendedTheme.colors.primary else ExtendedTheme.colors.onTopBarSecondary
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val snackbarHostState = LocalSnackbarHostState.current

            when (selectedMode) {
                LoginMode.QR_CODE -> {
                    QrLoginContent(
                        refreshTrigger = qrRefreshTrigger,
                        onSuccess = { bduss, sToken, cookieStr, baiduId ->
                            saveAccountAndFinish(
                                context = context,
                                coroutineScope = coroutineScope,
                                snackbarHostState = snackbarHostState,
                                navigator = navigator,
                                bduss = bduss,
                                sToken = sToken,
                                cookieStr = cookieStr,
                                baiduId = baiduId
                            )
                        }
                    )
                }

                LoginMode.WEB -> {
                    LazyLoad(loaded = loaded) {
                        webViewNavigator.loadUrl(LOGIN_URL)
                        loaded = true
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        WebView(
                            state = webViewState,
                            modifier = Modifier.fillMaxSize(),
                            navigator = webViewNavigator,
                            onCreated = {
                                WebViewHelper.applyDefaultSettings(it, context, enableZoom = false)
                            },
                            onDispose = {
                                WebViewHelper.destroyWebView(it)
                            },
                            client = remember(navigator) {
                                LoginWebViewClient(
                                    navigator,
                                    coroutineScope,
                                    snackbarHostState
                                )
                            },
                            chromeClient = remember { MyWebChromeClient(context, coroutineScope) }
                        )

                        if (isLoading) {
                            LinearProgressIndicator(
                                progress = animatedProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
