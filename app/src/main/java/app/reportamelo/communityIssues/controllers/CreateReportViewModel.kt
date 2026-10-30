package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.IssueTypes
import app.reportamelo.commons.Severity
import app.reportamelo.commons.utils.KeychainService
import app.reportamelo.commons.utils.KeychainToken
import app.reportamelo.commons.utils.SubscriptionManager
import app.reportamelo.communityIssues.datamodel.MatterToSolve
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.datamodel.mattersToResolve
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class CreateReportViewModel(
    private val settings: SettingsStore = SettingsStore.shared,
    private val userRepository: UserRepository = UserRepository.shared,
    private val counterProvider: suspend () -> Int? = { null },
    private val entitlementChecker: () -> Unit = { SubscriptionManager.checkEntitlement() }
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReportState())
    val state: StateFlow<CreateReportState> = _state.asStateFlow()

    // Equivalent of filteredMatters computed property
    fun getFilteredMatters(matters: List<MatterToSolve> = mattersToResolve): List<MatterToSolve> {
        val currentState = _state.value
        return computeFilteredMatters(
            matters = matters,
            searchText = currentState.searchText,
            severity = currentState.severity,
            issueType = currentState.issueType
        )
    }

    private fun computeFilteredMatters(
        matters: List<MatterToSolve>,
        searchText: String,
        severity: Severity,
        issueType: IssueTypes
    ): List<MatterToSolve> {
        val trimmedQuery = searchText.trim()

        if (trimmedQuery.isEmpty()) {
            return filterByIssueType(filterByStatus(matters, severity), issueType)
        }

        return matters.filter { matter ->
            matter.title.contains(trimmedQuery, ignoreCase = true) ||
                matter.description.contains(trimmedQuery, ignoreCase = true)
        }
    }

    fun checkState() {
        entitlementChecker()

        val planType = if (SubscriptionManager.mockIsPro) {
            "pro"
        } else {
            KeychainService.getToken(KeychainToken.PLAN_TYPE)
        }
        val reportsCount = _state.value.reportsCount

        val showReportsLimitSheet = when {
            planType.isEmpty() || planType == "freemium" -> {
                reportsCount >= MAX_REPORTS_PER_REGISTERED_USERS
            }
            planType == "freemium_for_guests" -> {
                reportsCount >= MAX_REPORTS_PER_UNREGISTERED_USERS
            }
            else -> {
                reportsCount > MAX_REPORTS_PER_PAID_USERS
            }
        }

        _state.update { it.copy(showReportsLimitSheet = showReportsLimitSheet) }
    }

    fun getReportsCount() {
        viewModelScope.launch {
            if (userRepository.isGuestUser()) {
                val currentDate = Date()
                val calendar = Calendar.getInstance()

                settings.lastReportDate?.let { lastDate ->
                    calendar.time = currentDate
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)

                    calendar.time = lastDate
                    val lastMonth = calendar.get(Calendar.MONTH)
                    val lastYear = calendar.get(Calendar.YEAR)

                    if (currentMonth != lastMonth || currentYear != lastYear) {
                        settings.reportsCount = 0
                    }
                }

                _state.update { it.copy(reportsCount = settings.reportsCount) }
            } else {
                val reportCount = counterProvider()

                if (reportCount != null) {
                    _state.update { it.copy(reportsCount = reportCount) }
                } else {
                    _state.update { it.copy(reportsCount = settings.reportsCount) }
                }
            }

            checkState()
        }
    }

    private fun filterByStatus(matters: List<MatterToSolve>, severity: Severity): List<MatterToSolve> {
        return matters.filter { it.severity.identifier == severity.identifier || severity == Severity.ALL }
    }

    private fun filterByIssueType(matters: List<MatterToSolve>, issueType: IssueTypes): List<MatterToSolve> {
        return matters.filter { it.issueType.identifier == issueType.identifier || issueType == IssueTypes.ALL }
    }

    fun handleSheetDismissal() {
        if (app.reportamelo.communityIssues.helpers.DeepLinkRouter.shared.activeTab.value != 1) {
            checkState()
        }
    }

    fun handleUserAction(interaction: InteractionType) {
        when (interaction) {
            InteractionType.PAID_A_SUBSCRIPTION -> {
                _state.update { it.copy(showReportsLimitSheet = false) }
            }
            InteractionType.VIEW_AD -> {
                _state.update {
                    it.copy(
                        reportsCount = it.reportsCount - 1,
                        showReportsLimitSheet = false
                    )
                }
            }
            InteractionType.NO_THANKS -> {
                noThanks()
            }
        }
    }

    private fun noThanks() {
        viewModelScope.launch {
            _state.update { it.copy(showReportsLimitSheet = false) }
            delay(64)
            app.reportamelo.communityIssues.helpers.DeepLinkRouter.shared.activeTab.value = 1
        }
    }

    fun toggleFeedbackTrigger() {
        _state.update { it.copy(feedbackTrigger = !it.feedbackTrigger) }
    }

    fun dismissReportsLimitSheet() {
        _state.update { it.copy(showReportsLimitSheet = false) }
    }

    fun updateSearchText(text: String) {
        _state.update { current ->
            current.copy(
                searchText = text,
                filteredMatters = computeFilteredMatters(
                    matters = mattersToResolve,
                    searchText = text,
                    severity = current.severity,
                    issueType = current.issueType
                )
            )
        }
    }

    fun updateIssueType(issueType: IssueTypes) {
        _state.update { current ->
            current.copy(
                issueType = issueType,
                filteredMatters = computeFilteredMatters(
                    matters = mattersToResolve,
                    searchText = current.searchText,
                    severity = current.severity,
                    issueType = issueType
                )
            )
        }
    }

    fun updateSeverity(severity: Severity) {
        _state.update { current ->
            current.copy(
                severity = severity,
                filteredMatters = computeFilteredMatters(
                    matters = mattersToResolve,
                    searchText = current.searchText,
                    severity = severity,
                    issueType = current.issueType
                )
            )
        }
    }

    companion object {
        const val MAX_REPORTS_PER_REGISTERED_USERS = 5
        const val MAX_REPORTS_PER_UNREGISTERED_USERS = 2
        const val MAX_REPORTS_PER_PAID_USERS = 100
    }
}

data class CreateReportState(
    val searchText: String = "",
    val issueType: IssueTypes = IssueTypes.ALL,
    val severity: Severity = Severity.ALL,
    val feedbackTrigger: Boolean = false,
    val showReportsLimitSheet: Boolean = false,
    val reportsCount: Int = 0,
    val filteredMatters: List<MatterToSolve> = mattersToResolve
)

enum class InteractionType { PAID_A_SUBSCRIPTION, VIEW_AD, NO_THANKS }
