package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.IssueStatus
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.communityIssues.helpers.DeepLinkRouter
import app.reportamelo.communityIssues.helpers.MapFocusCoordinator
import app.reportamelo.communityIssues.helpers.MapFocusRequest
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.models.ReportDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyReportsViewModelTest {

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

    private fun dao(
        id: String? = reportId,
        status: IssueStatus = IssueStatus.REJECTED
    ) = ReportDAO(
        id = id,
        lat = 13.7,
        lng = -89.2,
        address = "Alameda Manuel Enrique Araujo, San Salvador",
        title = "Large pothole on main avenue",
        description = "Deep pothole in the right lane near the intersection",
        statusId = status.identifier,
        cityId = "1",
        shareUrl = "/LleqAJRZ3mpZVdhs/report/20261120/SSC/SV/baches-por-todos-lados"
    )

    private fun pagesOf(vararg documents: ReportDAO): (Int) -> Flow<PaginatedResponse<ReportDAO>> =
        { page ->
            flowOf(
                PaginatedResponse(
                    documents = if (page == 1) documents.toList() else emptyList(),
                    total = documents.size,
                    page = page,
                    documentsPerPage = 5,
                    totalPages = 1,
                    hasNext = false,
                    hasPrev = false
                )
            )
        }

    @Test
    fun `a successful delete removes the report and announces it`() = runTest(testDispatcher) {
        var requestedId: String? = null

        val viewModel = MyReportsViewModel(
            listByUserProvider = pagesOf(dao()),
            deleteProvider = { id ->
                requestedId = id
                SuccessfulResult.DELETED
            }
        )
        viewModel.fetchReports()
        advanceUntilIdle()

        val events = mutableListOf<MyReportsEvent>()
        val collector = launch { viewModel.events.collect { events.add(it) } }

        viewModel.delete(viewModel.reports.value.single())
        advanceUntilIdle()

        assertEquals(reportId, requestedId)
        assertTrue(viewModel.reports.value.isEmpty())
        assertTrue(viewModel.deletingReportIds.value.isEmpty())
        assertEquals(listOf(MyReportsEvent.ReportDeleted), events)

        collector.cancel()
    }

    @Test
    fun `a failed delete keeps the report in the list and offers a retry`() =
        runTest(testDispatcher) {
            val viewModel = MyReportsViewModel(
                listByUserProvider = pagesOf(dao()),
                deleteProvider = { throw IllegalStateException("network down") }
            )
            viewModel.fetchReports()
            advanceUntilIdle()
            val target = viewModel.reports.value.single()

            val events = mutableListOf<MyReportsEvent>()
            val collector = launch { viewModel.events.collect { events.add(it) } }

            viewModel.delete(target)
            advanceUntilIdle()

            assertEquals(listOf(target), viewModel.reports.value)
            assertTrue(viewModel.deletingReportIds.value.isEmpty())
            assertEquals(listOf(MyReportsEvent.ReportDeleteFailed(target)), events)

            collector.cancel()
        }

    @Test
    fun `delete does nothing without a report or without an id`() = runTest(testDispatcher) {
        var called = false
        val viewModel = MyReportsViewModel(
            listByUserProvider = pagesOf(dao(id = null)),
            deleteProvider = {
                called = true
                SuccessfulResult.DELETED
            }
        )
        viewModel.fetchReports()
        advanceUntilIdle()

        viewModel.delete(null)
        viewModel.delete(viewModel.reports.value.single())
        advanceUntilIdle()

        assertFalse(called)
    }

    @Test
    fun `openInMaps publishes a one-shot focus request and switches to the Issues tab`() =
        runTest(testDispatcher) {
            val viewModel = MyReportsViewModel(
                listByUserProvider = pagesOf(dao(status = IssueStatus.CONFIRMED))
            )
            viewModel.fetchReports()
            advanceUntilIdle()
            val target = viewModel.reports.value.single()
            DeepLinkRouter.shared.activeTab.value = 0

            var received: MapFocusRequest? = null
            val collector = launch { received = MapFocusCoordinator.shared.requests.first() }
            advanceUntilIdle()

            viewModel.openInMaps(target)
            advanceUntilIdle()

            assertEquals(reportId, received?.reportId)
            assertEquals(target.coordinate, received?.coordinate)
            assertEquals("1", received?.cityId)
            assertEquals(1, DeepLinkRouter.shared.activeTab.value)

            collector.cancel()
        }

    @Test
    fun `a report without id is never focused on the map`() = runTest(testDispatcher) {
        val viewModel = MyReportsViewModel(listByUserProvider = pagesOf(dao(id = null)))
        viewModel.fetchReports()
        advanceUntilIdle()
        DeepLinkRouter.shared.activeTab.value = 0

        var received: MapFocusRequest? = null
        val collector = launch { received = MapFocusCoordinator.shared.requests.first() }
        advanceUntilIdle()

        viewModel.openInMaps(viewModel.reports.value.single())
        advanceUntilIdle()

        assertNull(received)
        assertEquals(0, DeepLinkRouter.shared.activeTab.value)

        collector.cancel()
    }
}
