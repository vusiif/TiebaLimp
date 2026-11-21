package com.huanchengfly.tieba.post.utils

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object ImageCompressUtil {
    fun compressImage(
        bitmap: Bitmap,
        quality: Int = 100
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, quality, baos)
        return baos.use { it.toByteArray() }
    }

    @JvmOverloads
    fun compressImage(
        bitmap: Bitmap,
        output: File,
        maxSizeKb: Int = 100,
        initialQuality: Int = 100
    ): File {
        val baos = ByteArrayOutputStream()
        var quality = initialQuality
        bitmap.compress(CompressFormat.JPEG, quality, baos)
        while (baos.toByteArray().size / 1024 > maxSizeKb && quality > 0) {
            baos.reset()
            quality -= 5
            bitmap.compress(CompressFormat.JPEG, quality, baos)
        }
        try {
            val fos = FileOutputStream(output)
            try {
                fos.write(baos.toByteArray())
                fos.flush()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return output
    }

    @JvmOverloads
    fun bitmapToFile(
        bitmap: Bitmap,
        output: File,
        format: CompressFormat = CompressFormat.JPEG
    ): File {
        val baos = ByteArrayOutputStream()
        bitmap.compress(format, 100, baos)
        try {
            val fos = FileOutputStream(output)
            try {
                fos.write(baos.toByteArray())
                fos.flush()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return output
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    fun imageToBase64(inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        return runCatching {
            inputStream.use {
                Base64.encodeToString(inputStream.readBytes(), Base64.DEFAULT)
            }
        }.getOrNull()
    }

    fun imageToBase64(file: File?): String? {
        if (file == null) {
            return null
        }
        var result: String? = null
        try {
            val `is`: InputStream = FileInputStream(file)
            result = imageToBase64(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }
}
