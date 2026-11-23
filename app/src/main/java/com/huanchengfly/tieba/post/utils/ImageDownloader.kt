package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.DownloadResult
import com.github.panpf.sketch.request.execute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.ImageUtil.ShareTaskCallback
import com.huanchengfly.tieba.post.utils.PermissionUtils.PermissionData
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.zhihu.matisse.MimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageDownloader {
    private fun pruneShareCache(folder: File) {
        runCatching {
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            folder.listFiles()?.forEach { file ->
                if (file.isFile && (file.lastModified() < oneDayAgo || file.length() == 0L)) {
                    file.delete()
                }
            }
        }
    }

    fun downloadForShare(context: Context, url: String?, taskCallback: ShareTaskCallback) {
        if (url == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val downloadResult = DownloadRequest(context, url).execute()
            if (downloadResult is DownloadResult.Success) {
                val inputStream = downloadResult.data.data.newInputStream()
                val pictureFolder = File(context.cacheDir, ".shareTemp")
                if (pictureFolder.exists() || pictureFolder.mkdirs()) {
                    pruneShareCache(pictureFolder)
                    val fileName = "share_" + System.currentTimeMillis()
                    val destFile = File(pictureFolder, fileName)
                    if (!destFile.exists()) {
                        withContext(Dispatchers.IO) {
                            destFile.createNewFile()
                        }
                    }
                    inputStream.use { input ->
                        if (destFile.canWrite()) {
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    ExifUtil.sanitizeExif(destFile)
                    val shareUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            context,
                            context.packageName + ".share.FileProvider",
                            destFile
                        )
                    } else {
                        Uri.fromFile(destFile)
                    }
                    withContext(Dispatchers.Main) {
                        taskCallback.onGetUri(shareUri)
                    }
                }
            }
        }
    }

    fun downloadAboveQ(context: Context, url: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val downloadResult = DownloadRequest(context, url).execute()
            if (downloadResult is DownloadResult.Success) {
                var mimeType = MimeType.JPEG.toString()
                var fileName = URLUtil.guessFileName(url, null, mimeType)
                downloadResult.data.data.newInputStream().use { inputStream ->
                    if (ImageFileUtil.isGifFile(inputStream)) {
                        mimeType = MimeType.GIF.toString()
                        fileName = FileUtil.changeFileExtension(fileName, ".gif")
                    }
                }
                downloadResult.data.data.newInputStream().use { inputStream ->
                    val relativePath =
                        Environment.DIRECTORY_PICTURES + File.separator + FileUtil.FILE_FOLDER
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.DESCRIPTION, fileName)
                    }
                    val cr = context.contentResolver
                    val uri: Uri = runCatching {
                        cr.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                        )
                    }.getOrNull() ?: return@launch
                    try {
                        cr.openFileDescriptor(uri, "w")?.use {
                            FileOutputStream(it.fileDescriptor).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_photo_saved, relativePath),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        cr.delete(uri, null, null)
                    }
                }
            }
        }
    }

    fun downloadBelowQ(context: Context, url: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            val downloadResult = DownloadRequest(context, url).execute()
            if (downloadResult is DownloadResult.Success) {
                var fileName = URLUtil.guessFileName(url, null, MimeType.JPEG.toString())
                downloadResult.data.data.newInputStream().use { inputStream ->
                    if (ImageFileUtil.isGifFile(inputStream)) {
                        fileName = FileUtil.changeFileExtension(fileName, ".gif")
                    }
                }
                downloadResult.data.data.newInputStream().use { inputStream ->
                    val pictureFolder =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val appDir = File(pictureFolder, FileUtil.FILE_FOLDER)
                    val dirExists =
                        withContext(Dispatchers.IO) { appDir.exists() || appDir.mkdirs() }
                    if (dirExists) {
                        val destFile = File(appDir, fileName)
                        if (!destFile.exists()) {
                            withContext(Dispatchers.IO) {
                                destFile.createNewFile()
                            }
                        }
                        destFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        context.sendBroadcast(
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(File(destFile.path))
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_photo_saved, destFile.path),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun download(context: Context, url: String?) {
        download(context, url, false, null)
    }

    @SuppressLint("StaticFieldLeak")
    fun download(
        context: Context,
        url: String?,
        forShare: Boolean,
        taskCallback: ShareTaskCallback?
    ) {
        if (forShare) {
            if (taskCallback != null) downloadForShare(context, url, taskCallback)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadAboveQ(context, url)
            return
        }
        askPermission(
            context,
            PermissionData(
                listOf(
                    PermissionUtils.READ_EXTERNAL_STORAGE,
                    PermissionUtils.WRITE_EXTERNAL_STORAGE
                ),
                context.getString(R.string.tip_permission_storage)
            ),
            R.string.toast_no_permission_save_photo
        ) {
            downloadBelowQ(context, url)
        }
    }
}
