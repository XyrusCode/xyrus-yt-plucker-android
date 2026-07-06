# YT-Plucker (Android)

Native Android port of the desktop **yt-grab** app — *videos & playlists, straight to disk*.
Built with **Kotlin + Jetpack Compose**, Clean Architecture (ViewModel + Coroutines/Flow), and a
**foreground service** so downloads keep running at full speed when the app is minimized or the
screen is off.

Downloads are powered by **[youtubedl-android](https://github.com/yausername/youtubedl-android)**
(bundled `yt-dlp` + `ffmpeg`), so it supports YouTube and ~1800 other sites, 4K, playlists, and
audio extraction — the same engine and format logic as the desktop app.

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
it as a workflow artifact. Pushing a `v*` tag additionally attaches the APKs to a GitHub Release.

This folder isn't wired to a remote yet. To publish and trigger the Action:

```bash
git init && git add . && git commit -m "Initial commit"
gh repo create Xyrus-YT-Plucker --private --source=. --remote=origin --push
# then, to cut a release:
git tag v0.1.0 && git push origin v0.1.0
```

## Notes & limitations

- **APK size:** youtubedl-android bundles Python + yt-dlp + ffmpeg per ABI (~70–100 MB). ABI splits
  keep each installed slice smaller; most phones want `arm64-v8a`.
- **Android 14 `dataSync` cap:** the OS enforces a cumulative ~6h/day budget for `dataSync`
  foreground services — fine for normal use, relevant only for very long batch sessions.
- **Storage:** v1 saves to the app-scoped Movies dir (no storage permission needed). Public-gallery
  export via MediaStore / SAF folder picking is a straightforward follow-up.
