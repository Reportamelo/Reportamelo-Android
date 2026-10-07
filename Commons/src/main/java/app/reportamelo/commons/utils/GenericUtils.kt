package app.reportamelo.commons.utils

import android.content.Context

fun getBundle(context: Context): String {
    return context.packageName ?: "dev.FranciscoHernandez.default"
}

fun userAlias(userName: String): String {
    return "@$userName"
}
