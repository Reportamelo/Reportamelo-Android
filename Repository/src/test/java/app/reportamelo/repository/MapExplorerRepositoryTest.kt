package app.reportamelo.repository

import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.MapExplorerQueryParams
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.network.MapExplorerService
import app.reportamelo.network.ServiceError
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapExplorerRepositoryTest {

    private lateinit var originalService: MapExplorerService

    @Before
    fun setUp() {
        originalService = MapExplorerRepository.service
    }

    @After
    fun tearDown() {
        MapExplorerRepository.service = originalService
    }

    @Test
    fun `reportsByLocation forwards query and headers to MapExplorerService`() = runTest {
        var capturedQuery: MapExplorerQueryParams? = null
        var capturedHeaders: Map<String, String>? = null
        val sampleReport = MapExplorerReport(
            id = "SV-SSC-20260907-Dhly4eZs2nVGJ2gY",
            lat = 13.7269865,
            lng = -89.1809165,
            address = "Area Urbana, Cuscatancingo, San Salvador, El Salvador",
            title = "Baches",
            description = "Baches",
            severityId = 1,
            statusId = 2,
            issueTypeId = 1,
            matterToSolveId = 1,
            cellIndex = "8f6d75db4a21d12",
            createdAtRaw = 1788768837882L,
            updatedAtRaw = 1788768889064L,
            userName = "tests.reportamelo",
            cityId = "577119db-c0d2-474a-98fa-d58c8a7e307f",
            shareUrl = "/Dhly4eZs2nVGJ2gY/report/20260907/SSC/SV/baches",
            reportContainer = "d8dcd05d-71be-45b3-bd65-d50af060c5da",
            profileId = "1eEWAXwkcmYnUog",
            ocl = "65GPRHC+QC",
            observations = "Las imágenes muestran claramente los baches reportados."
        )

        MapExplorerRepository.service = object : MapExplorerService() {
            override suspend fun reports(
                q: MapExplorerQueryParams,
                headers: Map<String, String>
            ): List<MapExplorerReport> {
                capturedQuery = q
                capturedHeaders = headers
                return listOf(sampleReport)
            }
        }

        val result = MapExplorerRepository.reportsByLocation(
            lat = 13.7269865,
            lng = -89.1809165,
            radius = 600,
            severityId = 1,
            issueTypeId = 1,
            cityId = "577119db-c0d2-474a-98fa-d58c8a7e307f"
        )

        assertEquals(1, result.size)
        assertEquals(sampleReport, result[0])
        assertEquals(listOf(1), capturedQuery?.issueTypeIds)
        assertEquals(listOf(1), capturedQuery?.severityIds)
        assertEquals("SV", capturedHeaders?.get("CountryCode"))
        assertEquals("577119db-c0d2-474a-98fa-d58c8a7e307f", capturedHeaders?.get("CityId"))
    }

    @Test(expected = CommonIntercommunicationErrors.InvalidPetition::class)
    fun `reportsByLocation maps BadRequest to InvalidPetition`() = runTest {
        MapExplorerRepository.service = object : MapExplorerService() {
            override suspend fun reports(
                q: MapExplorerQueryParams,
                headers: Map<String, String>
            ): List<MapExplorerReport> {
                throw ServiceError.BadRequest(GenericResponse(message = "Invalid", code = "INVALID_QUERY"))
            }
        }

        MapExplorerRepository.reportsByLocation(
            lat = 13.7269865,
            lng = -89.1809165,
            radius = 600,
            severityId = 1,
            issueTypeId = 1,
            cityId = "city-1"
        )
    }
}
