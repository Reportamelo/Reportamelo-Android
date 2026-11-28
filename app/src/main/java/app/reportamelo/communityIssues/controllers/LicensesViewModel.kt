package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.datamodel.License
import app.reportamelo.communityIssues.datamodel.LicenseCatalog
import app.reportamelo.communityIssues.datamodel.LicenseSource
import app.reportamelo.communityIssues.helpers.localized
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LicensesState(
    val isLoading: Boolean = true,
    val licenses: List<License> = emptyList(),
    val expandedIds: Set<String> = emptySet()
) {
    /** Drives the toolbar toggle, like `expandAll` in iOS `LicensesSubView.swift`. */
    val allExpanded: Boolean
        get() = licenses.isNotEmpty() && expandedIds.size == licenses.size
}

/**
 * Mirrors iOS `LicensesSubView.swift`: every license starts expanded, each one can be toggled, and
 * the toolbar button expands or collapses all of them. The button always reflects whether
 * everything is expanded, also after toggling items one by one.
 */
class LicensesViewModel(
    private val source: LicenseSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(LicensesState())
    val state: StateFlow<LicensesState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val unavailable = localized(R.string.license_text_not_available, "License text not available.")
            val licenses = withContext(ioDispatcher) {
                runCatching { LicenseCatalog.load(source, unavailable) }.getOrDefault(emptyList())
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    licenses = licenses,
                    expandedIds = licenses.mapTo(mutableSetOf()) { license -> license.id }
                )
            }
        }
    }

    fun toggle(id: String) {
        _state.update {
            it.copy(expandedIds = if (id in it.expandedIds) it.expandedIds - id else it.expandedIds + id)
        }
    }

    fun toggleAll() {
        _state.update {
            it.copy(
                expandedIds = if (it.allExpanded) emptySet() else it.licenses.mapTo(mutableSetOf()) { license -> license.id }
            )
        }
    }
}
