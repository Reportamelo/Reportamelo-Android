package app.reportamelo.commons.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun checkAdMobDomainStatus(): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://doubleclick.net")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            responseCode in 200..499
        } catch (e: Exception) {
            false
        }
    }
}

// Ensure the AdMob listener is handled appropriately inside your activity or fragment
// e.g. 
// adView.adListener = object : AdListener() {
//     override fun onAdFailedToLoad(adError: LoadAdError) {
//         if (adError.domain == "com.google.android.gms.ads") {
//             Log.d("AdMob", "Error code: ${adError.code}, Description: ${adError.message}")
//         }
//     }
// }
