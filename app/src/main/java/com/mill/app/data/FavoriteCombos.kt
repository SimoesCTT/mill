package com.mill.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Combo(val id: String, val name: String, val presetIds: List<String>)

object FavoriteCombos {

    private const val PREFS = "mill_fav_combos"
    private const val KEY = "combos_json"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Combo> {
        val raw = prefs(ctx).getString(KEY, null) ?: return defaultCombos()
        val arr = JSONArray(raw)
        val out = mutableListOf<Combo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("presetIds")
            val list = mutableListOf<String>()
            for (j in 0 until ids.length()) list.add(ids.getString(j))
            out.add(Combo(o.getString("id"), o.getString("name"), list))
        }
        return if (out.isEmpty()) defaultCombos() else out
    }

    fun save(ctx: Context, combos: List<Combo>) {
        val arr = JSONArray()
        combos.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("presetIds", JSONArray(c.presetIds))
            })
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, combo: Combo) {
        val list = load(ctx).toMutableList()
        list.removeAll { it.id == combo.id }
        list.add(combo)
        save(ctx, list)
    }

    fun remove(ctx: Context, id: String) {
        save(ctx, load(ctx).filterNot { it.id == id })
    }

    /** First-run defaults so users see the feature working immediately. */
    private fun defaultCombos(): MutableList<Combo> {
        return mutableListOf(
            Combo(
                id = "default_podcast",
                name = "Podcast package",
                presetIds = listOf("mp3_128", "normalize")
            ),
            Combo(
                id = "default_archive",
                name = "Archive quality",
                presetIds = listOf("flac", "mkv_remux", "sub_extract")
            ),
            Combo(
                id = "default_share",
                name = "Quick share",
                presetIds = listOf("mp4_phone", "thumbnail")
            )
        )
    }
}
