package com.mill.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object SizeEstimator {

    /**
     * Estimate output size in bytes for a preset.
     * Returns 0 if unknown.
     */
    fun estimate(ctx: Context, uri: Uri, preset: Preset): Long {
        return try {
            val (durationSec, videoBitrate) = probe(ctx, uri)
            if (durationSec <= 0.0) return 0L

            when (preset.id) {
                "mp3_320" -> (40_000 * durationSec / 8).toLong()    // 320 kbps
                "mp3_128" -> (16_000 * durationSec / 8).toLong()    // 128 kbps
                "aac_m4a" -> (24_000 * durationSec / 8).toLong()    // 192 kbps
                "ogg" -> (24_000 * durationSec / 8).toLong()
                "opus" -> (16_000 * durationSec / 8).toLong()
                "flac" -> (700_000 * durationSec / 8).toLong()      // ~700 kbps avg
                "wav" -> (1_411_200 * durationSec / 8).toLong()     // 44.1kHz 16-bit stereo
                "normalize" -> (24_000 * durationSec / 8).toLong()
                "mp4_tv" -> ((5_000_000 + 192_000) * durationSec / 8).toLong()  // 5 Mbps + 192k
                "mp4_web" -> ((2_500_000 + 128_000) * durationSec / 8).toLong()
                "mp4_phone" -> ((1_000_000 + 96_000) * durationSec / 8).toLong()
                "webm_vp9" -> ((1_500_000 + 128_000) * durationSec / 8).toLong()
                "mkv_remux", "mp4_remux" -> {
                    // Copy streams — size roughly equals source
                    val srcSize = try {
                        ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: 0L
                    } catch (_: Exception) { 0L }
                    srcSize
                }
                "gif" -> (10 * 480 * 480 / 8).toLong() // rough: 10s of 480w
                "thumbnail" -> 250_000L
                "sub_extract" -> 100_000L
                "audio_extract_m4a" -> if (videoBitrate > 0) 0L else 0L
                else -> 0L
            }
        } catch (_: Exception) { 0L }
    }

    private fun probe(ctx: Context, uri: Uri): Pair<Double, Long> {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(ctx, uri)
            val durationMs = mmr.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val bitrate = mmr.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_BITRATE
            )?.toLongOrNull() ?: 0L
            mmr.release()
            Pair(durationMs / 1000.0, bitrate)
        } catch (_: Exception) { Pair(0.0, 0L) }
    }

    fun human(bytes: Long): String {
        return when {
            bytes <= 0 -> "?"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
