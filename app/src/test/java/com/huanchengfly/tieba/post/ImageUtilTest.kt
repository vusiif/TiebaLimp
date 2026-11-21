package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.ImageFileUtil
import com.huanchengfly.tieba.post.utils.ImageUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class ImageUtilTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun getNonNullString_returnsFirstNonEmptyString() {
        assertEquals("hello", ImageUtil.getNonNullString(null, "", "hello", "world"))
        assertEquals("first", ImageUtil.getNonNullString("first", "second"))
        assertNull(ImageUtil.getNonNullString(null, ""))
        assertNull(ImageUtil.getNonNullString())
    }

    @Test
    fun isGifFile_identifiesGifMagicBytes() {
        val gif89a = "GIF89a".toByteArray()
        val gif87a = "GIF87a".toByteArray()
        val nonGif = "RIFF\u0000\u0000\u0000\u0000WEBP".toByteArray()

        assertTrue(ImageFileUtil.isGifFile(ByteArrayInputStream(gif89a)))
        assertTrue(ImageFileUtil.isGifFile(ByteArrayInputStream(gif87a)))
        assertFalse(ImageFileUtil.isGifFile(ByteArrayInputStream(nonGif)))
        assertFalse(ImageFileUtil.isGifFile(null as ByteArrayInputStream?))
    }

    @Test
    fun isGifFile_fileVariant() {
        val gifFile = tempFolder.newFile("sample.gif").apply {
            writeBytes("GIF89aTestBytes".toByteArray())
        }
        val textFile = tempFolder.newFile("sample.txt").apply {
            writeBytes("Hello World".toByteArray())
        }

        assertTrue(ImageFileUtil.isGifFile(gifFile))
        assertFalse(ImageFileUtil.isGifFile(textFile))
        assertFalse(ImageFileUtil.isGifFile(null as File?))
    }

    @Test
    fun copyFile_copiesBytesAccurately() {
        val srcFile = tempFolder.newFile("source.bin").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        }
        val destFile = File(tempFolder.root, "destination.bin")

        val success = ImageFileUtil.copyFile(srcFile, destFile)
        assertTrue(success)
        assertTrue(destFile.exists())
        assertEquals(srcFile.length(), destFile.length())
        assertTrue(srcFile.readBytes().contentEquals(destFile.readBytes()))
    }
}
