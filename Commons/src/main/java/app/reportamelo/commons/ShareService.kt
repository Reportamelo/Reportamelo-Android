package app.reportamelo.commons

import android.content.Context
import android.content.Intent

class ShareService(private val context: Context) {

    fun shareContent(text: String, title: String = "Share") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(shareIntent)
    }
}
