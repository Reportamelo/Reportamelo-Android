package app.reportamelo.communityIssues.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.reportamelo.communityIssues.datamodel.Locator
import app.reportamelo.communityIssues.datamodel.LocatorDAO
import app.reportamelo.models.Coordinate
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MiniMapLocator(
    modifier: Modifier = Modifier,
    coordinate: Coordinate,
    onExpandMap: (Coordinate) -> Unit,
    onChange: (Coordinate, Locator) -> Unit,
    onCoordinateChanged: (Coordinate) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
    onMapTouchChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initialLatLng = remember { LatLng(coordinate.lat, coordinate.lng) }
    var lastInternalCoordinate by remember { mutableStateOf(coordinate) }
    var hasUserInteracted by remember { mutableStateOf(false) }
    var hasMovedFromInitial by remember { mutableStateOf(false) }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(coordinate.lat, coordinate.lng), 16f)
    }

    DisposableEffect(Unit) {
        onDispose {
            onMapTouchChanged(false)
        }
    }

    // Keep MiniMapLocator camera synced ONLY when coordinate is updated externally (e.g. from MapPickerScreen)
    LaunchedEffect(coordinate.lat, coordinate.lng) {
        val internalDiff = FloatArray(1)
        Location.distanceBetween(
            lastInternalCoordinate.lat,
            lastInternalCoordinate.lng,
            coordinate.lat,
            coordinate.lng,
            internalDiff
        )
        if (internalDiff[0] > 1.5f && !cameraPositionState.isMoving) {
            lastInternalCoordinate = coordinate
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(coordinate.lat, coordinate.lng),
                    cameraPositionState.position.zoom.coerceAtLeast(16f)
                )
            )
        }
    }

    // Observe camera movement reactively without cancelling geocode coroutines on subsequent frames
    LaunchedEffect(cameraPositionState) {
        snapshotFlow {
            Triple(
                cameraPositionState.isMoving,
                cameraPositionState.cameraMoveStartedReason,
                cameraPositionState.position.target
            )
        }.collect { (isMoving, reason, target) ->
            if (reason == CameraMoveStartedReason.GESTURE) {
                hasUserInteracted = true
            }
            if (hasUserInteracted) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    initialLatLng.latitude,
                    initialLatLng.longitude,
                    target.latitude,
                    target.longitude,
                    results
                )
                if (results[0] > 0.5f || hasMovedFromInitial) {
                    hasMovedFromInitial = true
                    val updatedCoord = Coordinate(target.latitude, target.longitude)
                    lastInternalCoordinate = updatedCoord
                    onCoordinateChanged(updatedCoord)

                    if (!isMoving) {
                        geocodeJob?.cancel()
                        geocodeJob = coroutineScope.launch {
                            onLoadingChanged(true)
                            delay(250)
                            val resolvedLocator = LocatorDAO.findPlace(
                                context,
                                target.latitude,
                                target.longitude
                            )
                            onLoadingChanged(false)
                            onChange(updatedCoord, resolvedLocator)
                        }
                    }
                }
            }
        }
    }

    val centerOnUserLocation: () -> Unit = {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            val lat = location?.latitude ?: cameraPositionState.position.target.latitude
            val lng = location?.longitude ?: cameraPositionState.position.target.longitude
            coroutineScope.launch {
                hasUserInteracted = true
                hasMovedFromInitial = true
                val updatedCoord = Coordinate(lat, lng)
                lastInternalCoordinate = updatedCoord
                onCoordinateChanged(updatedCoord)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f)
                )
                onLoadingChanged(true)
                val resolvedLocator = LocatorDAO.findPlace(context, lat, lng)
                onLoadingChanged(false)
                onChange(updatedCoord, resolvedLocator)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            centerOnUserLocation()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                hasUserInteracted = true
                            }
                            onMapTouchChanged(anyPressed)
                        }
                    }
                }
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            hasUserInteracted = true
                            onMapTouchChanged(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            onMapTouchChanged(false)
                        }
                    }
                    false
                }
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = false, // Disabled so zoom does not interfere with vertical scroll
                    scrollGesturesEnabled = true, // Panning enabled
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false
                )
            )
        }

        // Centered "+" icon matching iOS MiniMapLocator
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Center",
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Top-right vertical pill controls (Current Location & Expand Map)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasFine || hasCoarse) {
                            centerOnUserLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Current Location",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.width(28.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                IconButton(
                    onClick = {
                        val target = cameraPositionState.position.target
                        val latestCenter = Coordinate(target.latitude, target.longitude)
                        lastInternalCoordinate = latestCenter
                        onExpandMap(latestCenter)
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand Map",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
