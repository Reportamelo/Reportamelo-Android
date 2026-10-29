package app.reportamelo.commons.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parsePostgresDate(dateString: String): ZonedDateTime? {
    val trimmed = dateString.trim()
    if (trimmed.isEmpty()) return null

    trimmed.toLongOrNull()?.let { raw ->
        val millis = if (raw < 100_000_000_000L) raw * 1000L else raw
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    }
    trimmed.toDoubleOrNull()?.let { raw ->
        val asLong = raw.toLong()
        val millis = if (asLong < 100_000_000_000L) (raw * 1000.0).toLong() else asLong
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    }

    return try {
        ZonedDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME)
    } catch (_: Exception) {
        try {
            Instant.parse(trimmed).atZone(ZoneId.systemDefault())
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(trimmed).atZoneSameInstant(ZoneId.systemDefault())
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(trimmed).atZone(ZoneId.systemDefault())
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}

fun formatRelativeDate(date: ZonedDateTime): String {
    return formatRelativeEpochMillis(date.toInstant().toEpochMilli())
}

fun formatRelativeDate(date: java.util.Date): String {
    return formatRelativeEpochMillis(date.time)
}

fun formatRelativeDate(dateString: String, nowMillis: Long = System.currentTimeMillis()): String {
    if (dateString.isBlank()) return ""
    val parsed = parsePostgresDate(dateString) ?: return dateString
    return formatRelativeEpochMillis(parsed.toInstant().toEpochMilli(), nowMillis = nowMillis)
}

fun formatRelativeEpochMillis(timeMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diffMillis = (nowMillis - timeMillis).coerceAtLeast(0L)
    val seconds = diffMillis / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    val weeks = days / 7L
    val months = days / 30L
    val years = days / 365L

    return when {
        years >= 1L -> "$years yr ago"
        months >= 1L -> "$months mo ago"
        weeks >= 1L -> "$weeks wk ago"
        days >= 1L -> if (days == 1L) "1 day ago" else "$days days ago"
        hours >= 1L -> "$hours hr ago"
        minutes >= 1L -> "$minutes min ago"
        else -> "now"
    }
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
