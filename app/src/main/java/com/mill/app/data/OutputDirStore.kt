package com.mill.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri

object OutputDirStore {
    private const val PREFS = "mill_output"
    private const val KEY = "custom_tree"
    private const val KEY_ENABLED = "use_custom"

    fun saveTree(ctx: Context, uri: Uri) {
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, uri.toString()).putBoolean(KEY_ENABLED, true).apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).putBoolean(KEY_ENABLED, false).apply()
    }

    fun getTree(ctx: Context): Uri? {
        val s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return null
        return Uri.parse(s)
    }

    fun isEnabled(ctx: Context): Boolean = getTree(ctx) != null
}
