# YT-Plucker (Android)

Native Android port of the desktop **Xyrus' YT Plucker** app — *pluck videos from YouTube & X,
straight to disk*. Built with **Kotlin + Jetpack Compose**, Clean Architecture (ViewModel +
Coroutines/Flow), and a **foreground service** so downloads keep running at full speed when the app
is minimized or the screen is off.

Downloads are powered by **[youtubedl-android](https://github.com/yausername/youtubedl-android)**
(bundled `yt-dlp` + `ffmpeg`), so it supports **YouTube and X (Twitter)** — plus ~1800 other sites,
4K, playlists, and audio extraction — with the same engine and format logic as the desktop app.
Height-capped quality selections carry a `/b` fallback so limited-resolution sources (like short X
clips) never hard-fail, and the source (YouTube / X) is surfaced after Analyze.

## Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/XyrusCode/xyrus-yt-plucker-android/main/docs/screenshots/1-browser.jpg" alt="Built-in browser on YouTube with one-tap download" width="240">
  &nbsp;&nbsp;
  <img src="https://raw.githubusercontent.com/XyrusCode/xyrus-yt-plucker-android/main/docs/screenshots/2-download-dialog.jpg" alt="Download dialog with quality selection" width="240">
  &nbsp;&nbsp;
  <img src="https://raw.githubusercontent.com/XyrusCode/xyrus-yt-plucker-android/main/docs/screenshots/3-downloads.jpg" alt="Downloads tab with live progress" width="240">
</p>

<p align="center">
  <em>Browse YouTube/X in-app and tap to download&nbsp;·&nbsp;Pick a quality&nbsp;·&nbsp;Track progress, saved straight to your gallery</em>
</p>

## Features (v3)

- **In-app browser** — open YouTube or X inside the app; a floating download button appears on a
  video page so you can grab it without leaving the browser.
- **Download tab** — paste/analyze/download with live progress; self-healing yt-dlp updates.
- **History tab** — lists everything you've downloaded (read from the device's media library); tap
  a row to open it in your device's native player/viewer.
- **Share target** — YT-Plucker appears in the YouTube / X share sheets. Sharing a video prefills
  the URL and jumps to the Download tab, ready to go.
- **Saved by type into the gallery** — finished files land in the public **Movies / Pictures /
  Music** folders (under a `YT-Plucker` album) via MediaStore, so they show up in Gallery/Music
  apps. Nothing is buffered in RAM — files stream to a private working dir then publish to the
  gallery.
- **Android TV** — the same APK installs and launches on Android TV (leanback launcher + banner,
  touchscreen not required), so you can pluck straight from the big screen with a remote.
- **Stable signing** — every build is signed with one committed key (identity
  `xyrus.code.yt-plucker`), so updates install straight over the previous version. Package id:
  `xyrus.code.ytplucker`.

## Architecture

```
UI (Compose)  ──▶  DownloadViewModel  ──startForegroundService──▶  DownloadService
   ▲  collectAsStateWithLifecycle                                      │  Dispatchers.IO
   │                                                                   ▼
   └──── StateFlow<Map<jobId, DownloadProgress>> ◀── DownloadManager (yt-dlp → disk)
                    (DownloadRepositoryImpl singleton)
```

- **`DownloadService`** owns each download's lifecycle, shows the `Downloading: X% • Y MB/s`
  notification, and holds a partial `WakeLock` while any job runs (declared `dataSync` foreground
  type for Android 14+).
- **`DownloadManager`** configures yt-dlp (format strings ported verbatim from the desktop
  `download.rs`) and lets it stream bytes straight to disk — nothing is buffered in the heap.
- **`DownloadRepositoryImpl`** is a Context-free singleton `StateFlow`, so a download stays
  observable across config changes / backgrounding.

Package: `tech.acachi.ytplucker`. Output goes to the app's external `Movies/` dir as `Title [id].ext`.

## Build

Requires JDK 17 and the Android SDK (via Android Studio or command-line tools).

```bash
./gradlew assembleDebug
# APKs land in app/build/outputs/apk/debug/  (per-ABI + a universal APK)
adb install app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

First launch unpacks the bundled Python/yt-dlp/ffmpeg payload (one-time, a few seconds).

> **Dependency version note:** youtubedl-android is pinned to `0.14.0` in
> `gradle/libs.versions.toml` **on purpose** — it is the newest tag that JitPack builds
> successfully for this library (every version ≥ 0.15.0 currently errors on JitPack's build
> infra). The wrapper is dated, but the app calls `updateYoutubeDL()` on first launch to pull a
> current yt-dlp binary, so extraction against today's sites keeps working. Before bumping the pin,
> confirm a green build exists at `https://jitpack.io/#yausername/youtubedl-android`.

## CI

`.github/workflows/android-build.yml` builds an **unsigned debug APK** on every push/PR and uploads
it as a workflow artifact. Pushing a `v*` tag additionally attaches the per-ABI APKs to a GitHub
Release.

This is the standalone Android repo (companion to the desktop app at
[`xyrus-yt-plucker`](https://github.com/XyrusCode/xyrus-yt-plucker)). To cut a release:

```bash
git tag v0.1.0 && git push origin v0.1.0
# → builds the APKs and attaches them to a GitHub Release for that tag.
```

## Notes & limitations

- **APK size:** youtubedl-android bundles Python + yt-dlp + ffmpeg per ABI (~70–100 MB). ABI splits
  keep each installed slice smaller; most phones want `arm64-v8a`.
- **Android 14 `dataSync` cap:** the OS enforces a cumulative ~6h/day budget for `dataSync`
  foreground services — fine for normal use, relevant only for very long batch sessions.
- **Storage:** v1 saves to the app-scoped Movies dir (no storage permission needed). Public-gallery
  export via MediaStore / SAF folder picking is a straightforward follow-up.
