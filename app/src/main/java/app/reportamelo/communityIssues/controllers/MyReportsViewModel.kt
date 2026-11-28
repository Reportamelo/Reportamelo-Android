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
import app.reportamelo.communityIssues.helpers.DeepLinkRouter
import app.reportamelo.communityIssues.helpers.MapFocusCoordinator
import app.reportamelo.communityIssues.helpers.MapFocusRequest
import app.reportamelo.network.services.*
import app.reportamelo.repository.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MyReportsViewModel @JvmOverloads constructor(
    private val listByUserProvider: (Int) -> kotlinx.coroutines.flow.Flow<PaginatedResponse<ReportDAO>> = { page ->
        ReportRepository.listByUser(page)
    },
    private val deleteProvider: suspend (String) -> SuccessfulResult = { reportId ->
        ReportRepository.delete(reportId)
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

    private val _deletingReportIds = MutableStateFlow<Set<String>>(emptySet())
    /** Reportes con un borrado en vuelo: la tarjeta se atenúa y deja de ser interactiva. */
    val deletingReportIds: StateFlow<Set<String>> = _deletingReportIds.asStateFlow()

    private val _events = Channel<MyReportsEvent>(Channel.BUFFERED)
    /** Eventos de un solo consumo (snackbars). Una rotación no los repite. */
    val events: Flow<MyReportsEvent> = _events.receiveAsFlow()

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

    /**
     * Borrado pesimista: se espera al servidor antes de quitar el reporte de la lista.
     * Es una acción irreversible sobre un reporte ya enviado, así que no hay "deshacer".
     */
    fun delete(report: Report? = null) {
        val id = report?.id ?: return
        if (id in _deletingReportIds.value) return

        viewModelScope.launch {
            _deletingReportIds.update { it + id }
            try {
                val result = deleteProvider(id)
                if (result == SuccessfulResult.DELETED) {
                    confirmDeletion(id)
                    _events.send(MyReportsEvent.ReportDeleted)
                } else {
                    _events.send(MyReportsEvent.ReportDeleteFailed(report))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.send(MyReportsEvent.ReportDeleteFailed(report))
            } finally {
                _deletingReportIds.update { it - id }
            }
        }
    }

    /**
     * Abre el reporte en el mapa **interno** de la app (tab Issues), no en una app externa.
     *
     * Publica la petición en [MapFocusCoordinator] y cambia de tab. El mapa carga ese
     * reporte explícitamente, aunque esté fuera del viewport o excluido por los filtros.
     */
    fun openInMaps(report: Report) {
        val id = report.id ?: return
        MapFocusCoordinator.shared.focus(
            MapFocusRequest(
                reportId = id,
                coordinate = report.coordinate,
                cityId = report.cityId
            )
        )
        DeepLinkRouter.shared.activeTab.value = 1
    }
}

/** Avisos de un solo consumo para la UI de Mis reportes. */
sealed interface MyReportsEvent {
    data object ReportDeleted : MyReportsEvent
    data class ReportDeleteFailed(val report: Report) : MyReportsEvent
}

