package app.reportamelo.communityIssues.controllers

import app.reportamelo.communityIssues.ui.components.isDateWithinInsightsRange
import app.reportamelo.models.BasicInfo
import app.reportamelo.models.DaySummary
import app.reportamelo.models.InsightsFilter
import app.reportamelo.models.MonthlyInsightsResponse
import app.reportamelo.network.InsightsService
import app.reportamelo.network.ServiceError
import app.reportamelo.repository.InsightsRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var originalService: InsightsService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        originalService = InsightsRepository.service
    }

    @After
    fun tearDown() {
        InsightsRepository.service = originalService
        Dispatchers.resetMain()
    }

    @Test
    fun `getInsights updates insights StateFlow and clears loading state`() {
        val sampleDay = DaySummary(
            interactions = 1,
            reports = listOf(
                BasicInfo(
                    id = "SV-SSC-20261027-d0f7eRHq4YMymJE3",
                    title = "Road damage",
                    status = "reported"
                )
            ),
            signatures = emptyList(),
            comments = emptyList(),
            petitions = emptyList()
        )
        InsightsRepository.service = object : InsightsService() {
            override suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
                return MonthlyInsightsResponse(
                    totalReports = 1,
                    totalSignatures = 0,
                    totalComments = 0,
                    totalPetitions = 0,
                    recentActivity = mapOf("2026-10-27" to sampleDay)
                )
            }
        }

        val viewModel = InsightsViewModel()
        viewModel.getInsights()

        assertFalse(viewModel.isLoading.value)
        assertEquals(1, viewModel.insights.value.totalReports)
        assertEquals(0, viewModel.insights.value.totalComments)
        assertEquals(1, viewModel.insights.value.recentActivity["2026-10-27"]?.interactions)
    }

    @Test
    fun `onDateSelected navigates to Activity when date has activity and NoActivity otherwise`() {
        val sampleDay = DaySummary(
            interactions = 1,
            reports = listOf(
                BasicInfo(
                    id = "SV-SSC-20261027-d0f7eRHq4YMymJE3",
                    title = "Road damage",
                    status = "reported"
                )
            ),
            signatures = emptyList(),
            comments = emptyList(),
            petitions = emptyList()
        )
        InsightsRepository.service = object : InsightsService() {
            override suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
                return MonthlyInsightsResponse(
                    totalReports = 1,
                    totalSignatures = 0,
                    totalComments = 0,
                    totalPetitions = 0,
                    recentActivity = mapOf("2026-10-27" to sampleDay)
                )
            }
        }

        val viewModel = InsightsViewModel()
        viewModel.getInsights()

        // Tap on a day with activity
        viewModel.onDateSelected("2026-10-27")
        assertEquals(listOf(InsightsNavigation.Activity("2026-10-27")), viewModel.navigationPath.value)

        // Navigate back
        viewModel.navigateBack()
        assertTrue(viewModel.navigationPath.value.isEmpty())

        // Tap on a day without activity
        viewModel.onDateSelected("2026-10-28")
        assertEquals(listOf(InsightsNavigation.NoActivity), viewModel.navigationPath.value)
    }

    @Test
    fun `getInsights shows alert when ServerError occurs`() {
        InsightsRepository.service = object : InsightsService() {
            override suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
                throw ServiceError.ServerError("500")
            }
        }

        val viewModel = InsightsViewModel()
        viewModel.getInsights()

        assertFalse(viewModel.isLoading.value)
        assertTrue(viewModel.showAlert.value)
        assertEquals("Server Error", viewModel.message.value)

        viewModel.dismissAlert()
        assertFalse(viewModel.showAlert.value)
    }

    @Test
    fun `isDateWithinInsightsRange enforces 2026 minimum and current month-year maximum`() {
        val referenceNow = LocalDate.of(2026, 10, 27)

        // Dates before 2026 are rejected
        assertFalse(isDateWithinInsightsRange(LocalDate.of(2025, 12, 31), referenceNow))

        // Dates within 2026 up to end of current month are allowed
        assertTrue(isDateWithinInsightsRange(LocalDate.of(2026, 1, 1), referenceNow))
        assertTrue(isDateWithinInsightsRange(LocalDate.of(2026, 10, 27), referenceNow))
        assertTrue(isDateWithinInsightsRange(LocalDate.of(2026, 10, 31), referenceNow))

        // Dates after current month or in future years are rejected
        assertFalse(isDateWithinInsightsRange(LocalDate.of(2026, 11, 1), referenceNow))
        assertFalse(isDateWithinInsightsRange(LocalDate.of(2027, 1, 1), referenceNow))
    }
}
