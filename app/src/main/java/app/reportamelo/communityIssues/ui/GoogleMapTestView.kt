package app.reportamelo.communityIssues.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.reportamelo.communityIssues.helpers.maps.rememberMapStyleOptions
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun GoogleMapTestView(
    modifier: Modifier = Modifier
) {
    val elSalvador = LatLng(13.7012837, -89.2270136)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(elSalvador, 10f)
    }

    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = rememberMapStyleOptions(isDarkTheme = isDarkTheme)
    val mapProperties = remember(mapStyleOptions) {
        MapProperties(mapStyleOptions = mapStyleOptions)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        // Match the dark style's base color so the map doesn't flash white before tiles load
        googleMapOptionsFactory = {
            GoogleMapOptions().apply {
                if (isDarkTheme) backgroundColor(0xFF242F3E.toInt())
            }
        },
        properties = mapProperties
    ) {
        Marker(
            state = MarkerState(position = elSalvador),
            title = "Reportamelo Location",
            snippet = "San Salvador"
        )
    }
}
