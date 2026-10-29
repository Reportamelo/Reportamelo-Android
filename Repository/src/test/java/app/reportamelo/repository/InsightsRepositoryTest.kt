package app.reportamelo.repository

import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.utils.getFullYear
import app.reportamelo.commons.utils.getMonthName
import app.reportamelo.models.BasicInfo
import app.reportamelo.models.DaySummary
import app.reportamelo.models.InsightsFilter
import app.reportamelo.models.MonthlyInsightsResponse
import app.reportamelo.network.InsightsService
import app.reportamelo.network.ServiceError
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class InsightsRepositoryTest {

    private lateinit var originalService: InsightsService

    @Before
    fun setUp() {
        originalService = InsightsRepository.service
    }

    @After
    fun tearDown() {
        InsightsRepository.service = originalService
    }

    @Test
    fun `insightsForThisMonth passes current year and lowercase English month name to service`() = runBlocking {
        var capturedFilter: InsightsFilter? = null

        InsightsRepository.service = object : InsightsService() {
            override suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
                capturedFilter = filter
                return MonthlyInsightsResponse(
                    totalReports = 1,
                    totalSignatures = 0,
                    totalComments = 0,
                    totalPetitions = 0,
                    recentActivity = mapOf(
                        "2026-10-27" to DaySummary(
                            interactions = 1,
                            reports = listOf(
                                BasicInfo(
                                    id = "SV-SSC-20261027-d0f7eRHq4YMymJE3",
                                    title = "Pothole",
                                    status = "reported"
                                )
                            ),
                            signatures = emptyList(),
                            comments = emptyList(),
                            petitions = emptyList()
                        )
                    )
                )
            }
        }

        val response = InsightsRepository.insightsForThisMonth()
        assertEquals(getFullYear(), capturedFilter?.year)
        assertEquals(getMonthName(), capturedFilter?.month)
        assertEquals(1, response.totalReports)
        assertEquals(0, response.totalComments)
        assertTrue(response.recentActivity.containsKey("2026-10-27"))
        assertEquals(
            "SV-SSC-20261027-d0f7eRHq4YMymJE3",
            response.recentActivity["2026-10-27"]?.reports?.first()?.id
        )
    }

    @Test
    fun `filterInsights maps ServiceError ServerError to CommonIntercommunicationErrors ServerError`() = runBlocking {
        InsightsRepository.service = object : InsightsService() {
            override suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
                throw ServiceError.ServerError("500")
            }
        }

        try {
            InsightsRepository.filterInsights(InsightsFilter(year = "2026", month = "october"))
            fail("Expected CommonIntercommunicationErrors.ServerError")
        } catch (e: CommonIntercommunicationErrors.ServerError) {
            assertEquals("500", e.message)
        }
    }
}
