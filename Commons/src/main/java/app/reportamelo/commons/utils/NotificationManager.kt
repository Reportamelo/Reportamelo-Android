package app.reportamelo.commons.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object AppNotificationManager {
    private const val TAG = "AppNotificationManager"
    
    var isPermissionGranted = false
    var deviceToken: String = ""

    fun requestAuthorization(activity: Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true
                registerForRemoteNotifications()
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
            }
        } else {
            isPermissionGranted = true
            registerForRemoteNotifications()
        }
    }

    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true
                registerForRemoteNotifications()
            } else {
                isPermissionGranted = false
                Log.w(TAG, "Notification permission denied")
            }
        }
    }

    private fun registerForRemoteNotifications() {
        // Equivalent to FirebaseMessaging.getInstance().token
        // Left unimplemented to avoid hard dependency, but this is where token generation is handled.
        Log.d(TAG, "Ready to fetch FCM Token")
    }

    fun handleNotificationTap(context: Context, data: Map<String, String>) {
        Log.d(TAG, "User tapped notification with data: $data")
        
        val route = data["route"]
        if (route == "reports") {
            DeepLinkRouter.shared.activeTab.value = 4
        }
    }
}
