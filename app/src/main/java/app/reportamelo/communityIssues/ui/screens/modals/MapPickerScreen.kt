package app.reportamelo.communityIssues.ui.screens.modals

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.reportamelo.commons.GeoCodingService
import app.reportamelo.communityIssues.datamodel.Locator
import app.reportamelo.communityIssues.datamodel.LocatorDAO
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.models.Coordinate
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MapModeOption(val title: String, val mapType: MapType) {
    STANDARD("Standard", MapType.NORMAL),
    IMAGERY("Satellite", MapType.SATELLITE),
    HYBRID("Hybrid", MapType.HYBRID)
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MapPickerScreen(
    modifier: Modifier = Modifier,
    initialCoordinate: Coordinate,
    model: ReportDataModel = ReportDataModel.shared,
    onDismiss: () -> Unit,
    onConfirm: (Coordinate, Locator) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val modelLocator by model.locator.collectAsState()

    var selectedMode by remember { mutableStateOf(MapModeOption.STANDARD) }
    var currentCoordinate by remember { mutableStateOf(initialCoordinate) }
    var locator by remember {
        mutableStateOf(
            if (modelLocator.cityId.isNotEmpty()) {
                modelLocator.copy(lat = initialCoordinate.lat, lng = initialCoordinate.lng)
            } else {
                LocatorDAO.defaultLocator(lat = initialCoordinate.lat, lng = initialCoordinate.lng)
            }
        )
    }
    var searchText by remember { mutableStateOf("") }
    var hasUserInteracted by remember { mutableStateOf(false) }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

    val initialLatLng = remember { LatLng(initialCoordinate.lat, initialCoordinate.lng) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(initialCoordinate.lat, initialCoordinate.lng),
            16f
        )
    }

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
            val updatedCoord = Coordinate(target.latitude, target.longitude)
            currentCoordinate = updatedCoord

            val results = FloatArray(1)
            Location.distanceBetween(
                initialLatLng.latitude,
                initialLatLng.longitude,
                target.latitude,
                target.longitude,
                results
            )

            if (hasUserInteracted && results[0] > 0.5f) {
                model.updateCoordinate(updatedCoord)
                model.setDifferentLocation(true)
            }

            if (!isMoving && (hasUserInteracted || results[0] > 0.5f)) {
                geocodeJob?.cancel()
                geocodeJob = coroutineScope.launch {
                    model.setLoadingAddress(true)
                    delay(250)
                    val resolved = LocatorDAO.findPlace(context, target.latitude, target.longitude)
                    locator = resolved
                    model.updateCoordinate(updatedCoord)
                    model.updateLocator(resolved)
                    model.setDifferentLocation(true)
                    model.setLoadingAddress(false)
                }
            }
        }
    }

    val centerOnUserLocation: () -> Unit = {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            val lat = location?.latitude ?: currentCoordinate.lat
            val lng = location?.longitude ?: currentCoordinate.lng
            coroutineScope.launch {
                hasUserInteracted = true
                val updatedCoord = Coordinate(lat, lng)
                currentCoordinate = updatedCoord
                model.updateCoordinate(updatedCoord)
                model.setDifferentLocation(true)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f)
                )
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar matching iOS MapPickerView (Circular Close, Centered Title + Subtitle, Orange Check)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Move the map where you want to\nreport the issue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    onClick = {
                        val target = cameraPositionState.position.target
                        val confirmedCoord = Coordinate(target.latitude, target.longitude)
                        val immediateLocator = locator.copy(
                            lat = confirmedCoord.lat,
                            lng = confirmedCoord.lng,
                            cityId = locator.cityId.ifEmpty {
                                LocatorDAO.defaultLocator(
                                    lat = confirmedCoord.lat,
                                    lng = confirmedCoord.lng
                                ).cityId
                            }
                        )
                        onConfirm(confirmedCoord, immediateLocator)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm Location",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Map Mode Segmented Control (Standard | Satellite | Hybrid)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                MapModeOption.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = MapModeOption.entries.size
                        ),
                        icon = {}
                    ) {
                        Text(mode.title)
                    }
                }
            }

            // Map Container with Target Reticle, Floating Controls, and Bottom Search Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.any { it.pressed }) {
                                        hasUserInteracted = true
                                    }
                                }
                            }
                        }
                        .pointerInteropFilter { motionEvent ->
                            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN ||
                                motionEvent.actionMasked == MotionEvent.ACTION_MOVE
                            ) {
                                hasUserInteracted = true
                            }
                            false
                        }
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(mapType = selectedMode.mapType),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            zoomGesturesEnabled = true,
                            scrollGesturesEnabled = true,
                            myLocationButtonEnabled = false,
                            compassEnabled = true
                        )
                    )
                }

                // Centered target reticle icon matching iOS "scope" icon
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Target Location",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Right-side vertical floating pill (Locate, Zoom In, Zoom Out)
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Current location",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.width(28.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom in",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.width(28.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom out",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom floating search bar matching iOS MapPickerView
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search a place") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                if (searchText.isNotBlank()) {
                                    coroutineScope.launch {
                                        val coords = withContext(Dispatchers.IO) {
                                            runCatching {
                                                GeoCodingService(context).getCoordinatesFromAddress(searchText)
                                            }.getOrNull()
                                        }
                                        if (coords != null) {
                                            hasUserInteracted = true
                                            val updatedCoord = Coordinate(coords.first, coords.second)
                                            currentCoordinate = updatedCoord
                                            model.updateCoordinate(updatedCoord)
                                            model.setDifferentLocation(true)
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(coords.first, coords.second),
                                                    16f
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        ),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}
