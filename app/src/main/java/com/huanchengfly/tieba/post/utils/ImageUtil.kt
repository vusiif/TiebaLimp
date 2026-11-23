package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.webkit.URLUtil
import android.widget.ImageView
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.github.panpf.sketch.disposeDisplay
import com.github.panpf.sketch.request.Depth
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.enqueue
import com.github.panpf.sketch.transform.CircleCropTransformation
import com.github.panpf.sketch.transform.RoundedCornersTransformation
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.utils.ThemeUtil.isNightMode
import com.zhihu.matisse.MimeType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtil {
    /**
     * 智能省流
     */
    const val SETTINGS_SMART_ORIGIN = 0

    /**
     * 智能无图
     */
    const val SETTINGS_SMART_LOAD = 1

    /**
     * 始终高质量
     */
    const val SETTINGS_ALL_ORIGIN = 2

    /**
     * 始终无图
     */
    const val SETTINGS_ALL_NO = 3

    const val LOAD_TYPE_SMALL_PIC = 0
    const val LOAD_TYPE_AVATAR = 1
    const val LOAD_TYPE_NO_RADIUS = 2
    const val LOAD_TYPE_ALWAYS_ROUND = 3
    const val TAG = "ImageUtil"

    interface ShareTaskCallback {
        fun onGetUri(uri: Uri)
    }

    @IntDef(LOAD_TYPE_SMALL_PIC, LOAD_TYPE_AVATAR, LOAD_TYPE_NO_RADIUS, LOAD_TYPE_ALWAYS_ROUND)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LoadType

    @IntDef(SETTINGS_SMART_ORIGIN, SETTINGS_SMART_LOAD, SETTINGS_ALL_ORIGIN, SETTINGS_ALL_NO)
    annotation class ImageLoadSettings

    // File helpers delegation
    fun isGifFile(file: File?): Boolean = ImageFileUtil.isGifFile(file)
    fun isGifFile(inputStream: InputStream?): Boolean = ImageFileUtil.isGifFile(inputStream)
    fun copyFile(src: FileInputStream?, dest: FileOutputStream?): Boolean = ImageFileUtil.copyFile(src, dest)
    fun copyFile(src: File?, dest: File?): Boolean = ImageFileUtil.copyFile(src, dest)
    fun checkGifFile(file: File) = ImageFileUtil.checkGifFile(file)

    // Compress & bitmap helpers delegation
    fun compressImage(bitmap: Bitmap, quality: Int = 100): ByteArray =
        ImageCompressUtil.compressImage(bitmap, quality)

    @JvmOverloads
    fun compressImage(bitmap: Bitmap, output: File, maxSizeKb: Int = 100, initialQuality: Int = 100): File =
        ImageCompressUtil.compressImage(bitmap, output, maxSizeKb, initialQuality)

    @JvmOverloads
    fun bitmapToFile(bitmap: Bitmap, output: File, format: CompressFormat = CompressFormat.JPEG): File =
        ImageCompressUtil.bitmapToFile(bitmap, output, format)

    fun drawableToBitmap(drawable: Drawable): Bitmap =
        ImageCompressUtil.drawableToBitmap(drawable)

    fun imageToBase64(inputStream: InputStream?): String? =
        ImageCompressUtil.imageToBase64(inputStream)

    fun imageToBase64(file: File?): String? =
        ImageCompressUtil.imageToBase64(file)

    // Download helpers delegation
    @SuppressLint("StaticFieldLeak")
    fun download(context: Context, url: String?) =
        ImageDownloader.download(context, url)

    @SuppressLint("StaticFieldLeak")
    fun download(
        context: Context,
        url: String?,
        forShare: Boolean,
        taskCallback: ShareTaskCallback?
    ) = ImageDownloader.download(context, url, forShare, taskCallback)

    fun getPicId(picUrl: String?): String {
        val fileName = URLUtil.guessFileName(picUrl, null, MimeType.JPEG.toString())
        return fileName.replace(".jpg", "")
    }

    @JvmStatic
    fun getNonNullString(vararg strings: String?): String? {
        for (url in strings) {
            if (!url.isNullOrEmpty()) {
                return url
            }
        }
        return null
    }

    @JvmStatic
    fun getRadiusPx(context: Context): Int {
        return DisplayUtil.dp2px(context, getRadiusDp(context).toFloat())
    }

    private fun getRadiusDp(context: Context): Int {
        return context.appPreferences.radius
    }

    fun clear(imageView: ImageView?) {
        imageView?.disposeDisplay()
    }

    @Volatile
    private var cachedNightPlaceholderColor: Int? = null
    @Volatile
    private var cachedLightPlaceholderColor: Int? = null

    fun getPlaceholderColor(context: Context): Int {
        val night = isNightMode()
        return if (night) {
            cachedNightPlaceholderColor ?: ContextCompat.getColor(context, R.color.color_place_holder_night).also {
                cachedNightPlaceholderColor = it
            }
        } else {
            cachedLightPlaceholderColor ?: ContextCompat.getColor(context, R.color.color_place_holder).also {
                cachedLightPlaceholderColor = it
            }
        }
    }

    fun getPlaceHolder(context: Context, radius: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.setColor(getPlaceholderColor(context))
        drawable.cornerRadius =
            DisplayUtil.dp2px(context, radius.toFloat()).toFloat()
        return drawable
    }

    fun getPlaceHolder(context: Context, radius: Float): Drawable {
        val drawable = GradientDrawable()
        drawable.setColor(getPlaceholderColor(context))
        drawable.cornerRadius = radius.dpToPxFloat()
        return drawable
    }

    private fun changeBrightness(imageView: ImageView, brightness: Int) {
        val cMatrix = ColorMatrix()
        cMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness.toFloat(), 0f, 1f, 0f, 0f, brightness.toFloat(),
                0f, 0f, 1f, 0f, brightness.toFloat(), 0f, 0f, 0f, 1f, 0f
            )
        )
        imageView.colorFilter = ColorMatrixColorFilter(cMatrix)
    }

    @SuppressLint("CheckResult")
    fun load(
        imageView: ImageView,
        @LoadType type: Int,
        url: String?,
        skipNetworkCheck: Boolean,
        noTransition: Boolean,
    ) {
        if (!Util.canLoadGlide(imageView.context)) {
            return
        }
        if (isNightMode()) {
            changeBrightness(imageView, -35)
        } else {
            imageView.clearColorFilter()
        }
        val radius = getRadiusDp(imageView.context).toFloat()
        val requestBuilder =
            if (skipNetworkCheck ||
                type == LOAD_TYPE_AVATAR ||
                imageLoadSettings == SETTINGS_ALL_ORIGIN ||
                imageLoadSettings == SETTINGS_SMART_ORIGIN ||
                (imageLoadSettings == SETTINGS_SMART_LOAD && NetworkUtil.isWifiConnected(imageView.context))
            ) {
                imageView.setTag(R.id.image_load_tag, true)
                DisplayRequest.Builder(imageView.context, url)
            } else {
                imageView.setTag(R.id.image_load_tag, false)
                DisplayRequest.Builder(imageView.context, url).depth(Depth.LOCAL)
            }
        when (type) {
            LOAD_TYPE_SMALL_PIC -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, radius))
                .transformations(RoundedCornersTransformation(radius.dpToPxFloat()))

            LOAD_TYPE_AVATAR -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 6f))
                .transformations(RoundedCornersTransformation(6f.dpToPxFloat()))

            LOAD_TYPE_NO_RADIUS -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 0f))

            LOAD_TYPE_ALWAYS_ROUND -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 100f))
                .transformations(CircleCropTransformation())
        }
        if (!noTransition) {
            requestBuilder.crossfade()
        }
        requestBuilder.target(imageView).build().enqueue()
    }

    @JvmStatic
    @JvmOverloads
    fun load(
        imageView: ImageView,
        @LoadType type: Int,
        url: String?,
        skipNetworkCheck: Boolean = false
    ) {
        load(imageView, type, url, skipNetworkCheck, false)
    }

    @JvmStatic
    fun getUrl(
        context: Context,
        isSmallPic: Boolean,
        originUrl: String,
        vararg smallPicUrls: String?
    ): String {
        val urls = mutableListOf(*smallPicUrls)
        if (isSmallPic) {
            if (needReverse(context)) {
                urls.reverse()
            }
            return urls.firstOrNull { !it.isNullOrEmpty() } ?: originUrl
        }
        return originUrl
    }

    private fun needReverse(context: Context): Boolean {
        return if (imageLoadSettings == SETTINGS_SMART_ORIGIN &&
            NetworkUtil.isWifiConnected(context)
        ) false
        else imageLoadSettings != SETTINGS_ALL_ORIGIN
    }

    @get:ImageLoadSettings
    private val imageLoadSettings: Int
        get() = INSTANCE.appPreferences.imageLoadType!!.toInt()
}

fun ImageUtil.download(
    context: Context,
    url: String?,
    forShare: Boolean,
    taskCallback: (Uri) -> Unit
) {
    download(context, url, forShare, object : ImageUtil.ShareTaskCallback {
        override fun onGetUri(uri: Uri) {
            taskCallback(uri)
        }
    })
}
