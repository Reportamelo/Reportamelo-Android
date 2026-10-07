package app.reportamelo.commons

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

class DeviceService(private val context: Context) {

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        // Check if we already saved a device ID to SharedPreferences (similar to Keychain)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getString("device_id", null)
        
        if (savedId != null) {
            return savedId
        }

        // Try to get Android's secure device ID
        var newDeviceID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        // Fallback: Use Build info or generate a fresh UUID
        if (newDeviceID.isNullOrEmpty()) {
            val buildInfo = Build.FINGERPRINT ?: Build.MODEL ?: Build.BOARD
            newDeviceID = if (buildInfo.isNotEmpty()) {
                UUID.nameUUIDFromBytes(buildInfo.toByteArray()).toString()
            } else {
                UUID.randomUUID().toString()
            }
        }

        // Save it so it survives until app uninstall/data clear
        prefs.edit().putString("device_id", newDeviceID).apply()

        return newDeviceID
    }
}
