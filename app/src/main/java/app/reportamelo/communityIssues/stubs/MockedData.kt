package app.reportamelo.communityIssues.stubs

import app.reportamelo.commons.AttachmentType
import app.reportamelo.commons.AttachmentValidatedBy
import app.reportamelo.commons.ReportAttachmentState
import app.reportamelo.models.Coordinate
import app.reportamelo.models.FriendlyCityDistribution
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.models.PreviewAttachment

// MARK: - Mocked data for testing purposes
class MapExplorerMockedData private constructor() {

    var report: MapExplorerReport = MapExplorerReport(
        id = "SV-SS-260601-aXWsaxls",
        lat = 13.701270,
        lng = -89.224432,
        address = "Lorem ipsum dolor sit ammet",
        title = "A big pothole in the middle of the street",
        description = "There is a big pothole that is affecting our cars",
        severityId = 1,
        statusId = 1,
        issueTypeId = 1,
        matterToSolveId = 1,
        reportedAtRaw = null,
        cellIndex = "",
        createdAtRaw = 1780036575602L,
        updatedAtRaw = 1780036575602L,
        reportedBy = "aXWsaxls",
        userName = "jhon.doe",
        cityId = "",
        petitionId = "",
        shareUrl = "",
        attachments = listOf(
            PreviewAttachment(
                id = "24b93d66-07ff-4141-91ce-408b615123c3",
                type = AttachmentType.IMAGE,
                createdAtRaw = 0L,
                updatedAtRaw = 0L,
                uploaderUserName = "jhon.doe",
                validatedBy = AttachmentValidatedBy.BOT,
                state = ReportAttachmentState.PENDING,
                fileName = "1783058838224-f02fb5e4-07d1-49d4-a9f5-742816b669c9.webp",
                reportContainer = "587d3ac3-0715-4958-8955-1d6d29a3d489"
            ),
            PreviewAttachment(
                id = "06d3df5f-ef2a-43c0-bd40-5125a12fe284",
                type = AttachmentType.IMAGE,
                createdAtRaw = 0L,
                updatedAtRaw = 0L,
                uploaderUserName = "jhon.doe",
                validatedBy = AttachmentValidatedBy.BOT,
                state = ReportAttachmentState.PENDING,
                fileName = "1783058837989-ab7a054b-7d99-4137-b9f9-ded39ce7732f.webp",
                reportContainer = "587d3ac3-0715-4958-8955-1d6d29a3d489"
            )
        ),
        assignedTo = null,
        institutionId = null,
        reportContainer = "",
        profileId = "06d3df5f06d3df5f"
    )

    companion object {
        val shared: MapExplorerMockedData by lazy { MapExplorerMockedData() }
    }
}

class SelectedMockedCity private constructor() {

    var city: FriendlyCityDistribution = FriendlyCityDistribution(
        cityId = "a67b90f9-1d76-4835-a994-03cd04f1d619",
        firstLevel = "El Salvador",
        secondLevel = "San Salvador",
        thirdLevel = "San Salvador",
        ZipCode = "1101",
        legalGroupName = "Distrito de San Salvador",
        coordinates = Coordinate(lat = 13.6979857, lng = -89.1918083),
        isCapitalCity = 1,
        isDepartmentalCapital = 1
    )

    companion object {
        val shared: SelectedMockedCity by lazy { SelectedMockedCity() }
    }
}
