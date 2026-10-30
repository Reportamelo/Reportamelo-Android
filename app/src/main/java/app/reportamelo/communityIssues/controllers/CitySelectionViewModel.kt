package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.*
import app.reportamelo.commons.utils.*
import app.reportamelo.communityIssues.datamodel.*
import app.reportamelo.models.*
import app.reportamelo.network.*
import app.reportamelo.network.services.*
import app.reportamelo.repository.*
import java.text.Normalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CitySelectionViewModel(
    private val countryCode: CountryCode = CountryCode.SV,
    private val userRepository: UserRepository = UserRepository.shared
) : ViewModel() {

    private val _state = MutableStateFlow(CitySelectionState())
    val state: StateFlow<CitySelectionState> = _state.asStateFlow()

    init {
        loadLocalCities()
    }

    val cities: List<FriendlyCityDistribution>
        get() = _state.value.friendlyCities.documents?.sortedByDescending { it.isCapitalCity ?: 0 } ?: emptyList()

    fun loadLocalCities() {
        val localCities = CitiesRepository.loadLocalCities(countryCode)
        _state.update { it.copy(friendlyCities = localCities) }
    }

    fun fetchCitiesByGroupingName() {
        val query = _state.value.searchText.trim()
        if (query.isEmpty()) {
            loadLocalCities()
            return
        }
        val filtered = CitiesRepository.filterLocal(
            countryCode = countryCode.name,
            groupingName = query
        )
        _state.update { it.copy(friendlyCities = filtered) }
    }

    fun fetchCitiesByStateName() {
        val query = _state.value.searchText.trim()
        if (query.isEmpty()) {
            loadLocalCities()
            return
        }
        val filtered = CitiesRepository.filterLocal(
            countryCode = countryCode.name,
            stateName = query
        )
        _state.update { it.copy(friendlyCities = filtered) }
    }

    fun fetchByCityName() {
        val query = _state.value.searchText.trim()
        if (query.isEmpty()) {
            loadLocalCities()
            return
        }
        val filtered = CitiesRepository.filterLocal(
            countryCode = countryCode.name,
            cityName = query
        )
        _state.update { it.copy(friendlyCities = filtered) }
    }

    fun performSearch() {
        when (_state.value.searchOptionsSelection) {
            CityFilter.LEGAL -> fetchCitiesByGroupingName()
            CityFilter.CITY -> fetchByCityName()
            CityFilter.STATE -> fetchCitiesByStateName()
        }
    }

    fun handleSearchOptionsSelectionChange(oldValue: CityFilter, newValue: CityFilter) {
        if (oldValue != newValue) {
            val newSearchText = if (newValue == CityFilter.LEGAL) "Municipio de " else ""
            _state.update {
                it.copy(
                    searchText = newSearchText,
                    isSearchActive = true,
                    searchOptionsSelection = newValue
                )
            }
            if (newSearchText.isNotBlank()) {
                performSearch()
            } else {
                loadLocalCities()
            }
        }
    }

    fun updateSearchText(text: String) {
        _state.update { it.copy(searchText = text) }
        if (text.isBlank()) {
            loadLocalCities()
        } else {
            performSearch()
        }
    }

    fun updateCity(city: FriendlyCityDistribution, onUpdated: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                CitiesRepository.saveSelectedCity(city)
                if (userRepository.isGuestUser() || LandingViewModel.shared.isGuest.value) {
                    onUpdated()
                } else {
                    val result = userRepository.updateDefaultReporting(city.cityId)
                    if (result == SuccessfulResult.UPDATED) {
                        onUpdated()
                    }
                }
            } catch (e: CommonIntercommunicationErrors.UnProcessable) {
                showError("Your petition cannot be processed, please try again.")
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                showError("It looks like that your network is experiencing some delays, please try again.")
            } catch (e: CommonIntercommunicationErrors.ServerError) {
                showError("Server error, please try again.")
            } catch (e: Exception) {
                showError("An unexpected error has occurred, please try again.")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(showErrorAlert = false) }
    }

    private fun showError(error: String) {
        _state.update {
            it.copy(
                messageError = error,
                showErrorAlert = true
            )
        }
    }
}

data class CitySelectionState(
    val page: Int = 1,
    val friendlyCities: FriendlyCities = FriendlyCities(
        documents = emptyList(),
        total = 0,
        page = 0,
        documentsPerPage = 0,
        totalPages = 0,
        hasNext = false,
        hasPrev = false
    ),
    val searchText: String = "",
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchOptionsSelection: CityFilter = CityFilter.CITY,
    val triggerFeedBack: Boolean = false,
    val showErrorAlert: Boolean = false,
    val messageError: String = ""
)

enum class CityFilter(val rawValue: String) {
    CITY("city"), LEGAL("legal"), STATE("state")
}
