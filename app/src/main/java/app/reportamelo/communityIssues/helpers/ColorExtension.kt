package app.reportamelo.communityIssues.helpers

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

fun Color.toHex(): String {
    val a = (this.alpha * 255).roundToInt()
    val r = (this.red * 255).roundToInt()
    val g = (this.green * 255).roundToInt()
    val b = (this.blue * 255).roundToInt()

    return if (a != 255) {
        String.format("%02X%02X%02X%02X", r, g, b, a)
    } else {
        String.format("%02X%02X%02X", r, g, b)
    }
}

fun Color.Companion.fromHex(hex: String): Color {
    val cleanHex = hex.replace("#", "").replace(Regex("[^a-zA-Z0-9]"), "")
    return when (cleanHex.length) {
        3 -> {
            val r = cleanHex[0].toString().repeat(2).toInt(16)
            val g = cleanHex[1].toString().repeat(2).toInt(16)
            val b = cleanHex[2].toString().repeat(2).toInt(16)
            Color(red = r, green = g, blue = b, alpha = 255)
        }
        6 -> {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Color(red = r, green = g, blue = b, alpha = 255)
        }
        8 -> {
            val a = cleanHex.substring(0, 2).toInt(16)
            val r = cleanHex.substring(2, 4).toInt(16)
            val g = cleanHex.substring(4, 6).toInt(16)
            val b = cleanHex.substring(6, 8).toInt(16)
            Color(red = r, green = g, blue = b, alpha = a)
        }
        else -> Color.White
    }
}
