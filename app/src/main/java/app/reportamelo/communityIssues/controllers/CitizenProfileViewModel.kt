package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.utils.*
import app.reportamelo.communityIssues.datamodel.*

import app.reportamelo.commons.*
import app.reportamelo.models.*
import app.reportamelo.repository.*
import app.reportamelo.network.services.*
import app.reportamelo.network.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class CitizenProfileViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(CitizenProfileState())
    val state: StateFlow<CitizenProfileState> = _state.asStateFlow()

    fun fetchCitizenPublicProfile(profileId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val citizen = UserRepository.shared.citizenProfile(id = profileId)
                _state.update { it.copy(citizen = citizen) }
            } catch (e: CommonIntercommunicationErrors.NotFound) {
                _state.update {
                    it.copy(
                        citizen = User(
                            names = "Deleted Citizen",
                            userName = "deleted",
                            profilePicture = "/avatars/019f4f22-1464-7336-8406-853b453b026d.png",
                            profileId = "deleted",
                            hideProfile = false
                        )
                    )
                }
            } catch (e: Exception) {
                println("Error fetching citizen public profile: ${e.message}")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchReports(profileId: String) {
        viewModelScope.launch {
            _state.update { it.copy(fetchingReports = true) }
            try {
                val documents = ReportRepository.listByProfile(profileId)
                val mappedReports = documents.map { it.toModel() }
                _state.update {
                    it.copy(
                        reports = mappedReports,
                        reportsSubmitted = mappedReports.size
                    )
                }
            } catch (e: Exception) {
                println("Error fetching reports by profile: ${e.message}")
            } finally {
                _state.update { it.copy(fetchingReports = false) }
            }
        }
    }
}

data class CitizenProfileState(
    val citizen: User = User(
        names = "Guest Citizen",
        userName = "guest",
        profilePicture = "/avatars/019f4f22-1464-7336-8406-853b453b026d.png",
        profileId = "guest",
        hideProfile = false
    ),
    val containerSize: Any? = null,
    val reportsSubmitted: Int = 0,
    val showBlockUserSheet: Boolean = false,
    val petitionsPublished: Int = 0,
    val petitions: List<PetitionPost> = emptyList(),
    val scrollProgress: Float = 0f,
    val scrollPosition: Any? = null,
    val isLoading: Boolean = false,
    val reports: List<app.reportamelo.communityIssues.datamodel.Report> = emptyList(),
    val fetchingReports: Boolean = false
)


