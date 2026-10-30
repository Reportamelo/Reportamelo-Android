package app.reportamelo.communityIssues.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.KeychainToken
import app.reportamelo.commons.LegacyKeychain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppNotificationManager {
    private const val TAG = "AppNotificationManager"
    private const val PREF_DEVICE_TOKEN = "push_device_token"

    private val _isPermissionGrantedFlow = MutableStateFlow(false)
    val isPermissionGrantedFlow: StateFlow<Boolean> = _isPermissionGrantedFlow.asStateFlow()

    private val _deviceTokenFlow = MutableStateFlow(
        AppPreferences.getString(PREF_DEVICE_TOKEN) ?: ""
    )
    val deviceTokenFlow: StateFlow<String> = _deviceTokenFlow.asStateFlow()

    var isPermissionGranted: Boolean
        get() = _isPermissionGrantedFlow.value
        set(value) {
            _isPermissionGrantedFlow.value = value
        }

    var deviceToken: String
        get() = _deviceTokenFlow.value
        set(value) {
            _deviceTokenFlow.value = value
            if (value.isNotEmpty()) {
                AppPreferences.setString(PREF_DEVICE_TOKEN, value)
            }
        }

    fun checkAuthorizationStatus(context: Context): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        isPermissionGranted = granted
        if (granted) {
            registerForRemoteNotifications()
        }
        return granted
    }

    fun onPermissionResult(granted: Boolean) {
        isPermissionGranted = granted
        if (granted) {
            registerForRemoteNotifications()
        } else {
            runCatching { Log.w(TAG, "Notification permission denied") }
        }
    }

    fun updateDeviceToken(token: String) {
        deviceToken = token
    }

    fun requestAuthorization(activity: Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onPermissionResult(true)
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    requestCode
                )
            }
        } else {
            onPermissionResult(true)
        }
    }

    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == 1001) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            onPermissionResult(granted)
        }
    }

    fun registerForRemoteNotifications() {
        runCatching { Log.d(TAG, "Ready to fetch FCM Token") }
        // Attempt to retrieve FCM token if FirebaseMessaging is available at runtime
        runCatching {
            val messagingClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            val instance = messagingClass.getMethod("getInstance").invoke(null)
            val task = messagingClass.getMethod("getToken").invoke(instance)
            val taskClass = Class.forName("com.google.android.gms.tasks.Task")
            val onSuccessListenerClass = Class.forName("com.google.android.gms.tasks.OnSuccessListener")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                onSuccessListenerClass.classLoader,
                arrayOf(onSuccessListenerClass)
            ) { _, method, args ->
                if (method.name == "onSuccess" && !args.isNullOrEmpty()) {
                    val fcmToken = args[0] as? String
                    if (!fcmToken.isNullOrEmpty()) {
                        updateDeviceToken(fcmToken)
                    }
                }
                null
            }
            taskClass.getMethod("addOnSuccessListener", onSuccessListenerClass).invoke(task, proxy)
        }

        // Fallback to saved token or device identifier if FCM token is not yet populated
        if (deviceToken.isEmpty()) {
            val savedOrDeviceId = AppPreferences.getString(PREF_DEVICE_TOKEN)?.takeIf { it.isNotEmpty() }
                ?: LegacyKeychain.getToken(KeychainToken.DEVICE_ID)?.takeIf { it.isNotEmpty() }
            if (!savedOrDeviceId.isNullOrEmpty()) {
                updateDeviceToken(savedOrDeviceId)
            }
        }
    }

    fun handleNotificationTap(context: Context, data: Map<String, String>) {
        runCatching { Log.d(TAG, "User tapped notification with data: $data") }

        val route = data["route"]
        if (route == "reports") {
            DeepLinkRouter.shared.activeTab.value = 4
        }
    }
}
