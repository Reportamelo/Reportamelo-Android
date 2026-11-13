package app.reportamelo.communityIssues.datamodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.PhotoUploadTracker
import app.reportamelo.models.*
import app.reportamelo.commons.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Date
import app.reportamelo.communityIssues.helpers.localized

// Stubs for missing models

enum class ReportStates(val rawValue: String) {
    NEW("new"),
    MODIFYING("modifying");

    companion object {
        fun fromRawValue(value: String?): ReportStates {
            return entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: NEW
        }
    }
}


data class Locator(
    var countryCode: String,
    var firstLevel: String,
    var secondLevel: String,
    var thirdLevel: String,
    var groupingId: String,
    var cityId: String,
    var groupingName: String,
    var groupingNameCode: String,
    var lat: Double,
    var lng: Double,
    var geoCode: String,
    var zipCode: String,
    var isCapitalCityRaw: Int,
    var isDepartmentalCapitalRaw: Int,
    var cityNameSortKey: String,
    var address: String
)


enum class ResponsibleEntity(val rawValue: String) {
    MUNICIPALITY("Alcaldía Municipal"),
    MOP_FOVIAL("MOP / FOVIAL"),
    ANDA("ANDA"),
    VMT("VMT"),
    CIVIL_PROTECTION("Protección Civil"),
    SHARED("Alcaldía / MOP")
}

data class MatterSeverity(val identifier: Int)
data class MatterIssueType(val identifier: Int)

data class MatterToSolve(
    val id: Int,
    val title: String,
    val icon: String? = null,
    val description: String,
    val issueType: IssueTypes,
    val severity: Severity,
    val image: String? = null,
    @DrawableRes val imageRes: Int? = null,
    val suggestions: List<String>? = null,
    val primaryEntity: ResponsibleEntity = ResponsibleEntity.MUNICIPALITY
) {
    constructor(
        id: Int,
        severity: MatterSeverity,
        issueType: MatterIssueType,
        title: String,
        description: String
    ) : this(
        id = id,
        title = title,
        description = description,
        issueType = IssueTypes.fromIdentifier(issueType.identifier),
        severity = Severity.fromIdentifier(severity.identifier)
    )
}

val mattersToResolve: List<MatterToSolve> = listOf(
    MatterToSolve(
        id = 1,
        title = localized(R.string.potholes, "Potholes"),
        description = localized(R.string.dangerous_potholes_that_cause_problems, "dangerous potholes that cause problems"),
        issueType = IssueTypes.ROAD,
        severity = Severity.LOW,
        image = "matt-hoffman-MQjJHTT-diQ-unsplash",
        imageRes = R.drawable.matt_hoffman_mqjjhtt_diq_unsplash,
        suggestions = listOf("pothole filling", "pothole repair", "pothole sealing", "pothole excavation", "pothole removal"),
        primaryEntity = ResponsibleEntity.SHARED
    ),
    MatterToSolve(
        id = 2,
        title = localized(R.string.water_leakage, "Water leakage"),
        description = localized(R.string.water_leakage_in_the_street_or_public_spaces, "Water leakage in the street or public spaces"),
        issueType = IssueTypes.BUILDING,
        severity = Severity.HIGH,
        image = "arun-prakash-p0_6IwEjK98-unsplash",
        imageRes = R.drawable.arun_prakash_p0_6iwejk98_unsplash,
        suggestions = listOf("water leak repair", "water leak sealing"),
        primaryEntity = ResponsibleEntity.ANDA
    ),
    MatterToSolve(
        id = 3,
        title = localized(R.string.street_sign, "Street sign"),
        description = "Missing street sign, damaged, or request to place one",
        issueType = IssueTypes.ROAD,
        severity = Severity.LOW,
        image = "zoshua-colah-1BEULYOAnio-unsplash",
        imageRes = R.drawable.zoshua_colah_1beulyoanio_unsplash,
        suggestions = listOf("place a street sign", "change a damaged street sign"),
        primaryEntity = ResponsibleEntity.VMT
    ),
    MatterToSolve(
        id = 4,
        title = localized(R.string.trafic_light, "Trafic light"),
        description = localized(R.string.damaged_traffic_lights_traffic_lights_that_don_t_work_well, "damaged traffic lights, traffic lights that don't work well"),
        issueType = IssueTypes.BUILDING,
        severity = Severity.HIGH,
        image = "eliobed-suarez-PN-YnI5stdQ-unsplash",
        imageRes = R.drawable.eliobed_suarez_pn_yni5stdq_unsplash,
        suggestions = listOf("replace a damaged traffic light", "place a new traffic light"),
        primaryEntity = ResponsibleEntity.VMT
    ),
    MatterToSolve(
        id = 5,
        title = localized(R.string.lamp, "Lamp"),
        description = localized(R.string.burned_lamps_missing_or_request_to_install_or_replace, "Burned lamps, missing or request to install or replace"),
        issueType = IssueTypes.BUILDING,
        severity = Severity.LOW,
        image = "mukesh-naik-PZK-dEVBF9g-unsplash",
        imageRes = R.drawable.mukesh_naik_pzk_devbf9g_unsplash,
        suggestions = listOf("place a new lamp", "replace a damaged lamp", "repair a burned lamp"),
        primaryEntity = ResponsibleEntity.MUNICIPALITY
    ),
    MatterToSolve(
        id = 6,
        title = localized(R.string.fallen_trees, "Fallen trees"),
        description = localized(R.string.fallen_trees_in_the_middle_of_the_street, "Fallen trees in the middle of the street, "),
        issueType = IssueTypes.BUILDING,
        severity = Severity.MEDIUM,
        image = "john-cameron-EVhuJCqYLxM-unsplash",
        imageRes = R.drawable.john_cameron_evhujcqylxm_unsplash,
        suggestions = listOf("tree trimming", "tree planting", "tree removal"),
        primaryEntity = ResponsibleEntity.MUNICIPALITY
    ),
    MatterToSolve(
        id = 7,
        title = localized(R.string.flood, "Flood"),
        description = localized(R.string.flood, "Flood"),
        issueType = IssueTypes.PUBLIC_SPACE,
        severity = Severity.HIGH,
        image = "phillip-flores-38wqGW802RM-unsplash",
        imageRes = R.drawable.phillip_flores_38wqgw802rm_unsplash,
        suggestions = listOf("retaing flood prone areas", "repair flood damaged structures"),
        primaryEntity = ResponsibleEntity.MUNICIPALITY
    ),
    MatterToSolve(
        id = 8,
        title = localized(R.string.waste_management, "Waste Management"),
        description = localized(R.string.problems_with_waste_management, "problems with waste management"),
        issueType = IssueTypes.PUBLIC_SPACE,
        severity = Severity.LOW,
        image = "pixel-shot-kxTwgF_uHow-unsplash",
        imageRes = R.drawable.pixel_shot_kxtwgf_uhow_unsplash,
        suggestions = listOf(
            "improve waste collection systems",
            "increase recycling programs",
            "promote waste reduction",
            "organize community clean-ups",
            "implement recycling incentives",
            "encourage composting",
            "provide information on recycling and waste reduction",
            "request for volunteers to assist with waste management tasks"
        ),
        primaryEntity = ResponsibleEntity.MUNICIPALITY
    ),
    MatterToSolve(
        id = 9,
        title = localized(R.string.animal_abuse, "Animal abuse"),
        description = localized(R.string.animal_abuse_animal_cruelty, "Animal abuse, animal cruelty"),
        issueType = IssueTypes.OTHER,
        severity = Severity.HIGH,
        image = "thebugadi-y7YP97DLqWU-unsplash",
        imageRes = R.drawable.thebugadi_y7yp97dlqwu_unsplash,
        suggestions = listOf("animal welfare programs", "animal control services", "community outreach and education campaigns"),
        primaryEntity = ResponsibleEntity.MUNICIPALITY
    ),
    MatterToSolve(
        id = 10,
        title = localized(R.string.lid_sewer, "Lid sewer"),
        description = localized(R.string.missing_lid_sewer, "Missing lid sewer"),
        issueType = IssueTypes.ROAD,
        severity = Severity.MEDIUM,
        image = "zoshua-colah-XiJZ4r76Uik-unsplash",
        imageRes = R.drawable.zoshua_colah_xijz4r76uik_unsplash,
        suggestions = listOf("lid", "sewer", "drainage"),
        primaryEntity = ResponsibleEntity.MUNICIPALITY
    )
)


object LocatorDAO {
    private val leadingPlusCodeRegex = "^[A-Z0-9]{4,8}\\+[A-Z0-9]{2,4},?\\s*".toRegex(RegexOption.IGNORE_CASE)
    private val disallowedAddressCharsRegex = "[^\\p{L}0-9\\s,.\\-#]".toRegex()

    fun sanitizeAddress(rawAddress: String): String {
        val withoutPlusCode = rawAddress.replace(leadingPlusCodeRegex, "")
        return withoutPlusCode
            .replace(disallowedAddressCharsRegex, "")
            .replace("\\s{2,}".toRegex(), " ")
            .trim()
    }

    fun defaultLocator(
        address: String = "Paseo General Escalón, Alameda Franklin Delano Roosevelt, San Salvador",
        lat: Double = 13.7159815,
        lng: Double = -89.1801214
    ): Locator {
        return Locator(
            countryCode = "SV",
            firstLevel = "El Salvador",
            secondLevel = "San Salvador",
            thirdLevel = "San Salvador",
            groupingId = "f30b2834-12ee-4b0c-85eb-660ee7b08ed4",
            cityId = "a67b90f9-1d76-4835-a994-03cd04f1d619",
            groupingName = "Municipio de San Salvador Centro",
            groupingNameCode = "SSC",
            lat = lat,
            lng = lng,
            geoCode = "0614",
            zipCode = "1101",
            isCapitalCityRaw = 1,
            isDepartmentalCapitalRaw = 1,
            cityNameSortKey = "san-salvador",
            address = sanitizeAddress(address)
        )
    }

    fun findBy(countryCode: String, cityId: String): Locator {
        return defaultLocator().copy(
            countryCode = countryCode.ifEmpty { "SV" },
            cityId = cityId.ifEmpty { "a67b90f9-1d76-4835-a994-03cd04f1d619" }
        )
    }

    suspend fun findPlace(
        context: android.content.Context,
        lat: Double,
        lng: Double
    ): Locator = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val reverseResult = runCatching {
            GeoCodingService(context).getReverseGeocodingResult(lat, lng)
        }.getOrNull()

        val rawAddress = reverseResult?.address
            ?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
        val sanitizedAddress = rawAddress?.let { sanitizeAddress(it) }?.takeIf { it.length >= ADDRESS_MIN_CHARACTERS }
            ?: "San Salvador, El Salvador - ${String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)}"

        val resolvedCityName = reverseResult?.cityName
            ?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
            ?: "San Salvador"

        defaultLocator(address = sanitizedAddress, lat = lat, lng = lng).copy(
            thirdLevel = resolvedCityName,
            cityNameSortKey = resolvedCityName.lowercase(java.util.Locale.US).replace(" ", "-")
        )
    }
}

data class SelectedCity(val coordinates: Coordinate)


class ReportDataModel private constructor() : ViewModel() {

    companion object {
        val shared = ReportDataModel()
    }

    private val settings = SettingsStore.shared

    private val _report = MutableStateFlow<Report>(createDefaultReport())
    val report: StateFlow<Report> = _report.asStateFlow()

    private val _locator = MutableStateFlow<Locator>(createDefaultLocator())
    val locator: StateFlow<Locator> = _locator.asStateFlow()

    private val _uploadTrackers = MutableStateFlow<List<PhotoUploadTracker>>(emptyList())
    val uploadTrackers: StateFlow<List<PhotoUploadTracker>> = _uploadTrackers.asStateFlow()

    private val _isAddressValid = MutableStateFlow(false)
    val isAddressValid: StateFlow<Boolean> = _isAddressValid.asStateFlow()

    private val _isTitleValid = MutableStateFlow(false)
    val isTitleValid: StateFlow<Boolean> = _isTitleValid.asStateFlow()

    private val _isDescriptionValid = MutableStateFlow(false)
    val isDescriptionValid: StateFlow<Boolean> = _isDescriptionValid.asStateFlow()

    private val _reportSession = MutableStateFlow(
        ReportSessionResponse(
            reportContainer = "",
            shareIndexHash = "",
            reportCreationOn = ""
        )
    )
    val reportSession: StateFlow<ReportSessionResponse> = _reportSession.asStateFlow()

    private val _isDifferentLocation = MutableStateFlow(false)
    val isDifferentLocation: StateFlow<Boolean> = _isDifferentLocation.asStateFlow()

    private val _titlePlaceholder = MutableStateFlow(localized(R.string.e_g_street_repair, "e.g. Street Repair"))
    val titlePlaceholder: StateFlow<String> = _titlePlaceholder.asStateFlow()

    private val _descriptionPlaceholder = MutableStateFlow(localized(R.string.e_g_street_is_blocked, "e.g. Street is blocked"))
    val descriptionPlaceholder: StateFlow<String> = _descriptionPlaceholder.asStateFlow()

    private val _isLoadingAddress = MutableStateFlow(false)
    val isLoadingAddress: StateFlow<Boolean> = _isLoadingAddress.asStateFlow()

    private fun createDefaultLocator(): Locator {
        return Locator(
            countryCode = "SV",
            firstLevel = "El Salvador",
            secondLevel = "San Salvador",
            thirdLevel = "San Salvador",
            groupingId = "f30b2834-12ee-4b0c-85eb-660ee7b08ed4",
            cityId = "a67b90f9-1d76-4835-a994-03cd04f1d619",
            groupingName = "Municipio de San Salvador Centro",
            groupingNameCode = "SSC",
            lat = 13.7159815,
            lng = -89.1801214,
            geoCode = "0614",
            zipCode = "1101",
            isCapitalCityRaw = 1,
            isDepartmentalCapitalRaw = 1,
            cityNameSortKey = "san-salvador",
            address = "Paseo General Escalón &, Alameda Franklin Delano Roosevelt, San Salvador"
        )
    }

    private fun createDefaultReport(): Report {
        var lat = 13.7159815
        var lng = -89.1801214

        return Report(
            coordinate = Coordinate(lat, lng),
            address = "",
            title = "",
            description = "",
            severityId = 1,
            statusId = 1,
            issueTypeId = 1,
            matterToSolveId = 1,
            cellIndex = "",
            createdAt = Date(),
            updatedAt = Date(),
            reportState = ReportStates.NEW,
            attachments = mutableListOf<PreviewAttachmentRequest>()
        )
    }

    fun setMatterToSolve(matter: MatterToSolve) {
        _report.update { current ->
            current.copy(
                matterToSolveId = matter.id,
                severityId = matter.severity.identifier,
                issueTypeId = matter.issueType.identifier,
                suggestedTitle = matter.title,
                suggestedDescription = matter.description
            )
        }
        _titlePlaceholder.value = matter.title
        _descriptionPlaceholder.value = matter.description
    }

    fun setLocator(countryCode: String, cityId: String) {
        _locator.value = LocatorDAO.findBy(countryCode, cityId)
    }

    fun getMetadataFromReportId(reportId: String, reportContainer: String): Pair<String, ReportSessionResponse> {
        val parts = reportId.split("-")
        return Pair(
            parts.getOrNull(0) ?: "",
            ReportSessionResponse(
                reportContainer = reportContainer,
                shareIndexHash = parts.getOrNull(3) ?: "",
                reportCreationOn = parts.getOrNull(2) ?: ""
            )
        )
    }

    fun updateReportSession(reportSession: ReportSessionResponse) {
        _reportSession.value = reportSession
        _report.update { current ->
            current.copy(reportContainer = reportSession.reportContainer)
        }
    }

    fun updateReport(report: Report) {
        _report.value = report
        _isTitleValid.value = titleValidator.all { it.fn(report.title) }
        _isDescriptionValid.value = descriptionValidator.all { it.fn(report.description) }
        _isAddressValid.value = addressValidator.all { it.fn(report.address) }
    }

    fun updateShareUrl(url: String) {
        _report.update { current ->
            current.copy(shareUrl = url)
        }
    }

    fun updateReportId(id: String) {
        _report.update { current ->
            current.copy(id = id)
        }
    }

    fun updateTitle(title: String) {
        _report.update { current ->
            current.copy(title = title)
        }
        _isTitleValid.value = titleValidator.all { it.fn(title) }
    }

    fun updateDescription(description: String) {
        _report.update { current ->
            current.copy(description = description)
        }
        _isDescriptionValid.value = descriptionValidator.all { it.fn(description) }
    }

    fun updateAddress(address: String) {
        _report.update { current ->
            current.copy(address = address)
        }
        _isAddressValid.value = addressValidator.all { it.fn(address) }
    }

    fun setTitleValid(valid: Boolean) {
        _isTitleValid.value = valid
    }

    fun setDescriptionValid(valid: Boolean) {
        _isDescriptionValid.value = valid
    }

    fun setAddressValid(valid: Boolean) {
        _isAddressValid.value = valid
    }

    fun setLoadingAddress(loading: Boolean) {
        _isLoadingAddress.value = loading
    }

    fun setUploadTrackers(trackers: List<PhotoUploadTracker>) {
        _uploadTrackers.value = trackers
    }

    fun addUploadTrackers(trackers: List<PhotoUploadTracker>) {
        _uploadTrackers.value = _uploadTrackers.value + trackers
    }

    fun removeUploadTracker(idOrName: String) {
        _uploadTrackers.value = _uploadTrackers.value.filterNot {
            it.id == idOrName || it.name.value == idOrName || it.key.value == idOrName
        }
    }

    fun setDifferentLocation(different: Boolean) {
        _isDifferentLocation.value = different
    }

    fun updateCoordinate(coordinate: Coordinate) {
        _report.update { current ->
            current.copy(coordinate = coordinate)
        }
    }

    fun updateLocator(locator: Locator) {
        _locator.value = locator
        _report.update { current ->
            current.copy(
                address = locator.address,
                cityId = locator.cityId.ifEmpty { current.cityId }
            )
        }
        _isAddressValid.value = addressValidator.all { it.fn(locator.address) }
    }

    fun prepareForModification(report: Report) {
        val modifiedReport = report.copy(reportState = ReportStates.MODIFYING)
        _report.value = modifiedReport
        _isTitleValid.value = titleValidator.all { it.fn(report.title) }
        _isDescriptionValid.value = descriptionValidator.all { it.fn(report.description) }
        _isAddressValid.value = addressValidator.all { it.fn(report.address) }
        _isDifferentLocation.value = true
        _uploadTrackers.value = mapAttachmentsToTrackers(report.attachments)
    }

    fun addAttachments(attachments: List<PhotoUploadTracker>) {
        _report.update { reportVal ->
            val currentAttachments = reportVal.attachments.toMutableList()

            if (reportVal.reportState == ReportStates.MODIFYING) {
                val existingKeys = attachments.filter { it.isExisting.value }.map { it.key.value }.toSet()
                currentAttachments.removeAll { !existingKeys.contains(it.key) }
            }

            val resolvedContainer = reportVal.reportContainer?.takeIf { it.isNotEmpty() }
                ?: _reportSession.value.reportContainer

            val newAttachments = attachments.filter { !it.isExisting.value }
            val payload = newAttachments.map { attachment ->
                PreviewAttachmentRequest(
                    attachmentId = "",
                    fileName = attachment.name.value,
                    type = app.reportamelo.commons.AttachmentType.IMAGE,
                    key = attachment.key.value,
                    notes = "",
                    reportContainer = resolvedContainer
                )
            }

            currentAttachments.addAll(payload)
            reportVal.copy(attachments = currentAttachments)
        }
    }

    fun setAttachmentId(groupings: List<ReportAttachmentGrouping>) {
        _report.update { reportVal ->
            val payload = reportVal.attachments.map { attachment ->
                val resolvedId = findAttachmentId(groupings, attachment.key).ifEmpty { attachment.attachmentId }
                PreviewAttachmentRequest(
                    attachmentId = resolvedId,
                    fileName = attachment.fileName,
                    type = attachment.type,
                    key = attachment.key,
                    notes = attachment.notes,
                    reportContainer = attachment.reportContainer
                )
            }
            reportVal.copy(attachments = payload.toMutableList())
        }
    }

    fun mapAttachmentsToTrackers(attachments: List<PreviewAttachmentRequest>): List<PhotoUploadTracker> {
        return attachments.map { attachment ->
            PhotoUploadTracker(
                keyParam = attachment.key,
                nameParam = attachment.fileName,
                urlParam = attachment.url.ifEmpty { attachment.attachmentId },
                isExistingParam = true
            )
        }
    }

    fun getAttachmentsAsTrackers(): List<PhotoUploadTracker> {
        return mapAttachmentsToTrackers(_report.value.attachments)
    }

    fun removeAttachments() {
        _report.update { current ->
            current.copy(attachments = mutableListOf())
        }
    }

    fun clear() {
        _uploadTrackers.value = emptyList()
        _isAddressValid.value = false
        _isTitleValid.value = false
        _isDescriptionValid.value = false
        _isDifferentLocation.value = false
        _isLoadingAddress.value = false
        _locator.value = createDefaultLocator()
        _report.value = createDefaultReport()
    }

    fun buildReportId(): String {
        val country = _locator.value.countryCode.ifEmpty { "SV" }
        val groupCode = _locator.value.groupingNameCode.ifEmpty { "SSC" }
        val createdOn = _reportSession.value.reportCreationOn.ifEmpty { "261027" }
        val hash = _reportSession.value.shareIndexHash.ifEmpty { "aXWsaxls" }
        return "$country-$groupCode-$createdOn-$hash"
    }

    private fun findAttachmentId(groupings: List<ReportAttachmentGrouping>, key: String): String {
        return groupings.firstOrNull { it.key == key }?.attachmentId ?: ""
    }
}
data class Report(
    var id: String? = null,
    var cityId: String? = null,
    var reportContainer: String? = null,
    var coordinate: Coordinate,
    var address: String,
    var title: String,
    var description: String,
    var severityId: Int,
    var statusId: Int,
    var issueTypeId: Int,
    var matterToSolveId: Int,
    var cellIndex: String,
    var createdAt: Date,
    var updatedAt: Date? = null,
    var reportedAt: Date? = null,
    var reportState: ReportStates,
    var attachments: MutableList<PreviewAttachmentRequest> = mutableListOf(),
    var suggestedTitle: String? = null,
    var suggestedDescription: String? = null,
    var shareUrl: String? = null,
    var observations: String? = null,
    var olc: String? = null,
    var reportedBy: String? = null
) {
    fun update(from: Report) {
        this.coordinate = from.coordinate
        this.address = from.address
        this.title = from.title
        this.description = from.description
        this.severityId = from.severityId
        this.statusId = from.statusId
        this.issueTypeId = from.issueTypeId
        this.matterToSolveId = from.matterToSolveId
        this.cellIndex = from.cellIndex
        this.createdAt = from.createdAt
        this.updatedAt = from.updatedAt
        this.reportedAt = from.reportedAt
        this.suggestedTitle = from.suggestedTitle
        this.suggestedDescription = from.suggestedDescription
        this.reportState = from.reportState
        this.attachments = from.attachments
        this.cityId = from.cityId
        this.reportContainer = from.reportContainer
        this.shareUrl = from.shareUrl
        this.observations = from.observations
        this.olc = from.olc
        this.reportedBy = from.reportedBy
    }
}

fun parseFlexibleDate(element: JsonElement?): Date? {
    if (element == null || element is JsonNull) return null
    val primitive = element as? JsonPrimitive ?: return null
    val raw = primitive.content.trim()
    if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) return null

    raw.toDoubleOrNull()?.let { timestamp ->
        val millis = if (timestamp > 1_000_000_000_000.0) {
            timestamp.toLong()
        } else {
            (timestamp * 1000.0).toLong()
        }
        return Date(millis)
    }

    runCatching { java.time.Instant.parse(raw) }.getOrNull()?.let { return Date.from(it) }
    runCatching {
        java.time.ZonedDateTime.parse(raw, java.time.format.DateTimeFormatter.ISO_DATE_TIME).toInstant()
    }.getOrNull()?.let { return Date.from(it) }
    runCatching { java.time.OffsetDateTime.parse(raw).toInstant() }.getOrNull()?.let { return Date.from(it) }
    runCatching {
        java.time.LocalDateTime.parse(raw).atZone(java.time.ZoneId.systemDefault()).toInstant()
    }.getOrNull()?.let { return Date.from(it) }

    return null
}

private val reportAttachmentJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

fun app.reportamelo.models.ReportDAO.toModel(): Report {
    val resolvedCoordinate = this.coordinate ?: app.reportamelo.models.Coordinate(this.lat, this.lng)
    val resolvedTitle = this.title.ifEmpty { this.suggestedTitle }
    val resolvedDescription = this.description.ifEmpty { this.suggestedDescription }
    val decodedAttachments = this.attachments.mapNotNull { element ->
        when (element) {
            is JsonObject -> runCatching {
                reportAttachmentJson.decodeFromJsonElement(PreviewAttachmentRequest.serializer(), element)
            }.getOrNull()
            else -> null
        }
    }.toMutableList()
    return Report(
        id = this.id,
        cityId = this.cityId,
        reportContainer = this.reportContainer,
        coordinate = resolvedCoordinate,
        address = this.address.ifEmpty { this.zipCode },
        title = resolvedTitle,
        description = resolvedDescription,
        severityId = this.severityId,
        statusId = this.statusId,
        issueTypeId = this.issueTypeId,
        matterToSolveId = this.matterToSolveId,
        cellIndex = this.cellIndex.ifEmpty { this.groupingId ?: "" },
        createdAt = parseFlexibleDate(this.createdAt) ?: java.util.Date(),
        updatedAt = parseFlexibleDate(this.updatedAt),
        reportedAt = parseFlexibleDate(this.reportedAt),
        reportState = ReportStates.fromRawValue(this.reportState),
        attachments = decodedAttachments,
        suggestedTitle = this.suggestedTitle.ifEmpty { null },
        suggestedDescription = this.suggestedDescription.ifEmpty { null },
        shareUrl = this.shareUrl,
        observations = this.observations,
        olc = this.olc,
        reportedBy = this.reportedBy
    )
}

fun Report.toDao(): app.reportamelo.models.Report {
    val currentLocator = ReportDataModel.shared.locator.value
    val currentSession = ReportDataModel.shared.reportSession.value
    val resolvedAddress = this.address.ifEmpty { currentLocator.address }
    val resolvedZipCode = currentLocator.zipCode.ifEmpty { resolvedAddress }
    val resolvedCityId = this.cityId?.takeIf { it.isNotEmpty() } ?: currentLocator.cityId
    val resolvedReportContainer = this.reportContainer?.takeIf { it.isNotEmpty() }
        ?: currentSession.reportContainer
    return app.reportamelo.models.Report(
        id = this.id,
        lat = this.coordinate.lat,
        lng = this.coordinate.lng,
        coordinate = this.coordinate,
        address = resolvedAddress,
        title = this.title,
        description = this.description,
        issueTypeId = this.issueTypeId,
        severityId = this.severityId,
        statusId = this.statusId,
        matterToSolveId = this.matterToSolveId,
        reportedAt = this.reportedAt?.time,
        cellIndex = this.cellIndex,
        createdAt = this.createdAt.time,
        updatedAt = (this.updatedAt ?: Date()).time,
        zipCode = resolvedZipCode,
        groupingId = currentLocator.groupingId,
        reportedBy = this.reportedBy,
        olc = this.olc,
        suggestedTitle = this.suggestedTitle?.takeIf { it.isNotEmpty() } ?: this.title,
        suggestedDescription = this.suggestedDescription?.takeIf { it.isNotEmpty() } ?: this.description,
        reportState = this.reportState.rawValue,
        attachments = this.attachments.map { it.attachmentId }.filter { it.isNotEmpty() },
        attachmentRequests = this.attachments.toList(),
        cityId = resolvedCityId,
        reportContainer = resolvedReportContainer,
        shareUrl = this.shareUrl,
        observations = this.observations
    )
}

val Report.issueType: app.reportamelo.commons.IssueTypes
    get() = app.reportamelo.commons.IssueTypes.values().firstOrNull { it.identifier == this.issueTypeId } ?: app.reportamelo.commons.IssueTypes.ALL

val Report.severity: app.reportamelo.commons.Severity
    get() = app.reportamelo.commons.Severity.values().firstOrNull { it.identifier == this.severityId } ?: app.reportamelo.commons.Severity.LOW

val Report.status: app.reportamelo.commons.IssueStatus
    get() = app.reportamelo.commons.IssueStatus.values().firstOrNull { it.identifier == this.statusId } ?: app.reportamelo.commons.IssueStatus.REPORTED

val Report.createdDate: String
    get() = app.reportamelo.commons.utils.formatRelativeDate(this.createdAt)

val Report.updatedDate: String
    get() = this.updatedAt?.let { app.reportamelo.commons.utils.formatRelativeDate(it) } ?: localized(R.string.not_yet, "Not yet")

val Report.reportedDate: String
    get() = this.reportedAt?.let { app.reportamelo.commons.utils.formatRelativeDate(it) } ?: localized(R.string.not_yet, "Not yet")

