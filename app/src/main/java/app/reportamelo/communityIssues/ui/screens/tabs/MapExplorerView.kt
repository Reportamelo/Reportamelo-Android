package app.reportamelo.communityIssues.ui.screens.tabs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import app.reportamelo.communityIssues.ui.components.BasicInformationView
import app.reportamelo.communityIssues.ui.components.CustomTabBar
import app.reportamelo.communityIssues.ui.components.DetailView
import app.reportamelo.communityIssues.ui.components.DetailsHeader
import app.reportamelo.communityIssues.ui.components.SearchSuggestion
import app.reportamelo.communityIssues.ui.components.SuggestionsResultList
import app.reportamelo.communityIssues.ui.screens.animations.PulseRingView
import app.reportamelo.communityIssues.ui.screens.modals.UserProfileScreen
import app.reportamelo.communityIssues.ui.theme.Dimensions
import app.reportamelo.models.Coordinate
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.services.SubscriptionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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

    Box(modifier = modifier.fillMaxSize()) {
        val topMapPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 68.dp
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            contentPadding = PaddingValues(top = topMapPadding, start = 32.dp, bottom = 90.dp)
        ) {
            reports.forEach { report ->
                val isSelected = report.id == selectedPlaceID
                MarkerComposable(
                    keys = arrayOf<Any>(report.id, isSelected, report.statusId, report.issueTypeId),
                    state = MarkerState(position = LatLng(report.lat, report.lng)),
                    title = report.title,
                    onClick = {
                        controller.setSelectedPlaceID(report.id)
                        controller.setExpandedItem(report)
                        true
                    }
                ) {
                    ReportAnnotationView(
                        report = report,
                        isSelected = isSelected
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
            CustomTabBar(
                items = searchItems,
                searchHint = "Reports, Petitions, Places...",
                selection = selection,
                onSelectionChange = { controller.updateSelection(it) },
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
            title = { Text("Location Permission") },
            text = { Text("Please enable location permissions in settings.") },
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
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationAlert = false }) {
                    Text("Cancel")
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

@Composable
private fun ReportAnnotationView(
    modifier: Modifier = Modifier,
    report: MapExplorerReport,
    isSelected: Boolean
) {
    val statusColor = remember(report.statusId) {
        when (IssueStatus.fromIdentifier(report.statusId)) {
            IssueStatus.CONFIRMED -> Color(0xFFE53935)
            IssueStatus.IN_PROGRESS -> Color(0xFFFB8C00)
            IssueStatus.FIXED -> Color(0xFF43A047)
            IssueStatus.PETITION_TO_SIGN -> Color(0xFF1E88E5)
            IssueStatus.ASSIGNED -> Color(0xFF8E24AA)
            else -> Color(0xFF1E88E5)
        }
    }

    val iconVector: ImageVector = remember(report.issueTypeId) {
        when (IssueTypes.fromIdentifier(report.issueTypeId)) {
            IssueTypes.ROAD -> Icons.Default.Build
            IssueTypes.PUBLIC_SPACE -> Icons.Default.Warning
            IssueTypes.BUILDING -> Icons.Default.ReportProblem
            else -> Icons.Default.Place
        }
    }

    val circleSize = if (isSelected) 50.dp else 28.dp
    val iconPadding = if (isSelected) 8.dp else 3.dp

    Box(
        modifier = modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            PulseRingView(tint = statusColor, size = 80.dp)
        }
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
                text = "Show more details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
