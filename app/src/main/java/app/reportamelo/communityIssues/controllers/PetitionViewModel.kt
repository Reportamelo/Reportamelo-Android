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
import app.reportamelo.models.Petition
import app.reportamelo.network.services.*
import app.reportamelo.repository.ReportRepository
import app.reportamelo.communityIssues.datamodel.Report
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

enum class PetitionStep {
    DETAILS,
    REPORTS,
    SIGNATURES,
    DONE
}

class PetitionViewModel : ViewModel() {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _petition = MutableStateFlow(
        Petition(
            id = "",
            title = "",
            description = "",
            targetSignatures = 10,
            currentSignatures = 0,
            categoryId = 1,
            statusId = 1,
            reportedBy = UUID.randomUUID().toString(),
            disabled = false,
            reportsIds = emptyList()
        )
    )
    val petition: StateFlow<Petition> = _petition.asStateFlow()

    private val _isTitleValid = MutableStateFlow(false)
    val isTitleValid: StateFlow<Boolean> = _isTitleValid.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _stepperAction = MutableStateFlow("")
    val stepperAction: StateFlow<String> = _stepperAction.asStateFlow()

    private val _isDescriptionValid = MutableStateFlow(false)
    val isDescriptionValid: StateFlow<Boolean> = _isDescriptionValid.asStateFlow()

    private val _minimumSignatures = MutableStateFlow(10)
    val minimumSignatures: StateFlow<Int> = _minimumSignatures.asStateFlow()

    private val _currentStep = MutableStateFlow(PetitionStep.DETAILS)
    val currentStep: StateFlow<PetitionStep> = _currentStep.asStateFlow()

    private val _doneTrigger = MutableStateFlow(false)
    val doneTrigger: StateFlow<Boolean> = _doneTrigger.asStateFlow()

    private val _shareUrl = MutableStateFlow("")
    val shareUrl: StateFlow<String> = _shareUrl.asStateFlow()

    fun prepareForModification(petition: Petition) {
        _petition.value = petition
    }

    val isValid: Boolean
        get() = _petition.value.title.isEmpty() || _petition.value.description.isEmpty() || _petition.value.targetSignatures == 0

    fun fetchReports() {
        viewModelScope.launch {
            try {
                val stream = ReportRepository.listByUser(1)
                stream.collect { result ->
                    val documents = result.documents ?: return@collect
                    _reports.value = documents.map { it.toModel() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun goNext() {
        val nextOrdinal = _currentStep.value.ordinal + 1
        PetitionStep.values().firstOrNull { it.ordinal == nextOrdinal }?.let {
            _currentStep.value = it
        }
    }

    fun goBack() {
        val prevOrdinal = _currentStep.value.ordinal - 1
        PetitionStep.values().firstOrNull { it.ordinal == prevOrdinal }?.let {
            _currentStep.value = it
        }
    }

    fun submit() {
        if (_currentStep.value == PetitionStep.SIGNATURES) {
            goNext()
        } else {
            println("otherSteps")
            goNext()
        }
    }

    val buttonMessage: String
        get() = if (_currentStep.value == PetitionStep.SIGNATURES) "Submit" else "Next"
}

