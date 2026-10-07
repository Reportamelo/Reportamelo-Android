package app.reportamelo.commons.utils

import android.text.format.DateUtils as AndroidDateUtils
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parsePostgresDate(dateString: String): ZonedDateTime? {
    return try {
        ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        null
    }
}

fun formatRelativeDate(date: ZonedDateTime): String {
    val now = System.currentTimeMillis()
    val time = date.toInstant().toEpochMilli()
    return AndroidDateUtils.getRelativeTimeSpanString(time, now, AndroidDateUtils.MINUTE_IN_MILLIS).toString()
}

fun applyFormat(timestamp: String?): String {
    if (timestamp != null) {
        val date = parsePostgresDate(timestamp) ?: return "Invalid date"
        return formatRelativeDate(date)
    }
    return "Never"
}

enum class DateFormat {
    DASHED, JOINED
}

fun createdAtNow(format: DateFormat): String {
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val formatter = if (format == DateFormat.JOINED) {
        DateTimeFormatter.ofPattern("yyyyMMdd")
    } else {
        DateTimeFormatter.ofPattern("yyyyMM-dd")
    }
    return now.format(formatter)
}

fun getMonthName(): String {
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
    return now.format(formatter).lowercase()
}

fun getFullYear(): String {
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy")
    return now.format(formatter)
}

fun ZonedDateTime.addingDays(days: Int): ZonedDateTime {
    return this.plusDays(days.toLong())
}

fun ZonedDateTime.reduceDays(days: Int): ZonedDateTime {
    return this.minusDays(days.toLong())
}
