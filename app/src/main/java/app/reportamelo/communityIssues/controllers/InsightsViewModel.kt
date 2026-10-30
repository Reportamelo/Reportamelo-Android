package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.communityIssues.datamodel.Report
import app.reportamelo.models.MonthlyInsightsResponse
import app.reportamelo.models.PetitionPost
import app.reportamelo.repository.InsightsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InsightsNavigation {
    data object MyReports : InsightsNavigation()
    data object MyPetitions : InsightsNavigation()
    data class Activity(val date: String) : InsightsNavigation()
    data object NoActivity : InsightsNavigation()
    data class ReportNav(val report: Report) : InsightsNavigation()
    data class PetitionNav(val petition: PetitionPost) : InsightsNavigation()
}

class InsightsViewModel : ViewModel() {

    private val _navigationPath = MutableStateFlow<List<InsightsNavigation>>(emptyList())
    val navigationPath: StateFlow<List<InsightsNavigation>> = _navigationPath.asStateFlow()

    private val _insights = MutableStateFlow(MonthlyInsightsResponse(0, 0, 0, 0, emptyMap()))
    val insights: StateFlow<MonthlyInsightsResponse> = _insights.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _showAlert = MutableStateFlow(false)
    val showAlert: StateFlow<Boolean> = _showAlert.asStateFlow()

    fun getInsights() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = InsightsRepository.insightsForThisMonth()
                _insights.value = response
            } catch (e: CommonIntercommunicationErrors.ServerError) {
                showError("Server Error")
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateTo(destination: InsightsNavigation) {
        _navigationPath.value = _navigationPath.value + destination
    }

    fun navigateBack() {
        if (_navigationPath.value.isNotEmpty()) {
            _navigationPath.value = _navigationPath.value.dropLast(1)
        }
    }

    fun setNavigationPath(path: List<InsightsNavigation>) {
        _navigationPath.value = path
    }

    fun onDateSelected(dateFormatted: String) {
        if (_insights.value.recentActivity.containsKey(dateFormatted)) {
            navigateTo(InsightsNavigation.Activity(date = dateFormatted))
        } else {
            navigateTo(InsightsNavigation.NoActivity)
        }
    }

    fun dismissAlert() {
        _showAlert.value = false
    }

    private fun showError(error: String) {
        _message.value = error
        _showAlert.value = true
    }
}
