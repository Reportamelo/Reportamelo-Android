package app.reportamelo.communityIssues.controllers

import app.reportamelo.models.AssignedInstitution
import app.reportamelo.models.InProgressMilestone
import app.reportamelo.models.IssueHistory
import app.reportamelo.models.Milestone
import app.reportamelo.models.Resolution
import app.reportamelo.models.ResolutionMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportTimelineViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val reportId = "SV-SS-20261120-aXWsaxls"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun resolution(history: IssueHistory) = Resolution(
        status = "reported",
        id = reportId,
        history = history,
        metadata = ResolutionMetadata(
            cityId = "1",
            groupingId = "SSC",
            assigned = AssignedInstitution(
                institutionName = "Alcaldía",
                institutionCode = "ALC",
                institutionId = "alc-1"
            ),
            resourceType = "report",
            resourceId = reportId
        )
    )

    @Test
    fun `a resolution with history renders content`() = runTest(testDispatcher) {
        val expected = resolution(
            IssueHistory(reported = Milestone(date = "2026-11-20", by = "citizen"))
        )
        val viewModel = ReportTimelineViewModel(reportId) { expected }

        advanceUntilIdle()

        assertEquals(ReportTimelineUiState.Content(expected), viewModel.uiState.value)
    }

    @Test
    fun `a report with no milestones is empty, not an error`() = runTest(testDispatcher) {
        val viewModel = ReportTimelineViewModel(reportId) {
            resolution(IssueHistory(inProgress = InProgressMilestone("Alcaldía", emptyList())))
        }

        advanceUntilIdle()

        assertEquals(ReportTimelineUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a failed fetch surfaces an error that retry can clear`() = runTest(testDispatcher) {
        var attempts = 0
        val loaded = resolution(IssueHistory(confirmed = Milestone(date = "2026-11-21")))
        val viewModel = ReportTimelineViewModel(reportId) {
            attempts += 1
            if (attempts == 1) throw IllegalStateException("network down") else loaded
        }

        advanceUntilIdle()
        assertEquals(ReportTimelineUiState.Error, viewModel.uiState.value)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(ReportTimelineUiState.Content(loaded), viewModel.uiState.value)
        assertEquals(2, attempts)
    }

    @Test
    fun `the first state is loading`() = runTest(testDispatcher) {
        val viewModel = ReportTimelineViewModel(reportId) {
            resolution(IssueHistory(fixed = Milestone(date = "2026-11-21")))
        }

        assertTrue(viewModel.uiState.value is ReportTimelineUiState.Loading)

        advanceUntilIdle()
    }
}
