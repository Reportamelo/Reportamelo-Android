package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.KeychainToken
import app.reportamelo.commons.LegacyKeychain
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.communityIssues.ui.components.ToastType
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.ModeratedContent
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.models.ReportViolation
import app.reportamelo.network.KeychainService
import app.reportamelo.repository.ModerationRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ModerationRole(val description: String) {
    MODERATOR("Moderator"),
    USER("User"),
    REPORTER("Reporter")
}

enum class ViolationContentType(val description: String) {
    ATTACHMENT("Attachments"),
    COMMENT("Comments")
}

data class ModerationToastMessage(
    val message: String,
    val type: ToastType
)

class ModerationViewModel @JvmOverloads constructor(
    private val myComplaintsProvider: suspend () -> PaginatedResponse<ReportViolation<ModeratedContent>> = {
        ModerationRepository.myComplaints()
    },
    private val indictedModeratedContentProvider: suspend () -> PaginatedResponse<ReportViolation<ModeratedContent>> = {
        ModerationRepository.indictedModeratedContent()
    },
    private val appealProvider: suspend (String, TypeOfContentToReport) -> GenericResponse = { id, type ->
        ModerationRepository.appeal(id, type)
    },
    private val removeProvider: suspend (String, TypeOfContentToReport) -> GenericResponse = { id, type ->
        ModerationRepository.remove(id, type)
    },
    private val unlockProvider: suspend (String) -> Unit = { profileId ->
        UserRepository.shared.unlock(profileId)
    },
    private val removeBlockedUserFromKeychainProvider: (String) -> Unit = { profileId ->
        KeychainService.removeFromArray("blockedUsers", profileId)
    },
    private val profileIdProvider: () -> String = {
        LegacyKeychain.getToken(KeychainToken.PROFILE_ID) ?: ""
    }
) : ViewModel() {

    private var activeLoadCount = 0

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showAlert = MutableStateFlow(false)
    val showAlert: StateFlow<Boolean> = _showAlert.asStateFlow()

    private val _showReportRemovalAlert = MutableStateFlow(false)
    val showReportRemovalAlert: StateFlow<Boolean> = _showReportRemovalAlert.asStateFlow()

    private val _showAlertError = MutableStateFlow(false)
    val showAlertError: StateFlow<Boolean> = _showAlertError.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedType = MutableStateFlow(ViolationContentType.ATTACHMENT)
    val selectedType: StateFlow<ViolationContentType> = _selectedType.asStateFlow()

    private val _showConfirmationOfRemoval = MutableStateFlow(false)
    val showConfirmationOfRemoval: StateFlow<Boolean> = _showConfirmationOfRemoval.asStateFlow()

    private val _indictedModeratedContent = MutableStateFlow<List<ReportViolation<ModeratedContent>>>(emptyList())
    val indictedModeratedContent: StateFlow<List<ReportViolation<ModeratedContent>>> = _indictedModeratedContent.asStateFlow()

    private val _myComplaints = MutableStateFlow<List<ReportViolation<ModeratedContent>>>(emptyList())
    val myComplaints: StateFlow<List<ReportViolation<ModeratedContent>>> = _myComplaints.asStateFlow()

    private val _selectedReportId = MutableStateFlow("")
    val selectedReportId: StateFlow<String> = _selectedReportId.asStateFlow()

    private val _selectedReportType = MutableStateFlow<TypeOfContentToReport?>(null)
    val selectedReportType: StateFlow<TypeOfContentToReport?> = _selectedReportType.asStateFlow()

    private val _selectedProfileId = MutableStateFlow("")
    val selectedProfileId: StateFlow<String> = _selectedProfileId.asStateFlow()

    private val _toastMessage = MutableStateFlow<ModerationToastMessage?>(null)
    val toastMessage: StateFlow<ModerationToastMessage?> = _toastMessage.asStateFlow()

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun prepareForAppeal(id: String, type: TypeOfContentToReport, profileId: String) {
        _selectedReportId.value = id
        _selectedReportType.value = type
        _selectedProfileId.value = profileId
        _showAlert.value = true
    }

    fun prepareForReportRemoval(id: String, type: TypeOfContentToReport, profileId: String) {
        _selectedReportId.value = id
        _selectedReportType.value = type
        _selectedProfileId.value = profileId
        _showReportRemovalAlert.value = true
    }

    fun dismissAlert() {
        _showAlert.value = false
    }

    fun dismissReportRemovalAlert() {
        _showReportRemovalAlert.value = false
    }

    fun dismissConfirmationOfRemoval() {
        _showConfirmationOfRemoval.value = false
    }

    fun dismissAlertError() {
        _showAlertError.value = false
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun unlock(profileId: String, reportId: String) {
        viewModelScope.launch {
            try {
                unlockProvider(profileId)
                removeBlockedUserFromKeychainProvider(profileId)
                removeProvider(reportId, TypeOfContentToReport.ACCOUNT)
                _showConfirmationOfRemoval.value = true
            } catch (_: Exception) {
                _showAlertError.value = true
            }
        }
    }

    fun remove(reportId: String, type: TypeOfContentToReport) {
        viewModelScope.launch {
            try {
                removeProvider(reportId, type)
                _showConfirmationOfRemoval.value = true
            } catch (_: Exception) {
                _showAlertError.value = true
            }
        }
    }

    fun appealForReportRemoval(id: String, type: TypeOfContentToReport) {
        viewModelScope.launch {
            try {
                appealProvider(id, type)
                _toastMessage.value = ModerationToastMessage(
                    message = "Your appeal has been sent",
                    type = ToastType.Success
                )
            } catch (e: Exception) {
                _toastMessage.value = ModerationToastMessage(
                    message = e.localizedMessage ?: e.message ?: "Error",
                    type = ToastType.Error
                )
            }
        }
    }

    fun loadComplaints() {
        viewModelScope.launch {
            beginLoading()
            try {
                val response = myComplaintsProvider()
                response.documents?.let { docs ->
                    _myComplaints.value = docs
                }
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                _toastMessage.value = ModerationToastMessage(
                    message = e.message,
                    type = ToastType.Error
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: e.message
            } finally {
                endLoading()
            }
        }
    }

    fun loadIndictedData() {
        val profileId = profileIdProvider()
        if (profileId.isEmpty()) return

        viewModelScope.launch {
            beginLoading()
            try {
                val response = indictedModeratedContentProvider()
                response.documents?.let { documents ->
                    _indictedModeratedContent.value = documents
                }
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                _toastMessage.value = ModerationToastMessage(
                    message = e.message,
                    type = ToastType.Error
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: e.message
            } finally {
                endLoading()
            }
        }
    }

    private fun beginLoading() {
        activeLoadCount += 1
        _isLoading.value = true
    }

    private fun endLoading() {
        activeLoadCount = (activeLoadCount - 1).coerceAtLeast(0)
        _isLoading.value = activeLoadCount > 0
    }
}
