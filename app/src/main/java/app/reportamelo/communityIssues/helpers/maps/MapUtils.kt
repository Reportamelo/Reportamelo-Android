package app.reportamelo.communityIssues.helpers.maps

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import com.google.android.gms.maps.model.LatLng

fun distance(from: LatLng, to: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
    return results[0]
}

fun openInAppleMaps(context: Context, location: LatLng, title: String) {
    // Android devices don't typically have Apple Maps. Fallback to Google Maps.
    openOnGoogleMaps(context, location, title)
}

fun openOnGoogleMaps(context: Context, location: LatLng, title: String) {
    val uri = "geo:0,0?q=${location.latitude},${location.longitude}(${Uri.encode(title)})"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    intent.setPackage("com.google.android.apps.maps")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback to browser
        val browserUri = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUri)))
    }
}

enum class CountryCode {
    SV,
    US
}

val ALLOWED_COUNTRIES = listOf(CountryCode.SV)

fun getCountryCode(identifier: String): CountryCode {
    return try {
        CountryCode.valueOf(identifier)
    } catch (e: Exception) {
        CountryCode.SV
    }
}

fun isAllowedCountry(identifier: String): Boolean {
    return ALLOWED_COUNTRIES.contains(getCountryCode(identifier))
}
