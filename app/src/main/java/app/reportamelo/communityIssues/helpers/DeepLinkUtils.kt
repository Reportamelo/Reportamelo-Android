package app.reportamelo.communityIssues.helpers

import app.reportamelo.models.*
import app.reportamelo.repository.MapExplorerRepository
import app.reportamelo.commons.CommonIntercommunicationErrors

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class ProfileLink(val profileId: String)

fun profileDeepLinkHandler(uri: Uri): ProfileLink? {
    val path = uri.path ?: return null
    if (path.contains("profile")) {
        return ProfileLink(profileId = "")
    }
    return null
}

enum class DeepLinkHandlerType {
    REPORT, PETITION, UPDATE_INFO, UNKNOWN
}

data class DeepLink(
    val resourceHash: String,
    val slug: String,
    val type: DeepLinkHandlerType,
    val countryCode: String,
    val cityCode: String,
    val date: String
) {
    val reportId: String
        get() = "$countryCode-$cityCode-$date-$resourceHash"
}

fun deepLinkHandler(uri: Uri): DeepLink? {
    val pathSegments = uri.pathSegments.filter { it.isNotEmpty() && it != "/" }
    
    if (pathSegments.size < 6) return null
    
    val resourceHash = pathSegments[0]
    val resourceType = pathSegments[1]
    val date = pathSegments[2]
    val cityCode = pathSegments[3]
    val countryCode = pathSegments[4]
    val slug = pathSegments[5]
    
    val type = when (resourceType) {
        "report" -> DeepLinkHandlerType.REPORT
        "petition" -> DeepLinkHandlerType.PETITION
        "update-info" -> DeepLinkHandlerType.UPDATE_INFO
        else -> DeepLinkHandlerType.UNKNOWN
    }
    
    return DeepLink(
        resourceHash = resourceHash,
        slug = slug,
        type = type,
        date = date,
        cityCode = cityCode,
        countryCode = countryCode
    )
}

// Stub classes

class DeepLinkRouter private constructor() {
    companion object {
        val shared = DeepLinkRouter()
    }

    var pendingDeepLink: DeepLink? = null
    var activeReportID: String? = null
    var activeTab = MutableStateFlow(0)
    var isPresented = MutableStateFlow(false)
    var isLoading = MutableStateFlow(false)
    var message = MutableStateFlow("")
    var report = MutableStateFlow<MapExplorerReport?>(null)
    var presentAlert = MutableStateFlow(false)
    var isReadyToRoute = false
    
    private val scope = CoroutineScope(Dispatchers.Main)

    fun handleIncomingURL(url: Uri) {
        val deepLink = deepLinkHandler(url) ?: return
        
        if (isReadyToRoute) {
            route(deepLink)
        } else {
            pendingDeepLink = deepLink
        }
    }
    
    fun processPendingDeepLink() {
        pendingDeepLink?.let { deepLink ->
            route(deepLink)
        }
        pendingDeepLink = null
    }
    
    private fun route(deepLink: DeepLink) {
        when (deepLink.type) {
            DeepLinkHandlerType.REPORT -> {
                activeTab.value = 1
                activeReportID = deepLink.reportId
                activeReportID?.let { retrieveAndPresentReportDetails(it) }
            }
            else -> {}
        }
    }
    
    private suspend fun dismissAndShowAlert(msg: String) {
        message.value = msg
        isLoading.value = false
        if (isPresented.value) {
            isPresented.value = false
            delay(500)
        }
        presentAlert.value = true
    }
    
    private fun retrieveAndPresentReportDetails(reportId: String) {
        scope.launch {
            isLoading.value = true
            try {
                isPresented.value = true
                val result = MapExplorerRepository.report(reportId, "SV", "")
                report.value = result
            } catch (e: CommonIntercommunicationErrors.InvalidPetition) {
                dismissAndShowAlert("That link is invalid")
            } catch (e: CommonIntercommunicationErrors.NotFound) {
                dismissAndShowAlert("That link does not exist")
            } catch (e: Exception) {
                dismissAndShowAlert("Something went wrong")
            }
            isLoading.value = false
        }
    }
}
