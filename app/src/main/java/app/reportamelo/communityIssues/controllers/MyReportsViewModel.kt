package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.utils.*
import app.reportamelo.communityIssues.datamodel.*
import app.reportamelo.models.* 
import app.reportamelo.communityIssues.datamodel.Report

import app.reportamelo.commons.*
import app.reportamelo.repository.*
import app.reportamelo.network.services.*
import app.reportamelo.network.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.network.services.*
import app.reportamelo.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MyReportsViewModel @JvmOverloads constructor(
    private val listByUserProvider: (Int) -> kotlinx.coroutines.flow.Flow<PaginatedResponse<ReportDAO>> = { page ->
        ReportRepository.listByUser(page)
    }
) : ViewModel() {
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _showDeleteAlert = MutableStateFlow(false)
    val showDeleteAlert: StateFlow<Boolean> = _showDeleteAlert.asStateFlow()

    private val _elementToDelete = MutableStateFlow<Set<Int>>(emptySet())
    val elementToDelete: StateFlow<Set<Int>> = _elementToDelete.asStateFlow()

    private val _reportToDelete = MutableStateFlow<Report?>(null)
    val reportToDelete: StateFlow<Report?> = _reportToDelete.asStateFlow()

    private val _refreshID = MutableStateFlow(UUID.randomUUID())
    val refreshID: StateFlow<UUID> = _refreshID.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    fun fetchReports(loadMore: Boolean = false) {
        viewModelScope.launch {
            if (loadMore) {
                if (!_hasNext.value || _isLoadingMore.value) return@launch
                _isLoadingMore.value = true
                _currentPage.value += 1
            } else {
                _isLoading.value = true
                _currentPage.value = 1
            }

            try {
                val stream = listByUserProvider(_currentPage.value)
                val startIndex = (_currentPage.value - 1) * 5

                stream.collect { result ->
                    _hasNext.value = result.hasNext
                    val documents = result.documents ?: return@collect
                    val newReports = documents.map { it.toModel() }

                    val currentReports = _reports.value.toMutableList()
                    val mergedReports = mutableListOf<Report>()

                    for (newReport in newReports) {
                        val existing = currentReports.find { it.id == newReport.id }
                        if (existing != null) {
                            existing.update(from = newReport)
                            mergedReports.add(existing)
                        } else {
                            mergedReports.add(newReport)
                        }
                    }

                    if (loadMore) {
                        if (currentReports.size >= startIndex) {
                            _reports.value = currentReports.take(startIndex) + mergedReports
                        } else {
                            _reports.value = currentReports + mergedReports
                        }
                    } else {
                        _reports.value = mergedReports
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (loadMore) {
                    _currentPage.value -= 1
                }
            } finally {
                if (loadMore) {
                    _isLoadingMore.value = false
                } else {
                    _isLoading.value = false
                }
            }
        }
    }

    fun confirmDeletion(id: String) {
        _reports.update { it.filter { report -> report.id != id } }
        _reportToDelete.value = null
    }

    fun updateReport(report: Report) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ReportRepository.update(report.toDao())
                if (result == SuccessfulResult.UPDATED) {
                    _refreshID.value = UUID.randomUUID()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun delete(report: Report? = null) {
        val id = report?.id ?: return
        viewModelScope.launch {
            try {
                val result = ReportRepository.delete(id)
                if (result == SuccessfulResult.DELETED) {
                    confirmDeletion(id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun openInMaps(coordinate: Coordinate) {
        viewModelScope.launch {
            MapExplorerViewModel.shared.loadReportsWith(coordinate)
            val targetAuthVm = MapExplorerViewModel.shared.authViewModel ?: AuthViewModel.shared
            targetAuthVm.setCameraPosition(
                to = coordinate,
                latitudeDelta = 0.000392298826163122953,
                longitudeDelta = 0.0003914447804127257768
            )
            app.reportamelo.communityIssues.helpers.DeepLinkRouter.shared.activeTab.value = 1
        }
    }

    fun openDetails(reportId: String, cityId: String) {
        viewModelScope.launch {
            try {
                MapExplorerRepository.report(reportId, "SV", cityId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

