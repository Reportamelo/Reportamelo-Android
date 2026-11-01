package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.communityIssues.datamodel.LocatorDAO
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.datamodel.ReportStates
import app.reportamelo.communityIssues.datamodel.mattersToResolve
import app.reportamelo.communityIssues.datamodel.toDao
import app.reportamelo.communityIssues.ui.components.wizard.ReportStep
import app.reportamelo.models.Coordinate
import app.reportamelo.models.CustomizedResponse
import app.reportamelo.models.ReportAttachmentGrouping
import app.reportamelo.models.ReportSessionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val model = ReportDataModel.shared

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        model.clear()
    }

    @After
    fun tearDown() {
        model.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `ReportController step navigation and button messages match iOS wizard`() {
        val controller = ReportController()

        assertEquals(ReportStep.LOCATION, controller.currentStep.value)
        assertEquals("Next", controller.getButtonMessage())
        assertEquals("Create a Report", controller.getNavigationTitle(ReportStates.NEW))
        assertEquals("Modifying a Report", controller.getNavigationTitle(ReportStates.MODIFYING))

        controller.goNext()
        assertEquals(ReportStep.MEDIA, controller.currentStep.value)
        assertEquals("Next", controller.getButtonMessage())

        controller.goNext()
        assertEquals(ReportStep.DETAILS, controller.currentStep.value)
        assertEquals("Submit", controller.getButtonMessage())

        controller.goNext()
        assertEquals(ReportStep.CONFIRMATION, controller.currentStep.value)

        // Going back from Details to Location via setStep
        controller.setStep(ReportStep.LOCATION)
        assertEquals(ReportStep.LOCATION, controller.currentStep.value)
    }

    @Test
    fun `ReportDataModel location update sets address, locator, and validates address`() {
        assertFalse(model.isDifferentLocation.value)
        assertFalse(model.isAddressValid.value)

        val newCoord = Coordinate(13.7012, -89.2245)
        val newLocator = LocatorDAO.defaultLocator(
            address = "Alameda Manuel Enrique Araujo, San Salvador, El Salvador",
            lat = newCoord.lat,
            lng = newCoord.lng
        )

        model.updateCoordinate(newCoord)
        model.updateLocator(newLocator)
        model.setDifferentLocation(true)

        assertEquals(newCoord, model.report.value.coordinate)
        assertEquals("Alameda Manuel Enrique Araujo, San Salvador, El Salvador", model.report.value.address)
        assertTrue(model.isDifferentLocation.value)
        assertTrue(model.isAddressValid.value)
    }

    @Test
    fun `LocatorDAO sanitizeAddress strips leading Plus Code and disallowed symbols`() {
        val raw = "PRMF+MPP, C. Principal a Cdad. Delgado, San Salvador, El Salvador "
        val sanitized = LocatorDAO.sanitizeAddress(raw)
        assertEquals("C. Principal a Cdad. Delgado, San Salvador, El Salvador", sanitized)
        assertFalse(sanitized.contains("+"))
    }

    @Test
    fun `ReportDataModel title and description updates enforce validators`() {
        // Too short title (< 5 chars)
        model.updateTitle("Hole")
        assertFalse(model.isTitleValid.value)

        // Valid title (5..40 chars)
        model.updateTitle("Large pothole on main avenue")
        assertTrue(model.isTitleValid.value)

        // Too short description (< 5 chars)
        model.updateDescription("Tiny")
        assertFalse(model.isDescriptionValid.value)

        // Valid description (5..150 chars)
        model.updateDescription("There is a deep pothole blocking the right lane near the intersection.")
        assertTrue(model.isDescriptionValid.value)
    }

    @Test
    fun `ReportDataModel uploadTrackers management adds and removes trackers`() {
        val tracker1 = PhotoUploadTracker(nameParam = "photo1.webp")
        val tracker2 = PhotoUploadTracker(nameParam = "photo2.webp")

        model.addUploadTrackers(listOf(tracker1, tracker2))
        assertEquals(2, model.uploadTrackers.value.size)

        tracker1.setPhase(ImagePhase.SUCCESS)
        tracker1.setKey("uploads/photo1.webp")
        assertEquals(ImagePhase.SUCCESS, model.uploadTrackers.value.first().phase.value)

        model.removeUploadTracker(tracker1.id)
        assertEquals(1, model.uploadTrackers.value.size)
        assertEquals("photo2.webp", model.uploadTrackers.value.first().name.value)
    }

    @Test
    fun `ReportDataModel buildReportId formats country, grouping code, date, and hash`() {
        model.updateReportSession(
            ReportSessionResponse(
                reportContainer = "container-123",
                shareIndexHash = "k9Lm2PqR",
                reportCreationOn = "261027"
            )
        )

        val generatedId = model.buildReportId()
        assertEquals("SV-SSC-261027-k9Lm2PqR", generatedId)
    }

    @Test
    fun `MiniMapLocator and MapPickerScreen synchronize coordinate and immediately enable Next button`() {
        assertFalse(model.isDifferentLocation.value && model.locator.value.cityId.isNotEmpty())

        val miniMapCoord = Coordinate(13.6985, -89.1910)
        model.updateCoordinate(miniMapCoord)
        model.setDifferentLocation(true)

        assertTrue(model.isDifferentLocation.value && model.locator.value.cityId.isNotEmpty())
        assertEquals(miniMapCoord, model.report.value.coordinate)

        val pickerCoord = Coordinate(13.7052, -89.2123)
        val pickerLocator = LocatorDAO.defaultLocator(
            address = "Colonia Escalón, San Salvador, El Salvador",
            lat = pickerCoord.lat,
            lng = pickerCoord.lng
        )
        model.updateCoordinate(pickerCoord)
        model.updateLocator(pickerLocator)
        model.setDifferentLocation(true)

        assertEquals(pickerCoord, model.report.value.coordinate)
        assertEquals(pickerCoord.lat, model.locator.value.lat, 0.00001)
        assertEquals(pickerCoord.lng, model.locator.value.lng, 0.00001)
        assertEquals("Colonia Escalón, San Salvador, El Salvador", model.report.value.address)
        assertTrue(model.isAddressValid.value)
    }

    @Test
    fun `Report toDao maps coordinate, address, title, description, reportState, and attachmentRequests`() {
        val coord = Coordinate(13.733762, -89.175536)
        model.setMatterToSolve(mattersToResolve.first { it.id == 1 })
        model.updateReportSession(
            ReportSessionResponse(
                reportContainer = "8db4e8f8-d9a1-48d3-a660-03a5ebdd63f3",
                shareIndexHash = "WzmQ0onUwYWgH8yC",
                reportCreationOn = "261031"
            )
        )
        model.updateCoordinate(coord)
        model.updateLocator(
            LocatorDAO.defaultLocator(
                address = "PRMF+MPP, C. Principal a Cdad. Delgado, San Salvador, El Salvador",
                lat = coord.lat,
                lng = coord.lng
            )
        )
        model.updateTitle("una cocoa")
        model.updateDescription("en mi casa")

        val tracker = PhotoUploadTracker(nameParam = "photo1.webp").apply {
            setKey("uploads/photo1.webp")
        }
        model.addAttachments(listOf(tracker))
        model.setAttachmentId(
            listOf(
                ReportAttachmentGrouping(
                    attachmentContainer = "8db4e8f8-d9a1-48d3-a660-03a5ebdd63f3",
                    attachmentId = "01a18fd0-7647-74ae-8659-5b28e23644c2",
                    key = "uploads/photo1.webp"
                )
            )
        )

        val dao = model.report.value.toDao()
        assertEquals(coord, dao.coordinate)
        assertEquals("C. Principal a Cdad. Delgado, San Salvador, El Salvador", dao.address)
        assertEquals("una cocoa", dao.title)
        assertEquals("en mi casa", dao.description)
        assertEquals("new", dao.reportState)
        assertEquals(1, dao.attachmentRequests.size)
        assertEquals("01a18fd0-7647-74ae-8659-5b28e23644c2", dao.attachmentRequests.first().attachmentId)
        assertEquals("8db4e8f8-d9a1-48d3-a660-03a5ebdd63f3", dao.attachmentRequests.first().reportContainer)
    }

    @Test
    fun `submitReport catches InvalidPetition without crashing and presents alert`() = runTest {
        var onCompleteCalled = false
        val controller = ReportController(
            submitGroupedAttachmentsProvider = {
                CustomizedResponse(
                    data = listOf(
                        ReportAttachmentGrouping(
                            attachmentContainer = "8db4e8f8-d9a1-48d3-a660-03a5ebdd63f3",
                            attachmentId = "01a18fd0-7647-74ae-8659-5b28e23644c2",
                            key = "uploads/photo1.webp"
                        )
                    ),
                    message = "OK",
                    code = "200"
                )
            },
            createReportProvider = { _, _, _, _, _, _, _ ->
                throw CommonIntercommunicationErrors.InvalidPetition("ERROR_INVALID_REQUEST")
            },
            createShareableLinkProvider = {
                "https://reportamelo.app/s/WzmQ0onUwYWgH8yC"
            },
            isGuestUserProvider = { true }
        )

        val tracker = PhotoUploadTracker(nameParam = "photo1.webp").apply {
            setKey("uploads/photo1.webp")
        }
        controller.setStep(ReportStep.DETAILS)
        controller.submitReport(model, listOf(tracker)) {
            onCompleteCalled = true
        }

        assertFalse(onCompleteCalled)
        assertFalse(controller.isLoading.value)
        assertTrue(controller.presentAlert.value)
        assertEquals("ERROR_INVALID_REQUEST", controller.alertMessage.value)
        assertEquals(ReportStep.DETAILS, controller.currentStep.value)
    }

    @Test
    fun `submitReport catches ServerError and shows friendly retry alert`() = runTest {
        var onCompleteCalled = false
        val controller = ReportController(
            submitGroupedAttachmentsProvider = {
                throw CommonIntercommunicationErrors.ServerError("HTTP 500")
            },
            createShareableLinkProvider = {
                "https://reportamelo.app/s/WzmQ0onUwYWgH8yC"
            },
            isGuestUserProvider = { true }
        )

        val tracker = PhotoUploadTracker(nameParam = "photo1.webp").apply {
            setKey("uploads/photo1.webp")
        }
        controller.submitReport(model, listOf(tracker)) {
            onCompleteCalled = true
        }

        assertFalse(onCompleteCalled)
        assertFalse(controller.isLoading.value)
        assertTrue(controller.presentAlert.value)
        assertEquals("Something went wrong, please try again later", controller.alertMessage.value)
    }

    @Test
    fun `submitReport happy path creates report with grouped attachments and share link`() = runTest {
        var capturedReportDao: app.reportamelo.models.Report? = null
        var capturedCityId: String? = null
        var capturedCountryCode: String? = null
        var counterIncreased = false
        var onCompleteCalled = false

        model.updateReportSession(
            ReportSessionResponse(
                reportContainer = "8db4e8f8-d9a1-48d3-a660-03a5ebdd63f3",
                shareIndexHash = "WzmQ0onUwYWgH8yC",
                reportCreationOn = "261031"
            )
        )
        model.updateTitle("Pothole on avenue")
        model.updateDescription("Deep pothole in the middle lane")
        model.updateLocator(
            LocatorDAO.defaultLocator(
                address = "Paseo General Escalón, San Salvador",
                lat = 13.7012,
                lng = -89.2245
            )
        )

        val controller = ReportController(
            submitGroupedAttachmentsProvider = {
                CustomizedResponse(
                    data = listOf(
                        ReportAttachmentGrouping(
                            attachmentContainer = "8db4e8f8-d9a1-48d3-a660-03a5ebdd63f3",
                            attachmentId = "01a18fd0-7647-74ae-8659-5b28e23644c2",
                            key = "uploads/photo1.webp"
                        )
                    ),
                    message = "OK",
                    code = "200"
                )
            },
            createReportProvider = { report, countryCode, cityId, _, _, _, _ ->
                capturedReportDao = report
                capturedCountryCode = countryCode
                capturedCityId = cityId
                "SV-SSC-261031-WzmQ0onUwYWgH8yC"
            },
            createShareableLinkProvider = {
                "https://reportamelo.app/s/WzmQ0onUwYWgH8yC"
            },
            isGuestUserProvider = { false },
            increaseCounterProvider = {
                counterIncreased = true
            }
        )

        val tracker = PhotoUploadTracker(nameParam = "photo1.webp").apply {
            setKey("uploads/photo1.webp")
        }
        controller.submitReport(model, listOf(tracker)) {
            onCompleteCalled = true
        }

        assertTrue(onCompleteCalled)
        assertTrue(counterIncreased)
        assertFalse(controller.presentAlert.value)
        assertFalse(controller.isLoading.value)
        assertEquals("SV-SSC-261031-WzmQ0onUwYWgH8yC", controller.reportId.value)
        assertEquals("https://reportamelo.app/s/WzmQ0onUwYWgH8yC", controller.shareableLink.value)
        assertEquals("SV", capturedCountryCode)
        assertEquals("a67b90f9-1d76-4835-a994-03cd04f1d619", capturedCityId)
        assertNotNull(capturedReportDao)
        assertEquals("Pothole on avenue", capturedReportDao?.title)
        assertEquals("Deep pothole in the middle lane", capturedReportDao?.description)
        assertEquals("Paseo General Escalón, San Salvador", capturedReportDao?.address)
        assertEquals("01a18fd0-7647-74ae-8659-5b28e23644c2", capturedReportDao?.attachmentRequests?.firstOrNull()?.attachmentId)
        assertTrue(model.report.value.attachments.isEmpty())
    }
}
