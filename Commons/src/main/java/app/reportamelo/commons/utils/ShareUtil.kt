package app.reportamelo.commons.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

fun shareFromClosure(context: Context, item: Uri?) {
    if (item == null) return
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, item.toString())
        type = "text/plain"
    }
    
    val chooser = Intent.createChooser(shareIntent, null)
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
