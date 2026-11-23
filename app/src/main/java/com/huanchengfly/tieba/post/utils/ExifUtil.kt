package com.huanchengfly.tieba.post.utils

import androidx.exifinterface.media.ExifInterface
import java.io.File

object ExifUtil {

    private val SENSITIVE_TAGS = listOf(
        // GPS location metadata
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_DOP,

        // Device and owner identification
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,

        // Exact shoot time down to milliseconds
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED
    )

    /**
     * Sanitizes sensitive EXIF metadata (GPS, Device Info, Owner, Timestamp) from [file],
     * while strictly preserving image orientation.
     * Returns true if sanitization completed successfully or no file needed sanitization.
     */
    fun sanitizeExif(file: File?): Boolean {
        if (file == null || !file.exists() || !file.canWrite()) {
            return false
        }
        return runCatching {
            val exif = ExifInterface(file.absolutePath)
            // Preserve orientation so display isn't inadvertently rotated
            val orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION)
                ?: ExifInterface.ORIENTATION_NORMAL.toString()

            var modified = false
            for (tag in SENSITIVE_TAGS) {
                if (exif.getAttribute(tag) != null) {
                    exif.setAttribute(tag, null)
                    modified = true
                }
            }

            if (modified) {
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation)
                exif.saveAttributes()
            }
            true
        }.getOrElse { false }
    }

    /**
     * Checks if the image file contains any sensitive metadata.
     */
    fun hasSensitiveExif(file: File?): Boolean {
        if (file == null || !file.exists()) return false
        return runCatching {
            val exif = ExifInterface(file.absolutePath)
            SENSITIVE_TAGS.any { tag -> exif.getAttribute(tag) != null }
        }.getOrDefault(false)
    }
}
