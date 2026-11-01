package app.reportamelo.communityIssues.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reportamelo.commons.CommentForType
import app.reportamelo.commons.IssueStatus
import app.reportamelo.commons.utils.buildShareURL
import app.reportamelo.communityIssues.controllers.DetailReportOptions
import app.reportamelo.communityIssues.controllers.DetailSubScreen
import app.reportamelo.communityIssues.controllers.DetailViewModel
import app.reportamelo.communityIssues.ui.screens.animations.CustomBottomToolbar
import app.reportamelo.communityIssues.ui.screens.modals.EvidencesScreen
import app.reportamelo.communityIssues.ui.screens.tabs.CitizenProfile
import app.reportamelo.communityIssues.ui.screens.tabs.CommentsSectionView
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.services.AdMobManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256094549412/5224354917"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailView(
    modifier: Modifier = Modifier,
    report: MapExplorerReport,
    onDismiss: () -> Unit = {},
    viewModel: DetailViewModel = remember(report.id) { DetailViewModel() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var showMoreMenu by remember { mutableStateOf(false) }
    var showReportOptionsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(report.id) {
        viewModel.initialize(report)
    }

    BackHandler(enabled = state.activeSubScreen != DetailSubScreen.NONE) {
        viewModel.closeSubScreen(report.id)
    }

    when (state.activeSubScreen) {
        DetailSubScreen.FOLLOW_UP -> {
            IssueTimeLineView(
                modifier = modifier,
                reportId = report.id,
                onBack = { viewModel.closeSubScreen(report.id) },
                onClose = onDismiss
            )
            return
        }

        DetailSubScreen.CITIZEN_PROFILE -> {
            CitizenProfile(
                modifier = modifier,
                profileId = state.selectedProfileId,
                onNavigateBack = { viewModel.closeSubScreen(report.id) },
                onClose = onDismiss
            )
            return
        }

        DetailSubScreen.COMMENTS -> {
            CommentsSectionView(
                modifier = modifier,
                commentFor = CommentForType.REPORT,
                resourceId = report.id,
                title = report.title,
                subtitle = report.description,
                onNavigateBack = { viewModel.closeSubScreen(report.id) },
                onClose = onDismiss
            )
            return
        }

        DetailSubScreen.EVIDENCES -> {
            EvidencesScreen(
                modifier = modifier,
                reportId = report.id,
                reportContainer = report.reportContainer,
                initialAttachments = report.attachments,
                onDismiss = { viewModel.closeSubScreen(report.id) },
                onClose = onDismiss
            )
            return
        }

        DetailSubScreen.NONE -> Unit
    }

    val issueStatus = remember(report.statusId) {
        IssueStatus.fromIdentifier(report.statusId)
    }
    val canShare = remember(issueStatus, report.shareUrl) {
        (issueStatus == IssueStatus.ASSIGNED ||
            issueStatus == IssueStatus.IN_PROGRESS ||
            issueStatus == IssueStatus.CONFIRMED ||
            issueStatus == IssueStatus.PETITION_TO_SIGN) &&
            buildShareURL(report.shareUrl) != null
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Top bar matching iOS DetailView toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (canShare || !state.isOwnProfile) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            if (canShare) {
                                IconButton(
                                    onClick = {
                                        val shareUri = buildShareURL(report.shareUrl)
                                        if (shareUri != null) {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(Intent.EXTRA_TEXT, shareUri.toString())
                                                type = "text/plain"
                                            }
                                            context.startActivity(
                                                Intent.createChooser(sendIntent, report.title)
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (!state.isOwnProfile) {
                                Box {
                                    IconButton(onClick = { showMoreMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreHoriz,
                                            contentDescription = "More options",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Copy Report ID") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showMoreMenu = false
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(
                                                    ClipData.newPlainText("Report ID", report.id)
                                                )
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Open in Google Maps") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Place,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showMoreMenu = false
                                                openCoordinatesInGoogleMaps(
                                                    context = context,
                                                    lat = report.lat,
                                                    lng = report.lng
                                                )
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Report content",
                                                    color = if (state.isGuest) {
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    }
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Report,
                                                    contentDescription = null,
                                                    tint = if (state.isGuest) {
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    }
                                                )
                                            },
                                            enabled = !state.isGuest,
                                            onClick = {
                                                showMoreMenu = false
                                                coroutineScope.launch {
                                                    delay(128)
                                                    showReportOptionsMenu = true
                                                }
                                            }
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showReportOptionsMenu,
                                        onDismissRequest = { showReportOptionsMenu = false },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            text = "Content options",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                        DetailReportOptions.entries.forEachIndexed { index, option ->
                                            DropdownMenuItem(
                                                text = { Text(option.description) },
                                                onClick = {
                                                    showReportOptionsMenu = false
                                                    coroutineScope.launch {
                                                        delay(128)
                                                        viewModel.selectReportOption(option.description)
                                                    }
                                                }
                                            )
                                            if (index < DetailReportOptions.entries.lastIndex) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
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

            DetailsHeader(
                title = report.title,
                description = report.description
            )

            BasicInformationView(report = report)

            EvidenceOfTheIssuesView(
                attachments = report.attachments,
                id = report.id,
                onShowEvidences = { viewModel.openSubScreen(DetailSubScreen.EVIDENCES) }
            )

            FollowUpSectionView(
                report = report,
                voteCount = state.voteCount,
                onShowProgress = { viewModel.openSubScreen(DetailSubScreen.FOLLOW_UP) }
            )

            MoreInformationView(
                report = report,
                onShowProfile = { profileId ->
                    viewModel.openCitizenProfile(profileId)
                }
            )

            // Latest Comments section (hidden for guest users as required)
            if (!state.isGuest) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Latest Comments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (state.latestComments.isEmpty()) {
                        Text(
                            text = "No comments yet. Tap Comment below to share how this issue affects you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openSubScreen(DetailSubScreen.COMMENTS) }
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            state.latestComments.forEach { comment ->
                                CommentRow(
                                    comment = comment,
                                    hideReportOption = state.isGuest
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Bottom Action Bar (Screenshot 1, 4, 9)
        CustomBottomToolbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            commentAction = {
                viewModel.openSubScreen(DetailSubScreen.COMMENTS)
            },
            addPhotoAction = {
                viewModel.openSubScreen(DetailSubScreen.EVIDENCES)
            },
            boostReportValidationAction = {
                val activity = context.findActivity()
                if (activity == null) {
                    viewModel.submitBoostVote(report.id)
                    return@CustomBottomToolbar
                }
                coroutineScope.launch {
                    viewModel.setBoostLoading(true)
                    val adUnitId = resolveRewardedAdUnitId(context)
                    val loaded = AdMobManager.loadRewardedAd(context, adUnitId)
                    if (loaded) {
                        AdMobManager.showRewardedAd(activity) {
                            viewModel.submitBoostVote(report.id)
                        }
                    } else {
                        viewModel.showBoostMessage(
                            "No ad is available to show right now. Please try again later."
                        )
                    }
                }
            },
            disableBoostButton = state.disableBoostButton,
            isGuestOverride = state.isGuest,
            isBoostLoading = state.isBoostLoading
        )
    }

    // Confirm content reporting alert
    if (state.showConfirmReportAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmReportAlert() },
            title = { Text("Confirm content reporting") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("I confirm that this report violates our community guidelines.")
                    if (state.selectedReportOption == DetailReportOptions.OTHER.description) {
                        OutlinedTextField(
                            value = state.customReason,
                            onValueChange = { viewModel.updateCustomReason(it) },
                            placeholder = { Text("Type your reason") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.reportContentViolation(report) }) {
                    Text("Report", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmReportAlert() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Boost status alert
    if (state.showBoostAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBoostAlert() },
            title = { Text("Boosting report to give more importance to it") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissBoostAlert() }) {
                    Text("Close")
                }
            }
        )
    }

    // Moderation success alert
    if (state.showSuccessfulAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessfulAlert() },
            title = { Text("Moderation report sent") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSuccessfulAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    // Moderation error alert
    if (state.showErrorAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissErrorAlert() },
            title = { Text("Error") },
            text = { Text(state.errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissErrorAlert() }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun resolveRewardedAdUnitId(context: Context): String {
    return try {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val configured = appInfo.metaData?.getString("ADMOB_REPORT_VALIDATION_AD_UNIT")
        if (!configured.isNullOrBlank()) configured else DEFAULT_TEST_REWARDED_AD_UNIT_ID
    } catch (_: Exception) {
        DEFAULT_TEST_REWARDED_AD_UNIT_ID
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
