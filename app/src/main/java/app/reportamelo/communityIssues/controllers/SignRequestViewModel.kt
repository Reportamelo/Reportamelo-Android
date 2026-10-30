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
import app.reportamelo.network.services.*
import app.reportamelo.repository.*
import app.reportamelo.communityIssues.ui.components.SignatureLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignRequestViewModel : ViewModel() {

    private val _petitions = MutableStateFlow<List<PetitionPost>>(emptyList())
    val petitions: StateFlow<List<PetitionPost>> = _petitions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _signatureCount = MutableStateFlow(125)
    val signatureCount: StateFlow<Int> = _signatureCount.asStateFlow()

    private val pageLimit: Int = 16

    private val _isPrimaryActionVisible = MutableStateFlow(true)
    val isPrimaryActionVisible: StateFlow<Boolean> = _isPrimaryActionVisible.asStateFlow()

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    private val _subtitle = MutableStateFlow<String?>(null)
    val subtitle: StateFlow<String?> = _subtitle.asStateFlow()

    private val _activeSubtitleIndex = MutableStateFlow<Int?>(null)
    val activeSubtitleIndex: StateFlow<Int?> = _activeSubtitleIndex.asStateFlow()

    private val _showSignatureModal = MutableStateFlow(false)
    val showSignatureModal: StateFlow<Boolean> = _showSignatureModal.asStateFlow()

    private val _showCreateRequestView = MutableStateFlow(false)
    val showCreateRequestView: StateFlow<Boolean> = _showCreateRequestView.asStateFlow()

    private val _value = MutableStateFlow(20.0)
    val value: StateFlow<Double> = _value.asStateFlow()

    private val _isSearchBarVisible = MutableStateFlow(false)
    val isSearchBarVisible: StateFlow<Boolean> = _isSearchBarVisible.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _issueType = MutableStateFlow(IssueTypes.ROAD)
    val issueType: StateFlow<IssueTypes> = _issueType.asStateFlow()

    private val _orderFilter = MutableStateFlow(OrderFilter.ASCENDING)
    val orderFilter: StateFlow<OrderFilter> = _orderFilter.asStateFlow()

    private val _severity = MutableStateFlow(Severity.LOW)
    val severity: StateFlow<Severity> = _severity.asStateFlow()

    private val _selectedItem = MutableStateFlow<Int?>(null)
    val selectedItem: StateFlow<Int?> = _selectedItem.asStateFlow()

    private val _strokes = MutableStateFlow<List<SignatureLine>>(emptyList())
    val strokes: StateFlow<List<SignatureLine>> = _strokes.asStateFlow()

    fun fetchPetitions(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                _currentPage.value = 1
                _canLoadMore.value = true
            }

            if (_isLoading.value || (!_canLoadMore.value && !reset)) return@launch

            _isLoading.value = true

            val query = PaginatedRequestQueryParams(
                page = _currentPage.value,
                limit = 10,
                issueTypeId = if (_issueType.value == IssueTypes.ALL) null else _issueType.value.identifier,
                severityId = if (_severity.value == Severity.ALL) null else _severity.value.identifier,
                ordering = _orderFilter.value.filter
            )

            println(query)

            // Mocked PetitionRepository logic
            /*
            try {
                val result = PetitionRepository.list(query, null)
                val documents = result.documents
                if (documents == null) {
                    _canLoadMore.value = false
                } else {
                    if (reset) {
                        _petitions.value = documents
                    } else {
                        val existingIds = _petitions.value.mapNotNull { it.id }.toSet()
                        val uniqueNewPetitions = documents.filter { doc ->
                            doc.id == null || !existingIds.contains(doc.id)
                        }
                        _petitions.value = _petitions.value + uniqueNewPetitions
                    }

                    _canLoadMore.value = result.hasNext && _currentPage.value < pageLimit
                    if (_canLoadMore.value) {
                        _currentPage.value += 1
                    }
                }
            } catch (e: Exception) {
                println(e)
                _canLoadMore.value = false
            }
            */

            _isLoading.value = false
        }
    }

    fun getSelectedIndex(petition: PetitionPost): Int {
        return _petitions.value.indexOfFirst { it.id == petition.id }.takeIf { it >= 0 } ?: 0
    }

    fun formatSigners(petition: PetitionPost): String {
        return if (petition.postSigners.hasCurrentUserSigned) {
            "${petition.currentSignatures} citizens and you have signed"
        } else {
            "${petition.currentSignatures} citizens have signed"
        }
    }
}

