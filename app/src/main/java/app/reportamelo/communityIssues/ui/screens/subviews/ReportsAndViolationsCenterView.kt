package app.reportamelo.communityIssues.ui.screens.subviews

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.commons.ReportViolationStatus
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.commons.utils.parsePostgresDate
import app.reportamelo.communityIssues.controllers.ModerationRole
import app.reportamelo.communityIssues.controllers.ModerationViewModel
import app.reportamelo.communityIssues.ui.components.LinkType
import app.reportamelo.communityIssues.ui.components.LinksView
import app.reportamelo.communityIssues.ui.components.LocalToastState
import app.reportamelo.communityIssues.ui.components.ToastType
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.repository.UserRepository
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val SecondarySubtitleColor = Color(0xFF8E8E93)

val TypeOfContentToReport.iconVector: ImageVector
    get() = when (this) {
        TypeOfContentToReport.IMAGE -> Icons.Outlined.Photo
        TypeOfContentToReport.REPORT -> Icons.Outlined.ChatBubbleOutline
        TypeOfContentToReport.VIDEO -> Icons.Outlined.Videocam
        TypeOfContentToReport.COMMENT -> Icons.AutoMirrored.Outlined.Comment
        TypeOfContentToReport.PETITION -> Icons.Outlined.Forum
        TypeOfContentToReport.ACCOUNT -> Icons.Outlined.AccountCircle
    }

internal fun setOpacityByStatus(status: ReportViolationStatus): Float {
    return when (status) {
        ReportViolationStatus.REJECTED,
        ReportViolationStatus.APPEALING,
        ReportViolationStatus.APPROVED -> 0.3f
        else -> 1f
    }
}

internal fun disableButtonByStatus(status: ReportViolationStatus): Boolean {
    return when (status) {
        ReportViolationStatus.REJECTED,
        ReportViolationStatus.APPEALING,
        ReportViolationStatus.APPROVED -> true
        else -> false
    }
}

internal fun formatViolationDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Unknown"
    val parsed = parsePostgresDate(dateString) ?: return "Unknown"
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    return parsed.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndViolationsCenterView(
    modifier: Modifier = Modifier,
    viewModel: ModerationViewModel = viewModel(),
    userRepository: UserRepository = UserRepository.shared,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val toastState = LocalToastState.current
    val coroutineScope = rememberCoroutineScope()

    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val indictedModeratedContent by viewModel.indictedModeratedContent.collectAsState()
    val myComplaints by viewModel.myComplaints.collectAsState()
    val showAlert by viewModel.showAlert.collectAsState()
    val showReportRemovalAlert by viewModel.showReportRemovalAlert.collectAsState()
    val showConfirmationOfRemoval by viewModel.showConfirmationOfRemoval.collectAsState()
    val selectedReportId by viewModel.selectedReportId.collectAsState()
    val selectedReportType by viewModel.selectedReportType.collectAsState()
    val selectedProfileId by viewModel.selectedProfileId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.67f)

    LaunchedEffect(Unit) {
        viewModel.loadComplaints()
        viewModel.loadIndictedData()
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { toast ->
            viewModel.clearToastMessage()
            coroutineScope.launch {
                toastState.show(message = toast.message, type = toast.type)
            }
        }
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text("Do you want to appeal for removal") },
            text = { Text("Are you sure you want to appeal for removal of this report?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val type = selectedReportType
                        viewModel.dismissAlert()
                        if (type != null) {
                            viewModel.appealForReportRemoval(selectedReportId, type)
                        }
                    }
                ) {
                    Text("Yes, remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("No, cancel")
                }
            }
        )
    }

    if (showReportRemovalAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReportRemovalAlert() },
            title = { Text("Moderation report") },
            text = { Text("Are you sure you want to remove this moderation report?") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.semantics {
                        contentDescription = "ConfirmRemoveModerationReport"
                    },
                    onClick = {
                        val type = selectedReportType
                        viewModel.dismissReportRemovalAlert()
                        if (type != null) {
                            if (type == TypeOfContentToReport.ACCOUNT) {
                                viewModel.unlock(selectedProfileId, reportId = selectedReportId)
                            } else {
                                viewModel.remove(reportId = selectedReportId, type = type)
                            }
                        }
                    }
                ) {
                    Text("Yes, remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissReportRemovalAlert() }) {
                    Text("No, cancel")
                }
            }
        )
    }

    if (showConfirmationOfRemoval) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmationOfRemoval() },
            title = { Text("Moderation report") },
            text = { Text("Your report has been removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissConfirmationOfRemoval() }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Card Container: Navigation bar + Header + Policies Link + Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground)
                .statusBarsPadding()
        ) {
            // Custom Top Bar matching iOS inline NavigationBar with circular back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (onBack != null) {
                    Surface(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(42.dp),
                        shape = CircleShape,
                        color = cardBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
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
                    text = "Reports and violations center",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            // Header description + Policies link
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Find information related with your content such as images, comments, reports, and petitions that violate our Community Guidelines. If you believe your we've mistakenly taken action , you can submit an appeal to have the action reversed.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = SecondarySubtitleColor
                )

                LinksView(
                    modifier = Modifier.fillMaxWidth(),
                    type = LinkType.Policies
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs matching iOS HStack(spacing: 32)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ModerationTabButton(
                    modifier = Modifier.weight(1f),
                    title = "Your account",
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) }
                )

                ModerationTabButton(
                    modifier = Modifier.weight(1f),
                    title = "Your reports",
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) }
                )

                Spacer(modifier = Modifier.width(24.dp))
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = dividerColor
        )

        // Content Area
        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    // Your account
                    if (indictedModeratedContent.isEmpty()) {
                        EmptyStateView(
                            title = "No violations yet.",
                            description = "Your account is in good standing.",
                            iconContent = { CheckmarkShieldIcon() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
                        ) {
                            items(
                                items = indictedModeratedContent,
                                key = { it.id ?: it.hashCode() }
                            ) { violation ->
                                val id = violation.id
                                if (id != null) {
                                    ViolationRow(
                                        id = id,
                                        status = violation.status,
                                        reason = violation.reason,
                                        date = violation.updatedAt,
                                        profileId = violation.contentAuthorProfileId,
                                        type = violation.type,
                                        role = ModerationRole.USER,
                                        observation = violation.observation,
                                        isOwnProfile = userRepository.isOwnProfile(violation.contentAuthorProfileId),
                                        onAppeal = {
                                            viewModel.prepareForAppeal(
                                                id = id,
                                                type = violation.type,
                                                profileId = violation.contentAuthorProfileId
                                            )
                                        },
                                        onCopyId = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("Moderation ID", id))
                                            coroutineScope.launch {
                                                toastState.show(
                                                    message = "Moderation ID copied to clipboard",
                                                    type = ToastType.Info
                                                )
                                            }
                                        },
                                        onRequestRemove = {}
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Your reports
                    if (myComplaints.isEmpty()) {
                        EmptyStateView(
                            title = "No reports yet.",
                            description = "You have not submitted any reports.",
                            iconContent = { InfoBubbleIcon() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
                        ) {
                            items(
                                items = myComplaints,
                                key = { it.id ?: it.hashCode() }
                            ) { violation ->
                                val id = violation.id
                                if (id != null) {
                                    val isRowDisabled = violation.status == ReportViolationStatus.REMOVED ||
                                        violation.status == ReportViolationStatus.APPROVED
                                    ViolationRow(
                                        id = id,
                                        status = violation.status,
                                        reason = violation.reason,
                                        date = violation.updatedAt,
                                        profileId = violation.contentAuthorProfileId,
                                        type = violation.type,
                                        role = ModerationRole.REPORTER,
                                        observation = violation.observation,
                                        isOwnProfile = userRepository.isOwnProfile(violation.contentAuthorProfileId),
                                        enabled = !isRowDisabled,
                                        onAppeal = {},
                                        onCopyId = {},
                                        onRequestRemove = {
                                            viewModel.prepareForReportRemoval(
                                                id = id,
                                                type = violation.type,
                                                profileId = violation.contentAuthorProfileId
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModerationTabButton(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onBackground else SecondarySubtitleColor
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent)
        )
    }
}

@Composable
fun CheckmarkShieldIcon(
    modifier: Modifier = Modifier
) {
    val shieldColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val checkColor = Color(0xFF34C759).copy(alpha = 0.75f)

    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 3.6.dp.toPx()

        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.82f, h * 0.21f)
            lineTo(w * 0.82f, h * 0.48f)
            cubicTo(
                w * 0.82f, h * 0.68f,
                w * 0.67f, h * 0.83f,
                w * 0.50f, h * 0.92f
            )
            cubicTo(
                w * 0.33f, h * 0.83f,
                w * 0.18f, h * 0.68f,
                w * 0.18f, h * 0.48f
            )
            lineTo(w * 0.18f, h * 0.21f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = shieldColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val checkPath = Path().apply {
            moveTo(w * 0.36f, h * 0.50f)
            lineTo(w * 0.46f, h * 0.61f)
            lineTo(w * 0.65f, h * 0.38f)
        }

        drawPath(
            path = checkPath,
            color = checkColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun InfoBubbleIcon(
    modifier: Modifier = Modifier
) {
    val bubbleColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val infoColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 3.6.dp.toPx()

        val bubblePath = Path().apply {
            moveTo(w * 0.24f, h * 0.14f)
            lineTo(w * 0.76f, h * 0.14f)
            cubicTo(w * 0.86f, h * 0.14f, w * 0.90f, h * 0.20f, w * 0.90f, h * 0.30f)
            lineTo(w * 0.90f, h * 0.58f)
            cubicTo(w * 0.90f, h * 0.68f, w * 0.86f, h * 0.74f, w * 0.76f, h * 0.74f)
            lineTo(w * 0.44f, h * 0.74f)
            lineTo(w * 0.26f, h * 0.88f)
            lineTo(w * 0.26f, h * 0.74f)
            lineTo(w * 0.24f, h * 0.74f)
            cubicTo(w * 0.14f, h * 0.74f, w * 0.10f, h * 0.68f, w * 0.10f, h * 0.58f)
            lineTo(w * 0.10f, h * 0.30f)
            cubicTo(w * 0.10f, h * 0.20f, w * 0.14f, h * 0.14f, w * 0.24f, h * 0.14f)
            close()
        }

        drawPath(
            path = bubblePath,
            color = bubbleColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawCircle(
            color = infoColor,
            radius = 2.3.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.31f)
        )

        drawLine(
            color = infoColor,
            start = Offset(w * 0.50f, h * 0.41f),
            end = Offset(w * 0.50f, h * 0.58f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    iconContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        iconContent()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 16.sp,
            color = SecondarySubtitleColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ViolationRow(
    modifier: Modifier = Modifier,
    id: String,
    status: ReportViolationStatus,
    reason: String,
    date: String? = null,
    profileId: String,
    type: TypeOfContentToReport,
    role: ModerationRole,
    observation: String? = null,
    isOwnProfile: Boolean = false,
    enabled: Boolean = true,
    onAppeal: () -> Unit = {},
    onCopyId: () -> Unit = {},
    onRequestRemove: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .alpha(setOpacityByStatus(status)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.iconVector,
                    contentDescription = type.description,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Content type: ")
                    }
                    append(type.description)
                },
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(
                    onClick = { if (enabled) menuExpanded = true },
                    enabled = enabled,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More options",
                        tint = if (role == ModerationRole.USER && !isOwnProfile) {
                            Color.Gray
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (role == ModerationRole.USER) {
                        val appealDisabled = !isOwnProfile || disableButtonByStatus(status)
                        DropdownMenuItem(
                            text = { Text("Appeal") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                                    contentDescription = null
                                )
                            },
                            enabled = !appealDisabled,
                            onClick = {
                                menuExpanded = false
                                onAppeal()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy ID") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onCopyId()
                            }
                        )
                    } else {
                        if (type == TypeOfContentToReport.ACCOUNT) {
                            DropdownMenuItem(
                                text = { Text("Unlock user") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LockOpen,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRequestRemove()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Remove from moderation") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRequestRemove()
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = status.description,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Violation",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = reason,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Last updated",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = formatViolationDate(date),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Observations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                if (observation != null) {
                    Text(
                        text = observation,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "None",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.67f)
        )
    }
}
