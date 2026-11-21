package com.huanchengfly.tieba.post.activities

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.LoadResult
import com.github.panpf.sketch.request.execute
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

object TranslucentThemeCropHelper {
    fun launchUCrop(
        activity: Activity,
        scope: CoroutineScope,
        sourceUri: Uri,
        progressView: View? = null,
    ) {
        progressView?.visibility = View.VISIBLE
        scope.launch {
            val result = LoadRequest(activity, sourceUri.toString()).execute()
            if (result is LoadResult.Success) {
                progressView?.visibility = View.GONE
                val file = ImageUtil.bitmapToFile(
                    result.bitmap,
                    File(activity.cacheDir, "origin_background.jpg")
                )
                val sourceFileUri = Uri.fromFile(file)
                val destUri = Uri.fromFile(File(activity.filesDir, "cropped_background.jpg"))
                val height = App.ScreenInfo.EXACT_SCREEN_HEIGHT.toFloat()
                val width = App.ScreenInfo.EXACT_SCREEN_WIDTH.toFloat()
                UCrop.of(sourceFileUri, destUri)
                    .withAspectRatio(width / height, 1f)
                    .withOptions(UCrop.Options().apply {
                        setShowCropFrame(true)
                        setShowCropGrid(true)
                        setStatusBarColor(
                            ColorUtils.getDarkerColor(
                                ThemeUtils.getColorByAttr(activity, R.attr.colorPrimary)
                            )
                        )
                        setToolbarColor(ThemeUtils.getColorByAttr(activity, R.attr.colorPrimary))
                        setToolbarWidgetColor(
                            ThemeUtils.getColorByAttr(activity, R.attr.colorTextOnPrimary)
                        )
                        setActiveControlsWidgetColor(
                            ThemeUtils.getColorByAttr(activity, R.attr.colorAccent)
                        )
                        setLogoColor(ThemeUtils.getColorByAttr(activity, R.attr.colorPrimary))
                        setCompressionFormat(Bitmap.CompressFormat.JPEG)
                    })
                    .start(activity)
            } else if (result is LoadResult.Error) {
                progressView?.visibility = View.GONE
                activity.toastShort(R.string.text_load_failed)
            }
        }
    }
}
