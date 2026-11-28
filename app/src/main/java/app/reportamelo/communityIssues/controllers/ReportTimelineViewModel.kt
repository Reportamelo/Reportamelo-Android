package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.models.Resolution
import app.reportamelo.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Un reporte sin ninguna etapa registrada es un caso normal, no un fallo. */
fun Resolution.hasNoUpdates(): Boolean =
    history.reported == null &&
        history.confirmed == null &&
        history.fixed == null &&
        history.inProgress?.updates.isNullOrEmpty()

sealed interface ReportTimelineUiState {
    data object Loading : ReportTimelineUiState
    data class Content(val resolution: Resolution) : ReportTimelineUiState
    /** El reporte existe pero aún no tiene resoluciones. */
    data object Empty : ReportTimelineUiState
    data object Error : ReportTimelineUiState
}

/**
 * Hospeda la carga de la línea de tiempo de un reporte.
 *
 * La acción Timeline navega de inmediato y la petición ocurre aquí, no dentro del
 * bottom sheet: así la transición no arrastra un spinner.
 */
class ReportTimelineViewModel @JvmOverloads constructor(
    private val reportId: String,
    private val fetchResolution: suspend (String) -> Resolution = { id ->
        ReportRepository.fetchResolutionByReport(id)
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportTimelineUiState>(ReportTimelineUiState.Loading)
    val uiState: StateFlow<ReportTimelineUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ReportTimelineUiState.Loading
            _uiState.value = try {
                val resolution = fetchResolution(reportId)
                if (resolution.hasNoUpdates()) {
                    ReportTimelineUiState.Empty
                } else {
                    ReportTimelineUiState.Content(resolution)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ReportTimelineUiState.Error
            }
        }
    }
}
