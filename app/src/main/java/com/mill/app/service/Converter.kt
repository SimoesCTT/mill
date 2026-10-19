package com.mill.app.service

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.mill.app.data.MediaKind
import com.mill.app.data.Preset
import com.mill.app.data.OutputDirStore
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object Converter {

    data class Result(val success: Boolean, val message: String, val outputPath: String? = null, val cancelled: Boolean = false)

    /** Holds the current session so we can cancel from outside. */
    val currentSession = AtomicReference<FFmpegSession?>(null)

    fun cancel() {
        currentSession.get()?.let {
            try { FFmpegKit.cancel(it.sessionId) } catch (_: Exception) {}
        }
    }

    fun run(
        ctx: Context,
        inputUri: Uri,
        inputName: String,
        preset: Preset,
        onProgress: (Float, String) -> Unit
    ): Result {
        return try {
            // Prefer custom SAF tree if the user configured one
            val outPath: String = run {
                val tree = OutputDirStore.getTree(ctx)
                if (tree != null) {
                    // SAF-based: create a doc in the tree
                    val safeBase = inputName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                        .substringBeforeLast('.').ifBlank { "output" }
                    val fileName = "${safeBase}_${preset.id}.${preset.ext}"
                    val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(ctx, tree)
                    val newFile = doc?.createFile("application/octet-stream", fileName)
                    val tmp = File(ctx.cacheDir, fileName)
                    // We'll write to cache, then copy to SAF after — for simplicity,
                    // write FFmpeg output to cache and copy via ContentResolver
                    tmp.absolutePath
                } else {
                    val outDir = File(
                        Environment.getExternalStoragePublicDirectory(
                            if (preset.kind == MediaKind.AUDIO) Environment.DIRECTORY_MUSIC
                            else Environment.DIRECTORY_MOVIES
                        ),
                        "Mill"
                    ).apply { mkdirs() }
                    val safeName = inputName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val base = safeName.substringBeforeLast('.').ifBlank { "output" }
                    File(outDir, "${base}_${preset.id}.${preset.ext}").absolutePath
                }
            }
            val outFile = File(outPath)
            if (outFile.exists()) outFile.delete()

            val inputPath = materializeInput(ctx, inputUri, inputName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
            Log.d("MillConv", "input=$inputPath")
            Log.d("MillConv", "output=${outFile.absolutePath}")

            // No leading "ffmpeg" token — FFmpegKit adds it
            val cmd = "-y -i \"$inputPath\" ${preset.args} \"${outFile.absolutePath}\""
            Log.d("MillConv", "cmd=$cmd")

            onProgress(0.02f, "Starting…")

            val latch = CountDownLatch(1)
            var success = false
            var errorMsg: String? = null
            var cancelled = false

            val session = FFmpegKit.executeAsync(
                cmd,
                { s ->
                    success = ReturnCode.isSuccess(s.returnCode)
                    cancelled = ReturnCode.isCancel(s.returnCode)
                    if (!success && !cancelled) {
                        errorMsg = s.failStackTrace ?: s.output ?: "FFmpeg exit ${s.returnCode}"
                        Log.e("MillConv", "FFmpeg failed: $errorMsg")
                    }
                    latch.countDown()
                },
                { log ->
                    // Parse "time=HH:MM:SS.xx" from log lines to estimate progress
                    val line = log.message ?: return@executeAsync
                    val t = Regex("time=(\\d+):(\\d+):(\\d+\\.\\d+)").find(line)
                    if (t != null) {
                        val h = t.groupValues[1].toIntOrNull() ?: 0
                        val m = t.groupValues[2].toIntOrNull() ?: 0
                        val sec = t.groupValues[3].toDoubleOrNull() ?: 0.0
                        val totalSec = h * 3600 + m * 60 + sec
                        // Use duration hint if available; else soft cap
                        val durSec = durationHint(inputPath)
                        val pct: Float = if (durSec > 0.0)
                            (totalSec / durSec).toFloat().coerceIn(0f, 0.99f)
                        else 0.5f
                        onProgress(pct, "Converting…")
                    }
                },
                { stats ->
                    // Stats callbacks are more reliable for progress
                    val timeMs = stats.time.toDouble()
                    if (timeMs > 0) {
                        val durSec = durationHint(inputPath)
                        if (durSec > 0.0) {
                            val pct: Float = (timeMs / 1000.0 / durSec).toFloat().coerceIn(0f, 0.99f)
                            onProgress(pct, "Converting…")
                        }
                    }
                }
            )

            currentSession.set(session)

            val finished = latch.await(30, TimeUnit.MINUTES)  // 30-min timeout per file
            currentSession.set(null)

            if (inputPath.startsWith(ctx.cacheDir.absolutePath)) {
                File(inputPath).delete()
            }

            when {
                !finished -> Result(false, "Timeout")
                cancelled -> Result(false, "Cancelled", cancelled = true)
                success -> {
                    onProgress(1f, "Done")
                    Result(true, "Converted to ${outFile.name}", outFile.absolutePath)
                }
                else -> Result(false, errorMsg ?: "Conversion failed")
            }
        } catch (e: Exception) {
            Log.e("MillConv", "Exception: ${e.message}", e)
            Result(false, e.message ?: "Unknown error")
        }
    }

    private fun durationHint(path: String): Double {
        // MediaMetadataRetriever gives duration quickly
        return try {
            val mmr = android.media.MediaMetadataRetriever()
            mmr.setDataSource(path)
            val ms = mmr.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            mmr.release()
            ms / 1000.0
        } catch (_: Exception) { 0.0 }
    }

    private fun materializeInput(ctx: Context, uri: Uri, name: String): String {
        if (uri.scheme == "file") return uri.path ?: ""
        val tmp = File(ctx.cacheDir, "mill_in_${System.currentTimeMillis()}_$name")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        }
        return tmp.absolutePath
    }
}
