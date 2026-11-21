package com.huanchengfly.tieba.post.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.ColorInt
import androidx.core.text.HtmlCompat
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.huanchengfly.tieba.post.App.Companion.translucentBackground
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.adapters.TranslucentThemeColorAdapter
import com.huanchengfly.tieba.post.adapters.WallpaperAdapter
import com.huanchengfly.tieba.post.components.MyLinearLayoutManager
import com.huanchengfly.tieba.post.components.dividers.HorizontalSpacesDecoration
import com.huanchengfly.tieba.post.interfaces.OnItemClickListener
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.widgets.theme.TintMaterialButton
import com.huanchengfly.tieba.post.dpToPx
import com.huanchengfly.tieba.post.enableChangingLayoutTransition
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil.TRANSLUCENT_THEME_DARK
import com.huanchengfly.tieba.post.utils.ThemeUtil.TRANSLUCENT_THEME_LIGHT
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.registerPickMediasLauncher
import com.huanchengfly.tieba.post.utils.requestPermission
import com.huanchengfly.tieba.post.utils.shouldUsePhotoPicker
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.yalantis.ucrop.UCrop
import java.io.File

class TranslucentThemeActivity : BaseActivity(), View.OnClickListener, OnSeekBarChangeListener,
    ColorPickerDialogListener {
    private var mUri: Uri? = null
    private var alpha = 0
    private var blur = 0
    private var mPalette: Palette? = null

    val mSelectColor: View by lazy { findViewById(R.id.select_color) }
    val recommendWallpapers: View by lazy { findViewById(R.id.recommend_wallpapers) }
    val recommendWallpapersRv: RecyclerView by lazy { findViewById(R.id.wallpapers_rv) }
    val mProgress: View by lazy { findViewById(R.id.progress) }
    val darkColorBtn: TintMaterialButton by lazy { findViewById(R.id.dark_color) }
    val lightColorBtn: TintMaterialButton by lazy { findViewById(R.id.light_color) }
    val backBtn: View by lazy { findViewById(R.id.button_back) }
    val bottomSheet: LinearLayout by lazy { findViewById(R.id.bottom_sheet) }
    val finishBtn: View by lazy { findViewById(R.id.button_finish) }
    val maskView: View by lazy { findViewById(R.id.mask) }
    val experimentalTipView: View by lazy { findViewById(R.id.experimental_tip) }
    val colorTheme: ViewGroup by lazy { findViewById(R.id.color_theme) }

    private val selectImageLauncher = registerPickMediasLauncher { (_, uris) ->
        if (uris.isNotEmpty()) {
            val sourceUri = uris[0]
            launchUCrop(sourceUri)
        }
    }

    var wallpapers: List<String>? = null
        set(value) {
            field = value
            refreshWallpapers()
        }
    private val wallpaperAdapter: WallpaperAdapter by lazy { WallpaperAdapter(this) }

    private val mTranslucentThemeColorAdapter: TranslucentThemeColorAdapter by lazy {
        TranslucentThemeColorAdapter(this)
    }

    private fun launchUCrop(sourceUri: Uri) {
        TranslucentThemeCropHelper.launchUCrop(this, this, sourceUri, mProgress)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            mUri = UCrop.getOutput(data!!)
            invalidateFinishBtn()
            refreshBackground()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError!!.printStackTrace()
        }
    }

    private fun refreshWallpapers() {
        if (wallpapers.isNullOrEmpty()) {
            recommendWallpapers.visibility = View.GONE
        } else {
            recommendWallpapers.visibility = View.VISIBLE
            wallpaperAdapter.setData(wallpapers)
        }
    }

    private fun refreshBackground() {
        TranslucentThemeImageHelper.renderBackground(
            activity = this,
            scope = this,
            uri = mUri,
            blur = blur,
            alpha = alpha,
            backgroundView = findViewById(R.id.background),
            progressView = mProgress
        ) { _, palette ->
            mPalette = palette
            mTranslucentThemeColorAdapter.setPalette(mPalette)
            mSelectColor.visibility = View.VISIBLE
        }
    }

    override fun refreshStatusBarColor() {
        ImmersionBar.with(this)
            .transparentBar()
            .init()
    }

    override fun getLayoutId(): Int = R.layout.activity_translucent_theme

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        experimentalTipView.setOnClickListener {
            showDialog {
                setTitle(R.string.title_translucent_theme_experimental_feature)
                setMessage(
                    HtmlCompat.fromHtml(
                        getString(R.string.tip_translucent_theme),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                )
                setNegativeButton(R.string.btn_close, null)
            }
        }
        listOf(
            findViewById(R.id.custom_color),
            findViewById(R.id.select_pic),
            darkColorBtn,
            lightColorBtn,
            backBtn,
            finishBtn
        ).forEach {
            it.setOnClickListener(this@TranslucentThemeActivity)
        }
        wallpapers = TranslucentWallpaperHelper.getCachedWallpapers(this)
        colorTheme.enableChangingLayoutTransition()
        wallpaperAdapter.setOnItemClickListener { _, item, _ ->
            launchUCrop(Uri.parse(item))
        }
        recommendWallpapersRv.addItemDecoration(
            HorizontalSpacesDecoration(0, 0, 16.dpToPx(), 16.dpToPx(), false)
        )
        recommendWallpapersRv.adapter = wallpaperAdapter
        recommendWallpapersRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mTranslucentThemeColorAdapter.onItemClickListener =
            OnItemClickListener { _: View?, themeColor: Int, _: Int, _: Int ->
                appPreferences.translucentPrimaryColor = toString(themeColor)
                maskView.post { ThemeUtils.refreshUI(this, this) }
            }
        findViewById<RecyclerView>(R.id.select_color_recycler_view).apply {
            addItemDecoration(HorizontalSpacesDecoration(0, 0, 12.dpToPx(), 12.dpToPx(), false))
            layoutManager = MyLinearLayoutManager(
                this@TranslucentThemeActivity,
                MyLinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = mTranslucentThemeColorAdapter
        }
        alpha = appPreferences.translucentBackgroundAlpha
        blur = appPreferences.translucentBackgroundBlur
        findViewById<SeekBar>(R.id.alpha).apply {
            progress = this@TranslucentThemeActivity.alpha
            setOnSeekBarChangeListener(this@TranslucentThemeActivity)
        }
        findViewById<SeekBar>(R.id.blur).apply {
            progress = this@TranslucentThemeActivity.blur
            setOnSeekBarChangeListener(this@TranslucentThemeActivity)
        }
        mProgress.setOnTouchListener { _: View?, _: MotionEvent? -> true }
        mProgress.visibility = View.GONE
        val file = File(filesDir, "cropped_background.jpg")
        if (file.exists()) {
            mUri = Uri.fromFile(file)
            invalidateFinishBtn()
        }
        TranslucentBottomSheetHelper.setupBottomSheet(bottomSheet, maskView)
        refreshBackground()
        refreshTheme()
        fetchWallpapers()
    }

    private fun fetchWallpapers() {
        TranslucentWallpaperHelper.fetchWallpapers(this, this) {
            wallpapers = it
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        appPreferences.translucentPrimaryColor = toString(color)
        ThemeUtils.refreshUI(this, this)
    }

    override fun onDialogDismissed(dialogId: Int) {}

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select_color -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun invalidateFinishBtn() {
        finishBtn.visibility = if (mUri != null) View.VISIBLE else View.GONE
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {
        when (seekBar.id) {
            R.id.alpha -> alpha = seekBar.progress
            R.id.blur -> blur = seekBar.progress
        }
        refreshBackground()
    }

    private fun refreshTheme() {
        TranslucentThemeColorUtil.applyThemeModeButtons(
            context = this,
            darkColorBtn = darkColorBtn,
            lightColorBtn = lightColorBtn,
            themeMode = appPreferences.translucentBackgroundTheme
        )
    }

    override fun finish() {
        ImageCacheUtil.clearImageMemoryCache(this)
        super.finish()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_finish -> {
                appPreferences.apply {
                    translucentBackgroundAlpha = alpha
                    translucentBackgroundBlur = blur
                }
                TranslucentThemeImageHelper.saveBackground(
                    activity = this,
                    scope = this,
                    uri = mUri,
                    blur = blur,
                    alpha = alpha,
                    progressView = mProgress
                ) {
                    ThemeUtil.switchTheme(ThemeUtil.THEME_TRANSLUCENT, false)
                    toastShort(R.string.toast_save_pic_success)
                    translucentBackground = null
                    mProgress.visibility = View.GONE
                    finish()
                }
            }
            R.id.button_back -> finish()
            R.id.select_pic -> askPermission {
                selectImageLauncher.launch(PickMediasRequest(mediaType = PickMediasRequest.ImageOnly))
            }
            R.id.custom_color -> {
                val primaryColorPicker = ColorPickerDialog.newBuilder()
                    .setDialogTitle(R.string.title_color_picker_primary)
                    .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                    .setShowAlphaSlider(true)
                    .setDialogId(0)
                    .setAllowPresets(false)
                    .setColor(ThemeUtils.getColorById(this, R.color.default_color_primary))
                    .create()
                primaryColorPicker.setColorPickerDialogListener(this)
                primaryColorPicker.show(
                    supportFragmentManager,
                    "ColorPicker_TranslucentThemePrimaryColor"
                )
            }
            R.id.dark_color -> {
                appPreferences.translucentBackgroundTheme = TRANSLUCENT_THEME_DARK
                refreshTheme()
            }
            R.id.light_color -> {
                appPreferences.translucentBackgroundTheme = TRANSLUCENT_THEME_LIGHT
                refreshTheme()
            }
        }
    }

    private fun askPermission(granted: () -> Unit) {
        if (shouldUsePhotoPicker()) {
            granted()
            return
        }
        requestPermission {
            unchecked = true
            permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                listOf(
                    PermissionUtils.READ_EXTERNAL_STORAGE,
                    PermissionUtils.WRITE_EXTERNAL_STORAGE
                )
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                listOf(PermissionUtils.READ_EXTERNAL_STORAGE)
            } else {
                listOf(PermissionUtils.READ_MEDIA_IMAGES)
            }
            description = getString(R.string.tip_permission_storage)
            onGranted = granted
            onDenied = { toastShort(R.string.toast_no_permission_insert_photo) }
        }
    }

    interface SavePicCallback<T> {
        fun onSuccess(t: T)
    }

    companion object {
        val TAG = TranslucentThemeActivity::class.java.simpleName
        const val REQUEST_CODE_CHOOSE = 2

        fun toString(alpha: Int, red: Int, green: Int, blue: Int): String {
            return TranslucentThemeColorUtil.argbToHexString(alpha, red, green, blue)
        }

        fun toString(@ColorInt color: Int): String {
            return TranslucentThemeColorUtil.colorToHexString(color)
        }
    }
}
