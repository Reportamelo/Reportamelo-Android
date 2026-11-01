package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.ReportViolationStatus
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.commons.VotingType
import app.reportamelo.models.Comment
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.models.ReportViolation
import app.reportamelo.models.Voting
import app.reportamelo.models.VotingResolution
import app.reportamelo.repository.CommentsRepository
import app.reportamelo.repository.ModerationRepository
import app.reportamelo.repository.UserRepository
import app.reportamelo.repository.VotingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailReportOptions(val id: String, val description: String) {
    WRONG_INFORMATION("wrongInformation", "Wrong Information"),
    EXPLICIT_CONTENT("explicitContent", "Explicit Content"),
    SPAM("spam", "Spam"),
    VIOLENCE("violence", "Violence"),
    PRIVACY_ISSUES("privacyIssues", "Privacy issue"),
    OTHER("other", "Other")
}

enum class DetailSubScreen {
    NONE,
    FOLLOW_UP,
    CITIZEN_PROFILE,
    COMMENTS,
    EVIDENCES
}

data class DetailViewState(
    val isGuest: Boolean = false,
    val isOwnProfile: Boolean = false,
    val voteCount: Int = 0,
    val disableBoostButton: Boolean = false,
    val isBoostLoading: Boolean = false,
    val latestComments: List<Comment> = emptyList(),
    val activeSubScreen: DetailSubScreen = DetailSubScreen.NONE,
    val selectedProfileId: String = "",
    val selectedReportOption: String = DetailReportOptions.OTHER.description,
    val customReason: String = "",
    val showConfirmReportAlert: Boolean = false,
    val showBoostAlert: Boolean = false,
    val message: String = "",
    val showErrorAlert: Boolean = false,
    val errorMessage: String = "",
    val showSuccessfulAlert: Boolean = false
)

class DetailViewModel(
    private val isGuestUserProvider: () -> Boolean = { UserRepository.shared.isGuestUser() },
    private val isOwnProfileProvider: (String) -> Boolean = { UserRepository.shared.isOwnProfile(it) },
    private val haveIVotedProvider: suspend (VotingType, String) -> VotingResolution = { type, id ->
        VotingRepository.havIVoted(type, id)
    },
    private val voteProvider: suspend (VotingType, Voting) -> SuccessfulResult = { type, payload ->
        VotingRepository.vote(type, payload)
    },
    private val listCommentsProvider: suspend (String, Int, Int) -> PaginatedResponse<Comment> = { reportId, page, limit ->
        CommentsRepository.list(reportId, page, limit)
    },
    private val moderateReportProvider: suspend (ReportViolation<MapExplorerReport>, TypeOfContentToReport) -> GenericResponse = { violation, type ->
        ModerationRepository.moderateReport(violation, type)
    }
) : ViewModel() {

    private val _state = MutableStateFlow(DetailViewState())
    val state: StateFlow<DetailViewState> = _state.asStateFlow()

    fun initialize(report: MapExplorerReport) {
        val isGuest = try {
            isGuestUserProvider()
        } catch (_: Exception) {
            false
        }
        val isOwn = try {
            isOwnProfileProvider(report.profileId)
        } catch (_: Exception) {
            false
        }

        _state.update {
            it.copy(
                isGuest = isGuest,
                isOwnProfile = isOwn
            )
        }

        if (!isGuest) {
            checkHaveIVoted(report.id)
            loadLatestComments(report.id)
        }
    }

    fun checkHaveIVoted(reportId: String) {
        viewModelScope.launch {
            try {
                val resolution = haveIVotedProvider(VotingType.REPORT, reportId)
                _state.update {
                    it.copy(
                        disableBoostButton = resolution.hasVoted,
                        voteCount = resolution.voteCount
                    )
                }
            } catch (_: Exception) {
                // Keep default vote state
            }
        }
    }

    fun loadLatestComments(reportId: String) {
        if (_state.value.isGuest) return
        viewModelScope.launch {
            try {
                val response = listCommentsProvider(reportId, 1, 3)
                _state.update {
                    it.copy(latestComments = response.documents.orEmpty())
                }
            } catch (_: Exception) {
                // Keep existing comments
            }
        }
    }

    fun openSubScreen(subScreen: DetailSubScreen) {
        if (subScreen == DetailSubScreen.COMMENTS && _state.value.isGuest) return
        _state.update { it.copy(activeSubScreen = subScreen) }
    }

    fun openCitizenProfile(profileId: String) {
        _state.update {
            it.copy(
                selectedProfileId = profileId,
                activeSubScreen = DetailSubScreen.CITIZEN_PROFILE
            )
        }
    }

    fun closeSubScreen(reportId: String? = null) {
        val previous = _state.value.activeSubScreen
        _state.update { it.copy(activeSubScreen = DetailSubScreen.NONE) }
        if (previous == DetailSubScreen.COMMENTS && reportId != null && !_state.value.isGuest) {
            loadLatestComments(reportId)
        }
    }

    fun selectReportOption(option: String) {
        _state.update {
            it.copy(
                selectedReportOption = option,
                customReason = "",
                showConfirmReportAlert = true
            )
        }
    }

    fun updateCustomReason(reason: String) {
        _state.update { it.copy(customReason = reason) }
    }

    fun dismissConfirmReportAlert() {
        _state.update { it.copy(showConfirmReportAlert = false) }
    }

    fun reportContentViolation(report: MapExplorerReport) {
        val current = _state.value
        val blockedReasonId = DetailReportOptions.entries
            .firstOrNull { it.description == current.selectedReportOption }?.id
            ?: DetailReportOptions.OTHER.id
        val finalReason = current.customReason.ifBlank { current.selectedReportOption }

        _state.update { it.copy(showConfirmReportAlert = false) }

        viewModelScope.launch {
            val violation = ReportViolation(
                type = TypeOfContentToReport.REPORT,
                content = report,
                contentAuthorProfileId = report.profileId,
                reason = finalReason,
                blockedReasonId = blockedReasonId,
                status = ReportViolationStatus.SENT_TO_MODERATION
            )
            try {
                moderateReportProvider(violation, TypeOfContentToReport.REPORT)
                _state.update {
                    it.copy(
                        message = "Your moderation petition was sent",
                        showSuccessfulAlert = true
                    )
                }
            } catch (_: CommonIntercommunicationErrors.ServerError) {
                _state.update {
                    it.copy(
                        errorMessage = "Server Error",
                        showErrorAlert = true
                    )
                }
            } catch (_: CommonIntercommunicationErrors.NetworkError) {
                _state.update {
                    it.copy(
                        errorMessage = "It looks like that your network is experiencing some delays, please try again.",
                        showErrorAlert = true
                    )
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = "Error",
                        showErrorAlert = true
                    )
                }
            }
        }
    }

    fun setBoostLoading(loading: Boolean) {
        _state.update { it.copy(isBoostLoading = loading) }
    }

    fun submitBoostVote(reportId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isBoostLoading = true) }
            try {
                val wasValidated = voteProvider(
                    VotingType.REPORT,
                    Voting(type = VotingType.REPORT, resourceId = reportId)
                )
                _state.update { current ->
                    val newVoteCount = if (wasValidated == SuccessfulResult.DONE) {
                        current.voteCount + 1
                    } else {
                        current.voteCount
                    }
                    current.copy(
                        isBoostLoading = false,
                        voteCount = newVoteCount,
                        disableBoostButton = true,
                        message = "Boost applied!",
                        showBoostAlert = true
                    )
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isBoostLoading = false,
                        message = "Failed to boost report",
                        showBoostAlert = true
                    )
                }
            }
        }
    }

    fun showBoostMessage(message: String) {
        _state.update {
            it.copy(
                isBoostLoading = false,
                message = message,
                showBoostAlert = true
            )
        }
    }

    fun dismissBoostAlert() {
        _state.update { it.copy(showBoostAlert = false) }
    }

    fun dismissErrorAlert() {
        _state.update { it.copy(showErrorAlert = false) }
    }

    fun dismissSuccessfulAlert() {
        _state.update { it.copy(showSuccessfulAlert = false) }
    }
}
