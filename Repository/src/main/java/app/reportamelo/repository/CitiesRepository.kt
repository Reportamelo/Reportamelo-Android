package app.reportamelo.repository

import android.content.Context
import app.reportamelo.models.FriendlyCityDistribution
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.models.CountryCode
import app.reportamelo.network.CityService
import app.reportamelo.models.Coordinate
import app.reportamelo.locator.database.LocatorDatabase
import app.reportamelo.locator.database.LocatorDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

typealias FriendlyCities = PaginatedResponse<FriendlyCityDistribution>

object CitiesRepository {
    private val service = CityService()
    var context: Context? = null // Must be set to use SharedPreferences and assets
    private val gson = Gson()
    
    private val dao: LocatorDao?
        get() = context?.let { LocatorDatabase.getDatabase(it).locatorDao() }

    fun loadLocalCities(countryCode: CountryCode): FriendlyCities {
        val ctx = context ?: return PaginatedResponse(emptyList(), hasNext = false, hasPrev = false)
        
        return try {
            val fileName = "${countryCode.name}.cities.json"
            val inputStream = ctx.assets.open(fileName)
            val reader = InputStreamReader(inputStream)
            
            val listType = object : TypeToken<List<FriendlyCityDistribution>>() {}.type
            val documents: MutableList<FriendlyCityDistribution> = gson.fromJson(reader, listType) ?: mutableListOf()
            
            val sharedPreferences = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedCityJson = sharedPreferences.getString("selected_city", null)
            
            if (savedCityJson != null) {
                val decodedCity = gson.fromJson(savedCityJson, FriendlyCityDistribution::class.java)
                if (decodedCity != null) {
                    documents.removeAll { it.cityId == decodedCity.cityId }
                    documents.add(0, decodedCity)
                }
            }
            
            PaginatedResponse(documents, hasNext = false, hasPrev = false)
        } catch (e: Exception) {
            println("Error decoding JSON: ${e.message}")
            PaginatedResponse(emptyList(), hasNext = false, hasPrev = false)
        }
    }

    suspend fun filter(
        countryCode: String,
        page: Int,
        departmentalCapital: Boolean? = null,
        stateName: String? = null,
        cityName: String? = null,
        groupingName: String? = null
    ): FriendlyCities = withContext(Dispatchers.IO) {
        val safeDao = dao ?: return@withContext PaginatedResponse(emptyList(), hasNext = false, hasPrev = false)
        val districts = when {
            departmentalCapital == true -> {
                safeDao.findDepartmentalCapitals(countryCode)
            }
            !groupingName.isNullOrEmpty() -> {
                safeDao.findDistrictsByGroupingName(groupingName, countryCode, 30)
            }
            !cityName.isNullOrEmpty() -> {
                safeDao.findDistrictsByThirdLevel(cityName, countryCode, 30)
            }
            !stateName.isNullOrEmpty() -> {
                safeDao.findDistrictsByState(stateName, countryCode, 30)
            }
            else -> {
                safeDao.findDistrictsByThirdLevel("", countryCode, 30)
            }
        }
        
        val documents = districts.map { district ->
            FriendlyCityDistribution(
                cityId = district.cityId,
                firstLevel = district.firstLevel ?: "",
                secondLevel = district.secondLevel ?: "",
                thirdLevel = district.thirdLevel ?: "",
                zipCode = district.zipCode ?: "",
                legalGroupName = district.legalGroupName ?: "",
                coordinates = Coordinate(lat = district.lat, lng = district.lng),
                isCapitalCity = if (district.isCapitalCity) 1 else 0,
                isDepartmentalCapital = if (district.isDepartmentalCapital) 1 else 0,
                groupingId = district.groupingId,
                groupingName = district.groupingName
            )
        }
        
        PaginatedResponse(documents, hasNext = false, hasPrev = false)
    }
}
