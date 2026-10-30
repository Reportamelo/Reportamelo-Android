package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.commons.*
import app.reportamelo.communityIssues.controllers.MyReportsViewModel
import app.reportamelo.communityIssues.datamodel.*
import app.reportamelo.communityIssues.datamodel.Report
import app.reportamelo.communityIssues.ui.theme.DarkInputText
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.communityIssues.ui.theme.LightInputText
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground

val IssueTypes.color: Color
    get() = when (this) {
        IssueTypes.ALL -> Color(0xFFF68322)
        IssueTypes.ROAD -> Color(0xFFFF3B30)
        IssueTypes.PUBLIC_SPACE -> Color(0xFFFF3B30)
        IssueTypes.BUILDING -> Color(0xFFFF3B30)
        IssueTypes.OTHER -> Color(0xFFFF3B30)
    }

val IssueTypes.iconName: String
    get() = when (this) {
        IssueTypes.ALL -> "asterisk"
        IssueTypes.ROAD -> "road.lanes"
        IssueTypes.PUBLIC_SPACE -> "tree"
        IssueTypes.BUILDING -> "building"
        IssueTypes.OTHER -> "ellipsis"
    }

val Severity.color: Color
    get() = when (this) {
        Severity.ALL -> Color(0xFFF68322)
        Severity.LOW -> Color(0xFF007AFF)
        Severity.MEDIUM -> Color(0xFFFF9500)
        Severity.HIGH -> Color(0xFFFF3B30)
    }

val Severity.iconName: String
    get() = when (this) {
        Severity.ALL -> "asterisk"
        Severity.LOW -> "dial.low"
        Severity.MEDIUM -> "dial.medium"
        Severity.HIGH -> "dial.high"
    }

val IssueStatus.color: Color
    get() = when (this) {
        IssueStatus.REPORTED -> Color(0xFFFF2D55)
        IssueStatus.CONFIRMED -> Color(0xFF34C759)
        IssueStatus.IN_PROGRESS -> Color(0xFFFF9500)
        IssueStatus.PETITION_TO_SIGN -> Color(0xFFAF52DE)
        IssueStatus.FIXED -> Color(0xFF007AFF)
        IssueStatus.ASSIGNED -> Color(0xFFFFCC00)
        IssueStatus.REJECTED -> Color(0xFFFF3B30)
        IssueStatus.RECTIFICATION -> Color(0xFF8E8E93)
        IssueStatus.HUMAN_REVIEW -> Color(0xFF32ADE6)
        IssueStatus.UNDER_REVIEW -> Color(0xFF007AFF)
    }

val IssueStatus.iconName: String
    get() = when (this) {
        IssueStatus.REPORTED -> "exclamationmark.bubble"
        IssueStatus.CONFIRMED -> "checkmark.square"
        IssueStatus.IN_PROGRESS -> "arrow.trianglehead.2.clockwise.rotate.90"
        IssueStatus.PETITION_TO_SIGN -> "signature"
        IssueStatus.FIXED -> "wrench.and.screwdriver"
        IssueStatus.ASSIGNED -> "person.badge.plus"
        IssueStatus.REJECTED -> "xmark.bin"
        IssueStatus.RECTIFICATION -> "eraser.trianglebadge.exclamationmark"
        IssueStatus.HUMAN_REVIEW -> "person.crop.circle.badge.plus"
        IssueStatus.UNDER_REVIEW -> "person.fill.checkmark"
    }

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
}

@Composable
fun HeaderText(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun BottomText(
    modifier: Modifier = Modifier,
    text: String,
    multiline: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 12.sp,
        lineHeight = if (multiline) 16.sp else 15.sp,
        color = Color(0xFF8E8E93),
        maxLines = if (multiline) Int.MAX_VALUE else 1,
        overflow = if (multiline) TextOverflow.Clip else TextOverflow.Ellipsis
    )
}

@Composable
fun GenericDatePresenterView(
    modifier: Modifier = Modifier,
    text: String,
    `when`: String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(18.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                HeaderText(text = text)
                BottomText(text = `when`)
            }
        }
    }
}

@Composable
fun BadgeIcon(
    modifier: Modifier = Modifier,
    iconName: String,
    tint: Color
) {
    when (iconName) {
        "road.lanes" -> {
            // Draws SF Symbol `road.lanes` (/ i \)
            Canvas(modifier = modifier.size(16.dp)) {
                val strokeWidth = 1.6.dp.toPx()
                val h = size.height
                val w = size.width
                // Left slanted lane line
                drawLine(
                    color = tint,
                    start = Offset(w * 0.34f, h * 0.15f),
                    end = Offset(w * 0.14f, h * 0.85f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Right slanted lane line
                drawLine(
                    color = tint,
                    start = Offset(w * 0.66f, h * 0.15f),
                    end = Offset(w * 0.86f, h * 0.85f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Center dashed line (3 segments)
                val segments = listOf(0.16f to 0.30f, 0.42f to 0.58f, 0.70f to 0.86f)
                segments.forEach { (startRatio, endRatio) ->
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.5f, h * startRatio),
                        end = Offset(w * 0.5f, h * endRatio),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        "dial.low", "dial.medium", "dial.high" -> {
            // Draws SF Symbol `dial.low` / `dial.medium` / `dial.high`
            Canvas(modifier = modifier.size(16.dp)) {
                val strokeWidth = 1.35.dp.toPx()
                val center = Offset(size.width * 0.5f, size.height * 0.56f)
                val dialRadius = size.minDimension * 0.27f
                drawCircle(
                    color = tint,
                    radius = dialRadius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                val angleDeg = when (iconName) {
                    "dial.low" -> -135f
                    "dial.medium" -> -90f
                    else -> -45f
                }
                val rad = Math.toRadians(angleDeg.toDouble())
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(
                        x = center.x + (dialRadius * 0.85f * kotlin.math.cos(rad)).toFloat(),
                        y = center.y + (dialRadius * 0.85f * kotlin.math.sin(rad)).toFloat()
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Outer tick dots around the dial
                val tickAngles = listOf(-195f, -155f, -115f, -90f, -65f, -25f, 15f)
                val tickDist = size.minDimension * 0.43f
                tickAngles.forEach { deg ->
                    val r = Math.toRadians(deg.toDouble())
                    drawCircle(
                        color = tint,
                        radius = 0.9.dp.toPx(),
                        center = Offset(
                            x = center.x + (tickDist * kotlin.math.cos(r)).toFloat(),
                            y = center.y + (tickDist * kotlin.math.sin(r)).toFloat()
                        )
                    )
                }
            }
        }
        "xmark.bin" -> {
            // Draws SF Symbol `xmark.bin` (bin with X inside)
            Canvas(modifier = modifier.size(16.dp)) {
                val strokeWidth = 1.35.dp.toPx()
                val w = size.width
                val h = size.height
                // Top rim
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.16f),
                    size = Size(w * 0.76f, h * 0.14f),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                // Bin trapezoid body
                val bodyPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.30f)
                    lineTo(w * 0.24f, h * 0.84f)
                    lineTo(w * 0.76f, h * 0.84f)
                    lineTo(w * 0.82f, h * 0.30f)
                    close()
                }
                drawPath(path = bodyPath, color = tint, style = Stroke(width = strokeWidth))
                // X inside bin
                drawLine(
                    color = tint,
                    start = Offset(w * 0.38f, h * 0.46f),
                    end = Offset(w * 0.62f, h * 0.70f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.62f, h * 0.46f),
                    end = Offset(w * 0.38f, h * 0.70f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        "checkmark.square" -> {
            Icon(
                imageVector = Icons.Outlined.CheckBox,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(16.dp)
            )
        }
        "person.badge.plus", "person.crop.circle.badge.plus" -> {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(16.dp)
            )
        }
        "exclamationmark.bubble" -> {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(15.dp)
            )
        }
        "arrow.trianglehead.2.clockwise.rotate.90" -> {
            Icon(
                imageVector = Icons.Outlined.Sync,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(16.dp)
            )
        }
        "signature" -> {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(15.dp)
            )
        }
        "wrench.and.screwdriver" -> {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(15.dp)
            )
        }
        "person.fill.checkmark" -> {
            Icon(
                imageVector = Icons.Outlined.HowToReg,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(16.dp)
            )
        }
        "tree" -> {
            Icon(
                imageVector = Icons.Outlined.Park,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(16.dp)
            )
        }
        "building" -> {
            Icon(
                imageVector = Icons.Outlined.Business,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(16.dp)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CustomBadgeView(
    modifier: Modifier = Modifier,
    title: String,
    color: Color,
    iconName: String
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) DarkInputText else LightInputText
    val pillBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        shape = CircleShape,
        color = pillBackground,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BadgeIcon(
                iconName = iconName,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReportCellView(
    modifier: Modifier = Modifier,
    report: Report,
    enableChevron: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.title,
                fontSize = 21.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.3).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = report.description,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = Color(0xFF8E8E93),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GenericDatePresenterView(
                    modifier = Modifier.weight(1f),
                    text = "Created",
                    `when` = report.createdDate
                )
                GenericDatePresenterView(
                    modifier = Modifier.weight(1f),
                    text = "Updated",
                    `when` = report.updatedDate
                )
                GenericDatePresenterView(
                    modifier = Modifier.weight(1f),
                    text = "Assigned",
                    `when` = report.reportedDate
                )
            }

            report.observations?.let { obs ->
                if (obs.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 10.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        HeaderText(text = "Observations")
                        BottomText(text = obs, multiline = true)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeaderText(text = "Issue type")
                    CustomBadgeView(
                        title = report.issueType.title,
                        color = report.issueType.color,
                        iconName = report.issueType.iconName
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeaderText(text = "Severity")
                    CustomBadgeView(
                        title = report.severity.title,
                        color = report.severity.color,
                        iconName = report.severity.iconName
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeaderText(text = "Status")
                    CustomBadgeView(
                        title = report.status.title,
                        color = report.status.color,
                        iconName = report.status.iconName
                    )
                }
            }
        }

        if (enableChevron) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsSubView(
    modifier: Modifier = Modifier,
    subViewName: String = "My Reports",
    mode: ViewOptions = ViewOptions.LIST,
    viewModel: MyReportsViewModel = viewModel(),
    onBack: (() -> Unit)? = null
) {
    val reports by viewModel.reports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val refreshID by viewModel.refreshID.collectAsState()

    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground

    LaunchedEffect(refreshID) {
        viewModel.fetchReports()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .radialGradientBackground(topLeading = true, radiusFraction = 0.54f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar matching iOS NavigationBar with circular back button and centered title
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
                    text = subViewName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading && reports.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingView()
                    }
                } else if (reports.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF8E8E93)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No reports yet.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please send us reports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E8E93),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = reports,
                            key = { it.id ?: it.hashCode() }
                        ) { report ->
                            val canModify = mode == ViewOptions.LIST_AND_MODIFY &&
                                (report.status == IssueStatus.REPORTED || report.status == IssueStatus.RECTIFICATION)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (canModify) {
                                            Modifier.clickable {
                                                ReportDataModel.shared.prepareForModification(report)
                                            }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                shape = RoundedCornerShape(24.dp),
                                color = cardBackground,
                                tonalElevation = 0.dp,
                                shadowElevation = 2.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                ReportCellView(
                                    report = report,
                                    enableChevron = canModify
                                )
                            }

                            if (report.id == reports.lastOrNull()?.id) {
                                LaunchedEffect(report.id) {
                                    viewModel.fetchReports(loadMore = true)
                                }
                            }
                        }

                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingView()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
