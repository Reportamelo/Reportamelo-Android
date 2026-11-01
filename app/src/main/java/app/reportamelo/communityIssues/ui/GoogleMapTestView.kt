package app.reportamelo.communityIssues.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState



@Composable
fun GoogleMapTestView() {
    val elSalvador = LatLng(13.7012837, -89.2270136)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(elSalvador, 10f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = elSalvador),
            title = "Reportamelo Location",
            snippet = "San Salvador"
        )
    }
}
