package app.reportamelo.communityIssues.helpers.maps

import androidx.compose.foundation.isSystemInDarkTheme
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun CityDelimiterView(
    context: Context,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(13.701270, -89.224432), 11f)
    }

    var cityBoundaries by remember { mutableStateOf<List<List<LatLng>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val resourceId = context.resources.getIdentifier("sv_sansalvador", "raw", context.packageName)
        if (resourceId != 0) {
            cityBoundaries = GeoJsonLoader.loadCityBoundaries(context, resourceId)
        }
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
        cityBoundaries.forEach { polygonCoords ->
            Polygon(
                points = polygonCoords,
                strokeColor = Color.Blue,
                strokeWidth = 2f,
                fillColor = Color.Blue.copy(alpha = 0.15f)
            )
        }
    }
}
