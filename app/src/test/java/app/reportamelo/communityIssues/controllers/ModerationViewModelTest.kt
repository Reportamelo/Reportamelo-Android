package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.ReportViolationStatus
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.communityIssues.ui.components.ToastType
import app.reportamelo.communityIssues.ui.screens.subviews.disableButtonByStatus
import app.reportamelo.communityIssues.ui.screens.subviews.setOpacityByStatus
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.ModeratedContent
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.models.ReportViolation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModerationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleViolation(
        id: String = "v-123",
        type: TypeOfContentToReport = TypeOfContentToReport.COMMENT,
        status: ReportViolationStatus = ReportViolationStatus.PENDING,
        profileId: String = "prof-1"
    ): ReportViolation<ModeratedContent> = ReportViolation(
        id = id,
        type = type,
        content = ModeratedContent(id = "mc-1", message = "Bad comment"),
        contentAuthorProfileId = profileId,
        reason = "Harassment or bullying",
        blockedReasonId = "reason-1",
        status = status,
        observation = "Under review",
        createdAt = "2026-10-27T12:00:00Z",
        updatedAt = "2026-10-27T12:00:00Z"
    )

    @Test
    fun `loadComplaints populates myComplaints and resets isLoading`() {
        val sample = sampleViolation()
        val viewModel = ModerationViewModel(
            myComplaintsProvider = {
                PaginatedResponse(
                    documents = listOf(sample),
                    total = 1,
                    page = 1,
                    documentsPerPage = 10,
                    totalPages = 1
                )
            }
        )

        viewModel.loadComplaints()

        assertFalse(viewModel.isLoading.value)
        assertEquals(1, viewModel.myComplaints.value.size)
        assertEquals("v-123", viewModel.myComplaints.value.first().id)
    }

    @Test
    fun `loadIndictedData skips call when profileId is empty and loads when present`() {
        var callCount = 0
        val sample = sampleViolation(id = "v-456")

        val emptyProfileVm = ModerationViewModel(
            profileIdProvider = { "" },
            indictedModeratedContentProvider = {
                callCount++
                PaginatedResponse(documents = listOf(sample))
            }
        )
        emptyProfileVm.loadIndictedData()
        assertEquals(0, callCount)
        assertTrue(emptyProfileVm.indictedModeratedContent.value.isEmpty())

        val validProfileVm = ModerationViewModel(
            profileIdProvider = { "prof-1" },
            indictedModeratedContentProvider = {
                callCount++
                PaginatedResponse(documents = listOf(sample))
            }
        )
        validProfileVm.loadIndictedData()
        assertEquals(1, callCount)
        assertEquals(1, validProfileVm.indictedModeratedContent.value.size)
        assertEquals("v-456", validProfileVm.indictedModeratedContent.value.first().id)
    }

    @Test
    fun `appealForReportRemoval emits success toast on completion`() {
        var appealedId: String? = null
        var appealedType: TypeOfContentToReport? = null

        val viewModel = ModerationViewModel(
            appealProvider = { id, type ->
                appealedId = id
                appealedType = type
                GenericResponse(message = "ok")
            }
        )

        viewModel.prepareForAppeal("rep-1", TypeOfContentToReport.COMMENT, "prof-1")
        assertTrue(viewModel.showAlert.value)
        assertEquals("rep-1", viewModel.selectedReportId.value)
        assertEquals(TypeOfContentToReport.COMMENT, viewModel.selectedReportType.value)

        viewModel.appealForReportRemoval("rep-1", TypeOfContentToReport.COMMENT)
        assertEquals("rep-1", appealedId)
        assertEquals(TypeOfContentToReport.COMMENT, appealedType)
        assertEquals(
            ModerationToastMessage("Your appeal has been sent", ToastType.Success),
            viewModel.toastMessage.value
        )

        viewModel.clearToastMessage()
        assertNull(viewModel.toastMessage.value)
    }

    @Test
    fun `remove and unlock update confirmation or error flags appropriately`() {
        var unlockedProfile: String? = null
        var removedFromKeychain: String? = null
        var removedReport: Pair<String, TypeOfContentToReport>? = null

        val viewModel = ModerationViewModel(
            unlockProvider = { profileId -> unlockedProfile = profileId },
            removeBlockedUserFromKeychainProvider = { profileId -> removedFromKeychain = profileId },
            removeProvider = { id, type ->
                removedReport = id to type
                GenericResponse(message = "removed")
            }
        )

        viewModel.unlock("prof-99", "rep-99")
        assertEquals("prof-99", unlockedProfile)
        assertEquals("prof-99", removedFromKeychain)
        assertEquals("rep-99" to TypeOfContentToReport.ACCOUNT, removedReport)
        assertTrue(viewModel.showConfirmationOfRemoval.value)

        viewModel.dismissConfirmationOfRemoval()
        assertFalse(viewModel.showConfirmationOfRemoval.value)

        viewModel.remove("rep-100", TypeOfContentToReport.REPORT)
        assertEquals("rep-100" to TypeOfContentToReport.REPORT, removedReport)
        assertTrue(viewModel.showConfirmationOfRemoval.value)
    }

    @Test
    fun `network error during loadComplaints emits error toast`() {
        val viewModel = ModerationViewModel(
            myComplaintsProvider = {
                throw CommonIntercommunicationErrors.NetworkError("Server Error")
            }
        )

        viewModel.loadComplaints()

        assertFalse(viewModel.isLoading.value)
        assertEquals(
            ModerationToastMessage("Server Error", ToastType.Error),
            viewModel.toastMessage.value
        )
    }

    @Test
    fun `setOpacityByStatus and disableButtonByStatus match iOS behavior`() {
        assertEquals(0.3f, setOpacityByStatus(ReportViolationStatus.APPEALING), 0.001f)
        assertEquals(0.3f, setOpacityByStatus(ReportViolationStatus.REJECTED), 0.001f)
        assertEquals(0.3f, setOpacityByStatus(ReportViolationStatus.APPROVED), 0.001f)
        assertEquals(1.0f, setOpacityByStatus(ReportViolationStatus.PENDING), 0.001f)
        assertEquals(1.0f, setOpacityByStatus(ReportViolationStatus.SENT_TO_MODERATION), 0.001f)

        assertTrue(disableButtonByStatus(ReportViolationStatus.APPEALING))
        assertTrue(disableButtonByStatus(ReportViolationStatus.REJECTED))
        assertTrue(disableButtonByStatus(ReportViolationStatus.APPROVED))
        assertFalse(disableButtonByStatus(ReportViolationStatus.PENDING))
        assertFalse(disableButtonByStatus(ReportViolationStatus.SENT_TO_MODERATION))
    }
}
