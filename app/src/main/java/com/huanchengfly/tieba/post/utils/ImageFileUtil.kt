package com.huanchengfly.tieba.post.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.channels.FileChannel

object ImageFileUtil {
    fun isGifFile(file: File?): Boolean {
        if (file == null) return false
        try {
            return FileInputStream(file).use { isGifFile(it) }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    fun isGifFile(inputStream: InputStream?): Boolean {
        if (inputStream == null) return false
        val bytes = ByteArray(4)
        try {
            inputStream.read(bytes)
            val str = String(bytes)
            return str.equals("GIF8", ignoreCase = true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun copyFile(src: FileInputStream?, dest: FileOutputStream?): Boolean {
        if (src == null || dest == null) {
            return false
        }
        val srcChannel: FileChannel?
        val dstChannel: FileChannel?
        try {
            srcChannel = src.channel
            dstChannel = dest.channel
            srcChannel.transferTo(0, srcChannel.size(), dstChannel)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        try {
            srcChannel.close()
            dstChannel.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    fun copyFile(src: File?, dest: File?): Boolean {
        if (src == null || dest == null) {
            return false
        }
        if (dest.exists()) {
            dest.delete()
        }
        try {
            dest.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val srcChannel: FileChannel?
        val dstChannel: FileChannel?
        try {
            srcChannel = FileInputStream(src).channel
            dstChannel = FileOutputStream(dest).channel
            srcChannel.transferTo(0, srcChannel.size(), dstChannel)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        try {
            srcChannel.close()
            dstChannel.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    fun checkGifFile(file: File) {
        if (isGifFile(file)) {
            val gifFile = File(file.parentFile, FileUtil.changeFileExtension(file.name, ".gif"))
            if (gifFile.exists()) {
                file.delete()
            } else {
                file.renameTo(gifFile)
            }
        }
    }
}
