package app.reportamelo.communityIssues.datamodel

import androidx.lifecycle.ViewModel
import app.reportamelo.commons.AppPreferences
import app.reportamelo.models.Coordinate
import app.reportamelo.models.FriendlyCityDistribution
import app.reportamelo.models.UserProfile
import app.reportamelo.repository.CitiesRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ln
import kotlin.math.max

data class CoordinateSpan(
    val latitudeDelta: Double = DEFAULT_LATITUDE_DELTA,
    val longitudeDelta: Double = DEFAULT_LONGITUDE_DELTA
) {
    companion object {
        const val DEFAULT_LATITUDE_DELTA = 0.016837009321045926
        const val DEFAULT_LONGITUDE_DELTA = 0.016440700713786782
        const val CITY_SELECTION_LATITUDE_DELTA = 0.005738743213994368
        const val CITY_SELECTION_LONGITUDE_DELTA = 0.003718218254761041
    }
}

data class MapCameraRegion(
    val center: Coordinate = Coordinate(
        lat = DEFAULT_LATITUDE,
        lng = DEFAULT_LONGITUDE
    ),
    val span: CoordinateSpan = CoordinateSpan()
) {
    val lat: Double get() = center.lat
    val lng: Double get() = center.lng

    fun toZoomLevel(): Float {
        val maxDelta = max(span.latitudeDelta, span.longitudeDelta).coerceAtLeast(1e-6)
        val zoom = (ln(360.0 / maxDelta) / ln(2.0)).toFloat()
        return zoom.coerceIn(2f, 21f)
    }

    fun toGoogleCameraPosition(): CameraPosition {
        return CameraPosition.fromLatLngZoom(
            LatLng(center.lat, center.lng),
            toZoomLevel()
        )
    }

    fun toLatLngBounds(): LatLngBounds {
        val halfLat = span.latitudeDelta / 2.0
        val halfLng = span.longitudeDelta / 2.0
        val southwest = LatLng(
            (center.lat - halfLat).coerceIn(-85.0, 85.0),
            (center.lng - halfLng).coerceIn(-180.0, 180.0)
        )
        val northeast = LatLng(
            (center.lat + halfLat).coerceIn(-85.0, 85.0),
            (center.lng + halfLng).coerceIn(-180.0, 180.0)
        )
        return LatLngBounds(southwest, northeast)
    }

    companion object {
        const val DEFAULT_LATITUDE = 13.86634819078747
        const val DEFAULT_LONGITUDE = -89.85026973265276
    }
}

class AuthViewModel : ViewModel() {

    companion object {
        const val KEY_SELECTED_CITY = "selected_city"
        const val KEY_MAP_LATITUDE = "map_latitude"
        const val KEY_MAP_LONGITUDE = "map_longitude"
        const val KEY_MAP_LATITUDE_DELTA = "map_latitude_delta"
        const val KEY_MAP_LONGITUDE_DELTA = "map_longitude_delta"
        const val KEY_SAVE_LAST_LOCATION = "saveLastLocation"

        val shared = AuthViewModel()
    }

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _user = MutableStateFlow<Any?>(null)
    val user: StateFlow<Any?> = _user.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isCheckingStatus = MutableStateFlow(true)
    val isCheckingStatus: StateFlow<Boolean> = _isCheckingStatus.asStateFlow()

    private val _isGuestUser = MutableStateFlow(false)
    val isGuestUser: StateFlow<Boolean> = _isGuestUser.asStateFlow()

    private val _landingViewMode = MutableStateFlow(false)
    val landingViewMode: StateFlow<Boolean> = _landingViewMode.asStateFlow()

    private val _selectedCity = MutableStateFlow<FriendlyCityDistribution?>(null)
    val selectedCity: StateFlow<FriendlyCityDistribution?> = _selectedCity.asStateFlow()

    private val _cameraPosition = MutableStateFlow(MapCameraRegion())
    val cameraPosition: StateFlow<MapCameraRegion> = _cameraPosition.asStateFlow()

    init {
        loadCityFromUserDefaults()
        setupInitialCameraPosition()
    }

    fun setSelectedCity(city: FriendlyCityDistribution?) {
        _selectedCity.value = city
        saveCityToUserDefaults()
        if (city != null) {
            setupInitialCameraPosition()
        }
    }

    fun setCameraPosition(
        to: Coordinate,
        latitudeDelta: Double = CoordinateSpan.DEFAULT_LATITUDE_DELTA,
        longitudeDelta: Double = CoordinateSpan.DEFAULT_LONGITUDE_DELTA
    ) {
        _cameraPosition.value = MapCameraRegion(
            center = to,
            span = CoordinateSpan(
                latitudeDelta = latitudeDelta,
                longitudeDelta = longitudeDelta
            )
        )
    }

    fun setupInitialCameraPosition() {
        val city = _selectedCity.value ?: return
        val latDelta = AppPreferences.getDouble(KEY_MAP_LATITUDE_DELTA, 0.0)
        val lonDelta = AppPreferences.getDouble(KEY_MAP_LONGITUDE_DELTA, 0.0)

        val span = if (latDelta != 0.0 && lonDelta != 0.0) {
            CoordinateSpan(latitudeDelta = latDelta, longitudeDelta = lonDelta)
        } else {
            CoordinateSpan(
                latitudeDelta = CoordinateSpan.DEFAULT_LATITUDE_DELTA,
                longitudeDelta = CoordinateSpan.DEFAULT_LONGITUDE_DELTA
            )
        }

        val shouldLoadLastLocation = AppPreferences.getBoolean(KEY_SAVE_LAST_LOCATION, false)
        val center = if (
            shouldLoadLastLocation &&
            AppPreferences.hasKey(KEY_MAP_LATITUDE) &&
            AppPreferences.hasKey(KEY_MAP_LONGITUDE)
        ) {
            Coordinate(
                lat = AppPreferences.getDouble(KEY_MAP_LATITUDE, city.coordinates.lat),
                lng = AppPreferences.getDouble(KEY_MAP_LONGITUDE, city.coordinates.lng)
            )
        } else {
            Coordinate(lat = city.coordinates.lat, lng = city.coordinates.lng)
        }

        _cameraPosition.value = MapCameraRegion(
            center = center,
            span = span
        )
    }

    fun updateLastLocation(latitude: Double, longitude: Double) {
        AppPreferences.setDouble(KEY_MAP_LATITUDE, latitude)
        AppPreferences.setDouble(KEY_MAP_LONGITUDE, longitude)
    }

    fun updateCameraSpan(latitudeDelta: Double, longitudeDelta: Double) {
        if (latitudeDelta > 0.0 && longitudeDelta > 0.0) {
            AppPreferences.setDouble(KEY_MAP_LATITUDE_DELTA, latitudeDelta)
            AppPreferences.setDouble(KEY_MAP_LONGITUDE_DELTA, longitudeDelta)
        }
    }

    private fun saveCityToUserDefaults() {
        val city = _selectedCity.value
        if (city != null) {
            CitiesRepository.saveSelectedCity(city)
        }
    }

    private fun loadCityFromUserDefaults() {
        val savedCity = CitiesRepository.getSavedSelectedCity()
        if (savedCity != null) {
            _selectedCity.value = savedCity
        }
    }

    fun setGuestUser(isGuest: Boolean) {
        _isGuestUser.value = isGuest
    }

    fun setLandingViewMode(mode: Boolean) {
        _landingViewMode.value = mode
    }

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }

    fun setCheckingStatus(checking: Boolean) {
        _isCheckingStatus.value = checking
    }

    fun checkStatus() {
        _isCheckingStatus.value = true
        if (_selectedCity.value == null) {
            loadCityFromUserDefaults()
            setupInitialCameraPosition()
        }
        _isCheckingStatus.value = false
    }

    fun logout() {
        _user.value = null
        _isLoggedIn.value = false
    }
}
