package app.reportamelo.commons

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Severity(val title: String, val identifier: Int) {
    ALL("All", 0),
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3);

    companion object {
        fun fromIdentifier(id: Int): Severity = entries.find { it.identifier == id } ?: LOW
    }
}

enum class IssueTypes(val title: String, val identifier: Int) {
    ALL("All", 0),
    ROAD("Road", 1),
    PUBLIC_SPACE("Public Space", 2),
    BUILDING("Building", 3),
    OTHER("Other", 4);

    companion object {
        fun fromIdentifier(id: Int): IssueTypes = entries.find { it.identifier == id } ?: ALL
    }
}

enum class IssueStatus(val title: String, val identifier: Int) {
    REPORTED("Reported", 1),
    CONFIRMED("Confirmed", 2),
    IN_PROGRESS("In Progress", 3),
    PETITION_TO_SIGN("Petition to Sign", 4),
    FIXED("Fixed", 5),
    ASSIGNED("Assigned", 6),
    REJECTED("Rejected", 7),
    RECTIFICATION("Rectification", 8),
    HUMAN_REVIEW("Human Review", 9),
    UNDER_REVIEW("Under Review", 10);

    companion object {
        fun fromIdentifier(id: Int): IssueStatus = entries.find { it.identifier == id } ?: REPORTED
    }
}

enum class OrderFilter(val title: String, val filter: String) {
    ASCENDING("Oldest to Newest", "asc"),
    DESCENDING("Newest to Oldest", "desc")
}

@Serializable
enum class AttachmentType(val value: String) {
    @SerialName("image") IMAGE("image"),
    @SerialName("video") VIDEO("video"),
    @SerialName("document") DOCUMENT("document")
}

@Serializable
enum class AttachmentValidatedBy(val value: String) {
    @SerialName("bot") BOT("bot"),
    @SerialName("municipality") MUNICIPALITY("municipality"),
    @SerialName("citizen") CITIZEN("citizen"),
    @SerialName("manually") MANUALLY("manually")
}

@Serializable
enum class ReportAttachmentState(val value: String) {
    @SerialName("confirmed") CONFIRMED("confirmed"),
    @SerialName("pending") PENDING("pending"),
    @SerialName("inappropriate") INAPPROPRIATE("inappropriate"),
    @SerialName("deleted") DELETED("deleted"),
    @SerialName("manualRevision") MANUAL_REVISION("manualRevision")
}

@Serializable
enum class VotingType(val description: String, val rawValue: String = description.lowercase()) {
    @SerialName("report") REPORT("Report", "report"),
    @SerialName("petition") PETITION("Petition", "petition")
}

@Serializable
enum class AvatarCreatedFrom(val rawValue: String) {
    @SerialName("optionsSelector") OPTIONS_SELECTOR("optionsSelector"),
    @SerialName("avatar") AVATAR("avatar"),
    @SerialName("camera") CAMERA("camera"),
    @SerialName("photo") PHOTO("photo"),
    @SerialName("initials") INITIALS("initials"),
    @SerialName("monogram") MONOGRAM("monogram"),
    @SerialName("GoogleAuth") GOOGLE_AUTH("GoogleAuth"),
    @SerialName("Memoji") MEMOJI("Memoji")
}

@Serializable
enum class UserType(val description: String) {
    @SerialName("guest") GUEST("Guest"),
    @SerialName("citizen") CITIZEN("Citizen"),
    @SerialName("government") GOVERNMENT("Government")
}


@Serializable
enum class Categories(val title: String, val identifier: Int, val minimumAmountOfSignatures: Int) {
    @SerialName("all") ALL("All", 0, 100),
    @SerialName("prevention") PREVENTION("Prevention", 1, 10),
    @SerialName("corrective") CORRECTIVE("Corrective", 2, 10),
    @SerialName("repair") REPAIR("Repair", 3, 20),
    @SerialName("replacement") REPLACEMENT("Replacement", 4, 20),
    @SerialName("construction") CONSTRUCTION("Construction", 5, 50),
    @SerialName("installation") INSTALLATION("Installation", 6, 40),
    @SerialName("inspection") INSPECTION("Inspection", 7, 20),
    @SerialName("emergency") EMERGENCY("Emergency", 8, 5);

    companion object {
        fun fromIdentifier(id: Int): Categories = entries.find { it.identifier == id } ?: ALL
    }
}

@Serializable
enum class CountryCode(val iso3166Alpha3Code: String, val countryName: String) {
    @SerialName("SV") SV("SLV", "El Salvador"),
    @SerialName("US") US("USA", "United States"),
    @SerialName("GT") GT("GTM", "Guatemala"),
    @SerialName("NI") NI("NIC", "Nicaragua")
}

@Serializable
enum class PetitionState(val description: String) {
    @SerialName("new") NEW("New"),
    @SerialName("modifying") MODIFYING("Modifying"),
    @SerialName("voting") VOTING("Voting"),
    @SerialName("closed") CLOSED("Closed"),
    @SerialName("rejected") REJECTED("Rejected"),
    @SerialName("resolved") RESOLVED("Resolved"),
    @SerialName("cancelled") CANCELLED("Cancelled")
}

@Serializable
enum class PostVisibility(val description: String) {
    @SerialName("hidden") HIDDEN("Hidden"),
    @SerialName("draft") DRAFT("Draft"),
    @SerialName("published") PUBLISHED("Published")
}

@Serializable
enum class TypeOfContentToReport(val description: String, val rawValue: String = description.lowercase()) {
    @SerialName("image") IMAGE("Image", "image"),
    @SerialName("report") REPORT("Report", "report"),
    @SerialName("video") VIDEO("Video", "video"),
    @SerialName("comment") COMMENT("Comment", "comment"),
    @SerialName("petition") PETITION("Petition", "petition"),
    @SerialName("account") ACCOUNT("Account", "account")
}

@Serializable
enum class ReportViolationStatus(val description: String) {
    @SerialName("approved") APPROVED("Approved"),
    @SerialName("rejected") REJECTED("Rejected"),
    @SerialName("pending") PENDING("Pending"),
    @SerialName("appealing") APPEALING("Appealing"),
    @SerialName("sentToModeration") SENT_TO_MODERATION("Sent to Moderation"),
    @SerialName("removed") REMOVED("Removed")
}
