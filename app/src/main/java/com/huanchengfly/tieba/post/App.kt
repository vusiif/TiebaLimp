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
import org.litepal.LitePal
import kotlin.concurrent.thread

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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                if (Config.userAgent == null) {
                    Config.userAgent = WebSettings.getDefaultUserAgent(this@App)
                }
            } catch (_: Throwable) {
                Config.userAgent = System.getProperty("http.agent")
            }
        }
        thread {
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
        var inited: Boolean = false

        var isOAIDSupported: Boolean = false
        var statusCode: Int = -200
        var oaid: String = ""
        var encodedOAID: String = ""
        var isTrackLimited: Boolean = false
        var userAgent: String? = null
        var appFirstInstallTime: Long = 0L
        var appLastUpdateTime: Long = 0L

        fun init(context: Context) {
            if (!inited) {
                isOAIDSupported = DeviceID.supportedOAID(context)
                if (isOAIDSupported) {
                    DeviceID.getOAID(context, OAIDGetter)
                } else {
                    statusCode = -200
                    isTrackLimited = false
                }
                appFirstInstallTime = context.packageInfo.firstInstallTime
                appLastUpdateTime = context.packageInfo.lastUpdateTime
                inited = true
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
