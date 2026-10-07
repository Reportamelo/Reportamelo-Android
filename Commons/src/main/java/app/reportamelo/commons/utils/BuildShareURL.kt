package app.reportamelo.commons.utils

import android.net.Uri

enum class ShareType(val value: String) {
    REPORT("report"),
    PETITION("petition")
}

// Assuming Endpoints is available in the project. If not, this is a placeholder.
object EndpointsStub {
    const val baseURL = "https://example.com/api"
    const val shareableURL = "https://example.com/share"
}

fun buildShareURLWithComponents(index: String, type: ShareType, slug: String): Uri? {
    return Uri.parse(EndpointsStub.shareableURL).buildUpon()
        .appendPath(index)
        .appendPath(type.value)
        .appendPath(slug)
        .build()
}

fun buildShareURL(path: String): Uri? {
    return Uri.parse(EndpointsStub.shareableURL).buildUpon()
        .appendPath(path)
        .build()
}

fun getURL(path: String): Uri? {
    return Uri.parse(EndpointsStub.baseURL).buildUpon()
        .appendPath(path)
        .build()
}

enum class ReportAttachmentState {
    PENDING, CONFIRMED, INAPPROPRIATE, DELETED
}

fun buildPreviewAttachmentURL(reportContainer: String, fileName: String, state: ReportAttachmentState, updatedAtRaw: Long? = null): Uri? {
    var version: Long = 1L
    if (state == ReportAttachmentState.INAPPROPRIATE || state == ReportAttachmentState.DELETED) {
        version = (2..100).random().toLong()
    }
    
    if (updatedAtRaw != null) {
        version = updatedAtRaw
    }
    
    val fragment = when (state) {
        ReportAttachmentState.PENDING -> "review"
        ReportAttachmentState.CONFIRMED -> "validated"
        else -> "review"
    }
    
    return Uri.parse(EndpointsStub.baseURL).buildUpon()
        .appendPath("attachments")
        .appendPath(fragment)
        .appendPath(reportContainer)
        .appendPath(fileName)
        .appendQueryParameter("v", version.toString())
        .build()
}

fun urlFromString(string: String): Uri? {
    if (string.startsWith("http")) {
        return Uri.parse(string)
    }
    return getURL(string)
}
