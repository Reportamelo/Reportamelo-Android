package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.BlockReasons
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.LegacyKeychain
import app.reportamelo.commons.ReportViolationStatus
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.communityIssues.datamodel.Report
import app.reportamelo.communityIssues.datamodel.toModel
import app.reportamelo.models.BlockUserReason
import app.reportamelo.models.PetitionPost
import app.reportamelo.models.ReportDAO
import app.reportamelo.models.ReportViolation
import app.reportamelo.models.User
import app.reportamelo.repository.ModerationRepository
import app.reportamelo.repository.ReportRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CitizenProfileViewModel(
    private val citizenProfileProvider: suspend (String) -> User = {
        UserRepository.shared.citizenProfile(id = it)
    },
    private val blockUserProvider: suspend (BlockUserReason) -> Unit = {
        UserRepository.shared.block(it)
    },
    private val persistBlockedUserProvider: (String) -> Unit = { profileId ->
        val blocked = LegacyKeychain.loadBlockedUsers().toMutableSet()
        blocked.add(profileId)
        LegacyKeychain.saveBlockedUsers(blocked.toList())
    },
    private val listReportsByProfileProvider: suspend (String) -> List<ReportDAO> = {
        ReportRepository.listByProfile(it)
    },
    private val moderateAccountProvider: suspend (ReportViolation<User>, TypeOfContentToReport) -> Any = { violation, type ->
        ModerationRepository.moderateAccount(reason = violation, type = type)
    }
) : ViewModel() {

    private val _state = MutableStateFlow(CitizenProfileState())
    val state: StateFlow<CitizenProfileState> = _state.asStateFlow()

    fun fetchCitizenPublicProfile(profileId: String) {
        if (profileId.isBlank() || profileId.equals("guest", ignoreCase = true)) {
            _state.update {
                it.copy(
                    citizen = guestCitizenFallback(profileId),
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val citizen = citizenProfileProvider(profileId)
                val resolvedCitizen = if (citizen.hideProfile || citizen.names.isBlank() || citizen.userName.isBlank()) {
                    guestCitizenFallback(profileId)
                } else {
                    citizen
                }
                _state.update { it.copy(citizen = resolvedCitizen) }
            } catch (_: CommonIntercommunicationErrors.NotFound) {
                _state.update { it.copy(citizen = guestCitizenFallback(profileId)) }
            } catch (e: Exception) {
                println("Error fetching citizen public profile: ${e.message}")
                _state.update { it.copy(citizen = guestCitizenFallback(profileId)) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchReports(profileId: String) {
        if (profileId.isBlank() || profileId.equals("guest", ignoreCase = true)) {
            _state.update { it.copy(reports = emptyList(), reportsSubmitted = 0, fetchingReports = false) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(fetchingReports = true) }
            try {
                val documents = listReportsByProfileProvider(profileId)
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

    fun setShowBlockUserSheet(show: Boolean) {
        _state.update { it.copy(showBlockUserSheet = show) }
    }

    fun dismissBlockResultAlert() {
        _state.update { it.copy(showBlockResultAlert = false) }
    }

    fun isBlockReasonValid(reason: String): Boolean {
        val trimmed = reason.trim()
        return trimmed.length in 12..150
    }

    fun blockUser(
        profileId: String,
        reasonText: String,
        selectedReason: BlockReasons
    ) {
        val trimmedReason = reasonText.trim()
        if (!isBlockReasonValid(trimmedReason)) return

        viewModelScope.launch {
            _state.update { it.copy(isBlocking = true) }
            try {
                val violation = ReportViolation(
                    type = TypeOfContentToReport.ACCOUNT,
                    content = _state.value.citizen,
                    contentAuthorProfileId = profileId,
                    reason = trimmedReason,
                    blockedReasonId = selectedReason.id,
                    status = ReportViolationStatus.SENT_TO_MODERATION
                )
                runCatching {
                    moderateAccountProvider(violation, TypeOfContentToReport.ACCOUNT)
                }
                blockUserProvider(
                    BlockUserReason(
                        profileId = profileId,
                        reason = trimmedReason,
                        blockedReasonId = selectedReason.id
                    )
                )
                runCatching {
                    persistBlockedUserProvider(profileId)
                }

                _state.update {
                    it.copy(
                        isBlocking = false,
                        showBlockUserSheet = false,
                        showBlockResultAlert = true,
                        blockSucceeded = true
                    )
                }
            } catch (e: Exception) {
                println("Error blocking user: ${e.message}")
                _state.update {
                    it.copy(
                        isBlocking = false,
                        showBlockUserSheet = false,
                        showBlockResultAlert = true,
                        blockSucceeded = false
                    )
                }
            }
        }
    }

    companion object {
        fun guestCitizenFallback(profileId: String = "guest"): User = User(
            names = "Guest citizen",
            userName = "guest",
            profilePicture = "/avatars/019f4f22-1464-7336-8406-853b453b026d.png",
            profileId = profileId.ifEmpty { "guest" },
            hideProfile = true
        )
    }
}

data class CitizenProfileState(
    val citizen: User = CitizenProfileViewModel.guestCitizenFallback(),
    val containerSize: Any? = null,
    val reportsSubmitted: Int = 0,
    val showBlockUserSheet: Boolean = false,
    val petitionsPublished: Int = 0,
    val petitions: List<PetitionPost> = emptyList(),
    val scrollProgress: Float = 0f,
    val scrollPosition: Any? = null,
    val isLoading: Boolean = false,
    val reports: List<Report> = emptyList(),
    val fetchingReports: Boolean = false,
    val isBlocking: Boolean = false,
    val showBlockResultAlert: Boolean = false,
    val blockSucceeded: Boolean = false
)
