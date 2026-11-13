package app.reportamelo.communityIssues.ui.screens.tabs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import app.reportamelo.commons.IssueStatus
import app.reportamelo.commons.IssueTypes
import app.reportamelo.communityIssues.LocalAuthViewModel
import app.reportamelo.communityIssues.LocalDeepLinkRouter
import app.reportamelo.communityIssues.LocalSettingsStore
import app.reportamelo.communityIssues.controllers.MapExplorerViewModel
import app.reportamelo.communityIssues.datamodel.AuthViewModel
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.DeepLinkRouter
import app.reportamelo.communityIssues.helpers.maps.SearchCompleter
import app.reportamelo.communityIssues.helpers.maps.rememberMapStyleOptions
import app.reportamelo.communityIssues.ui.components.BasicInformationView
import app.reportamelo.communityIssues.ui.components.CustomTabBar
import app.reportamelo.communityIssues.ui.components.DetailView
import app.reportamelo.communityIssues.ui.components.DetailsHeader
import app.reportamelo.communityIssues.ui.components.SearchSuggestion
import app.reportamelo.communityIssues.ui.components.SuggestionsResultList
import app.reportamelo.communityIssues.ui.screens.modals.UserProfileScreen
import app.reportamelo.communityIssues.ui.screens.subviews.color
import app.reportamelo.communityIssues.ui.screens.subviews.titleRes
import app.reportamelo.communityIssues.ui.theme.Dimensions
import app.reportamelo.models.Coordinate
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.services.SubscriptionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

private val REPORT_ID_REGEX = Regex("^[a-zA-Z]{2}-[a-zA-Z]{2,3}-\\d{8}-[a-zA-Z0-9]{16}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapExplorerView(
    modifier: Modifier = Modifier,
    controller: MapExplorerViewModel = MapExplorerViewModel.shared,
    appState: AuthViewModel = LocalAuthViewModel.current,
    settings: SettingsStore = LocalSettingsStore.current,
    router: DeepLinkRouter = LocalDeepLinkRouter.current
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val searchCompleter = remember(context) { SearchCompleter(context) }

    val cameraRegion by appState.cameraPosition.collectAsState()
    val reports by controller.reports.collectAsState()
    val searchMarker by controller.searchMarker.collectAsState()
    val searchItems by controller.searchItems.collectAsState()
    val selection by controller.selectionState.collectAsState()
    val searchText by controller.searchText.collectAsState()
    val isSearchExpanded by controller.isSearchExpanded.collectAsState()
    val searchedReportOverview by controller.searchedReportOverview.collectAsState()
    val selectedPlaceID by controller.selectedPlaceID.collectAsState()
    val expandedItem by controller.expandedItem.collectAsState()
    val showUserProfileOverlay by controller.showUserProfileOverlay.collectAsState()
    val routerIsPresented by router.isPresented.collectAsState()
    val routerReport by router.report.collectAsState()
    val isGuestUser by appState.isGuestUser.collectAsState()
    val userProfile by appState.userProfile.collectAsState()
    val rawSuggestions by searchCompleter.suggestions.collectAsState()
    val isPro by SubscriptionManager.isPro.collectAsState()

    var showLocationAlert by remember { mutableStateOf(false) }

    val hasLocationPermission = remember(context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    val cameraPositionState = rememberCameraPositionState {
        position = cameraRegion.toGoogleCameraPosition()
    }

    // Keep GoogleMap camera synchronized with AuthViewModel.cameraPosition (center & span)
    LaunchedEffect(cameraRegion) {
        val targetCameraPosition = cameraRegion.toGoogleCameraPosition()
        val currentTarget = cameraPositionState.position.target
        val latDiff = abs(currentTarget.latitude - targetCameraPosition.target.latitude)
        val lngDiff = abs(currentTarget.longitude - targetCameraPosition.target.longitude)
        val zoomDiff = abs(cameraPositionState.position.zoom - targetCameraPosition.zoom)

        if (latDiff > 1e-6 || lngDiff > 1e-6 || zoomDiff > 0.05f) {
            cameraPositionState.position = targetCameraPosition
            try {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newCameraPosition(targetCameraPosition),
                    durationMs = 400
                )
            } catch (_: Exception) {
                cameraPositionState.position = targetCameraPosition
            }
        }
    }

    // Equivalent to iOS .task { controller.authViewModel = appState; controller.settings = settings; await controller.loadReports() }
    LaunchedEffect(appState, settings) {
        controller.authViewModel = appState
        controller.settings = settings
        appState.checkStatus()
        controller.loadReports()
    }

    // Equivalent to iOS .onMapCameraChange(frequency: .onEnd)
    var wasMovingByGesture by remember { mutableStateOf(false) }
    LaunchedEffect(cameraPositionState.isMoving, cameraPositionState.cameraMoveStartedReason) {
        if (cameraPositionState.isMoving) {
            if (cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                wasMovingByGesture = true
            }
        } else if (wasMovingByGesture) {
            wasMovingByGesture = false
            val center = cameraPositionState.position.target
            controller.handleMapMovement(Coordinate(lat = center.latitude, lng = center.longitude))

            val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            if (bounds != null) {
                val latDelta = abs(bounds.northeast.latitude - bounds.southwest.latitude)
                val lngDelta = abs(bounds.northeast.longitude - bounds.southwest.longitude)
                appState.updateCameraSpan(latitudeDelta = latDelta, longitudeDelta = lngDelta)
            }

            if (settings.saveLastLocation) {
                appState.updateLastLocation(
                    latitude = center.latitude,
                    longitude = center.longitude
                )
            }
        }
    }

    // Equivalent to iOS .onChange(of: controller.searchText)
    LaunchedEffect(searchText) {
        if (REPORT_ID_REGEX.matches(searchText)) {
            controller.searchReport(searchText)
        } else {
            controller.clearSearchedReportOverview()
            searchCompleter.update(
                query = searchText,
                center = cameraRegion.center,
                countryCode = settings.countryCode
            )
        }
    }

    BackHandler(enabled = isSearchExpanded) {
        controller.setSearchExpanded(false)
    }

    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = rememberMapStyleOptions(isDarkTheme = isDarkTheme)
    val mapProperties = remember(hasLocationPermission, mapStyleOptions) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = mapStyleOptions
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        val topMapPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 68.dp
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // Match the dark style's base color so the map doesn't flash white before tiles load
            googleMapOptionsFactory = {
                GoogleMapOptions().apply {
                    if (isDarkTheme) backgroundColor(0xFF242F3E.toInt())
                }
            },
            properties = mapProperties,
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            contentPadding = PaddingValues(top = topMapPadding, start = 32.dp, bottom = 90.dp)
        ) {
            reports.forEach { report ->
                key(report.id) {
                    ReportMarker(
                        report = report,
                        isSelected = report.id == selectedPlaceID,
                        onClick = {
                            controller.setExpandedItem(report)
                            controller.setSelectedPlaceID(report.id)
                        }
                    )
                }
            }

            searchMarker?.let { marker ->
                Marker(
                    state = MarkerState(
                        position = LatLng(marker.coordinate.lat, marker.coordinate.lng)
                    ),
                    title = marker.title,
                    snippet = marker.description.ifEmpty { null }
                )
            }
        }

        // Pulse of the selected report, drawn over the map (see PulseRingsOverlay)
        reports.firstOrNull { it.id == selectedPlaceID }?.let { selectedReport ->
            PulseRingsOverlay(
                cameraPositionState = cameraPositionState,
                center = LatLng(selectedReport.lat, selectedReport.lng),
                tint = IssueStatus.fromIdentifier(selectedReport.statusId).color,
                size = 80.dp,
                markerSize = 50.dp
            )
        }

        // Bottom gradient overlay matching iOS MapExplorerView.bottom
        BottomMapGradient(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(225.dp)
        )

        // Top CustomTabBar inset matching iOS safeAreaInset(edge: .top)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
                .zIndex(30f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Equivalent to iOS IssueStatus.title: the localized titles are resolved here
            val searchTitles = searchItems.map { stringResource(it.titleRes) }
            CustomTabBar(
                items = searchTitles,
                searchHint = stringResource(R.string.reports_petitions_places),
                selection = selection?.let { stringResource(it.titleRes) } ?: "",
                onSelectionChange = { title ->
                    controller.updateSelection(searchItems.getOrNull(searchTitles.indexOf(title)))
                },
                searchText = searchText,
                onSearchTextChange = { controller.updateSearchText(it) },
                isSearchExpanded = isSearchExpanded,
                onSearchExpandedChange = { controller.setSearchExpanded(it) },
                onSearchActivated = { controller.setSearchActivated(it) },
                onLocationTap = {
                    val fineGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val coarseGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!fineGranted && !coarseGranted) {
                        showLocationAlert = true
                    } else {
                        controller.centerMapOnUserLocation()
                    }
                }
            )
        }

        // Search Expanded Overlay: ReportOverviewList or SuggestionsResultList
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = scaleIn(
                initialScale = 0.95f,
                transformOrigin = TransformOrigin(0.5f, 0f),
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = scaleOut(
                targetScale = 0.95f,
                transformOrigin = TransformOrigin(0.5f, 0f)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .windowInsetsPadding(
                    WindowInsets.ime.union(
                        WindowInsets.navigationBars.add(WindowInsets(bottom = 84.dp))
                    )
                )
                .padding(top = 68.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            val overviewReport = searchedReportOverview
            val overlayShape = RoundedCornerShape(Dimensions.ThemeRadius * 2)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (overviewReport != null) {
                            Modifier.heightIn(max = 450.dp)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )
                    .shadow(
                        elevation = 16.dp,
                        shape = overlayShape
                    )
                    .clip(overlayShape),
                shape = overlayShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
                tonalElevation = 6.dp
            ) {
                if (overviewReport != null) {
                    ReportOverviewList(
                        report = overviewReport,
                        onShowMoreDetails = {
                            coroutineScope.launch {
                                controller.setSearchExpanded(false)
                                delay(128)
                                router.report.value = overviewReport
                                router.activeTab.value = 1
                                router.isPresented.value = true
                            }
                        }
                    )
                } else {
                    val uiSuggestions = remember(rawSuggestions) {
                        rawSuggestions.map { item ->
                            SearchSuggestion(
                                id = item.id,
                                title = item.title,
                                subtitle = item.subtitle
                            )
                        }
                    }
                    SuggestionsResultList(
                        searchText = searchText,
                        suggestions = uiSuggestions,
                        onSuggestionSelected = { selected ->
                            val matching = rawSuggestions.firstOrNull { it.id == selected.id }
                            controller.setSearchExpanded(false)
                            coroutineScope.launch {
                                val targetCoordinate = matching?.resolvedCoordinate
                                    ?: matching?.let { searchCompleter.resolveCoordinate(it) }
                                controller.applySuggestion(
                                    suggestion = selected.title,
                                    coordinate = targetCoordinate
                                )
                            }
                        },
                        isPaidUser = isPro
                    )
                }
            }
        }
    }

    // Sheet for selected map report (equivalent to .sheet(item: $controller.expandedItem))
    expandedItem?.let { selectedReport ->
        ModalBottomSheet(
            onDismissRequest = { controller.setExpandedItem(null) }
        ) {
            DetailView(
                report = selectedReport,
                onDismiss = { controller.setExpandedItem(null) }
            )
        }
    }

    // Sheet for deep-linked or overview-selected report (equivalent to .sheet(isPresented: $router.isPresented))
    if (routerIsPresented) {
        routerReport?.let { deepLinkedReport ->
            ModalBottomSheet(
                onDismissRequest = { router.isPresented.value = false }
            ) {
                DetailView(
                    report = deepLinkedReport,
                    onDismiss = { router.isPresented.value = false }
                )
            }
        }
    }

    // Full-screen overlay for UserProfile
    if (showUserProfileOverlay) {
        ModalBottomSheet(
            onDismissRequest = { controller.setShowUserProfileOverlay(false) }
        ) {
            UserProfileScreen(
                authViewModel = appState,
                onDismiss = { controller.setShowUserProfileOverlay(false) },
                onLogout = {
                    controller.setShowUserProfileOverlay(false)
                    appState.logout()
                }
            )
        }
    }

    // Location Permission Alert matching iOS .alert("Location Permission", isPresented: $showLocationAlert)
    if (showLocationAlert) {
        AlertDialog(
            onDismissRequest = { showLocationAlert = false },
            title = { Text(stringResource(R.string.location_permission)) },
            text = { Text(stringResource(R.string.please_enable_location_permissions_in_settings)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationAlert = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun BottomMapGradient(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color.Black else Color.White

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.5f to baseColor.copy(alpha = 0.1f),
                        1.0f to baseColor.copy(alpha = 0.9f)
                    )
                )
            )
            .blur(radius = 30.dp)
    )
}

// Equivalent to iOS MapExplorerView.annotationView(_:)
@Composable
@GoogleMapComposable
private fun ReportMarker(
    report: MapExplorerReport,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Equivalent to iOS .animation(.easeInOut(duration: 0.25), value: isSelected)
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = EaseInOut),
        label = "ReportMarkerSelection"
    )
    val position = remember(report.lat, report.lng) { LatLng(report.lat, report.lng) }
    val markerState = remember(position) { MarkerState(position = position) }

    // MarkerComposable rasterizes its content to a bitmap, so the animated progress
    // is passed as a key to re-render the icon on every frame of the transition
    MarkerComposable(
        keys = arrayOf<Any>(report.id, selectionProgress, report.issueTypeId),
        state = markerState,
        anchor = Offset(0.5f, 0.5f),
        zIndex = if (isSelected) 1f else 0f,
        title = report.title,
        onClick = {
            onClick()
            true
        }
    ) {
        ReportAnnotationView(
            report = report,
            selectionProgress = selectionProgress
        )
    }
}

// Counterpart of iOS PulseRingView for map markers. A marker icon is a static bitmap and
// animating map shapes forces the whole map to re-render every frame, so the rings are
// drawn on a Compose canvas above the map, following the marker's screen position.
// The animated values are only read in the draw phase: no recomposition per frame.
@Composable
private fun PulseRingsOverlay(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    center: LatLng,
    tint: Color,
    size: Dp,
    markerSize: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseRingTransition")
    val rings = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = EaseInOut),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(index * 200, StartOffsetType.Delay)
            ),
            label = "PulseRingProgress"
        )
    }
    val markerCutout = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Reading the position redraws the rings while the camera moves
        cameraPositionState.position
        val point = cameraPositionState.projection?.toScreenLocation(center) ?: return@Canvas
        val ringCenter = Offset(point.x.toFloat(), point.y.toFloat())
        val maxRadius = size.toPx()

        // The canvas sits above the map, so cut the marker out to keep the rings behind it
        markerCutout.reset()
        markerCutout.addOval(Rect(center = ringCenter, radius = markerSize.toPx() / 2))

        clipPath(markerCutout, clipOp = ClipOp.Difference) {
            rings.forEach { ring ->
                // scaleEffect 0 -> 2 over a ring of diameter `size`, opacity 0.4 -> 0
                val progress = ring.value
                drawCircle(
                    color = tint.copy(alpha = 0.4f * (1f - progress)),
                    radius = maxRadius * progress,
                    center = ringCenter
                )
            }
        }
    }
}

@Composable
private fun ReportAnnotationView(
    modifier: Modifier = Modifier,
    report: MapExplorerReport,
    selectionProgress: Float
) {
    val iconVector: ImageVector = remember(report.issueTypeId) {
        when (IssueTypes.fromIdentifier(report.issueTypeId)) {
            IssueTypes.ROAD -> Icons.Default.Build
            IssueTypes.PUBLIC_SPACE -> Icons.Default.Warning
            IssueTypes.BUILDING -> Icons.Default.ReportProblem
            else -> Icons.Default.Place
        }
    }

    val circleSize = lerp(28.dp, 50.dp, selectionProgress)
    val iconPadding = lerp(3.dp, 8.dp, selectionProgress)

    Box(
        modifier = modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(
                    width = 2.dp,
                    color = Color.Gray.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .padding(iconPadding),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = report.title,
                tint = Color.Black,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReportOverviewList(
    modifier: Modifier = Modifier,
    report: MapExplorerReport,
    onShowMoreDetails: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DetailsHeader(title = report.title, description = report.description)

        BasicInformationView(report = report)

        Button(
            onClick = onShowMoreDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.show_more_details),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
