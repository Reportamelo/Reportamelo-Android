package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.BlockReasons
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.models.BlockUserReason
import app.reportamelo.models.User
import app.reportamelo.repository.UserRepository
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
class CitizenProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchCitizenPublicProfile falls back to Guest citizen and @guest when hideProfile is true`() = runTest(testDispatcher) {
        val viewModel = CitizenProfileViewModel(
            citizenProfileProvider = { id ->
                User(
                    names = "Private User",
                    userName = "private.user",
                    profilePicture = "/avatars/private.png",
                    profileId = id,
                    hideProfile = true
                )
            }
        )
        viewModel.fetchCitizenPublicProfile("profile-hidden-1")
        advanceUntilIdle()

        val citizen = viewModel.state.value.citizen
        assertEquals("Guest citizen", citizen.names)
        assertEquals("guest", citizen.userName)
        assertTrue(citizen.hideProfile)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `fetchCitizenPublicProfile falls back to Guest citizen and @guest when user is deleted or NotFound`() = runTest(testDispatcher) {
        val viewModel = CitizenProfileViewModel(
            citizenProfileProvider = {
                throw CommonIntercommunicationErrors.NotFound
            }
        )
        viewModel.fetchCitizenPublicProfile("deleted-profile-99")
        advanceUntilIdle()

        val citizen = viewModel.state.value.citizen
        assertEquals("Guest citizen", citizen.names)
        assertEquals("guest", citizen.userName)
        assertTrue(citizen.hideProfile)
    }

    @Test
    fun `isBlockReasonValid requires at least 12 characters`() {
        val viewModel = CitizenProfileViewModel()
        assertFalse(viewModel.isBlockReasonValid(""))
        assertFalse(viewModel.isBlockReasonValid("too short"))
        assertFalse(viewModel.isBlockReasonValid("11 chars!!!"))
        assertTrue(viewModel.isBlockReasonValid("12 chars now"))
        assertTrue(viewModel.isBlockReasonValid("This user is spamming reports repeatedly"))
    }

    @Test
    fun `blockUser sends moderation and block request when reason has at least 12 characters`() = runTest(testDispatcher) {
        var blockedPayload: BlockUserReason? = null
        var moderationCalled = false

        val viewModel = CitizenProfileViewModel(
            blockUserProvider = { data ->
                blockedPayload = data
            },
            persistBlockedUserProvider = {},
            moderateAccountProvider = { _, _ ->
                moderationCalled = true
                Unit
            }
        )

        viewModel.blockUser(
            profileId = "offender-123",
            reasonText = "Repeatedly posting fake community issues",
            selectedReason = BlockReasons.WRONG_INFORMATION
        )
        advanceUntilIdle()

        assertTrue(moderationCalled)
        assertNotNull(blockedPayload)
        assertEquals("offender-123", blockedPayload?.profileId)
        assertEquals(BlockReasons.WRONG_INFORMATION.id, blockedPayload?.blockedReasonId)
        assertEquals("Repeatedly posting fake community issues", blockedPayload?.reason)
        assertTrue(viewModel.state.value.showBlockResultAlert)
        assertTrue(viewModel.state.value.blockSucceeded)
    }
}
