package app.reportamelo.communityIssues.ui.screens.modals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.ImagePhase
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.ui.components.PhotoChooser
import app.reportamelo.communityIssues.ui.components.PhotoPreview
import app.reportamelo.models.GroupedAttachmentPayload
import app.reportamelo.models.PreviewAttachment
import app.reportamelo.repository.EvidenceRepository
import app.reportamelo.repository.ReportRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.launch
import app.reportamelo.communityIssues.ui.theme.toolbarButtonBackground

private val AttachEvidencePeach = Color(0xFFF3B391)
private val AttachEvidenceOrange = Color(0xFFE67E22)

@Composable
fun EvidencesScreen(
    modifier: Modifier = Modifier,
    reportId: String,
    reportContainer: String = "",
    initialAttachments: List<PreviewAttachment> = emptyList(),
    onDismiss: () -> Unit,
    onClose: (() -> Unit)? = null,
    onNavigateToPhotoDetail: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(initialAttachments.isEmpty()) }
    var evidences by remember { mutableStateOf(initialAttachments) }
    var isAttachingNewEvidences by remember { mutableStateOf(false) }
    var selectedPhotoIdForDetail by remember { mutableStateOf<String?>(null) }
    val isGuest = remember { UserRepository.shared.isGuestUser() }

    val loadEvidences: suspend () -> Unit = {
        if (evidences.isEmpty()) isLoading = true
        try {
            val response = ReportRepository.fetchAttachments(reportId = reportId, page = 1)
            val fetched = response.documents.orEmpty()
            if (fetched.isNotEmpty()) {
                evidences = fetched
            }
        } catch (_: Exception) {
            // Keep initialAttachments fallback if network fails
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(reportId) {
        loadEvidences()
    }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top navigation bar (< Back, Add Photo +, X Close) matching Screenshot 11
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .toolbarButtonBackground()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(26.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (evidences.size < 24 && !isGuest) {
                    IconButton(
                        onClick = { isAttachingNewEvidences = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .toolbarButtonBackground()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.attach_new_evidence),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .toolbarButtonBackground()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large title & subtitle matching Screenshot 11
        Text(
            text = stringResource(R.string.evidences),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.you_can_take_a_look_of_what_is_happening),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                evidences.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.no_evidences_yet),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(evidences, key = { it.id }) { photo ->
                            PhotoPreview(
                                attachment = photo,
                                isFullMode = true,
                                onClick = {
                                    selectedPhotoIdForDetail = photo.id
                                    onNavigateToPhotoDetail(photo.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPhotoIdForDetail?.let { photoId ->
        Dialog(
            onDismissRequest = { selectedPhotoIdForDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PhotoDetailScreen(
                photos = evidences,
                initialPhotoId = photoId,
                onDismiss = { selectedPhotoIdForDetail = null }
            )
        }
    }

    if (isAttachingNewEvidences) {
        AttachEvidencesScreen(
            reportId = reportId,
            fallbackContainer = reportContainer,
            onDismiss = { isAttachingNewEvidences = false },
            onEvidencesAttached = {
                isAttachingNewEvidences = false
                coroutineScope.launch {
                    loadEvidences()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachEvidencesScreen(
    modifier: Modifier = Modifier,
    reportId: String = "",
    fallbackContainer: String = "",
    onDismiss: () -> Unit,
    onEvidencesAttached: () -> Unit = onDismiss
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reportDataModel = ReportDataModel.shared
    val uploadTrackers by reportDataModel.uploadTrackers.collectAsState()
    val reportSession by reportDataModel.reportSession.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        reportDataModel.setUploadTrackers(emptyList())
        try {
            val session = ReportRepository.start()
            reportDataModel.updateReportSession(session.data)
        } catch (_: Exception) {
            // Fallback container will be used if start() fails
        }
    }

    val hasReadyPhotos = uploadTrackers.isNotEmpty() && uploadTrackers.all {
        it.phase.value == ImagePhase.SUCCESS || it.key.value.isNotEmpty()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Top bar with X Close button and centered "Evidences" title (Screenshot 12)
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .toolbarButtonBackground()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }

                Text(
                    text = stringResource(R.string.evidences),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.attach_new_evidence),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            PhotoChooser(
                model = reportDataModel,
                maxImages = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!hasReadyPhotos || isSubmitting) return@Button
                    isSubmitting = true
                    val container = reportSession.reportContainer.ifEmpty { fallbackContainer }
                    val payloads = uploadTrackers.map { tracker ->
                        GroupedAttachmentPayload(
                            attachmentContainer = container,
                            key = tracker.key.value,
                            previewFileName = "preview_${tracker.name.value}",
                            fileName = tracker.name.value,
                            reportId = reportId,
                            notes = ""
                        )
                    }
                    coroutineScope.launch {
                        EvidenceRepository.publishExternalContributions(payloads) { _ ->
                            isSubmitting = false
                            reportDataModel.setUploadTrackers(emptyList())
                            onEvidencesAttached()
                        }
                    }
                },
                enabled = hasReadyPhotos && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AttachEvidenceOrange,
                    contentColor = Color.White,
                    disabledContainerColor = AttachEvidencePeach.copy(alpha = 0.55f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(R.string.attach_new_evidence),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
