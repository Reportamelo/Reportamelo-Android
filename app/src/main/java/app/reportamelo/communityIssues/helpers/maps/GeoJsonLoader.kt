package app.reportamelo.communityIssues.helpers.maps

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.io.InputStreamReader

object GeoJsonLoader {
    fun loadCityBoundaries(context: Context, rawResourceId: Int): List<List<LatLng>> {
        val polygons = mutableListOf<List<LatLng>>()
        try {
            val inputStream = context.resources.openRawResource(rawResourceId)
            val jsonString = InputStreamReader(inputStream).readText()
            val jsonObject = JSONObject(jsonString)

            val features = jsonObject.optJSONArray("features") ?: return emptyList()
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.optJSONObject("geometry") ?: continue
                val type = geometry.optString("type")
                val coordinates = geometry.optJSONArray("coordinates") ?: continue

                if (type == "Polygon") {
                    polygons.add(parsePolygonCoords(coordinates))
                } else if (type == "MultiPolygon") {
                    for (j in 0 until coordinates.length()) {
                        polygons.add(parsePolygonCoords(coordinates.getJSONArray(j)))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return polygons
    }

    private fun parsePolygonCoords(coordsArray: org.json.JSONArray): List<LatLng> {
        val latLngs = mutableListOf<LatLng>()
        // GeoJSON has coordinates as [[[lng, lat], [lng, lat]...]]
        val exteriorRing = coordsArray.optJSONArray(0) ?: return emptyList()
        for (i in 0 until exteriorRing.length()) {
            val point = exteriorRing.getJSONArray(i)
            val lng = point.getDouble(0)
            val lat = point.getDouble(1)
            latLngs.add(LatLng(lat, lng))
        }
        return latLngs
    }
}
