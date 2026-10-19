package com.huanchengfly.tieba.post.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.enqueue
import com.github.panpf.sketch.resize.Scale
import com.zhihu.matisse.engine.ImageEngine

class SketchImageEngine : ImageEngine {
    override fun loadThumbnail(
        context: Context,
        resize: Int,
        placeholder: Drawable?,
        imageView: ImageView,
        uri: Uri?,
    ) {
        val requestBuilder = DisplayRequest.Builder(context, uri?.toString())
            .resize(resize, resize, scale = Scale.CENTER_CROP)
        if (placeholder != null) {
            requestBuilder.placeholder(placeholder)
        }
        requestBuilder.target(imageView).build().enqueue()
    }

    override fun loadGifThumbnail(
        context: Context,
        resize: Int,
        placeholder: Drawable?,
        imageView: ImageView,
        uri: Uri?,
    ) {
        val requestBuilder = DisplayRequest.Builder(context, uri?.toString())
            .resize(resize, resize, scale = Scale.CENTER_CROP)
        if (placeholder != null) {
            requestBuilder.placeholder(placeholder)
        }
        requestBuilder.target(imageView).build().enqueue()
    }

    override fun loadImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: ImageView,
        uri: Uri?,
    ) {
        DisplayRequest.Builder(context, uri?.toString())
            .resize(resizeX, resizeY)
            .target(imageView)
            .build()
            .enqueue()
    }

    override fun loadLargeImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: SubsamplingScaleImageView,
        uri: Uri?,
    ) {
        if (uri != null) {
            imageView.setImage(ImageSource.uri(uri))
        }
    }

    override fun loadGifImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: ImageView,
        uri: Uri?,
    ) {
        DisplayRequest.Builder(context, uri?.toString())
            .resize(resizeX, resizeY)
            .target(imageView)
            .build()
            .enqueue()
    }

    override fun supportAnimatedGif(): Boolean = true
}