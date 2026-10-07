package app.reportamelo.commons.utils

import android.util.Log

object AppLogger {
    private const val SUBSYSTEM = "dev.FranciscoHernandez.Comunity-Issues"
    const val VIEW_CYCLE = "viewcycle"
    const val STATISTICS = "statistics"

    var disableLogs: Boolean = false

    fun info(tag: String, message: String) {
        if (!disableLogs) {
            Log.i("$SUBSYSTEM:$tag", message)
        }
    }
}

interface DataLogger<T> {
    fun log(data: T)
}

data class LogDetail(
    val viewName: String,
    val methodName: String? = null,
    val data: String? = null
)

class JSONLogger<T> : DataLogger<T> {
    override fun log(data: T) {
        if (!AppLogger.disableLogs) {
            try {
                // To avoid GSON dependency here, using a simpler toString mechanism for JSON logs 
                // Or you can include GSON/Moshi in your build.gradle and use it here.
                Log.d("JSON LOG", data.toString())
            } catch (e: Exception) {
                Log.e("JSON LOG", "Error encoding JSON", e)
            }
        }
    }
}

class StringLogger : DataLogger<String> {
    override fun log(data: String) {
        if (!AppLogger.disableLogs) {
            Log.d("LOG", data)
        }
    }
}
