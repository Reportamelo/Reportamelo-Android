package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.utils.*
import app.reportamelo.models.Coordinate
import app.reportamelo.communityIssues.datamodel.*

import app.reportamelo.commons.*
import app.reportamelo.models.*

import app.reportamelo.repository.*
import app.reportamelo.network.services.*
import app.reportamelo.network.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import app.reportamelo.models.*

// import app.reportamelo.network.services.*
// import app.reportamelo.repository.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID


data class IssueMarker(
    val id: String,
    val title: String,
    val description: String,
    val status: Int,
    val coordinate: Coordinate
)

class MapExplorerViewModel : ViewModel() {
    companion object {
        val shared = MapExplorerViewModel()
    }

    var authViewModel: AuthViewModel? = null
    var settings: SettingsStore? = null

    private val _reports = MutableStateFlow<List<MapExplorerReport>>(emptyList())
    val reports: StateFlow<List<MapExplorerReport>> = _reports.asStateFlow()

    private val _hasCenteredOnUser = MutableStateFlow(false)
    val hasCenteredOnUser: StateFlow<Boolean> = _hasCenteredOnUser.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedStatuses = MutableStateFlow<Set<IssueStatus>>(IssueStatus.values().toSet())
    val selectedStatuses: StateFlow<Set<IssueStatus>> = _selectedStatuses.asStateFlow()

    private val _searchMarker = MutableStateFlow<IssueMarker?>(null)
    val searchMarker: StateFlow<IssueMarker?> = _searchMarker.asStateFlow()

    private val _showSearchOverlay = MutableStateFlow(false)
    val showSearchOverlay: StateFlow<Boolean> = _showSearchOverlay.asStateFlow()

    private val _showUserProfileOverlay = MutableStateFlow(false)
    val showUserProfileOverlay: StateFlow<Boolean> = _showUserProfileOverlay.asStateFlow()

    private val _showDetailView = MutableStateFlow(false)
    val showDetailView: StateFlow<Boolean> = _showDetailView.asStateFlow()

    private val _isPresented = MutableStateFlow(false)
    val isPresented: StateFlow<Boolean> = _isPresented.asStateFlow()

    private val _selectedPlaceID = MutableStateFlow<String?>(null)
    val selectedPlaceID: StateFlow<String?> = _selectedPlaceID.asStateFlow()

    private val _expandedItem = MutableStateFlow<MapExplorerReport?>(null)
    val expandedItem: StateFlow<MapExplorerReport?> = _expandedItem.asStateFlow()

    private val _selection = MutableStateFlow("")
    val selectionState: StateFlow<String> = _selection.asStateFlow()
    var selection: String
        get() = _selection.value
        set(value) {
            _selection.value = value
            viewModelScope.launch {
                loadReports()
            }
        }

    fun updateSelection(value: String) {
        selection = value
    }

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    fun setSearchExpanded(expanded: Boolean) {
        _isSearchExpanded.value = expanded
    }

    private val _isSearchActivated = MutableStateFlow(false)
    val isSearchActivated: StateFlow<Boolean> = _isSearchActivated.asStateFlow()

    fun setSearchActivated(activated: Boolean) {
        _isSearchActivated.value = activated
    }

    fun setExpandedItem(report: MapExplorerReport?) {
        _expandedItem.value = report
    }

    fun setSelectedPlaceID(placeId: String?) {
        _selectedPlaceID.value = placeId
    }

    fun setShowUserProfileOverlay(show: Boolean) {
        _showUserProfileOverlay.value = show
    }

    fun setShowSearchOverlay(show: Boolean) {
        _showSearchOverlay.value = show
    }

    fun clearSearchedReportOverview() {
        _searchedReportOverview.value = null
    }

    private val _searchItems = MutableStateFlow<List<String>>(
        listOf(
            IssueStatus.CONFIRMED.title,
            IssueStatus.IN_PROGRESS.title,
            IssueStatus.FIXED.title,
            IssueStatus.PETITION_TO_SIGN.title,
            IssueStatus.ASSIGNED.title
        )
    )
    val searchItems: StateFlow<List<String>> = _searchItems.asStateFlow()

    private val _isAwaitingLocation = MutableStateFlow(false)
    val isAwaitingLocation: StateFlow<Boolean> = _isAwaitingLocation.asStateFlow()

    private var currentFetchID = UUID.randomUUID()

    private val _searchedReportOverview = MutableStateFlow<MapExplorerReport?>(null)
    val searchedReportOverview: StateFlow<MapExplorerReport?> = _searchedReportOverview.asStateFlow()

    suspend fun loadReports() {
        val authVm = authViewModel ?: return
        val fetchID = UUID.randomUUID()
        currentFetchID = fetchID

        val statusIds = if (selection.isEmpty()) {
            listOf(
                IssueStatus.CONFIRMED.identifier,
                IssueStatus.IN_PROGRESS.identifier,
                IssueStatus.FIXED.identifier,
                IssueStatus.PETITION_TO_SIGN.identifier,
                IssueStatus.ASSIGNED.identifier
            )
        } else {
            IssueStatus.values().filter { it.title == selection }.map { it.identifier }
        }

        val currentCenter = authVm.cameraPosition.value.center
        val query = MapExplorerQueryParams(
            lat = currentCenter.lat,
            lng = currentCenter.lng,
            radius = 600,
            issueTypeIds = listOf(1),
            severityIds = listOf(1),
            statusIds = statusIds
        )

        try {
            val fetchedReports = MapExplorerRepository.reportsByLocation(
                lat = query.lat,
                lng = query.lng,
                radius = query.radius,
                severityId = query.severityIds.firstOrNull(),
                issueTypeId = query.issueTypeIds.firstOrNull(),
                cityId = authVm.selectedCity.value?.cityId ?: "1"
            )
            if (currentFetchID == fetchID) {
                _reports.value = fetchedReports
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun loadReportsWith(coordinates: Coordinate) {
        val fetchID = UUID.randomUUID()
        currentFetchID = fetchID

        val statusIds = if (selection.isEmpty()) {
            IssueStatus.values().map { it.identifier }
        } else {
            IssueStatus.values().filter { it.title == selection }.map { it.identifier }
        }

        try {
            val query = MapExplorerQueryParams(
                lat = coordinates.lat,
                lng = coordinates.lng,
                radius = 300,
                issueTypeIds = IssueTypes.values().map { it.identifier },
                severityIds = Severity.values().map { it.identifier },
                statusIds = statusIds
            )

            val fetchedReports = MapExplorerRepository.reportsByLocation(
                lat = query.lat,
                lng = query.lng,
                radius = query.radius,
                severityId = query.severityIds.firstOrNull(),
                issueTypeId = query.issueTypeIds.firstOrNull(),
                cityId = authViewModel?.selectedCity?.value?.cityId ?: "1"
            )
            if (currentFetchID == fetchID) {
                _reports.value = fetchedReports
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun searchReport(id: String) {
        try {
            val result = MapExplorerRepository.report(id, CountryCode.SV.name, "")
            _searchedReportOverview.value = result
            authViewModel?.setCameraPosition(
                to = Coordinate(result.lat, result.lng),
                latitudeDelta = 0.009,
                longitudeDelta = 0.009
            )
        } catch (e: Exception) {
            _searchedReportOverview.value = null
            handleException(e)
        }
    }

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun performSearch(targetCoordinate: Coordinate? = null, title: String = "Result") {
        val coordinate = targetCoordinate
            ?: authViewModel?.cameraPosition?.value?.center
            ?: Coordinate(MapCameraRegion.DEFAULT_LATITUDE, MapCameraRegion.DEFAULT_LONGITUDE)
        _searchMarker.value = IssueMarker(
            id = UUID.randomUUID().toString(),
            title = title,
            description = "",
            status = 1,
            coordinate = coordinate
        )
        authViewModel?.setCameraPosition(
            to = coordinate,
            latitudeDelta = if (targetCoordinate != null) 0.009769149244501563 else 0.03,
            longitudeDelta = if (targetCoordinate != null) 0.006849679212379556 else 0.03
        )
        if (targetCoordinate != null) {
            viewModelScope.launch {
                loadReportsWith(coordinate)
            }
        }
    }

    fun applySuggestion(suggestion: String, coordinate: Coordinate? = null) {
        _searchText.value = suggestion
        _showSearchOverlay.value = false
        performSearch(targetCoordinate = coordinate, title = suggestion.ifEmpty { "Result" })
    }

    fun centerMapOnUserLocation(userLocation: Coordinate? = null) {
        val authVm = authViewModel ?: return
        val location = userLocation
            ?: authVm.selectedCity.value?.coordinates
            ?: Coordinate(13.69, -89.19)
        _isAwaitingLocation.value = false
        _hasCenteredOnUser.value = true
        authVm.setCameraPosition(
            to = location,
            latitudeDelta = 0.0082,
            longitudeDelta = 0.0082
        )

        viewModelScope.launch {
            loadReportsWith(location)
        }
    }

    fun handleMapMovement(center: Coordinate) {
        viewModelScope.launch {
            delay(550)
            // Reverse geocoding / region check after camera movement settles
        }
    }

    private fun handleException(e: Exception) {
        when (e) {
            is CommonIntercommunicationErrors.InvalidPetition -> android.util.Log.e("MapExplorerViewModel", "Invalid request (${e.message})")
            is CommonIntercommunicationErrors.NotFound -> android.util.Log.e("MapExplorerViewModel", "Reports not found")
            is CommonIntercommunicationErrors.ServerError -> android.util.Log.e("MapExplorerViewModel", "Server error (${e.message})")
            is CommonIntercommunicationErrors.NetworkError -> android.util.Log.e("MapExplorerViewModel", "Network error: ${e.message}")
            else -> android.util.Log.e("MapExplorerViewModel", "An unexpected error occurred: ${e.message}")
        }
    }
}
