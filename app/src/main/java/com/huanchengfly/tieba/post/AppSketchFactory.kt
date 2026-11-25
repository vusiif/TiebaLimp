package com.huanchengfly.tieba.post

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.decode.GifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.GifMovieDrawableDecoder
import com.github.panpf.sketch.decode.HeifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.WebpAnimatedDrawableDecoder
import com.github.panpf.sketch.http.OkHttpStack
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDrawableDecodeInterceptor
import com.github.panpf.sketch.request.pauseLoadWhenScrolling
import com.github.panpf.sketch.resize.LongImageScaleDecider
import com.github.panpf.sketch.resize.Precision
import com.huanchengfly.tieba.post.api.getUserAgent
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import java.util.concurrent.TimeUnit

/**
 * Factory helper for initializing the global Sketch image loading pipeline.
 */
object AppSketchFactory {
    fun createSketch(context: Context): Sketch {
        val isLowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true ||
            Runtime.getRuntime().maxMemory() <= 128L * 1024 * 1024
        val sketchHttpClient = RetrofitTiebaApi.baseOkHttpClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", getUserAgent())
                    .build()
                chain.proceed(request)
            }
            .build()
        return Sketch.Builder(context).apply {
            httpStack(OkHttpStack(sketchHttpClient))
            globalImageOptions(ImageOptions {
                crossfade(durationMillis = 200)
                resizeApplyToDrawable(true)
                resizePrecision(Precision.LESS_PIXELS)
                resizeScale(LongImageScaleDecider())
                pauseLoadWhenScrolling(true)
                disallowReuseBitmap(false)
                if (isLowRam) {
                    bitmapConfig(Bitmap.Config.RGB_565)
                }
            })
            components {
                addDrawableDecodeInterceptor(PauseLoadWhenScrollingDrawableDecodeInterceptor())
                addDrawableDecoder(
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> GifAnimatedDrawableDecoder.Factory()
                        else -> GifMovieDrawableDecoder.Factory()
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    addDrawableDecoder(WebpAnimatedDrawableDecoder.Factory())
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    addDrawableDecoder(HeifAnimatedDrawableDecoder.Factory())
                }
            }
        }.build()
    }
}
