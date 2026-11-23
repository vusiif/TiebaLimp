package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Looper
import android.os.Process
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import com.github.gzuliyujiang.oaid.DeviceID
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.SketchFactory
import com.github.panpf.sketch.sketch
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.OAIDGetter
import com.huanchengfly.tieba.post.ui.common.theme.interfaces.ThemeSwitcher
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ActivityStackManager
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.SharedPreferencesUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.packageInfo
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.litepal.LitePal

@HiltAndroidApp
class App : Application(), SketchFactory {

    @RequiresApi(api = 28)
    private fun setWebViewPath(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName(context)
            if (applicationContext.packageName != processName) {
                WebView.setDataDirectorySuffix(processName!!)
            }
        }
    }

    private fun getProcessName(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        val manager = context.getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return null
        for (processInfo in manager.runningAppProcesses ?: emptyList()) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }

    override fun onCreate() {
        INSTANCE = this
        super.onCreate()
        ClientUtils.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setWebViewPath(this)
        }
        LitePal.initialize(this)
        AccountUtil.init(this)
        Config.init(this)
        AppIconUtil.setIcon()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        ThemeUtils.init(ThemeDelegate)
        registerActivityLifecycleCallbacks(ClipBoardLinkDetector)
        registerActivityLifecycleCallbacks(OAIDGetter)
        val cachedUa = SharedPreferencesUtil.get(this, SharedPreferencesUtil.SP_WEBVIEW_INFO)
            .getString("user_agent", null)
        if (!cachedUa.isNullOrEmpty()) {
            Config.userAgent = cachedUa
        } else {
            Looper.myQueue().addIdleHandler {
                try {
                    if (Config.userAgent == null) {
                        val ua = WebSettings.getDefaultUserAgent(this@App)
                        Config.userAgent = ua
                        SharedPreferencesUtil.put(this@App, SharedPreferencesUtil.SP_WEBVIEW_INFO, "user_agent", ua)
                    }
                } catch (_: Throwable) {
                    Config.userAgent = System.getProperty("http.agent")
                }
                false
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            BlockManager.init()
            EmoticonManager.init(this@App)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            sketch.memoryCache.trim(level)
            sketch.bitmapPool.trim(level)
        } catch (_: Throwable) {
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            sketch.memoryCache.clear()
            sketch.bitmapPool.clear()
        } catch (_: Throwable) {
        }
    }

    @Keep
    fun mzNightModeUseOf(): Int = 2

    override fun getResources(): Resources {
        val fontScale = appPreferences.fontScale
        val resources = super.getResources()
        if (resources.configuration.fontScale != fontScale) {
            val configuration = resources.configuration
            configuration.fontScale = fontScale
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        return resources
    }

    fun addActivity(activity: Activity) = ActivityStackManager.addActivity(activity)

    @JvmOverloads
    fun removeActivity(activity: Activity, finish: Boolean = false) =
        ActivityStackManager.removeActivity(activity, finish)

    fun removeAllActivity() = ActivityStackManager.removeAllActivity()

    object Config {
        @Volatile
        var inited: Boolean = false

        @Volatile
        var isOAIDSupported: Boolean = false
        @Volatile
        var statusCode: Int = -200
        @Volatile
        var oaid: String = ""
        @Volatile
        var encodedOAID: String = ""
        @Volatile
        var isTrackLimited: Boolean = false
        @Volatile
        var userAgent: String? = null
        @Volatile
        var appFirstInstallTime: Long = 0L
        @Volatile
        var appLastUpdateTime: Long = 0L

        fun init(context: Context) {
            if (!inited) {
                val pkgInfo = context.packageInfo
                appFirstInstallTime = pkgInfo.firstInstallTime
                appLastUpdateTime = pkgInfo.lastUpdateTime
                inited = true

                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        val supported = DeviceID.supportedOAID(context)
                        isOAIDSupported = supported
                        if (supported) {
                            DeviceID.getOAID(context, OAIDGetter)
                        } else {
                            statusCode = -200
                            isTrackLimited = false
                        }
                    }
                }
            }
        }
    }

    object ScreenInfo {
        @JvmField
        var EXACT_SCREEN_HEIGHT = 0

        @JvmField
        var EXACT_SCREEN_WIDTH = 0

        @JvmField
        var SCREEN_HEIGHT = 0

        @JvmField
        var SCREEN_WIDTH = 0

        @JvmField
        var DENSITY = 0f
    }

    companion object {
        const val TAG = "App"

        @JvmStatic
        var translucentBackground: Drawable? = null

        private val packageName: String
            get() = INSTANCE.packageName

        @JvmStatic
        lateinit var INSTANCE: App
            private set

        val isInitialized: Boolean
            get() = this::INSTANCE.isInitialized

        val isSystemNight: Boolean
            get() = nightMode == Configuration.UI_MODE_NIGHT_YES

        val isFirstRun: Boolean
            get() = SharedPreferencesUtil.get(SharedPreferencesUtil.SP_APP_DATA)
                .getBoolean("first", true)

        private val nightMode: Int
            get() = INSTANCE.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    object ThemeDelegate : ThemeSwitcher by AppThemeDelegate {
        @SuppressLint("DiscouragedApi")
        fun getColorByAttr(context: Context, attrId: Int, theme: String): Int =
            AppThemeDelegate.getColorByAttr(context, attrId, theme)
    }

    override fun createSketch(): Sketch = AppSketchFactory.createSketch(this)
}
