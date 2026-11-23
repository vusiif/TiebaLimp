package com.huanchengfly.tieba.post

import androidx.exifinterface.media.ExifInterface
import com.huanchengfly.tieba.post.utils.ExifUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExifUtilTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // 1x1 blank JPEG valid binary structure
    private val minimalJpegBytes = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x10.toByte(),
        0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x48.toByte(), 0x00.toByte(), 0x48.toByte(),
        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x43.toByte(),
        0x00.toByte(), 0x03.toByte(), 0x02.toByte(), 0x02.toByte(), 0x03.toByte(), 0x02.toByte(),
        0x02.toByte(), 0x03.toByte(), 0x03.toByte(), 0x03.toByte(), 0x03.toByte(), 0x04.toByte(),
        0x03.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x08.toByte(), 0x05.toByte(),
        0x05.toByte(), 0x04.toByte(), 0x04.toByte(), 0x05.toByte(), 0x0A.toByte(), 0x07.toByte(),
        0x07.toByte(), 0x06.toByte(), 0x08.toByte(), 0x0C.toByte(), 0x0A.toByte(), 0x0C.toByte(),
        0x0C.toByte(), 0x0B.toByte(), 0x0A.toByte(), 0x0B.toByte(), 0x0B.toByte(), 0x0D.toByte(),
        0x0E.toByte(), 0x12.toByte(), 0x10.toByte(), 0x0D.toByte(), 0x0E.toByte(), 0x11.toByte(),
        0x0E.toByte(), 0x0B.toByte(), 0x0B.toByte(), 0x10.toByte(), 0x16.toByte(), 0x10.toByte(),
        0x11.toByte(), 0x13.toByte(), 0x14.toByte(), 0x15.toByte(), 0x15.toByte(), 0x15.toByte(),
        0x0C.toByte(), 0x0F.toByte(), 0x17.toByte(), 0x18.toByte(), 0x16.toByte(), 0x14.toByte(),
        0x18.toByte(), 0x12.toByte(), 0x14.toByte(), 0x15.toByte(), 0x14.toByte(), 0xFF.toByte(),
        0xC0.toByte(), 0x00.toByte(), 0x0B.toByte(), 0x08.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x00.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x11.toByte(), 0x00.toByte(),
        0xFF.toByte(), 0xC4.toByte(), 0x00.toByte(), 0x1F.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x01.toByte(), 0x05.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(),
        0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x02.toByte(),
        0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(),
        0x09.toByte(), 0x0A.toByte(), 0x0B.toByte(), 0xFF.toByte(), 0xDA.toByte(), 0x00.toByte(),
        0x08.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x3F.toByte(),
        0x00.toByte(), 0x7F.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xD9.toByte()
    )

    @Test
    fun testSanitizeExif_nullAndNonExistentFiles() {
        assertFalse(ExifUtil.sanitizeExif(null))
        assertFalse(ExifUtil.sanitizeExif(File("/non/existent/path/image.jpg")))
        assertFalse(ExifUtil.hasSensitiveExif(null))
        assertFalse(ExifUtil.hasSensitiveExif(File("/non/existent/path/image.jpg")))
    }

    @Test
    fun testSanitizeExif_stripsSensitiveMetadataAndPreservesOrientation() {
        val testFile = tempFolder.newFile("sample_with_exif.jpg").apply {
            writeBytes(minimalJpegBytes)
        }

        // Write sensitive metadata into the test image
        val initialExif = ExifInterface(testFile.absolutePath)
        initialExif.setAttribute(ExifInterface.TAG_MAKE, "Apple")
        initialExif.setAttribute(ExifInterface.TAG_MODEL, "iPhone 15 Pro")
        initialExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "39/1,54/1,20/1")
        initialExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
        initialExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "116/1,23/1,45/1")
        initialExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
        initialExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        initialExif.saveAttributes()

        // Verify that sensitive EXIF tags exist before sanitization
        assertTrue(ExifUtil.hasSensitiveExif(testFile))

        // Execute sanitization
        val result = ExifUtil.sanitizeExif(testFile)
        assertTrue(result)

        // Verify that sensitive EXIF tags are completely stripped
        assertFalse(ExifUtil.hasSensitiveExif(testFile))

        val sanitizedExif = ExifInterface(testFile.absolutePath)
        assertNull(sanitizedExif.getAttribute(ExifInterface.TAG_MAKE))
        assertNull(sanitizedExif.getAttribute(ExifInterface.TAG_MODEL))
        assertNull(sanitizedExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(sanitizedExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
        assertNull(sanitizedExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        assertNull(sanitizedExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))

        // Verify that orientation is strictly preserved
        assertEquals(
            ExifInterface.ORIENTATION_ROTATE_90.toString(),
            sanitizedExif.getAttribute(ExifInterface.TAG_ORIENTATION)
        )
    }

    @Test
    fun testCleanFile_hasNoSensitiveExif() {
        val cleanFile = tempFolder.newFile("clean.jpg").apply {
            writeBytes(minimalJpegBytes)
        }

        assertFalse(ExifUtil.hasSensitiveExif(cleanFile))
        val result = ExifUtil.sanitizeExif(cleanFile)
        assertTrue(result)
        assertFalse(ExifUtil.hasSensitiveExif(cleanFile))
    }
}
