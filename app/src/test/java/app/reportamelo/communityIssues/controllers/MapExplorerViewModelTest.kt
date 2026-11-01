package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.IssueStatus
import app.reportamelo.communityIssues.helpers.maps.SearchCompleter
import app.reportamelo.communityIssues.helpers.maps.SearchSuggestion
import app.reportamelo.communityIssues.ui.components.shouldShowAd
import app.reportamelo.models.Coordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapExplorerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setSearchExpanded and setSearchActivated update StateFlows`() {
        val viewModel = MapExplorerViewModel()

        assertFalse(viewModel.isSearchExpanded.value)
        assertFalse(viewModel.isSearchActivated.value)

        viewModel.setSearchExpanded(true)
        assertTrue(viewModel.isSearchExpanded.value)

        viewModel.setSearchActivated(true)
        assertTrue(viewModel.isSearchActivated.value)

        viewModel.setSearchExpanded(false)
        viewModel.setSearchActivated(false)
        assertFalse(viewModel.isSearchExpanded.value)
        assertFalse(viewModel.isSearchActivated.value)
    }

    @Test
    fun `updateSearchText and updateSelection update StateFlows`() {
        val viewModel = MapExplorerViewModel()

        viewModel.updateSearchText("Colonia Escalante")
        assertEquals("Colonia Escalante", viewModel.searchText.value)

        viewModel.updateSelection(IssueStatus.CONFIRMED.title)
        assertEquals(IssueStatus.CONFIRMED.title, viewModel.selectionState.value)

        viewModel.updateSelection("")
        assertEquals("", viewModel.selectionState.value)
    }

    @Test
    fun `applySuggestion updates searchText and creates searchMarker`() {
        val viewModel = MapExplorerViewModel()
        val coord = Coordinate(lat = 13.7013, lng = -89.2244)

        viewModel.applySuggestion("San Salvador", coord)

        assertEquals("San Salvador", viewModel.searchText.value)
        assertFalse(viewModel.showSearchOverlay.value)
        val marker = viewModel.searchMarker.value
        assertNotNull(marker)
        assertEquals("San Salvador", marker?.title)
        assertEquals(13.7013, marker?.coordinate?.lat ?: 0.0, 1e-4)
        assertEquals(-89.2244, marker?.coordinate?.lng ?: 0.0, 1e-4)
    }

    @Test
    fun `SearchCompleter returns multiple suggestions for partial queries and guarantees unique keys`() {
        val completer = SearchCompleter(context = null, ioDispatcher = testDispatcher, debounceMs = 0L)

        val sanSalResults = completer.findLocalSuggestions("San sal", "SV")
        assertTrue("Expected multiple results for 'San sal', got ${sanSalResults.size}", sanSalResults.size > 1)
        assertEquals(sanSalResults.size, sanSalResults.map { it.id }.toSet().size)

        val cuscatanResults = completer.findLocalSuggestions("cuscatan", "SV")
        assertTrue("Expected at least 3 results for 'cuscatan', got ${cuscatanResults.size}", cuscatanResults.size >= 3)
        assertTrue(cuscatanResults.any { it.title.contains("Cuscatancingo", ignoreCase = true) })
        assertEquals(cuscatanResults.size, cuscatanResults.map { it.id }.toSet().size)
    }

    @Test
    fun `SearchCompleter deduplicateSuggestions prevents duplicate LazyColumn keys`() {
        val duplicates = listOf(
            SearchSuggestion(title = "Salva", subtitle = "Salva, Romania"),
            SearchSuggestion(title = "Salva", subtitle = "Salva, Romania"),
            SearchSuggestion(title = "San Salvador", subtitle = "San Salvador, El Salvador")
        )

        val deduplicated = SearchCompleter.deduplicateSuggestions(duplicates)
        assertEquals(2, deduplicated.size)
        assertEquals(2, deduplicated.map { it.id }.toSet().size)
    }

    @Test
    fun `SearchCompleter update emits suggestions StateFlow and clears on empty query`() = runTest {
        val completer = SearchCompleter(context = null, ioDispatcher = testDispatcher, debounceMs = 0L)

        completer.update("Salva")
        assertTrue(completer.suggestions.value.isNotEmpty())

        completer.update("   ")
        assertTrue(completer.suggestions.value.isEmpty())
    }

    @Test
    fun `shouldShowAd matches iOS AdBackoffUtils inverse exponential backoff`() {
        assertFalse(shouldShowAd(0))
        assertFalse(shouldShowAd(1))
        assertTrue(shouldShowAd(2))
        assertFalse(shouldShowAd(3))
        assertFalse(shouldShowAd(4))
        assertFalse(shouldShowAd(5))
        assertTrue(shouldShowAd(6))
        assertTrue(shouldShowAd(14))
    }
}
