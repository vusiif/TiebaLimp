package com.huanchengfly.tieba.post.utils

import android.content.Context
import com.huanchengfly.tieba.post.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

object DateTimeUtils {
    private val FORMATTER_TODAY = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }
    private val FORMATTER_MONTH_DAY = ThreadLocal.withInitial {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    }
    private val FORMATTER_YEAR_MONTH_DAY = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    private val RELATIVE_TIME_CACHE: MutableMap<Long, String> = Collections.synchronizedMap(
        object : LinkedHashMap<Long, String>(512, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>?): Boolean {
                return size > 512
            }
        }
    )

    fun clearCache() {
        RELATIVE_TIME_CACHE.clear()
    }

    @JvmStatic
    fun getRelativeTimeString(
        context: Context,
        timestampString: String
    ): String {
        return getRelativeTimeString(context, fixTimestamp(timestampString))
    }

    @JvmStatic
    fun getRelativeTimeString(
        context: Context,
        timestamp: Long
    ): String {
        val fixedTime = fixTimestamp(timestamp)
        val now = System.currentTimeMillis()
        val diff = now - fixedTime

        // Only cache formatted times that are older than 1 hour and won't change minute-by-minute
        if (diff > 3600_000L) {
            RELATIVE_TIME_CACHE[fixedTime]?.let { return it }
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = fixedTime
        }
        val currentCalendar = Calendar.getInstance()
        val result = if (currentCalendar.after(calendar)) {
            if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
                if (calendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH) &&
                    calendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
                ) {
                    if (calendar.get(Calendar.HOUR_OF_DAY) == currentCalendar.get(Calendar.HOUR_OF_DAY)) {
                        if (calendar.get(Calendar.MINUTE) == currentCalendar.get(Calendar.MINUTE)) {
                            if (calendar.get(Calendar.SECOND) == currentCalendar.get(Calendar.SECOND)) {
                                context.getString(R.string.relative_date_after)
                            } else {
                                context.getString(
                                    R.string.relative_date_second,
                                    currentCalendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND)
                                )
                            }
                        } else {
                            context.getString(
                                R.string.relative_date_minute,
                                currentCalendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE)
                            )
                        }
                    } else {
                        val formattedTime = FORMATTER_TODAY.get()?.format(Date(fixedTime)).orEmpty()
                        context.getString(
                            R.string.relative_date_today,
                            formattedTime
                        )
                    }
                } else {
                    FORMATTER_MONTH_DAY.get()?.format(Date(fixedTime)).orEmpty()
                }
            } else {
                FORMATTER_YEAR_MONTH_DAY.get()?.format(Date(fixedTime)).orEmpty()
            }
        } else {
            context.getString(R.string.relative_date_after)
        }

        if (diff > 3600_000L) {
            RELATIVE_TIME_CACHE[fixedTime] = result
        }

        return result
    }

    @JvmStatic
    fun isToday(timestamp: Long): Boolean {
        val fixedTime = fixTimestamp(timestamp)
        val date = DateFormat.getDateInstance().format(fixedTime)
        val todayDate = DateFormat.getDateInstance().format(System.currentTimeMillis())
        return date == todayDate
    }

    fun fixTimestamp(timestamp: Long): Long {
        return if (timestamp in 1..9999999999L) {
            timestamp * 1000L
        } else {
            timestamp
        }
    }

    fun fixTimestamp(timestampString: String): Long {
        val longVal = timestampString.toLongOrNull() ?: return 0L
        return fixTimestamp(longVal)
    }
}
