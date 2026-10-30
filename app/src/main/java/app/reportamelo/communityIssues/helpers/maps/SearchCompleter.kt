package app.reportamelo.communityIssues.helpers.maps

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import app.reportamelo.commons.CountryCode
import app.reportamelo.models.Coordinate
import app.reportamelo.models.PaginatedRequestQueryParams
import app.reportamelo.repository.CitiesRepository
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

data class SearchSuggestion(
    val title: String,
    val subtitle: String,
    val address: Address? = null,
    val coordinate: Coordinate? = null,
    val placeId: String? = null,
    val uniqueId: String = "$title|$subtitle"
) {
    val id: String get() = uniqueId

    val resolvedCoordinate: Coordinate?
        get() = coordinate ?: if (address != null && address.hasLatitude() && address.hasLongitude()) {
            Coordinate(lat = address.latitude, lng = address.longitude)
        } else {
            null
        }
}

class SearchCompleter(
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val debounceMs: Long = 160L
) {
    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SearchSuggestion>> = _suggestions.asStateFlow()

    private val placesClient: PlacesClient? by lazy {
        val ctx = context ?: return@lazy null
        runCatching {
            if (!Places.isInitialized()) {
                val appInfo = ctx.packageManager.getApplicationInfo(
                    ctx.packageName,
                    PackageManager.GET_META_DATA
                )
                val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
                if (!apiKey.isNullOrBlank()) {
                    Places.initialize(ctx.applicationContext, apiKey)
                }
            }
            if (Places.isInitialized()) Places.createClient(ctx) else null
        }.getOrNull()
    }

    private var sessionToken: AutocompleteSessionToken? = null

    suspend fun update(
        query: String,
        center: Coordinate = DEFAULT_SV_CENTER,
        countryCode: String = "SV"
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }

        // 1. Emit immediate local autocomplete matches so UI responds with multiple items right away
        val localMatches = findLocalSuggestions(trimmed, countryCode)
        if (localMatches.isNotEmpty()) {
            _suggestions.value = deduplicateSuggestions(localMatches)
        }

        try {
            if (debounceMs > 0) {
                delay(debounceMs)
            }

            val combined = withContext(ioDispatcher) {
                coroutineScope {
                    val placesDeferred = async {
                        queryGooglePlaces(trimmed, center, countryCode)
                    }
                    val photonDeferred = async {
                        queryPhotonAutocomplete(trimmed, center, countryCode)
                    }
                    val nominatimDeferred = async {
                        queryNominatimAutocomplete(trimmed, countryCode)
                    }
                    val remoteCitiesDeferred = async {
                        queryBackendCities(trimmed, countryCode)
                    }
                    val geocoderDeferred = async {
                        queryBoundedGeocoder(trimmed, localMatches, countryCode)
                    }

                    val placesResults = placesDeferred.await()
                    val photonResults = photonDeferred.await()
                    val nominatimResults = nominatimDeferred.await()
                    val remoteCityResults = remoteCitiesDeferred.await()
                    val geocoderResults = geocoderDeferred.await()

                    val allCandidates = buildList {
                        addAll(placesResults)
                        addAll(localMatches)
                        addAll(remoteCityResults)
                        addAll(photonResults)
                        addAll(nominatimResults)
                        addAll(geocoderResults)
                    }

                    rankAndDeduplicateSuggestions(
                        query = trimmed,
                        candidates = allCandidates
                    )
                }
            }

            _suggestions.value = combined
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            if (_suggestions.value.isEmpty()) {
                _suggestions.value = deduplicateSuggestions(localMatches)
            }
        }
    }

    suspend fun resolveCoordinate(suggestion: SearchSuggestion): Coordinate? {
        suggestion.resolvedCoordinate?.let { return it }

        val placeId = suggestion.placeId
        val client = placesClient
        if (!placeId.isNullOrBlank() && client != null) {
            val resolvedFromPlace = withContext(ioDispatcher) {
                fetchPlaceCoordinate(client, placeId)
            }
            if (resolvedFromPlace != null) {
                return resolvedFromPlace
            }
        }

        val ctx = context ?: return null
        return withContext(ioDispatcher) {
            runCatching {
                val geocoder = Geocoder(ctx, Locale("es", "SV"))
                val searchQuery = if (suggestion.subtitle.isNotBlank()) {
                    "${suggestion.title}, ${suggestion.subtitle}"
                } else {
                    "${suggestion.title}, El Salvador"
                }
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(
                    searchQuery,
                    3,
                    SV_MIN_LAT,
                    SV_MIN_LNG,
                    SV_MAX_LAT,
                    SV_MAX_LNG
                )
                val firstValid = results?.firstOrNull { it.hasLatitude() && it.hasLongitude() }
                if (firstValid != null) {
                    Coordinate(lat = firstValid.latitude, lng = firstValid.longitude)
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    internal fun findLocalSuggestions(
        query: String,
        countryCode: String = "SV"
    ): List<SearchSuggestion> {
        val normalizedQuery = normalizeForSearch(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val results = mutableListOf<SearchSuggestion>()

        // Match from CitiesRepository local JSON if available
        val repoCities = runCatching {
            val code = runCatching { CountryCode.valueOf(countryCode.uppercase(Locale.ROOT)) }
                .getOrDefault(CountryCode.SV)
            CitiesRepository.loadLocalCities(code).documents.orEmpty()
        }.getOrDefault(emptyList())

        for (city in repoCities) {
            val cityTitle = city.thirdLevel.trim()
            val dept = city.secondLevel.trim()
            val country = city.firstLevel.trim().ifEmpty { "El Salvador" }
            val grouping = city.groupingName?.trim().orEmpty()
            val coord = Coordinate(lat = city.coordinates.lat, lng = city.coordinates.lng)

            if (matchesQuery(cityTitle, normalizedQuery) || matchesQuery(dept, normalizedQuery)) {
                results.add(
                    SearchSuggestion(
                        title = cityTitle,
                        subtitle = "$dept, $country",
                        coordinate = coord
                    )
                )
            }
            if (grouping.isNotEmpty() && matchesQuery(grouping, normalizedQuery)) {
                results.add(
                    SearchSuggestion(
                        title = grouping,
                        subtitle = "$dept, $country",
                        coordinate = coord
                    )
                )
            }
        }

        // Match against comprehensive Salvadoran municipalities, districts, streets & landmarks catalog
        if (countryCode.equals("SV", ignoreCase = true)) {
            for (entry in SV_PLACES_CATALOG) {
                if (
                    matchesQuery(entry.title, normalizedQuery) ||
                    matchesQuery(entry.subtitle, normalizedQuery) ||
                    entry.keywords.any { matchesQuery(it, normalizedQuery) }
                ) {
                    results.add(
                        SearchSuggestion(
                            title = entry.title,
                            subtitle = entry.subtitle,
                            coordinate = Coordinate(lat = entry.lat, lng = entry.lng)
                        )
                    )
                }
            }
        }

        return rankAndDeduplicateSuggestions(query, results)
    }

    private suspend fun queryBackendCities(
        query: String,
        countryCode: String
    ): List<SearchSuggestion> {
        return withTimeoutOrNull(2200L) {
            runCatching {
                val code = runCatching { CountryCode.valueOf(countryCode.uppercase(Locale.ROOT)) }
                    .getOrDefault(CountryCode.SV)
                val response = CitiesRepository.filter(
                    PaginatedRequestQueryParams(
                        countryCode = code.iso3166Alpha3Code,
                        page = 1,
                        cityName = query
                    )
                )
                response.documents.orEmpty().map { city ->
                    val title = city.thirdLevel.ifBlank { city.legalGroupName }
                    val subtitle = listOf(city.secondLevel, city.firstLevel.ifBlank { "El Salvador" })
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    SearchSuggestion(
                        title = title,
                        subtitle = subtitle,
                        coordinate = Coordinate(
                            lat = city.coordinates.lat,
                            lng = city.coordinates.lng
                        )
                    )
                }
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    private suspend fun queryGooglePlaces(
        query: String,
        center: Coordinate,
        countryCode: String
    ): List<SearchSuggestion> {
        val client = placesClient ?: return emptyList()
        return withTimeoutOrNull(2200L) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val token = sessionToken ?: AutocompleteSessionToken.newInstance().also {
                        sessionToken = it
                    }
                    val bounds = RectangularBounds.newInstance(
                        LatLng(
                            (center.lat - 0.25).coerceAtLeast(SV_MIN_LAT),
                            (center.lng - 0.25).coerceAtLeast(SV_MIN_LNG)
                        ),
                        LatLng(
                            (center.lat + 0.25).coerceAtMost(SV_MAX_LAT),
                            (center.lng + 0.25).coerceAtMost(SV_MAX_LNG)
                        )
                    )
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .setCountries(listOf(countryCode.uppercase(Locale.ROOT)))
                        .setLocationBias(bounds)
                        .setSessionToken(token)
                        .build()

                    client.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            if (continuation.isActive) {
                                val mapped = response.autocompletePredictions.map { prediction ->
                                    SearchSuggestion(
                                        title = prediction.getPrimaryText(null).toString(),
                                        subtitle = prediction.getSecondaryText(null).toString(),
                                        placeId = prediction.placeId
                                    )
                                }
                                continuation.resume(mapped)
                            }
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                } catch (_: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
            }
        } ?: emptyList()
    }

    private suspend fun fetchPlaceCoordinate(
        client: PlacesClient,
        placeId: String
    ): Coordinate? {
        return withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val request = FetchPlaceRequest.builder(
                        placeId,
                        listOf(Place.Field.LAT_LNG)
                    ).setSessionToken(sessionToken).build()

                    client.fetchPlace(request)
                        .addOnSuccessListener { response ->
                            sessionToken = null
                            val latLng = response.place.latLng
                            if (continuation.isActive) {
                                continuation.resume(
                                    if (latLng != null) {
                                        Coordinate(lat = latLng.latitude, lng = latLng.longitude)
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                } catch (_: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    private suspend fun queryPhotonAutocomplete(
        query: String,
        center: Coordinate,
        countryCode: String
    ): List<SearchSuggestion> {
        return withTimeoutOrNull(2500L) {
            runCatching {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val bbox = "$SV_MIN_LNG,$SV_MIN_LAT,$SV_MAX_LNG,$SV_MAX_LAT"
                val urlStr = "https://photon.komoot.io/api/?q=$encoded&lat=${center.lat}&lon=${center.lng}&limit=12&bbox=$bbox"
                val jsonText = httpGet(urlStr) ?: return@runCatching emptyList()
                val root = JSONObject(jsonText)
                val features = root.optJSONArray("features") ?: return@runCatching emptyList()

                val list = mutableListOf<SearchSuggestion>()
                for (i in 0 until features.length()) {
                    val feature = features.optJSONObject(i) ?: continue
                    val geometry = feature.optJSONObject("geometry")
                    val coords = geometry?.optJSONArray("coordinates")
                    val lng = coords?.optDouble(0) ?: continue
                    val lat = coords.optDouble(1)
                    if (lat.isNaN() || lng.isNaN()) continue

                    val props = feature.optJSONObject("properties") ?: continue
                    val propCountryCode = props.optString("countrycode", "")
                    if (propCountryCode.isNotEmpty() && !propCountryCode.equals(countryCode, ignoreCase = true)) {
                        continue
                    }

                    val name = props.optString("name").trim()
                    val street = props.optString("street").trim()
                    val district = props.optString("district").trim()
                        .ifEmpty { props.optString("locality").trim() }
                    val city = props.optString("city").trim()
                        .ifEmpty { props.optString("county").trim() }
                    val state = props.optString("state").trim()
                    val country = props.optString("country").trim().ifEmpty { "El Salvador" }

                    val title = name.ifEmpty { street }.ifEmpty { city }.ifEmpty { state }
                    if (title.isEmpty()) continue

                    val subtitleParts = mutableListOf<String>()
                    if (street.isNotEmpty() && !street.equals(title, ignoreCase = true)) {
                        subtitleParts.add(street)
                    }
                    if (district.isNotEmpty() && !district.equals(title, ignoreCase = true)) {
                        subtitleParts.add(district)
                    }
                    if (city.isNotEmpty() && !city.equals(title, ignoreCase = true) && !subtitleParts.contains(city)) {
                        subtitleParts.add(city)
                    }
                    if (state.isNotEmpty() && !subtitleParts.contains(state)) {
                        subtitleParts.add(state)
                    }
                    if (country.isNotEmpty() && !subtitleParts.contains(country)) {
                        subtitleParts.add(country)
                    }

                    list.add(
                        SearchSuggestion(
                            title = title,
                            subtitle = subtitleParts.joinToString(", "),
                            coordinate = Coordinate(lat = lat, lng = lng)
                        )
                    )
                }
                list
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    private suspend fun queryNominatimAutocomplete(
        query: String,
        countryCode: String
    ): List<SearchSuggestion> {
        return withTimeoutOrNull(2500L) {
            runCatching {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val cc = countryCode.lowercase(Locale.ROOT)
                val urlStr = "https://nominatim.openstreetmap.org/search?q=$encoded&countrycodes=$cc&format=jsonv2&addressdetails=1&limit=10&accept-language=es"
                val jsonText = httpGet(urlStr) ?: return@runCatching emptyList()
                val array = JSONArray(jsonText)

                val list = mutableListOf<SearchSuggestion>()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val lat = item.optString("lat").toDoubleOrNull() ?: continue
                    val lon = item.optString("lon").toDoubleOrNull() ?: continue
                    val displayName = item.optString("display_name").trim()
                    val rawName = item.optString("name").trim()
                    val addr = item.optJSONObject("address")

                    val road = addr?.optString("road").orEmpty().trim()
                    val suburb = addr?.optString("suburb").orEmpty().trim()
                        .ifEmpty { addr?.optString("neighbourhood").orEmpty().trim() }
                    val city = addr?.optString("city").orEmpty().trim()
                        .ifEmpty { addr?.optString("town").orEmpty().trim() }
                        .ifEmpty { addr?.optString("village").orEmpty().trim() }
                        .ifEmpty { addr?.optString("municipality").orEmpty().trim() }
                    val state = addr?.optString("state").orEmpty().trim()
                    val country = addr?.optString("country").orEmpty().trim().ifEmpty { "El Salvador" }

                    val title = rawName
                        .ifEmpty { road }
                        .ifEmpty { displayName.substringBefore(",").trim() }
                    if (title.isEmpty()) continue

                    val subtitleParts = mutableListOf<String>()
                    if (suburb.isNotEmpty() && !suburb.equals(title, ignoreCase = true)) {
                        subtitleParts.add(suburb)
                    }
                    if (city.isNotEmpty() && !city.equals(title, ignoreCase = true) && !subtitleParts.contains(city)) {
                        subtitleParts.add(city)
                    }
                    if (state.isNotEmpty() && !subtitleParts.contains(state)) {
                        subtitleParts.add(state)
                    }
                    if (country.isNotEmpty() && !subtitleParts.contains(country)) {
                        subtitleParts.add(country)
                    }

                    val subtitle = if (subtitleParts.isNotEmpty()) {
                        subtitleParts.joinToString(", ")
                    } else {
                        displayName.substringAfter(",", "").trim()
                    }

                    list.add(
                        SearchSuggestion(
                            title = title,
                            subtitle = subtitle,
                            coordinate = Coordinate(lat = lat, lng = lon)
                        )
                    )
                }
                list
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    private fun queryBoundedGeocoder(
        query: String,
        localMatches: List<SearchSuggestion>,
        countryCode: String
    ): List<SearchSuggestion> {
        val ctx = context ?: return emptyList()
        return runCatching {
            val geocoder = Geocoder(ctx, Locale("es", countryCode))
            val queriesToRun = buildList {
                add("$query, El Salvador")
                localMatches.take(2).forEach { local ->
                    val candidate = "${local.title}, El Salvador"
                    if (!contains(candidate)) {
                        add(candidate)
                    }
                }
            }

            val suggestions = mutableListOf<SearchSuggestion>()
            for (q in queriesToRun) {
                @Suppress("DEPRECATION")
                val addresses = runCatching {
                    geocoder.getFromLocationName(
                        q,
                        8,
                        SV_MIN_LAT,
                        SV_MIN_LNG,
                        SV_MAX_LAT,
                        SV_MAX_LNG
                    )
                }.getOrNull().orEmpty()

                for (address in addresses) {
                    if (!isWithinAllowedCountry(address, countryCode)) continue

                    val title = (address.thoroughfare ?: address.featureName ?: address.locality ?: address.subAdminArea ?: "")
                        .trim()
                    if (title.isEmpty()) continue

                    val locality = address.locality ?: address.subLocality ?: ""
                    val admin = address.adminArea ?: address.subAdminArea ?: ""
                    val country = address.countryName ?: "El Salvador"

                    val subtitleParts = listOf(locality, admin, country)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    val subtitle = subtitleParts.joinToString(", ")
                        .ifEmpty { address.getAddressLine(0).orEmpty() }

                    val coord = if (address.hasLatitude() && address.hasLongitude()) {
                        Coordinate(lat = address.latitude, lng = address.longitude)
                    } else {
                        null
                    }

                    suggestions.add(
                        SearchSuggestion(
                            title = title,
                            subtitle = subtitle,
                            address = address,
                            coordinate = coord
                        )
                    )
                }
            }
            suggestions
        }.getOrDefault(emptyList())
    }

    private fun isWithinAllowedCountry(address: Address, countryCode: String): Boolean {
        val addrCode = address.countryCode?.trim().orEmpty()
        if (addrCode.isNotEmpty()) {
            return addrCode.equals(countryCode, ignoreCase = true)
        }
        if (address.hasLatitude() && address.hasLongitude()) {
            val lat = address.latitude
            val lng = address.longitude
            return lat in SV_MIN_LAT..SV_MAX_LAT && lng in SV_MIN_LNG..SV_MAX_LNG
        }
        return address.countryName?.contains("El Salvador", ignoreCase = true) == true
    }

    private fun httpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2200
                readTimeout = 2200
                setRequestProperty("User-Agent", "Reportamelo-Android/1.0 (https://reportamelo.app)")
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val SV_MIN_LAT = 13.14
        private const val SV_MAX_LAT = 14.46
        private const val SV_MIN_LNG = -90.15
        private const val SV_MAX_LNG = -87.65
        val DEFAULT_SV_CENTER = Coordinate(lat = 13.6929, lng = -89.2182)

        internal fun normalizeForSearch(text: String): String {
            val decomposed = Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            return decomposed
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        internal fun matchesQuery(candidate: String, normalizedQuery: String): Boolean {
            if (normalizedQuery.isEmpty()) return false
            val normalizedCandidate = normalizeForSearch(candidate)
            if (normalizedCandidate.isEmpty()) return false
            if (normalizedCandidate.startsWith(normalizedQuery) || normalizedCandidate.contains(normalizedQuery)) {
                return true
            }
            val queryTokens = normalizedQuery.split(" ").filter { it.isNotEmpty() }
            val candidateTokens = normalizedCandidate.split(" ").filter { it.isNotEmpty() }
            return queryTokens.all { qToken ->
                candidateTokens.any { cToken -> cToken.startsWith(qToken) }
            }
        }

        internal fun deduplicateSuggestions(items: List<SearchSuggestion>): List<SearchSuggestion> {
            val seen = HashSet<String>()
            val result = ArrayList<SearchSuggestion>(items.size)
            for (item in items) {
                val normKey = "${normalizeForSearch(item.title)}|${normalizeForSearch(item.subtitle)}"
                if (seen.add(normKey)) {
                    val idx = result.size
                    result.add(
                        item.copy(uniqueId = "${idx}|${item.title}|${item.subtitle}")
                    )
                }
            }
            return result
        }

        internal fun rankAndDeduplicateSuggestions(
            query: String,
            candidates: List<SearchSuggestion>
        ): List<SearchSuggestion> {
            val normalizedQuery = normalizeForSearch(query)
            val deduplicated = deduplicateSuggestions(candidates)
            val sorted = deduplicated.sortedBy { suggestion ->
                val normTitle = normalizeForSearch(suggestion.title)
                val normSubtitle = normalizeForSearch(suggestion.subtitle)
                when {
                    normTitle == normalizedQuery -> 0
                    normTitle.startsWith(normalizedQuery) -> 1
                    normTitle.split(" ").any { it.startsWith(normalizedQuery) } -> 2
                    normTitle.contains(normalizedQuery) -> 3
                    normSubtitle.startsWith(normalizedQuery) -> 4
                    else -> 5
                }
            }
            return sorted.take(15).mapIndexed { index, item ->
                item.copy(uniqueId = "${index}|${item.title}|${item.subtitle}")
            }
        }

        private data class LocalPlaceEntry(
            val title: String,
            val subtitle: String,
            val lat: Double,
            val lng: Double,
            val keywords: List<String> = emptyList()
        )

        private val SV_PLACES_CATALOG: List<LocalPlaceEntry> = listOf(
            // Cuscatancingo & Cuscatlan area (matching iOS MKLocalSearchCompleter results)
            LocalPlaceEntry(
                title = "Cuscatancingo, San Salvador, El Salvador",
                subtitle = "",
                lat = 13.7290,
                lng = -89.1836,
                keywords = listOf("cuscatancingo", "san salvador centro")
            ),
            LocalPlaceEntry(
                title = "Avenida Cuscatancingo",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.7042,
                lng = -89.1887,
                keywords = listOf("cuscatancingo", "avenida cuscatancingo", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Avenida Cuscatancingo",
                subtitle = "Apopa, San Salvador, El Salvador",
                lat = 13.7982,
                lng = -89.1795,
                keywords = listOf("cuscatancingo", "avenida cuscatancingo", "apopa")
            ),
            LocalPlaceEntry(
                title = "Antiguo Cuscatlán",
                subtitle = "La Libertad, El Salvador",
                lat = 13.6649,
                lng = -89.2532,
                keywords = listOf("cuscatlan", "antiguo cuscatlan", "la libertad este")
            ),
            LocalPlaceEntry(
                title = "Nuevo Cuscatlán",
                subtitle = "La Libertad, El Salvador",
                lat = 13.6486,
                lng = -89.2653,
                keywords = listOf("cuscatlan", "nuevo cuscatlan", "la libertad este")
            ),
            LocalPlaceEntry(
                title = "Parque Cuscatlán",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.6989,
                lng = -89.2064,
                keywords = listOf("cuscatlan", "parque cuscatlan", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Estadio Cuscatlán",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.6811,
                lng = -89.2224,
                keywords = listOf("cuscatlan", "estadio cuscatlan", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Cuscatlán",
                subtitle = "El Salvador",
                lat = 13.7214,
                lng = -88.9352,
                keywords = listOf("cuscatlan", "cojutepeque")
            ),

            // San Salvador metropolitan area & streets/landmarks
            LocalPlaceEntry(
                title = "San Salvador",
                subtitle = "San Salvador, El Salvador",
                lat = 13.6929,
                lng = -89.2182,
                keywords = listOf("san salvador", "capital")
            ),
            LocalPlaceEntry(
                title = "Centro Histórico de San Salvador",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.6976,
                lng = -89.1912,
                keywords = listOf("san salvador", "centro historico")
            ),
            LocalPlaceEntry(
                title = "Paseo General Escalón",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.7014,
                lng = -89.2381,
                keywords = listOf("escalon", "paseo general escalon", "colonia escalon", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Colonia Escalón",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.7052,
                lng = -89.2425,
                keywords = listOf("escalon", "colonia escalon", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Alameda Franklin Delano Roosevelt",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.7005,
                lng = -89.2192,
                keywords = listOf("roosevelt", "alameda roosevelt", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Monumento al Divino Salvador del Mundo",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.7013,
                lng = -89.2244,
                keywords = listOf("salvador del mundo", "san salvador", "divino salvador")
            ),
            LocalPlaceEntry(
                title = "Volcán de San Salvador",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7347,
                lng = -89.2861,
                keywords = listOf("san salvador", "volcan de san salvador", "el boqueron")
            ),
            LocalPlaceEntry(
                title = "Boulevard de Los Héroes",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.7091,
                lng = -89.2121,
                keywords = listOf("boulevard de los heroes", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Alameda Juan Pablo II",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.7048,
                lng = -89.1995,
                keywords = listOf("alameda juan pablo ii", "san salvador")
            ),
            LocalPlaceEntry(
                title = "Colonia San Benito",
                subtitle = "San Salvador, San Salvador, El Salvador",
                lat = 13.6915,
                lng = -89.2412,
                keywords = listOf("san benito", "colonia san benito", "san salvador")
            ),

            // Major Municipalities & Districts across El Salvador
            LocalPlaceEntry(
                title = "Soyapango",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7102,
                lng = -89.1399,
                keywords = listOf("soyapango", "san salvador este")
            ),
            LocalPlaceEntry(
                title = "Mejicanos",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7267,
                lng = -89.2050,
                keywords = listOf("mejicanos", "san salvador centro")
            ),
            LocalPlaceEntry(
                title = "Apopa",
                subtitle = "San Salvador, El Salvador",
                lat = 13.8014,
                lng = -89.1783,
                keywords = listOf("apopa", "san salvador oeste")
            ),
            LocalPlaceEntry(
                title = "Ciudad Delgado",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7242,
                lng = -89.1706,
                keywords = listOf("delgado", "ciudad delgado", "san salvador centro")
            ),
            LocalPlaceEntry(
                title = "Ilopango",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7017,
                lng = -89.1094,
                keywords = listOf("ilopango", "san salvador este")
            ),
            LocalPlaceEntry(
                title = "San Marcos",
                subtitle = "San Salvador, El Salvador",
                lat = 13.6589,
                lng = -89.1831,
                keywords = listOf("san marcos", "san salvador sur")
            ),
            LocalPlaceEntry(
                title = "San Martín",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7369,
                lng = -89.0558,
                keywords = listOf("san martin", "san salvador este")
            ),
            LocalPlaceEntry(
                title = "Tonacatepeque",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7811,
                lng = -89.1186,
                keywords = listOf("tonacatepeque", "san salvador este")
            ),
            LocalPlaceEntry(
                title = "Ayutuxtepeque",
                subtitle = "San Salvador, El Salvador",
                lat = 13.7397,
                lng = -89.2036,
                keywords = listOf("ayutuxtepeque", "san salvador centro")
            ),
            LocalPlaceEntry(
                title = "Santa Tecla",
                subtitle = "La Libertad, El Salvador",
                lat = 13.6754,
                lng = -89.2916,
                keywords = listOf("santa tecla", "la libertad sur", "merliot")
            ),
            LocalPlaceEntry(
                title = "Ciudad Merliot",
                subtitle = "Santa Tecla, La Libertad, El Salvador",
                lat = 13.6789,
                lng = -89.2712,
                keywords = listOf("merliot", "ciudad merliot", "santa tecla")
            ),
            LocalPlaceEntry(
                title = "Colón",
                subtitle = "La Libertad, El Salvador",
                lat = 13.7292,
                lng = -89.3669,
                keywords = listOf("colon", "lourdes", "la libertad oeste")
            ),
            LocalPlaceEntry(
                title = "Puerto de La Libertad",
                subtitle = "La Libertad, El Salvador",
                lat = 13.4883,
                lng = -89.3222,
                keywords = listOf("la libertad", "puerto de la libertad", "surf city")
            ),
            LocalPlaceEntry(
                title = "San Juan Opico",
                subtitle = "La Libertad, El Salvador",
                lat = 13.8761,
                lng = -89.3597,
                keywords = listOf("san juan opico", "opico", "la libertad centro")
            ),
            LocalPlaceEntry(
                title = "Quezaltepeque",
                subtitle = "La Libertad, El Salvador",
                lat = 13.8314,
                lng = -89.2744,
                keywords = listOf("quezaltepeque", "la libertad norte")
            ),
            LocalPlaceEntry(
                title = "Santa Ana",
                subtitle = "Santa Ana, El Salvador",
                lat = 13.9948,
                lng = -89.5572,
                keywords = listOf("santa ana", "santa ana centro")
            ),
            LocalPlaceEntry(
                title = "Chalchuapa",
                subtitle = "Santa Ana, El Salvador",
                lat = 13.9867,
                lng = -89.6811,
                keywords = listOf("chalchuapa", "santa ana oeste")
            ),
            LocalPlaceEntry(
                title = "Metapán",
                subtitle = "Santa Ana, El Salvador",
                lat = 14.3333,
                lng = -89.4500,
                keywords = listOf("metapan", "santa ana norte")
            ),
            LocalPlaceEntry(
                title = "San Miguel",
                subtitle = "San Miguel, El Salvador",
                lat = 13.4817,
                lng = -88.1762,
                keywords = listOf("san miguel", "san miguel centro")
            ),
            LocalPlaceEntry(
                title = "Sonsonate",
                subtitle = "Sonsonate, El Salvador",
                lat = 13.7189,
                lng = -89.7242,
                keywords = listOf("sonsonate", "sonsonate centro")
            ),
            LocalPlaceEntry(
                title = "Ahuachapán",
                subtitle = "Ahuachapán, El Salvador",
                lat = 13.9196,
                lng = -89.8485,
                keywords = listOf("ahuachapan", "ahuachapan centro")
            ),
            LocalPlaceEntry(
                title = "Cojutepeque",
                subtitle = "Cuscatlán, El Salvador",
                lat = 13.7214,
                lng = -88.9352,
                keywords = listOf("cojutepeque", "cuscatlan sur")
            ),
            LocalPlaceEntry(
                title = "Zacatecoluca",
                subtitle = "La Paz, El Salvador",
                lat = 13.5084,
                lng = -88.8700,
                keywords = listOf("zacatecoluca", "la paz este")
            ),
            LocalPlaceEntry(
                title = "San Vicente",
                subtitle = "San Vicente, El Salvador",
                lat = 13.6419,
                lng = -88.7851,
                keywords = listOf("san vicente", "san vicente sur")
            ),
            LocalPlaceEntry(
                title = "Usulután",
                subtitle = "Usulután, El Salvador",
                lat = 13.3444,
                lng = -88.4397,
                keywords = listOf("usulutan", "usulutan este")
            ),
            LocalPlaceEntry(
                title = "Sensuntepeque",
                subtitle = "Cabañas, El Salvador",
                lat = 13.8757,
                lng = -88.6280,
                keywords = listOf("sensuntepeque", "cabanas este")
            ),
            LocalPlaceEntry(
                title = "Chalatenango",
                subtitle = "Chalatenango, El Salvador",
                lat = 14.0413,
                lng = -88.9410,
                keywords = listOf("chalatenango", "chalatenango sur")
            ),
            LocalPlaceEntry(
                title = "San Francisco Gotera",
                subtitle = "Morazán, El Salvador",
                lat = 13.6953,
                lng = -88.1086,
                keywords = listOf("san francisco gotera", "morazan sur")
            ),
            LocalPlaceEntry(
                title = "La Unión",
                subtitle = "La Unión, El Salvador",
                lat = 13.3378,
                lng = -87.8423,
                keywords = listOf("la union", "la union sur")
            )
        )
    }
}
