package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.CommentForType
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.ReportViolationStatus
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.commons.VotingType
import app.reportamelo.models.Comment
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.models.ReportViolation
import app.reportamelo.models.VotingResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleReport = MapExplorerReport(
        id = "SV-SSC-20260906-40nPehqkecslz7GJ",
        lat = 13.726914,
        lng = -89.179982,
        address = "3a Calle Oriente, Cuscatancingo, San Salvador, El Salvador",
        title = "Baches",
        description = "Baches",
        severityId = 1,
        statusId = 3,
        issueTypeId = 1,
        matterToSolveId = 1,
        cellIndex = "8f6d75db4a21d12",
        createdAtRaw = 1788768837L,
        updatedAtRaw = 1788768837L,
        userName = "tests.reportamelo",
        cityId = "577119db-c0d2-474a-98fa-d58c8a7e307f",
        shareUrl = "https://reportamelo.app/reports/SV-SSC-20260906-40nPehqkecslz7GJ",
        reportContainer = "d8dcd05d-71be-45b3-bd65-d50af060c5da",
        profileId = "profile-123"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize for registered user checks vote status and fetches latest comments`() = runTest(testDispatcher) {
        val sampleComment = Comment(
            id = "c-1",
            commentFor = CommentForType.REPORT,
            resourceId = sampleReport.id,
            message = "Affects daily commute",
            name = "Alice",
            userName = "alice",
            profileId = "p-alice",
            createdAt = "2026-09-07T10:00:00Z"
        )
        val viewModel = DetailViewModel(
            isGuestUserProvider = { false },
            isOwnProfileProvider = { false },
            haveIVotedProvider = { _, _ -> VotingResolution(hasVoted = true, voteCount = 5) },
            listCommentsProvider = { _, _, _ ->
                PaginatedResponse(
                    documents = listOf(sampleComment),
                    total = 1,
                    page = 1,
                    documentsPerPage = 3,
                    totalPages = 1,
                    hasNext = false,
                    hasPrev = false
                )
            }
        )

        viewModel.initialize(sampleReport)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isGuest)
        assertFalse(state.isOwnProfile)
        assertTrue(state.disableBoostButton)
        assertEquals(5, state.voteCount)
        assertEquals(1, state.latestComments.size)
        assertEquals("Affects daily commute", state.latestComments.first().message)
    }

    @Test
    fun `initialize for guest user does not fetch latest comments and blocks comment subscreen`() = runTest(testDispatcher) {
        var commentsCalled = false
        val viewModel = DetailViewModel(
            isGuestUserProvider = { true },
            isOwnProfileProvider = { false },
            listCommentsProvider = { _, _, _ ->
                commentsCalled = true
                PaginatedResponse(
                    documents = emptyList(),
                    total = 0,
                    page = 1,
                    documentsPerPage = 3,
                    totalPages = 0,
                    hasNext = false,
                    hasPrev = false
                )
            }
        )

        viewModel.initialize(sampleReport)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isGuest)
        assertFalse(commentsCalled)
        assertTrue(viewModel.state.value.latestComments.isEmpty())

        // Guest user cannot navigate to COMMENTS sub-screen
        viewModel.openSubScreen(DetailSubScreen.COMMENTS)
        assertEquals(DetailSubScreen.NONE, viewModel.state.value.activeSubScreen)

        // Guest user can still navigate to FOLLOW_UP, CITIZEN_PROFILE, and EVIDENCES
        viewModel.openSubScreen(DetailSubScreen.FOLLOW_UP)
        assertEquals(DetailSubScreen.FOLLOW_UP, viewModel.state.value.activeSubScreen)

        viewModel.openCitizenProfile("profile-123")
        assertEquals(DetailSubScreen.CITIZEN_PROFILE, viewModel.state.value.activeSubScreen)
        assertEquals("profile-123", viewModel.state.value.selectedProfileId)
    }

    @Test
    fun `submitBoostVote increments voteCount and disables boost button to prevent spam`() = runTest(testDispatcher) {
        val viewModel = DetailViewModel(
            isGuestUserProvider = { false },
            isOwnProfileProvider = { false },
            haveIVotedProvider = { _, _ -> VotingResolution(hasVoted = false, voteCount = 2) },
            voteProvider = { type, payload ->
                assertEquals(VotingType.REPORT, type)
                assertEquals(sampleReport.id, payload.resourceId)
                SuccessfulResult.DONE
            }
        )

        viewModel.initialize(sampleReport)
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.voteCount)
        assertFalse(viewModel.state.value.disableBoostButton)

        viewModel.submitBoostVote(sampleReport.id)
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.voteCount)
        assertTrue(viewModel.state.value.disableBoostButton)
        assertTrue(viewModel.state.value.showBoostAlert)
        assertEquals("Boost applied!", viewModel.state.value.message)
    }

    @Test
    fun `reportContentViolation sends moderation request and shows success alert`() = runTest(testDispatcher) {
        var capturedViolation: ReportViolation<MapExplorerReport>? = null
        val viewModel = DetailViewModel(
            isGuestUserProvider = { false },
            isOwnProfileProvider = { false },
            moderateReportProvider = { violation, type ->
                capturedViolation = violation
                assertEquals(TypeOfContentToReport.REPORT, type)
                GenericResponse(id = "MOD_1", message = "ok", code = "OK")
            }
        )

        viewModel.selectReportOption(DetailReportOptions.WRONG_INFORMATION.description)
        assertTrue(viewModel.state.value.showConfirmReportAlert)

        viewModel.reportContentViolation(sampleReport)
        advanceUntilIdle()

        assertNotNull(capturedViolation)
        assertEquals(DetailReportOptions.WRONG_INFORMATION.id, capturedViolation?.blockedReasonId)
        assertEquals("Wrong Information", capturedViolation?.reason)
        assertEquals(ReportViolationStatus.SENT_TO_MODERATION, capturedViolation?.status)
        assertTrue(viewModel.state.value.showSuccessfulAlert)
        assertEquals("Your moderation petition was sent", viewModel.state.value.message)
    }

    @Test
    fun `reportContentViolation handles ServerError and shows error alert`() = runTest(testDispatcher) {
        val viewModel = DetailViewModel(
            moderateReportProvider = { _, _ ->
                throw CommonIntercommunicationErrors.ServerError("500")
            }
        )

        viewModel.selectReportOption(DetailReportOptions.SPAM.description)
        viewModel.reportContentViolation(sampleReport)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showErrorAlert)
        assertEquals("Server Error", viewModel.state.value.errorMessage)
    }
}
