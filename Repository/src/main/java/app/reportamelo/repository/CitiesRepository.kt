package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.locator.data.DistrictEntity
import app.reportamelo.locator.database.LocatorLocalDataSource
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

    fun filterLocal(
        countryCode: String = CountryCode.SV.name,
        departmentalCapital: Boolean? = null,
        stateName: String? = null,
        cityName: String? = null,
        groupingName: String? = null,
        limit: Int = 30
    ): FriendlyCities {
        val districts: List<DistrictEntity> = when {
            departmentalCapital == true -> {
                LocatorLocalDataSource.findDepartmentalCapitals(countryCode = countryCode)
            }
            !groupingName.isNullOrEmpty() -> {
                LocatorLocalDataSource.findDistrictsByGroupingName(
                    groupingName = groupingName,
                    countryCode = countryCode,
                    limit = limit
                )
            }
            !cityName.isNullOrEmpty() -> {
                LocatorLocalDataSource.findDistrictsByThirdLevel(
                    thirdLevel = cityName,
                    countryCode = countryCode,
                    limit = limit
                )
            }
            !stateName.isNullOrEmpty() -> {
                LocatorLocalDataSource.findDistrictsByState(
                    stateName = stateName,
                    countryCode = countryCode,
                    limit = limit
                )
            }
            else -> {
                LocatorLocalDataSource.findDistrictsByThirdLevel(
                    thirdLevel = "",
                    countryCode = countryCode,
                    limit = limit
                )
            }
        }

        val documents = districts.map { it.toFriendlyCityDistribution() }
        return FriendlyCities(
            documents = documents,
            total = documents.size,
            page = 1,
            documentsPerPage = limit,
            totalPages = 1,
            hasNext = false,
            hasPrev = false
        )
    }

    suspend fun filter(q: PaginatedRequestQueryParams): FriendlyCities {
        val localResult = filterLocal(
            countryCode = q.countryCode ?: CountryCode.SV.name,
            departmentalCapital = q.departmentalCapital,
            stateName = q.stateName,
            cityName = q.cityName,
            groupingName = q.groupingName
        )
        if (!localResult.documents.isNullOrEmpty()) {
            return localResult
        }
        return try {
            service.filter(q)
        } catch (_: Exception) {
            localResult
        }
    }
}

fun DistrictEntity.toFriendlyCityDistribution(): FriendlyCityDistribution {
    return FriendlyCityDistribution(
        cityId = cityId,
        firstLevel = firstLevel ?: "",
        secondLevel = secondLevel ?: "",
        thirdLevel = thirdLevel ?: "",
        ZipCode = zipCode,
        legalGroupName = legalGroupName ?: "",
        coordinates = Coordinate(lat = lat, lng = lng),
        isCapitalCity = if (isCapitalCity) 1 else 0,
        isDepartmentalCapital = if (isDepartmentalCapital) 1 else 0,
        groupingId = groupingId,
        groupingName = groupingName
    )
}
