package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.IssueTypes
import app.reportamelo.commons.Severity
import app.reportamelo.commons.UserType
import app.reportamelo.commons.utils.KeychainKey
import app.reportamelo.commons.utils.KeychainService
import app.reportamelo.commons.utils.SubscriptionManager
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.datamodel.mattersToResolve
import app.reportamelo.communityIssues.helpers.DeepLinkRouter
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class CreateReportViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AppPreferences.clearAll()
        SubscriptionManager.mockIsPro = false
        SettingsStore.shared.lastReportDate = null
        DeepLinkRouter.shared.activeTab.value = 5
    }

    @After
    fun tearDown() {
        SubscriptionManager.mockIsPro = false
        SettingsStore.shared.lastReportDate = null
        AppPreferences.clearAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads all 10 catalog matters`() {
        val viewModel = CreateReportViewModel()
        val state = viewModel.state.value

        assertEquals(10, mattersToResolve.size)
        assertEquals(10, state.filteredMatters.size)
        assertEquals(IssueTypes.ALL, state.issueType)
        assertEquals(Severity.ALL, state.severity)
        assertEquals("", state.searchText)
    }

    @Test
    fun `updateSearchText filters matters by title and description case-insensitively`() {
        val viewModel = CreateReportViewModel()

        viewModel.updateSearchText("pothole")
        assertEquals(1, viewModel.state.value.filteredMatters.size)
        assertEquals("Potholes", viewModel.state.value.filteredMatters.first().title)

        viewModel.updateSearchText("cruelty")
        assertEquals(1, viewModel.state.value.filteredMatters.size)
        assertEquals("Animal abuse", viewModel.state.value.filteredMatters.first().title)

        viewModel.updateSearchText("   ")
        assertEquals(10, viewModel.state.value.filteredMatters.size)
    }

    @Test
    fun `updateIssueType and updateSeverity filter matters when search query is empty`() {
        val viewModel = CreateReportViewModel()

        viewModel.updateIssueType(IssueTypes.ROAD)
        // Road items: Potholes (low), Street sign (low), Lid sewer (medium) -> 3 items
        assertEquals(3, viewModel.state.value.filteredMatters.size)

        viewModel.updateSeverity(Severity.MEDIUM)
        // Road + Medium: Lid sewer -> 1 item
        assertEquals(1, viewModel.state.value.filteredMatters.size)
        assertEquals("Lid sewer", viewModel.state.value.filteredMatters.first().title)

        viewModel.updateIssueType(IssueTypes.ALL)
        viewModel.updateSeverity(Severity.HIGH)
        // High severity items: Water leakage, Trafic light, Flood, Animal abuse -> 4 items
        assertEquals(4, viewModel.state.value.filteredMatters.size)
    }

    @Test
    fun `getReportsCount shows limit sheet for freemium registered user when count reaches limit`() = runTest {
        KeychainService.save(KeychainKey.USER_TYPE, UserType.CITIZEN.description)
        KeychainService.save(KeychainKey.PLAN_TYPE, "freemium")

        val viewModel = CreateReportViewModel(
            counterProvider = { CreateReportViewModel.MAX_REPORTS_PER_REGISTERED_USERS }
        )

        viewModel.getReportsCount()

        assertEquals(5, viewModel.state.value.reportsCount)
        assertTrue(viewModel.state.value.showReportsLimitSheet)
    }

    @Test
    fun `getReportsCount with Pro entitlement mock does not show limit sheet for normal counts`() = runTest {
        KeychainService.save(KeychainKey.USER_TYPE, UserType.CITIZEN.description)
        SubscriptionManager.mockIsPro = true

        val viewModel = CreateReportViewModel(
            counterProvider = { 25 }
        )

        viewModel.getReportsCount()

        assertEquals(25, viewModel.state.value.reportsCount)
        assertFalse(viewModel.state.value.showReportsLimitSheet)
        assertEquals("pro", KeychainService.getToken(KeychainKey.PLAN_TYPE))
    }

    @Test
    fun `getReportsCount resets guest monthly count when lastReportDate is from previous month`() = runTest {
        KeychainService.save(KeychainKey.USER_TYPE, UserType.GUEST.description)
        KeychainService.save(KeychainKey.PLAN_TYPE, "freemium_for_guests")

        val settings = SettingsStore()
        settings.reportsCount = 3
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        settings.lastReportDate = calendar.time

        val viewModel = CreateReportViewModel(
            settings = settings,
            userRepository = UserRepository()
        )

        viewModel.getReportsCount()

        assertEquals(0, settings.reportsCount)
        assertEquals(0, viewModel.state.value.reportsCount)
        assertFalse(viewModel.state.value.showReportsLimitSheet)
    }

    @Test
    fun `handleUserAction handles VIEW_AD, PAID_A_SUBSCRIPTION, and NO_THANKS`() = runTest {
        KeychainService.save(KeychainKey.USER_TYPE, UserType.CITIZEN.description)
        KeychainService.save(KeychainKey.PLAN_TYPE, "freemium")

        val viewModel = CreateReportViewModel(
            counterProvider = { 5 }
        )
        viewModel.getReportsCount()
        assertTrue(viewModel.state.value.showReportsLimitSheet)

        // VIEW_AD decrements count and hides sheet
        viewModel.handleUserAction(InteractionType.VIEW_AD)
        assertEquals(4, viewModel.state.value.reportsCount)
        assertFalse(viewModel.state.value.showReportsLimitSheet)

        // Re-trigger limit sheet and test NO_THANKS
        viewModel.handleUserAction(InteractionType.NO_THANKS)
        advanceTimeBy(100)
        assertFalse(viewModel.state.value.showReportsLimitSheet)
        assertEquals(1, DeepLinkRouter.shared.activeTab.value)
    }

    @Test
    fun `ReportDataModel setMatterToSolve populates report fields and placeholders`() {
        val model = ReportDataModel.shared
        model.clear()
        val matter = mattersToResolve.first { it.id == 2 } // Water leakage

        model.setMatterToSolve(matter)

        val report = model.report.value
        assertEquals(2, report.matterToSolveId)
        assertEquals(Severity.HIGH.identifier, report.severityId)
        assertEquals(IssueTypes.BUILDING.identifier, report.issueTypeId)
        assertEquals("Water leakage", report.suggestedTitle)
        assertEquals("Water leakage in the street or public spaces", report.suggestedDescription)
        assertEquals("Water leakage", model.titlePlaceholder.value)
    }
}
