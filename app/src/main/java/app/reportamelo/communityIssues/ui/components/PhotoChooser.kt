package app.reportamelo.communityIssues.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.reportamelo.communityIssues.controllers.ImagePhase
import app.reportamelo.communityIssues.controllers.PhotoUploadTracker
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.services.ImageEncoderService
import app.reportamelo.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PhotoChooser(
    modifier: Modifier = Modifier,
    model: ReportDataModel = ReportDataModel.shared,
    maxImages: Int = 6
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uploadTrackers by model.uploadTrackers.collectAsState()
    val reportSession by model.reportSession.collectAsState()
    val report by model.report.collectAsState()

    var alertErrorMessage by remember { mutableStateOf<String?>(null) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var showCameraSettingsDialog by remember { mutableStateOf(false) }

    val startParallelUpload: (PhotoUploadTracker) -> Unit = { tracker ->
        coroutineScope.launch {
            val container = reportSession.reportContainer.ifEmpty {
                report.reportContainer ?: UUID.randomUUID().toString()
            }
            ImageEncoderService.processAndUpload(
                tracker = tracker,
                reportContainer = container
            )
        }
    }

    val enqueueUriList: (List<Uri>) -> Unit = { uris ->
        val remainingSlots = (maxImages - uploadTrackers.size).coerceAtLeast(0)
        val selectedUris = uris.take(remainingSlots)
        if (selectedUris.isNotEmpty()) {
            coroutineScope.launch {
                val createdTrackers = withContext(Dispatchers.IO) {
                    selectedUris.mapNotNull { uri ->
                        runCatching {
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: return@mapNotNull null
                            val previewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val ratio = if (previewBitmap != null && previewBitmap.height > 0) {
                                previewBitmap.width.toFloat() / previewBitmap.height.toFloat()
                            } else {
                                0.75f
                            }
                            val fileName = "${UUID.randomUUID()}.webp"
                            PhotoUploadTracker(
                                nameParam = fileName,
                                rawBytes = bytes,
                                bitmapParam = previewBitmap,
                                aspectRatioParam = ratio
                            )
                        }.getOrNull()
                    }
                }
                if (createdTrackers.isNotEmpty()) {
                    model.addUploadTrackers(createdTrackers)
                    // Launch optimization + upload in parallel for each selected photo
                    createdTrackers.forEach { tracker ->
                        startParallelUpload(tracker)
                    }
                }
            }
        }
    }

    val enqueueCapturedBitmap: (Bitmap) -> Unit = { bitmap ->
        if (uploadTrackers.size < maxImages) {
            coroutineScope.launch {
                val tracker = withContext(Dispatchers.IO) {
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    val bytes = out.toByteArray()
                    val ratio = if (bitmap.height > 0) {
                        bitmap.width.toFloat() / bitmap.height.toFloat()
                    } else {
                        0.75f
                    }
                    PhotoUploadTracker(
                        nameParam = "${UUID.randomUUID()}.webp",
                        rawBytes = bytes,
                        bitmapParam = bitmap,
                        aspectRatioParam = ratio
                    )
                }
                model.addUploadTrackers(listOf(tracker))
                startParallelUpload(tracker)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            enqueueUriList(uris)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            enqueueCapturedBitmap(bitmap)
        }
    }

    val launchCameraSafely: () -> Unit = {
        try {
            cameraLauncher.launch(null)
        } catch (e: SecurityException) {
            showCameraSettingsDialog = true
        } catch (e: ActivityNotFoundException) {
            alertErrorMessage = "No camera application is available on this device."
        } catch (e: Exception) {
            alertErrorMessage = e.localizedMessage ?: "Unable to open the camera."
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraSafely()
        } else {
            val activity = context.findActivity()
            val shouldShowRationale = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
                )
            if (shouldShowRationale) {
                showCameraRationaleDialog = true
            } else {
                showCameraSettingsDialog = true
            }
        }
    }

    val requestOrLaunchCamera: () -> Unit = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            launchCameraSafely()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val handleDelete: (PhotoUploadTracker) -> Unit = { tracker ->
        coroutineScope.launch {
            val previousPhase = tracker.phase.value
            try {
                tracker.setPhase(ImagePhase.DELETING)
                val container = reportSession.reportContainer.ifEmpty {
                    report.reportContainer ?: ""
                }
                if (tracker.key.value.isNotEmpty() && container.isNotEmpty() && !tracker.isExisting.value) {
                    runCatching {
                        ReportRepository.deleteTemporalPicture(
                            reportContainer = container,
                            key = tracker.key.value
                        )
                    }
                }
                model.removeUploadTracker(tracker.id)
            } catch (e: Exception) {
                tracker.setPhase(previousPhase)
                alertErrorMessage = e.localizedMessage ?: "Could not delete image"
            }
        }
    }

    if (alertErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { alertErrorMessage = null },
            title = { Text("Alert") },
            text = { Text(alertErrorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { alertErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
            title = { Text("Camera Permission Required") },
            text = {
                Text("Please allow camera access so you can take photos of the community issue to attach to your report.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraRationaleDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCameraSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showCameraSettingsDialog = false },
            title = { Text("Camera Access Disabled") },
            text = {
                Text("Camera permission is disabled. Please enable Camera access in App Settings to take photos.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraSettingsDialog = false
                        runCatching {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isMaxReached = uploadTrackers.size >= maxImages

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ThemedButton(
                    message = "Take Photo",
                    onClick = requestOrLaunchCamera,
                    type = ThemedButtonType.OUTLINE,
                    icon = Icons.Default.CameraAlt,
                    isEnabled = !isMaxReached
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ThemedButton(
                    message = "Gallery",
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    type = ThemedButtonType.OUTLINE,
                    icon = Icons.Filled.PhotoLibrary,
                    isEnabled = !isMaxReached
                )
            }
        }

        if (uploadTrackers.isNotEmpty()) {
            MasonryGrid(
                columns = 2,
                spacing = 10.dp,
                data = uploadTrackers
            ) { tracker ->
                val phase by tracker.phase.collectAsState()
                val progress by tracker.uploadProgress.collectAsState()
                val name by tracker.name.collectAsState()
                val bitmap by tracker.bitmap.collectAsState()
                val url by tracker.url.collectAsState()
                val aspectRatio by tracker.aspectRatio.collectAsState()

                PreviewImageToUpload(
                    name = name.ifEmpty { tracker.id },
                    phase = phase,
                    bitmap = bitmap,
                    url = url?.toString(),
                    aspectRatio = aspectRatio,
                    currentValue = progress.toFloat(),
                    total = 1f,
                    onDelete = { handleDelete(tracker) },
                    onRetry = { startParallelUpload(tracker) }
                )
            }
        }
    }
}
