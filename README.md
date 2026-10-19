# 🌾 Mill

> *Media conversion, refined.*

A modern Android media converter. Fast, private, offline. Zero cloud, zero accounts, zero tracking.

Powered by FFmpeg. Everything runs on your device.

---

## Features

### Conversion
- **18 presets** across video, audio, remux, and utility
- **Video** — MP4 for TV/Web/Phone (H.264), Remux MKV/MP4 (no re-encode), WebM VP9+Opus, Animated GIF, Thumbnail JPEG, Subtitle extraction
- **Audio** — MP3 320/128, FLAC, AAC/M4A, OGG Vorbis, Opus, WAV, EBU R128 normalization
- **Remux** — instant stream copy, no quality loss

### Batch & Queue
- **Multi-select** — pick many files at once
- **Multi-format** — one source → many outputs (MP3 + FLAC + thumbnail in one tap)
- **Sequential queue** — processes one job at a time
- **Live progress** — real percentage per job
- **Cancel individual or all** — stop mid-conversion
- **Persistent queue** — survives app restarts

### Power User
- **Preset combos** — save your favorite multi-format workflows
- **File size estimates** — see output size before you convert
- **Custom output folder** — SAF picker, per-app
- **Watch folder** — auto-convert any new file dropped into a folder
- **Share intent** — send files from any app to Mill

### Local & Private
- **100% on-device** — FFmpeg runs natively, nothing leaves your phone
- **No accounts** — no sign-up, no telemetry
- **No internet permission** required for core functionality
- **Open source** — GPL-3.0

---

## Download

Prebuilt APKs: [Releases](https://github.com/SimoesCTT/mill/releases)

Also available on **[Corn Store](https://simoesctt.github.io/corn-store-index/)** — an index of free and open-source Android apps.

---

## Build

```bash
git clone https://github.com/SimoesCTT/mill.git
cd mill
./gradlew assembleDebug
Requirements:

Android Studio Hedgehog (2023.1.1) or newer

JDK 17+

Gradle 8.9 (bundled via wrapper)

Android SDK 35

Release build:

./gradlew assembleReleaseAPK at app/build/outputs/apk/release/app-release.apk.

Tech Stack
Layer	Technology
Language	Kotlin 2.1
Build	Gradle 8.9, AGP 8.7.2
Media engine	FFmpegKit 8.1 (maintained fork)
UI	Material 3, ViewBinding
Async	Kotlin Coroutines
Persistence	SharedPreferences + JSON
Foreground service	ConvertService (dataSync)
Design
Dark corporate. Material 3. High contrast, clean typography, card-based layouts.

Background: #121212

Surface: #1E1E1E

Accent: #FFFFFF

Muted text: #888888

No frills, no gimmicks.

Permissions
READ_MEDIA_VIDEO / READ_MEDIA_AUDIO — access media files (Android 13+)

READ_EXTERNAL_STORAGE — legacy fallback (Android 12 and below)

MANAGE_EXTERNAL_STORAGE — write to any output folder

FOREGROUND_SERVICE + FOREGROUND_SERVICE_DATA_SYNC — background conversions

POST_NOTIFICATIONS — progress notifications

License
GPL-3.0 — free software, forever. See LICENSE.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Author
Americo N. F. Simoes — @SimoesCTT

🌾 Media conversion, refined.
