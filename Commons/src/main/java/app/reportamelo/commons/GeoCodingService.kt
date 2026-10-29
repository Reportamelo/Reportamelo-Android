package app.reportamelo.commons

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

data class ReverseGeocodingResult(
    val country: String,
    val cityName: String,
    val address: String
)

class GeoCodingService(private val context: Context) {

    suspend fun getReverseGeocodingResult(latitude: Double, longitude: Double): ReverseGeocodingResult? {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(1500L) {
                try {
                    runInterruptible {
                        val geocoder = Geocoder(context, Locale.getDefault())

                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val mapItem = addresses[0]

                            val country = mapItem.countryName ?: "Unknown"
                            val cityName = mapItem.locality ?: mapItem.subAdminArea ?: "Unknown"
                            val address = mapItem.getAddressLine(0) ?: "Unknown"

                            ReverseGeocodingResult(
                                country = country,
                                cityName = cityName,
                                address = address
                            )
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun getCoordinatesFromAddress(query: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(2000L) {
                try {
                    runInterruptible {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(query, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val item = addresses[0]
                            Pair(item.latitude, item.longitude)
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
