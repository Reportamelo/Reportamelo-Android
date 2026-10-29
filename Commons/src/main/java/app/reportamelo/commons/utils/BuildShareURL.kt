package app.reportamelo.commons.utils

import android.net.Uri
import app.reportamelo.commons.Apis

enum class ShareType(val value: String) {
    REPORT("report"),
    PETITION("petition")
}

object EndpointsStub {
    val baseURL: String get() = Apis.baseURL
    val shareableURL: String get() = Apis.shareableURL
}

fun resolveUrlString(string: String): String? {
    val trimmed = string.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    return "${Apis.baseURL.trimEnd('/')}/${trimmed.trimStart('/')}"
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
