package com.huanchengfly.tieba.post.ui.page.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCodeScanner
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
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.request.DisplayRequest
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.BaiduQrLoginHelper
import com.huanchengfly.tieba.post.api.QrLoginState
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.webview.MyWebChromeClient
import com.huanchengfly.tieba.post.ui.page.webview.MyWebViewClient
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
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.parseCookie
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
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
                                it.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
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
                    qrState = QrLoginState.Expired
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
                                            text = "已扫描",
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

private fun saveAccountAndFinish(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    navigator: DestinationsNavigator?,
    bduss: String,
    sToken: String,
    cookieStr: String,
    baiduId: String? = null,
    onSuccess: (() -> Unit)? = null,
    onFailure: (() -> Unit)? = null,
) {
    if (!baiduId.isNullOrEmpty() && ClientUtils.baiduId.isNullOrEmpty()) {
        coroutineScope.launch {
            ClientUtils.saveBaiduId(context, baiduId)
        }
    }
    coroutineScope.launch {
        snackbarHostState.showSnackbar(
            context.getString(R.string.text_please_wait),
            duration = SnackbarDuration.Indefinite
        )
    }
    coroutineScope.launch {
        AccountUtil.fetchAccountFlow(bduss, sToken, cookieStr)
            .catch {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.text_login_failed,
                            it.getErrorMessage()
                        ), duration = SnackbarDuration.Short
                    )
                }
                onFailure?.invoke()
            }
            .flowOn(Dispatchers.Main)
            .collect { account ->
                AccountUtil.newAccount(account.uid, account) { success ->
                    if (success) {
                        AccountUtil.switchAccount(context, account.id)
                        onSuccess?.invoke()
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.text_login_success),
                                duration = SnackbarDuration.Short
                            )
                        }
                        coroutineScope.launch {
                            delay(1500L)
                            navigator?.navigateUp()
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.text_login_failed_default),
                                duration = SnackbarDuration.Short
                            )
                        }
                        onFailure?.invoke()
                    }
                }
            }
    }
}

class LoginWebViewClient(
    nativeNavigator: DestinationsNavigator? = null,
    val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
) : MyWebViewClient(nativeNavigator) {
    private var isLoadingAccount = false

    override fun injectCookies(url: String) {}

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url == null || isLoadingAccount) {
            return
        }
        val cookieStr = CookieManager.getInstance().getCookie(url) ?: return
        val cookies = parseCookie(cookieStr).mapKeys { it.key.uppercase() }
        val bduss = cookies["BDUSS"]
        val sToken = cookies["STOKEN"]
        val baiduId = cookies["BAIDUID"]
        if (url.startsWith("https://tieba.baidu.com/index/tbwise/") || url.startsWith("https://tiebac.baidu.com/index/tbwise/")) {
            if (bduss == null || sToken == null) {
                return
            }
            isLoadingAccount = true
            saveAccountAndFinish(
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                navigator = nativeNavigator,
                bduss = bduss,
                sToken = sToken,
                cookieStr = cookieStr,
                baiduId = baiduId,
                onFailure = {
                    navigator.loadUrl(LOGIN_URL)
                    isLoadingAccount = false
                }
            )
        }
    }
}