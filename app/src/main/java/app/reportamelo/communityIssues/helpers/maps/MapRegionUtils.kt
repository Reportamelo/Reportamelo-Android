package app.reportamelo.communityIssues.helpers.maps

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

object MapRegionUtils {
    val applePark: CameraPosition = CameraPosition.builder()
        .target(LatLng(37.3346, -122.0090))
        .zoom(10f)
        .build()

    val defaultRegion: CameraPosition = CameraPosition.builder()
        .target(LatLng(13.6929, -89.2182))
        .zoom(10f)
        .build()
}
