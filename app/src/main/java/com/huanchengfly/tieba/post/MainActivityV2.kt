package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.plusAssign
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.systemuicontroller.SystemUiController
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.ClipBoardForumLink
import com.huanchengfly.tieba.post.components.ClipBoardLink
import com.huanchengfly.tieba.post.components.ClipBoardThreadLink
import com.huanchengfly.tieba.post.services.NotifyJobService
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.NavGraphs
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.isBookPosture
import com.huanchengfly.tieba.post.ui.utils.isSeparating
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AppStartupProfiler
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.JobServiceUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.compose.LaunchActivityForResult
import com.huanchengfly.tieba.post.utils.compose.LaunchActivityRequest
import com.huanchengfly.tieba.post.utils.newIntentFilter
import com.huanchengfly.tieba.post.utils.registerPickMediasLauncher
import com.huanchengfly.tieba.post.utils.requestPermission
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val newMessageReceiver: BroadcastReceiver = NewMessageReceiver()

    private val notificationCountFlow: MutableSharedFlow<Int> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val pickMediasLauncher =
        registerPickMediasLauncher {
            emitGlobalEvent(GlobalEvent.SelectedImages(it.id, it.uris))
        }

    private val mLaunchActivityForResultLauncher = registerForActivityResult(
        LaunchActivityForResult()
    ) {
        emitGlobalEvent(GlobalEvent.ActivityResult(it.requesterId, it.resultCode, it.intent))
    }

    private val devicePostureFlow: StateFlow<DevicePosture> by lazy {
        WindowInfoTracker.getOrCreate(this)
            .windowLayoutInfo(this)
            .flowWithLifecycle(lifecycle)
            .map { layoutInfo ->
                val foldingFeature =
                    layoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .firstOrNull()
                when {
                    isBookPosture(foldingFeature) ->
                        DevicePosture.BookPosture(foldingFeature.bounds)

                    isSeparating(foldingFeature) ->
                        DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

                    else -> DevicePosture.NormalPosture
                }
            }
            .stateIn(
                scope = lifecycleScope,
                started = SharingStarted.Eagerly,
                initialValue = DevicePosture.NormalPosture
            )
    }

    private var direction: Direction? = null
    private var waitingNavCollectorToNavigate = AtomicBoolean(false)
    private var myNavController: NavHostController? = null
        set(value) {
            field = value
            if (value != null && waitingNavCollectorToNavigate.get() && direction != null) {
                launch {
                    value.currentDestinationFlow
                        .take(1)
                        .collect {
                            if (waitingNavCollectorToNavigate.get() && direction != null) {
                                value.navigate(direction!!)
                                waitingNavCollectorToNavigate.set(false)
                                direction = null
                            }
                        }
                }
            }
        }

    private fun navigate(direction: Direction) {
        if (myNavController == null) {
            waitingNavCollectorToNavigate.set(true)
            this.direction = direction
        } else {
            myNavController?.navigate(direction)
        }
    }

    private fun checkIntent(intent: Intent): Boolean {
        if (MainActivityIntentDispatcher.isTiebaDispatchIntent(intent)) {
            val resolvedDirection = MainActivityIntentDispatcher.resolveDirection(intent)
            if (resolvedDirection != null) {
                navigate(resolvedDirection)
            }
            return true
        }
        return false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (!checkIntent(it)) {
                myNavController?.handleDeepLink(it)
            }
        }
    }

    private fun fetchAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (AccountUtil.isLoggedIn()) {
                AccountUtil.fetchAccountFlow()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        toastShort(e.getErrorMessage())
                        e.printStackTrace()
                    }
                    .collect()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && AccountUtil.isLoggedIn()) {
            requestPermission {
                permissions = listOf(PermissionUtils.POST_NOTIFICATIONS)
                description = getString(R.string.desc_permission_post_notifications)
            }
        }
    }

    private fun initAutoSign() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                TiebaUtil.initAutoSign(this@MainActivityV2)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching {
            ContextCompat.registerReceiver(
                this,
                newMessageReceiver,
                newIntentFilter(NotifyJobService.ACTION_NEW_MESSAGE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val builder = JobInfo.Builder(
                        JobServiceUtil.getJobId(this@MainActivityV2),
                        ComponentName(this@MainActivityV2, NotifyJobService::class.java)
                    )
                        .setPersisted(true)
                        .setPeriodic(30 * 60 * 1000L)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    jobScheduler.schedule(builder.build())
                }
            }
        }
        handler.postDelayed({
            requestNotificationPermission()
        }, 100)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppStartupProfiler.markFirstActivityCreated()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(0)
        window.setBackgroundDrawable(ColorDrawable(0))
        launch {
            ClientUtils.setActiveTimestamp()
        }
        intent?.let { checkIntent(it) }
        window.decorView.post {
            AppStartupProfiler.markFirstFrameRendered()
        }
    }

    override fun onCreateContent(systemUiController: SystemUiController) {
        super.onCreateContent(systemUiController)
        fetchAccount()
        initAutoSign()
    }

    private fun openClipBoardLink(link: ClipBoardLink) {
        when (link) {
            is ClipBoardThreadLink -> {
                myNavController?.navigate(Uri.parse("tblite://thread/${link.threadId}"))
            }

            is ClipBoardForumLink -> {
                myNavController?.navigate(Uri.parse("tblite://forum/${link.forumName}"))
            }

            else -> {}
        }
    }

    @OptIn(ExperimentalMaterialNavigationApi::class)
    @Composable
    override fun Content() {
        ClipBoardDetectDialog(onOpenLink = { openClipBoardLink(it) })
        BatteryOptimizationDialog()

        onGlobalEvent<GlobalEvent.StartSelectImages> {
            pickMediasLauncher.launch(
                PickMediasRequest(it.id, it.maxCount, it.mediaType)
            )
        }
        onGlobalEvent<GlobalEvent.StartActivityForResult> {
            mLaunchActivityForResultLauncher.launch(
                LaunchActivityRequest(
                    it.requesterId,
                    it.intent
                )
            )
        }
        onGlobalEvent<GlobalEvent.AccountSwitched> {
            fetchAccount()
        }
        TiebaLiteLocalProvider {
            TranslucentThemeBackground {
                val navController = rememberNavController()
                val engine = TiebaNavHostDefaults.rememberNavHostEngine()
                val navigator = TiebaNavHostDefaults.rememberBottomSheetNavigator()
                val currentDestination by navController.currentDestinationAsState()

                navController.navigatorProvider += navigator

                CompositionLocalProvider(
                    LocalNavController provides navController,
                    LocalDestination provides currentDestination,
                ) {
                    ModalBottomSheetLayout(
                        bottomSheetNavigator = navigator,
                        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        sheetBackgroundColor = ExtendedTheme.colors.windowBackground,
                        scrimColor = Color.Black.copy(alpha = 0.32f),
                    ) {
                        DestinationsNavHost(
                            navController = navController,
                            navGraph = NavGraphs.root,
                            engine = engine,
                        )
                    }
                }

                SideEffect {
                    myNavController = navController
                }
            }
        }
    }

    @Composable
    fun TiebaLiteLocalProvider(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalNotificationCountFlow provides notificationCountFlow,
            LocalDevicePosture provides devicePostureFlow.collectAsState(),
        ) {
            content()
        }
    }

    private inner class NewMessageReceiver : BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == NotifyJobService.ACTION_NEW_MESSAGE) {
                val channel = intent.getStringExtra("channel")
                val count = intent.getIntExtra("count", 0)
                if (channel != null && channel == NotifyJobService.CHANNEL_TOTAL) {
                    lifecycleScope.launch {
                        notificationCountFlow.emit(count)
                    }
                }
            }
        }
    }
}
