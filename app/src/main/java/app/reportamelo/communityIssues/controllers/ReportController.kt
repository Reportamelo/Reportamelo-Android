package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.datamodel.ReportStates
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.datamodel.issueType
import app.reportamelo.communityIssues.datamodel.severity
import app.reportamelo.communityIssues.datamodel.status
import app.reportamelo.communityIssues.datamodel.toDao
import app.reportamelo.communityIssues.ui.components.wizard.ReportStep
import app.reportamelo.models.CustomizedResponse
import app.reportamelo.models.GroupedAttachmentPayload
import app.reportamelo.models.ReportAttachmentGrouping
import app.reportamelo.models.ReportSessionResponse
import app.reportamelo.models.ReportToShare
import app.reportamelo.repository.CounterRepository
import app.reportamelo.repository.ReportRepository
import app.reportamelo.repository.ShareRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReportError(message: String) : Exception(message)

class ReportController : ViewModel() {

    private val _reportId = MutableStateFlow("")
    val reportId: StateFlow<String> = _reportId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _presentAlert = MutableStateFlow(false)
    val presentAlert: StateFlow<Boolean> = _presentAlert.asStateFlow()

    private val _alertMessage = MutableStateFlow("")
    val alertMessage: StateFlow<String> = _alertMessage.asStateFlow()

    private val _shareableLink = MutableStateFlow("")
    val shareableLink: StateFlow<String> = _shareableLink.asStateFlow()

    private val _doneTrigger = MutableStateFlow(false)
    val doneTrigger: StateFlow<Boolean> = _doneTrigger.asStateFlow()

    private val _currentStep = MutableStateFlow(ReportStep.LOCATION)
    val currentStep: StateFlow<ReportStep> = _currentStep.asStateFlow()

    private val _groupingResult = MutableStateFlow<CustomizedResponse<List<ReportAttachmentGrouping>>?>(null)
    val groupingResult: StateFlow<CustomizedResponse<List<ReportAttachmentGrouping>>?> = _groupingResult.asStateFlow()

    fun startReporting(model: ReportDataModel) {
        _currentStep.value = ReportStep.LOCATION
        viewModelScope.launch {
            try {
                if (model.report.value.reportState == ReportStates.MODIFYING) {
                    val reportId = model.report.value.id ?: throw ReportError("noIdentifier")
                    val cityId = model.report.value.cityId ?: throw ReportError("noIdentifier")
                    val reportContainer = model.report.value.reportContainer ?: ""

                    val (countryCode, reportSession) = model.getMetadataFromReportId(reportId, reportContainer)

                    model.updateReportSession(reportSession)
                    model.setLocator(countryCode, cityId)
                } else {
                    val result = ReportRepository.start()
                    model.updateReportSession(result.data)
                }
            } catch (e: Exception) {
                val datePart = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
                val fallbackSession = ReportSessionResponse(
                    reportContainer = UUID.randomUUID().toString(),
                    shareIndexHash = UUID.randomUUID().toString().replace("-", "").take(8),
                    reportCreationOn = datePart
                )
                model.updateReportSession(fallbackSession)
            }
        }
    }

    suspend fun createShareableLink(model: ReportDataModel) {
        val currentReport = model.report.value
        val currentLocator = model.locator.value
        val resolvedReportId = currentReport.id?.takeIf { it.isNotEmpty() } ?: model.buildReportId()

        val url = runCatching {
            ShareRepository.createShareableLink(
                payload = ReportToShare(
                    reportId = resolvedReportId,
                    title = currentReport.title,
                    city = currentLocator.thirdLevel,
                    country = currentLocator.firstLevel,
                    description = currentReport.description,
                    severity = currentReport.severity.name.lowercase(Locale.ROOT),
                    issueType = currentReport.issueType.name.lowercase(Locale.ROOT),
                    status = currentReport.status.name.lowercase(Locale.ROOT),
                    coordinate = currentReport.coordinate,
                    cellIndex = currentReport.cellIndex,
                    openCodeLocation = currentReport.olc ?: "",
                    attachments = currentReport.attachments
                )
            )
        }.getOrElse {
            val hash = model.reportSession.value.shareIndexHash.ifEmpty { "aXWsaxls" }
            "https://reportamelo.app/s/$hash"
        }

        _shareableLink.value = url
        model.updateReport(model.report.value.copy(shareUrl = url))
    }

    suspend fun createReport(using: ReportDataModel) {
        val result = ReportRepository.create(
            report = using.report.value.toDao(),
            shareIndexHash = using.reportSession.value.shareIndexHash,
            reportContainer = using.reportSession.value.reportContainer,
            groupingNameCode = using.locator.value.groupingNameCode,
            groupingId = using.locator.value.groupingId
        )
        using.updateReport(using.report.value.copy(id = result))
        _reportId.value = result
    }

    suspend fun submitGroupedAttachments(
        attachments: List<PhotoUploadTracker>,
        using: ReportDataModel
    ): CustomizedResponse<List<ReportAttachmentGrouping>>? {
        val newAttachments = attachments.filter { !it.isExisting.value }
        if (newAttachments.isEmpty()) return null

        var reportId = using.buildReportId()
        if (using.report.value.reportState == ReportStates.MODIFYING) {
            using.report.value.id?.let {
                reportId = it
            }
        }

        val payload = newAttachments.map { tracker ->
            GroupedAttachmentPayload(
                attachmentContainer = using.reportSession.value.reportContainer,
                key = tracker.key.value,
                previewFileName = "preview_${tracker.name.value}",
                fileName = tracker.name.value,
                reportId = reportId,
                notes = ""
            )
        }

        return ReportRepository.submitGroupedAttachments(payload)
    }

    suspend fun modify(using: ReportDataModel, attachments: List<PhotoUploadTracker>) {
        try {
            val id = using.report.value.id ?: return
            _reportId.value = id

            val url = using.report.value.shareUrl ?: ""
            _shareableLink.value = url

            ReportRepository.update(using.report.value.toDao())
            submitGroupedAttachments(attachments, using)
        } catch (e: Exception) {
            showAlert(e.localizedMessage ?: "Error")
        }
    }

    fun submitReport(
        model: ReportDataModel,
        attachments: List<PhotoUploadTracker>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            model.addAttachments(attachments)

            if (model.report.value.reportState == ReportStates.MODIFYING) {
                modify(model, attachments)
                model.removeAttachments()
                onComplete()
            } else {
                try {
                    val deferredGroup1 = async {
                        val result = submitGroupedAttachments(attachments, model)
                        if (result != null) {
                            model.setAttachmentId(result.data)
                        }
                        createReport(model)
                    }

                    val deferredGroup2 = async {
                        createShareableLink(model)
                        if (!UserRepository.shared.isGuestUser()) {
                            runCatching { CounterRepository.getCount() }
                        }
                    }

                    awaitAll(deferredGroup1, deferredGroup2)

                    model.removeAttachments()

                    SettingsStore.shared.reportsCount += 1
                    SettingsStore.shared.lastReportDate = Date()

                    onComplete()
                } catch (e: Exception) {
                    showAlert(e.localizedMessage ?: "Error")
                }
            }

            _isLoading.value = false
        }
    }

    fun showAlert(message: String) {
        _presentAlert.value = true
        _alertMessage.value = message
    }

    fun dismissAlert() {
        _presentAlert.value = false
        _alertMessage.value = ""
    }

    fun setStep(step: ReportStep) {
        _currentStep.value = step
    }

    fun goNext() {
        val next = ReportStep.fromRawValue(_currentStep.value.rawValue + 1)
        if (next != null) {
            _currentStep.value = next
        }
    }

    fun goBack() {
        val prev = ReportStep.fromRawValue(_currentStep.value.rawValue - 1)
        if (prev != null) {
            _currentStep.value = prev
        }
    }

    fun getNavigationTitle(reportState: ReportStates): String {
        return if (reportState == ReportStates.NEW) "Create a Report" else "Modifying a Report"
    }

    fun getButtonMessage(step: ReportStep = _currentStep.value): String {
        return when (step) {
            ReportStep.DETAILS -> "Submit"
            ReportStep.CONFIRMATION -> "Done"
            else -> "Next"
        }
    }

    val buttonMessage: String
        get() = getButtonMessage(_currentStep.value)

    fun submit(model: ReportDataModel, uploadTrackers: List<PhotoUploadTracker>) {
        if (_currentStep.value == ReportStep.DETAILS) {
            submitReport(model, uploadTrackers) {
                goNext()
            }
        } else {
            goNext()
        }
    }
}
