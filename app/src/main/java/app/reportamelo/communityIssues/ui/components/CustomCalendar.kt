package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.models.DaySummary
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun isDateWithinInsightsRange(date: LocalDate, now: LocalDate = LocalDate.now()): Boolean {
    val minMonth = YearMonth.of(2026, 1)
    val currentYearMonth = YearMonth.from(now)
    val maxMonth = if (currentYearMonth.isBefore(minMonth)) minMonth else currentYearMonth
    val minDate = minMonth.atDay(1)
    val maxDate = maxMonth.atEndOfMonth()
    return !date.isBefore(minDate) && !date.isAfter(maxDate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCalendar(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    activityMap: Map<String, DaySummary>,
    onDateSelected: (LocalDate) -> Unit
) {
    val minMonth = remember { YearMonth.of(2026, 1) }
    val maxMonth = remember {
        val now = YearMonth.now()
        if (now.isBefore(minMonth)) minMonth else now
    }
    val minDate = remember { minMonth.atDay(1) }
    val maxDate = remember { maxMonth.atEndOfMonth() }

    var currentMonth by remember {
        val initial = YearMonth.from(selectedDate)
        mutableStateOf(
            when {
                initial.isBefore(minMonth) -> minMonth
                initial.isAfter(maxMonth) -> maxMonth
                else -> initial
            }
        )
    }

    var showDateInputModal by remember { mutableStateOf(false) }

    val canGoBack = currentMonth.isAfter(minMonth)
    val canGoForward = currentMonth.isBefore(maxMonth)
    val today = remember { LocalDate.now() }
    val orangeColor = MaterialTheme.colorScheme.primary

    if (showDateInputModal) {
        val initialMillis = remember(selectedDate) {
            selectedDate.coerceIn(minDate, maxDate)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = IntRange(minMonth.year, maxMonth.year),
            initialDisplayMode = DisplayMode.Input,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    return !date.isBefore(minDate) && !date.isAfter(maxDate)
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year in minMonth.year..maxMonth.year
                }
            }
        )

        val selectedMillis = datePickerState.selectedDateMillis
        val pickedDate = remember(selectedMillis) {
            selectedMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
            }
        }
        val isConfirmEnabled = pickedDate != null &&
            !pickedDate.isBefore(minDate) &&
            !pickedDate.isAfter(maxDate)

        DatePickerDialog(
            onDismissRequest = { showDateInputModal = false },
            confirmButton = {
                TextButton(
                    enabled = isConfirmEnabled,
                    onClick = {
                        pickedDate?.let { validDate ->
                            currentMonth = YearMonth.from(validDate)
                            showDateInputModal = false
                            onDateSelected(validDate)
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateInputModal = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Month header + navigation arrows matching iOS UICalendarView
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDateInputModal = true }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                Text(
                    text = "$monthName ${currentMonth.year}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Select date",
                    tint = orangeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = if (canGoBack) orangeColor else orangeColor.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canGoBack) {
                            if (currentMonth.isAfter(minMonth)) {
                                currentMonth = currentMonth.minusMonths(1)
                            }
                        }
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = if (canGoForward) orangeColor else orangeColor.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canGoForward) {
                            if (currentMonth.isBefore(maxMonth)) {
                                currentMonth = currentMonth.plusMonths(1)
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday labels: SUN MON TUE WED THU FRI SAT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
        val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

        var day = 1
        for (week in 0..5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                for (d in 0..6) {
                    if (week == 0 && d < firstDayOfWeek || day > daysInMonth) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )
                    } else {
                        val date = currentMonth.atDay(day)
                        val dateString = date.format(formatter)
                        val summary = activityMap[dateString]
                        val isHighlighted = date == selectedDate || date == today

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onDateSelected(date)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isHighlighted) orangeColor.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isHighlighted) orangeColor else MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier.height(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (summary != null && summary.interactions > 0) {
                                    val dotSize = when {
                                        summary.interactions >= 5 -> 8.dp
                                        summary.interactions >= 3 -> 6.dp
                                        else -> 4.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(dotSize)
                                            .clip(CircleShape)
                                            .background(orangeColor)
                                    )
                                }
                            }
                        }
                        day++
                    }
                }
            }
            if (day > daysInMonth) break
        }
    }
}
