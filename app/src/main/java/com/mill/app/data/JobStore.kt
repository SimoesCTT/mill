package com.mill.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object JobStore {
    private const val PREFS = "mill_jobs"
    private const val KEY = "jobs_json"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Job> {
        val raw = prefs(ctx).getString(KEY, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val out = mutableListOf<Job>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Job(
                id = o.getString("id"),
                inputUri = o.getString("inputUri"),
                inputName = o.getString("inputName"),
                presetId = o.getString("presetId"),
                presetName = o.optString("presetName", o.getString("presetId")),
                outputPath = o.optString("outputPath").takeIf { it.isNotBlank() },
                status = runCatching { JobStatus.valueOf(o.getString("status")) }
                    .getOrDefault(JobStatus.QUEUED),
                progress = o.optDouble("progress", 0.0).toFloat(),
                message = o.optString("message"),
                startedAt = o.optLong("startedAt"),
                finishedAt = o.optLong("finishedAt")
            ))
        }
        return out
    }

    fun save(ctx: Context, jobs: List<Job>) {
        val arr = JSONArray()
        jobs.forEach { j ->
            arr.put(JSONObject().apply {
                put("id", j.id)
                put("inputUri", j.inputUri)
                put("inputName", j.inputName)
                put("presetId", j.presetId)
                put("presetName", j.presetName)
                put("outputPath", j.outputPath ?: "")
                put("status", j.status.name)
                put("progress", j.progress.toDouble())
                put("message", j.message)
                put("startedAt", j.startedAt)
                put("finishedAt", j.finishedAt)
            })
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun addJob(ctx: Context, job: Job) {
        val list = load(ctx)
        list.add(job)
        save(ctx, list)
    }

    fun update(ctx: Context, id: String, transform: (Job) -> Job) {
        val list = load(ctx)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = transform(list[idx])
            save(ctx, list)
        }
    }

    fun remove(ctx: Context, id: String) {
        save(ctx, load(ctx).filterNot { it.id == id })
    }

    fun clearDone(ctx: Context) {
        save(ctx, load(ctx).filterNot {
            it.status == JobStatus.DONE ||
            it.status == JobStatus.FAILED ||
            it.status == JobStatus.CANCELLED
        })
    }
}
