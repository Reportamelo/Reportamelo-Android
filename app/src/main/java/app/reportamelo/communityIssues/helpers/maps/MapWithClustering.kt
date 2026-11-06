package app.reportamelo.communityIssues.helpers.maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState

data class MapClusterItem(
    val itemPosition: LatLng,
    val itemTitle: String,
    val itemSnippet: String,
    val itemZIndex: Float?
) : ClusterItem {
    override fun getPosition(): LatLng = itemPosition
    override fun getTitle(): String = itemTitle
    override fun getSnippet(): String = itemSnippet
    override fun getZIndex(): Float? = itemZIndex
}

@Composable
fun MapWithClustering(
    modifier: Modifier = Modifier,
    items: List<MapClusterItem>
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(13.701270, -89.224432), 11f)
    }

    val mapStyleOptions = rememberMapStyleOptions()
    val mapProperties = remember(mapStyleOptions) {
        MapProperties(mapStyleOptions = mapStyleOptions)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties
    ) {
        Clustering(
            items = items,
            onClusterClick = {
                // Handle cluster click
                false
            },
            onClusterItemClick = {
                // Handle single item click
                false
            }
        )
    }
}
