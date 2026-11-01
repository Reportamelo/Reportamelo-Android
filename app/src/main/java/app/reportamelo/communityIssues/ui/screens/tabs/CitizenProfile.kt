package app.reportamelo.communityIssues.ui.screens.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.commons.BlockReasons
import app.reportamelo.commons.IssueStatus
import app.reportamelo.commons.IssueTypes
import app.reportamelo.commons.Severity
import app.reportamelo.commons.utils.resolveUrlString
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.CitizenProfileViewModel
import app.reportamelo.communityIssues.datamodel.Report
import app.reportamelo.communityIssues.datamodel.createdDate
import app.reportamelo.communityIssues.datamodel.issueType
import app.reportamelo.communityIssues.datamodel.reportedDate
import app.reportamelo.communityIssues.datamodel.severity
import app.reportamelo.communityIssues.datamodel.status
import app.reportamelo.communityIssues.datamodel.updatedDate
import app.reportamelo.repository.UserRepository
import coil.compose.SubcomposeAsyncImage

enum class AppTab {
    REPORTS,
    PETITIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenProfile(
    modifier: Modifier = Modifier,
    profileId: String,
    onNavigateBack: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    viewModel: CitizenProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.REPORTS) }
    var showMenu by remember { mutableStateOf(false) }

    val isGuest = remember { UserRepository.shared.isGuestUser() }
    val isOwnProfile = remember(profileId) { UserRepository.shared.isOwnProfile(profileId) }

    LaunchedEffect(profileId) {
        viewModel.fetchCitizenPublicProfile(profileId)
        viewModel.fetchReports(profileId)
    }

    val citizen = state.citizen
    val isHiddenOrGuest = citizen.hideProfile ||
        citizen.names.isBlank() ||
        citizen.names.equals("Guest citizen", ignoreCase = true) ||
        citizen.names.equals("Deleted Citizen", ignoreCase = true) ||
        citizen.userName.equals("guest", ignoreCase = true) ||
        citizen.userName.equals("deleted", ignoreCase = true)

    val displayName = if (isHiddenOrGuest) {
        stringResource(R.string.guest_citizen)
    } else {
        citizen.names
    }

    val displayUserName = if (isHiddenOrGuest) {
        "@guest"
    } else {
        "@${citizen.userName.removePrefix("@")}"
    }

    val avatarUrl = remember(citizen.profilePicture, isHiddenOrGuest) {
        if (isHiddenOrGuest ||
            citizen.profilePicture.isBlank() ||
            citizen.profilePicture.contains("019f4f22-1464-7336-8406-853b453b026d")
        ) {
            null
        } else {
            resolveUrlString(citizen.profilePicture)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Soft purple top gradient matching Screenshot 5 & 6
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFC084FC).copy(alpha = 0.55f),
                            Color(0xFFE9D5FF).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar (< Back, Profile title, ... Menu, X Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isOwnProfile) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Options"
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_profile_id)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Profile ID", profileId))
                                    }
                                )
                                if (!isGuest) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.report_user),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.setShowBlockUserSheet(true)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Avatar (130.dp) matching Screenshot 5
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        error = {
                            Image(
                                painter = painterResource(id = R.drawable.user_b),
                                contentDescription = displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.user_b),
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = displayUserName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Counters row matching Screenshot 5
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                            append("${state.reportsSubmitted} ")
                        }
                        append(stringResource(R.string.reports_submitted))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                            append("${state.petitionsPublished} ")
                        }
                        append(stringResource(R.string.petitions_published))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented Control ("Reports" | "Petitions") matching Screenshot 5
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(4.dp)
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val title = when (tab) {
                        AppTab.REPORTS -> stringResource(R.string.reports)
                        AppTab.PETITIONS -> stringResource(R.string.petitions)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                AppTab.REPORTS -> {
                    if (state.fetchingReports) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (state.reports.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_reports_yet),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.please_sent_us_reports),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            state.reports.forEach { report ->
                                CitizenReportCard(report = report)
                            }
                        }
                    }
                }

                AppTab.PETITIONS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_petitions_yet),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (state.showBlockUserSheet) {
        BlockUserSheet(
            isSubmitting = state.isBlocking,
            isReasonValid = { viewModel.isBlockReasonValid(it) },
            onDismiss = { viewModel.setShowBlockUserSheet(false) },
            onConfirmBlock = { reasonText, selectedReason ->
                viewModel.blockUser(
                    profileId = profileId,
                    reasonText = reasonText,
                    selectedReason = selectedReason
                )
            }
        )
    }

    if (state.showBlockResultAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBlockResultAlert() },
            title = {
                Text(
                    if (state.blockSucceeded) {
                        stringResource(R.string.we_ve_received_your_report)
                    } else {
                        stringResource(R.string.something_went_wrong)
                    }
                )
            },
            text = {
                Text(
                    if (state.blockSucceeded) {
                        stringResource(R.string.meanwhile_you_can_copy_report_code_or_share_it_with_others)
                    } else {
                        stringResource(R.string.an_unexpected_error_has_occurred_please_try_again)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissBlockResultAlert() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun CitizenReportCard(
    modifier: Modifier = Modifier,
    report: Report
) {
    val issueTypeText = when (report.issueType) {
        IssueTypes.ROAD -> stringResource(R.string.road)
        IssueTypes.PUBLIC_SPACE -> stringResource(R.string.public_space)
        IssueTypes.BUILDING -> stringResource(R.string.building)
        IssueTypes.OTHER -> stringResource(R.string.other)
        IssueTypes.ALL -> stringResource(R.string.all)
    }
    val severityText = when (report.severity) {
        Severity.LOW -> stringResource(R.string.low)
        Severity.MEDIUM -> stringResource(R.string.medium)
        Severity.HIGH -> stringResource(R.string.high)
        Severity.ALL -> stringResource(R.string.all)
    }
    val statusText = when (report.status) {
        IssueStatus.REPORTED -> stringResource(R.string.reported)
        IssueStatus.ASSIGNED -> stringResource(R.string.assigned)
        IssueStatus.IN_PROGRESS -> stringResource(R.string.in_progress)
        IssueStatus.CONFIRMED -> stringResource(R.string.confirmed)
        IssueStatus.FIXED -> stringResource(R.string.fixed)
        IssueStatus.REJECTED -> stringResource(R.string.rejected)
        IssueStatus.RECTIFICATION -> stringResource(R.string.rectification)
        IssueStatus.HUMAN_REVIEW -> stringResource(R.string.human_review)
        IssueStatus.UNDER_REVIEW -> stringResource(R.string.under_review)
        IssueStatus.PETITION_TO_SIGN -> stringResource(R.string.petition)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (report.description.isNotBlank()) {
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ProfileReportMetaRow(label = stringResource(R.string.created), value = report.createdDate)
            ProfileReportMetaRow(label = stringResource(R.string.updated), value = report.updatedDate)
            ProfileReportMetaRow(label = stringResource(R.string.assigned), value = report.reportedDate)
            ProfileReportMetaRow(
                label = stringResource(R.string.observations),
                value = report.observations?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.no_observations)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileReportChip(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.issue_type),
                    value = issueTypeText
                )
                ProfileReportChip(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.severity),
                    value = severityText
                )
                ProfileReportChip(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.status),
                    value = statusText
                )
            }
        }
    }
}

@Composable
private fun ProfileReportMetaRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileReportChip(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockUserSheet(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
    isReasonValid: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirmBlock: (String, BlockReasons) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember { mutableStateOf(BlockReasons.WRONG_INFORMATION) }
    var reasonText by remember { mutableStateOf("") }
    var showConfirmAlert by remember { mutableStateOf(false) }

    val isValid = isReasonValid(reasonText) && !isSubmitting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: "Block User" + X button (Screenshots 7 & 8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.block_user),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            // Reason section (Screenshots 7 & 8)
            Text(
                text = stringResource(R.string.reason).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(BlockReasons.entries) { reason ->
                        val isSelected = reason == selectedReason
                        val localizedLabel = blockReasonLabel(reason)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable { selectedReason = reason }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = localizedLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Details input section (Screenshots 7 & 8)
            Text(
                text = stringResource(R.string.enter_details_here).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )

            OutlinedTextField(
                value = reasonText,
                onValueChange = { if (it.length <= 150) reasonText = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.why_are_you_reporting_this_user),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Block User button (disabled until >= 12 chars, bright red when valid - Screenshot 7 vs 8)
            Button(
                onClick = { showConfirmAlert = true },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF1744),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFFF8A80).copy(alpha = 0.55f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.block_user),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showConfirmAlert) {
        AlertDialog(
            onDismissRequest = { showConfirmAlert = false },
            title = { Text(stringResource(R.string.confirm_user_blocking)) },
            text = { Text(stringResource(R.string.i_confirm_that_this_user_violates_our_community_guidelines)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmAlert = false
                        onConfirmBlock(reasonText, selectedReason)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.block),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun blockReasonLabel(reason: BlockReasons): String {
    return when (reason) {
        BlockReasons.WRONG_INFORMATION -> stringResource(R.string.wrong_information)
        BlockReasons.HARASSMENT -> stringResource(R.string.harassment_or_bullying)
        BlockReasons.SELF_PROMOTION -> stringResource(R.string.unwanted_self_promotion)
        BlockReasons.HATE_SPEECH -> stringResource(R.string.hate_speech_or_discrimination)
        BlockReasons.VIOLENCE -> stringResource(R.string.threats_or_violence)
        BlockReasons.INAPPROPRIATE_CONTENT -> stringResource(R.string.inappropriate_or_explicit_content)
        BlockReasons.PRIVACY_VIOLATION -> stringResource(R.string.privacy_violation_or_personal_data)
        BlockReasons.IMPERSONATION -> stringResource(R.string.impersonation_or_fake_account)
        BlockReasons.SPAM_SCAM -> stringResource(R.string.spam_or_scam)
        BlockReasons.DUPLICATE_REPORT -> stringResource(R.string.duplicate_report)
        BlockReasons.OTHER -> stringResource(R.string.other_issue)
    }
}
