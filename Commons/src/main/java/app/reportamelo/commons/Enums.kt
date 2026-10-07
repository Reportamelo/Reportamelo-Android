package app.reportamelo.commons

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

enum class AttachmentType(val value: String) {
    IMAGE("image"),
    VIDEO("video"),
    DOCUMENT("document")
}

enum class AttachmentValidatedBy(val value: String) {
    BOT("bot"),
    MUNICIPALITY("municipality"),
    CITIZEN("citizen"),
    MANUALLY("manually")
}

enum class ReportAttachmentState(val value: String) {
    CONFIRMED("confirmed"),
    PENDING("pending"),
    INAPPROPRIATE("inappropriate"),
    DELETED("deleted"),
    MANUAL_REVISION("manualRevision")
}

enum class VotingType(val description: String) {
    REPORT("Report"),
    PETITION("Petition")
}

enum class AvatarCreatedFrom {
    OPTIONS_SELECTOR, AVATAR, CAMERA, PHOTO, INITIALS, MONOGRAM, GOOGLE_AUTH, MEMOJI
}

enum class UserType(val description: String) {
    GUEST("Guest"),
    CITIZEN("Citizen"),
    GOVERNMENT("Government")
}


enum class Categories(val title: String, val identifier: Int, val minimumAmountOfSignatures: Int) {
    ALL("All", 0, 100),
    PREVENTION("Prevention", 1, 10),
    CORRECTIVE("Corrective", 2, 10),
    REPAIR("Repair", 3, 20),
    REPLACEMENT("Replacement", 4, 20),
    CONSTRUCTION("Construction", 5, 50),
    INSTALLATION("Installation", 6, 40),
    INSPECTION("Inspection", 7, 20),
    EMERGENCY("Emergency", 8, 5);

    companion object {
        fun fromIdentifier(id: Int): Categories = entries.find { it.identifier == id } ?: ALL
    }
}

enum class CountryCode(val iso3166Alpha3Code: String, val countryName: String) {
    SV("SLV", "El Salvador"),
    US("USA", "United States"),
    GT("GTM", "Guatemala"),
    NI("NIC", "Nicaragua")
}

enum class PetitionState(val description: String) {
    NEW("New"),
    MODIFYING("Modifying"),
    VOTING("Voting"),
    CLOSED("Closed"),
    REJECTED("Rejected"),
    RESOLVED("Resolved"),
    CANCELLED("Cancelled")
}

enum class PostVisibility(val description: String) {
    HIDDEN("Hidden"),
    DRAFT("Draft"),
    PUBLISHED("Published")
}

enum class TypeOfContentToReport(val description: String) {
    IMAGE("Image"),
    REPORT("Report"),
    VIDEO("Video"),
    COMMENT("Comment"),
    PETITION("Petition"),
    ACCOUNT("Account")
}

enum class ReportViolationStatus(val description: String) {
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PENDING("Pending"),
    APPEALING("Appealing"),
    SENT_TO_MODERATION("Sent to Moderation"),
    REMOVED("Removed")
}
