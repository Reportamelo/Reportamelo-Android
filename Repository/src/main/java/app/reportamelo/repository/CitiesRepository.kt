package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CitiesRepository {

    var service = app.reportamelo.network.CityService(ServiceClient(Apis.apiV1))
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun loadLocalCities(countryCode: CountryCode = CountryCode.SV): FriendlyCities {
        val fileName = "${countryCode.name}.cities.json"
        val rawJson = try {
            val assetStream = AppPreferences.appContext?.assets?.open(fileName)
            val resourceStream = assetStream ?: CitiesRepository::class.java.classLoader?.getResourceAsStream(fileName)
            resourceStream?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        } ?: return FriendlyCities(emptyList(), 0, 0, 0, 0, false, false)

        val loadedCities = try {
            json.decodeFromString<List<FriendlyCityDistribution>>(rawJson).toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }

        val savedCityJson = AppPreferences.getString("selected_city")
        if (!savedCityJson.isNullOrEmpty()) {
            try {
                val savedCity = json.decodeFromString<FriendlyCityDistribution>(savedCityJson)
                loadedCities.removeAll { it.cityId == savedCity.cityId }
                loadedCities.add(0, savedCity)
            } catch (_: Exception) {
                // Ignore malformed saved city
            }
        }

        return FriendlyCities(
            documents = loadedCities,
            total = loadedCities.size,
            page = 0,
            documentsPerPage = 0,
            totalPages = 0,
            hasNext = false,
            hasPrev = false
        )
    }

    fun getSavedSelectedCity(): FriendlyCityDistribution? {
        val savedCityJson = AppPreferences.getString("selected_city") ?: return null
        return try {
            json.decodeFromString<FriendlyCityDistribution>(savedCityJson)
        } catch (_: Exception) {
            null
        }
    }

    fun saveSelectedCity(city: FriendlyCityDistribution) {
        try {
            val encoded = json.encodeToString(city)
            AppPreferences.setString("selected_city", encoded)
        } catch (_: Exception) {
            // Ignore encoding error
        }
    }

    suspend fun filter(q: PaginatedRequestQueryParams): FriendlyCities {
        return try {
            service.filter(q)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
