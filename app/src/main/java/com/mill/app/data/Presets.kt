package com.mill.app.data

enum class MediaKind { VIDEO, AUDIO }

data class Preset(
    val id: String,
    val name: String,
    val kind: MediaKind,
    val description: String,
    val ext: String,
    val args: String   // ffmpeg args after -i {IN}
)

object Presets {

    val all = listOf(
        // ── VIDEO ───────────────────────────────────────────
        Preset(
            id = "mp4_tv",
            name = "MP4 for TV",
            kind = MediaKind.VIDEO,
            description = "H.264 1080p + AAC 192k",
            ext = "mp4",
            args = "-c:v libx264 -preset medium -crf 22 -vf scale=-2:1080 -c:a aac -b:a 192k -movflags +faststart"
        ),
        Preset(
            id = "mp4_web",
            name = "MP4 for Web",
            kind = MediaKind.VIDEO,
            description = "H.264 720p + AAC 128k",
            ext = "mp4",
            args = "-c:v libx264 -preset medium -crf 26 -vf scale=-2:720 -c:a aac -b:a 128k -movflags +faststart"
        ),
        Preset(
            id = "mp4_phone",
            name = "MP4 for Phone",
            kind = MediaKind.VIDEO,
            description = "H.264 480p + AAC 96k",
            ext = "mp4",
            args = "-c:v libx264 -preset fast -crf 28 -vf scale=-2:480 -c:a aac -b:a 96k"
        ),
        Preset(
            id = "mkv_remux",
            name = "Remux to MKV",
            kind = MediaKind.VIDEO,
            description = "Copy streams, no re-encode",
            ext = "mkv",
            args = "-c copy"
        ),
        Preset(
            id = "mp4_remux",
            name = "Remux to MP4",
            kind = MediaKind.VIDEO,
            description = "Copy streams, no re-encode",
            ext = "mp4",
            args = "-c copy -movflags +faststart"
        ),
        Preset(
            id = "webm_vp9",
            name = "WebM (VP9+Opus)",
            kind = MediaKind.VIDEO,
            description = "Modern open format",
            ext = "webm",
            args = "-c:v libvpx-vp9 -crf 32 -b:v 0 -c:a libopus -b:a 128k"
        ),
        Preset(
            id = "gif",
            name = "Animated GIF",
            kind = MediaKind.VIDEO,
            description = "First 10 seconds, 480w, 12fps",
            ext = "gif",
            args = "-t 10 -vf fps=12,scale=480:-1:flags=lanczos -loop 0"
        ),
        Preset(
            id = "sub_extract",
            name = "Extract subtitles",
            kind = MediaKind.VIDEO,
            description = "Pull subtitle stream as .srt",
            ext = "srt",
            args = "-map 0:s:0"
        ),
        Preset(
            id = "audio_extract_m4a",
            name = "Extract audio (.m4a)",
            kind = MediaKind.VIDEO,
            description = "Copy audio stream, no re-encode",
            ext = "m4a",
            args = "-vn -c:a copy"
        ),
        Preset(
            id = "thumbnail",
            name = "Thumbnail JPEG",
            kind = MediaKind.VIDEO,
            description = "Frame at 5s, 1280w",
            ext = "jpg",
            args = "-ss 5 -frames:v 1 -vf scale=1280:-1 -q:v 2"
        ),

        // ── AUDIO ───────────────────────────────────────────
        Preset(
            id = "mp3_320",
            name = "MP3 320 kbps",
            kind = MediaKind.AUDIO,
            description = "High-quality MP3",
            ext = "mp3",
            args = "-vn -c:a libmp3lame -b:a 320k"
        ),
        Preset(
            id = "mp3_128",
            name = "MP3 128 kbps",
            kind = MediaKind.AUDIO,
            description = "Portable MP3",
            ext = "mp3",
            args = "-vn -c:a libmp3lame -b:a 128k"
        ),
        Preset(
            id = "flac",
            name = "FLAC (lossless)",
            kind = MediaKind.AUDIO,
            description = "Bit-perfect extraction",
            ext = "flac",
            args = "-vn -c:a flac"
        ),
        Preset(
            id = "aac_m4a",
            name = "AAC (.m4a)",
            kind = MediaKind.AUDIO,
            description = "AAC 192 kbps in M4A",
            ext = "m4a",
            args = "-vn -c:a aac -b:a 192k"
        ),
        Preset(
            id = "ogg",
            name = "OGG Vorbis",
            kind = MediaKind.AUDIO,
            description = "Open format, quality 5",
            ext = "ogg",
            args = "-vn -c:a libvorbis -q:a 5"
        ),
        Preset(
            id = "opus",
            name = "Opus",
            kind = MediaKind.AUDIO,
            description = "Modern, 128 kbps",
            ext = "opus",
            args = "-vn -c:a libopus -b:a 128k"
        ),
        Preset(
            id = "wav",
            name = "WAV (PCM)",
            kind = MediaKind.AUDIO,
            description = "Uncompressed PCM",
            ext = "wav",
            args = "-vn -c:a pcm_s16le"
        ),
        Preset(
            id = "normalize",
            name = "Normalize audio",
            kind = MediaKind.AUDIO,
            description = "EBU R128 loudness, MP3 out",
            ext = "mp3",
            args = "-vn -af loudnorm -c:a libmp3lame -b:a 192k"
        )
    )
}
