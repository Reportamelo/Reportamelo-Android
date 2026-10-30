package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.communityIssues.controllers.InsightsNavigation
import app.reportamelo.communityIssues.ui.components.CustomCalendar
import app.reportamelo.communityIssues.ui.theme.Dimensions
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.models.DaySummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InsightsCalendarView(
    modifier: Modifier = Modifier,
    activityData: Map<String, DaySummary> = emptyMap(),
    onNavigate: (InsightsNavigation) -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US) }

    Column(modifier = modifier.fillMaxWidth()) {
        CustomCalendar(
            selectedDate = selectedDate,
            activityMap = activityData,
            onDateSelected = { date ->
                selectedDate = date
                val dateFormatted = date.format(formatter)
                if (activityData.containsKey(dateFormatted)) {
                    onNavigate(InsightsNavigation.Activity(date = dateFormatted))
                } else {
                    onNavigate(InsightsNavigation.NoActivity)
                }
            }
        )
    }
}

@Composable
fun SimpleView(
    modifier: Modifier = Modifier,
    title: String,
    selectedDay: DaySummary? = null,
    dateFormatter: String = "",
    onBack: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground

    val localizedDateString = remember(dateFormatter) {
        if (dateFormatter.isBlank()) {
            ""
        } else {
            try {
                val parsed = LocalDate.parse(
                    dateFormatter,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
                )
                parsed.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
            } catch (_: Exception) {
                dateFormatter
            }
        }
    }

    val dynamicTitle = if (title == "NoActivityView") {
        "No Activity"
    } else if (localizedDateString.isNotEmpty()) {
        localizedDateString
    } else {
        title
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top orange gradient matching Color.orange.opacity(0.121).frame(height: 180)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF9500).copy(alpha = 0.121f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar with circular back button and centered inline title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (onBack != null) {
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .align(Alignment.CenterStart),
                        shape = CircleShape,
                        color = cardBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Text(
                    text = dynamicTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            if (title == "NoActivityView") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalendarExclamationIcon(
                        calendarColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        badgeColor = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.background
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No activity",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You didn't create any reports on this day.",
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (selectedDay != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Section 1: Reports
                    item {
                        ActivitySectionHeader(title = "Reports")
                        if (selectedDay.reports.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                selectedDay.reports.forEach { report ->
                                    Surface(
                                        modifier = Modifier.widthIn(max = 260.dp),
                                        shape = RoundedCornerShape(Dimensions.ThemeCardCornerRadius),
                                        color = cardBackground,
                                        tonalElevation = 0.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                                        )
                                    ) {
                                        Text(
                                            text = report.id,
                                            modifier = Modifier.padding(
                                                horizontal = 18.dp,
                                                vertical = 16.dp
                                            ),
                                            fontSize = 17.sp,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            EmptyActivitySectionCard(
                                text = "No reports available",
                                cardBackground = cardBackground
                            )
                        }
                    }

                    // Section 2: Signatures
                    item {
                        ActivitySectionHeader(title = "Signatures")
                        if (selectedDay.signatures.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                selectedDay.signatures.forEach { signature ->
                                    Surface(
                                        modifier = Modifier.widthIn(max = 260.dp),
                                        shape = RoundedCornerShape(Dimensions.ThemeCardCornerRadius),
                                        color = cardBackground,
                                        tonalElevation = 0.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                                        )
                                    ) {
                                        Text(
                                            text = signature.id,
                                            modifier = Modifier.padding(
                                                horizontal = 18.dp,
                                                vertical = 16.dp
                                            ),
                                            fontSize = 17.sp,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            EmptyActivitySectionCard(
                                text = "No signatures available",
                                cardBackground = cardBackground
                            )
                        }
                    }

                    // Section 3: Petitions
                    item {
                        ActivitySectionHeader(title = "Petitions")
                        if (selectedDay.petitions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                selectedDay.petitions.forEach { petition ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(Dimensions.ThemeCardCornerRadius),
                                        color = cardBackground,
                                        tonalElevation = 0.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                                        )
                                    ) {
                                        Text(
                                            text = petition.title,
                                            modifier = Modifier.padding(
                                                horizontal = 18.dp,
                                                vertical = 16.dp
                                            ),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        } else {
                            EmptyActivitySectionCard(
                                text = "No petitions available",
                                cardBackground = cardBackground
                            )
                        }
                    }

                    // Section 4: Comments
                    item {
                        ActivitySectionHeader(title = "Comments")
                        if (selectedDay.comments.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                selectedDay.comments.forEach { comment ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(Dimensions.ThemeCardCornerRadius),
                                        color = cardBackground,
                                        tonalElevation = 0.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                                        )
                                    ) {
                                        Text(
                                            text = comment.title,
                                            modifier = Modifier.padding(
                                                horizontal = 18.dp,
                                                vertical = 16.dp
                                            ),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        } else {
                            EmptyActivitySectionCard(
                                text = "No comments available",
                                cardBackground = cardBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitySectionHeader(
    modifier: Modifier = Modifier,
    title: String
) {
    Text(
        text = title,
        modifier = modifier.padding(start = 12.dp, bottom = 10.dp),
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    )
}

@Composable
private fun EmptyActivitySectionCard(
    modifier: Modifier = Modifier,
    text: String,
    cardBackground: Color
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = cardBackground,
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun CalendarExclamationIcon(
    modifier: Modifier = Modifier,
    calendarColor: Color,
    badgeColor: Color,
    backgroundColor: Color
) {
    Canvas(modifier = modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 3.2.dp.toPx()

        val calLeft = w * 0.08f
        val calTop = h * 0.12f
        val calWidth = w * 0.72f
        val calHeight = h * 0.64f
        val corner = 8.dp.toPx()

        // Calendar outer frame
        drawRoundRect(
            color = calendarColor,
            topLeft = Offset(calLeft, calTop),
            size = Size(calWidth, calHeight),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // Calendar solid top bar
        val headerHeight = calHeight * 0.28f
        val headerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset(calLeft, calTop),
                        size = Size(calWidth, headerHeight)
                    ),
                    topLeft = CornerRadius(corner, corner),
                    topRight = CornerRadius(corner, corner),
                    bottomLeft = CornerRadius.Zero,
                    bottomRight = CornerRadius.Zero
                )
            )
        }
        drawPath(path = headerPath, color = calendarColor)

        // Grid of 2x4 dots inside calendar body
        val dotRadius = 2.1.dp.toPx()
        val cols = 4
        val rows = 2
        val gridTop = calTop + headerHeight + (calHeight - headerHeight) * 0.32f
        val rowSpacing = (calHeight - headerHeight) * 0.36f
        val colStart = calLeft + calWidth * 0.20f
        val colSpacing = calWidth * 0.20f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawCircle(
                    color = calendarColor,
                    radius = dotRadius,
                    center = Offset(
                        x = colStart + c * colSpacing,
                        y = gridTop + r * rowSpacing
                    )
                )
            }
        }

        // Exclamation badge at bottom-right
        val badgeCenter = Offset(w * 0.74f, h * 0.72f)
        val badgeRadius = w * 0.20f

        // Background halo to separate badge from calendar frame
        drawCircle(
            color = backgroundColor,
            radius = badgeRadius + 2.5.dp.toPx(),
            center = badgeCenter
        )

        // Orange badge circle
        drawCircle(
            color = badgeColor,
            radius = badgeRadius,
            center = badgeCenter
        )

        // Exclamation mark stem
        drawLine(
            color = Color.White,
            start = Offset(badgeCenter.x, badgeCenter.y - badgeRadius * 0.45f),
            end = Offset(badgeCenter.x, badgeCenter.y + badgeRadius * 0.08f),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Exclamation mark dot
        drawCircle(
            color = Color.White,
            radius = 1.6.dp.toPx(),
            center = Offset(badgeCenter.x, badgeCenter.y + badgeRadius * 0.42f)
        )
    }
}
