package app.reportamelo.repository

import app.reportamelo.commons.*
import app.reportamelo.models.*
import app.reportamelo.network.*
import android.location.Location

object GeoCodingRepository {
    
    private val service = GeoCodingService()
    
    suspend fun updateLocationDetails(location: Location) {
        // Implementation from Swift was empty, left empty here
    }
}
