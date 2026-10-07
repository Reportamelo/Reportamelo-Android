package app.reportamelo.commons

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

data class ReverseGeocodingResult(
    val country: String,
    val cityName: String,
    val address: String
)

class GeoCodingService(private val context: Context) {

    suspend fun getReverseGeocodingResult(latitude: Double, longitude: Double): ReverseGeocodingResult? {
        return withContext(Dispatchers.IO) {
            // Mimic the Task.sleep from the iOS app
            delay(500) 
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                
                // Get maximum 1 result
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val mapItem = addresses[0]
                    
                    val country = mapItem.countryName ?: "Unknown"
                    val cityName = mapItem.locality ?: mapItem.subAdminArea ?: "Unknown"
                    val address = mapItem.getAddressLine(0) ?: "Unknown"
                    
                    return@withContext ReverseGeocodingResult(
                        country = country,
                        cityName = cityName,
                        address = address
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }
}
