package com.huanchengfly.tieba.post.activities

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.palette.graphics.Palette
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.resize.Scale
import com.huanchengfly.tieba.post.components.transformations.SketchBlurTransformation
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

object TranslucentThemeImageHelper {
    fun renderBackground(
        activity: TranslucentThemeActivity,
        scope: CoroutineScope,
        uri: Uri?,
        blur: Int,
        alpha: Int,
        backgroundView: View,
        progressView: View,
        onSuccess: (drawable: Drawable, palette: Palette) -> Unit
    ) {
        progressView.visibility = View.VISIBLE
        if (uri == null) {
            backgroundView.setBackgroundColor(Color.BLACK)
            progressView.visibility = View.GONE
            return
        }
        scope.launch {
            val result = DisplayRequest(activity, uri.toString()) {
                resizeScale(Scale.CENTER_CROP)
                if (blur > 0) {
                    transformations(SketchBlurTransformation(blur))
                }
            }.execute()
            if (result is DisplayResult.Success) {
                result.drawable.alpha = alpha
                backgroundView.background = result.drawable
                val palette = Palette.from(ImageUtil.drawableToBitmap(result.drawable)).generate()
                progressView.visibility = View.GONE
                onSuccess(result.drawable, palette)
            }
        }
    }

    fun saveBackground(
        activity: TranslucentThemeActivity,
        scope: CoroutineScope,
        uri: Uri?,
        blur: Int,
        alpha: Int,
        progressView: View,
        onSuccess: (File) -> Unit
    ) {
        runCatching {
            val oldFilePath = activity.appPreferences.translucentThemeBackgroundPath
            if (oldFilePath != null) {
                val oldFile = File(oldFilePath)
                oldFile.delete()
            }
        }
        progressView.visibility = View.VISIBLE
        scope.launch {
            val result = DisplayRequest(activity, uri.toString()) {
                resizeScale(Scale.CENTER_CROP)
                if (blur > 0) {
                    transformations(SketchBlurTransformation(blur))
                }
            }.execute()
            if (result is DisplayResult.Success) {
                result.drawable.alpha = alpha
                val bitmap = ImageUtil.drawableToBitmap(result.drawable)
                val file = ImageUtil.compressImage(
                    bitmap,
                    File(activity.filesDir, "background_${System.currentTimeMillis()}.jpg"),
                    maxSizeKb = 512,
                    initialQuality = 97
                )
                activity.appPreferences.translucentThemeBackgroundPath = file.absolutePath
                ThemeUtils.refreshUI(activity, activity)
                onSuccess(file)
            }
        }
    }
}
