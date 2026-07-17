# YT-Plucker — Ad Reel

Two ways to get the same ~21-second, 6-scene ad for YT-Plucker:

| | **Remotion (real MP4)** — recommended | **HTML reel** (this file) |
|---|---|---|
| What | Motion graphics built in code, rendered to an actual `.mp4` | Self-contained autoplaying webpage you screen-record |
| Output | `remotion/out/ytplucker-ad-16x9.mp4` + `-9x16.mp4` | your screen recording |
| Where | [`remotion/`](remotion/) — `npm install && npm run render` | [`ytplucker-ad.html`](ytplucker-ad.html) |

**Rendered MP4s land here on this machine:**
`C:\Users\Xyrus\Desktop\XyrusCode\Xyrus-YT-Plucker\docs\ad\remotion\out\`
(1920×1080 and 1080×1920, 21s, 30fps, silent). See [remotion/README.md](remotion/README.md).

The rest of this file covers the **HTML reel** — the no-toolchain option you record yourself.

---

## Quick start

1. Double-click `ytplucker-ad.html` (or drag it into Chrome/Edge). It starts playing and loops.
2. Press **Fullscreen** (or the `F` key).
3. Screen-record ~21 seconds (one full loop). Done.

### On-screen controls (top bar)

| Button / key | Does |
|---|---|
| **↻ Replay** / `R` | Restart from the top |
| **16:9** / `L` | Landscape — YouTube / web (1920×1080) |
| **9:16** / `V` | Vertical — Shorts / Reels / phone (1080×1920) |
| **❚❚ Pause** / `Space` | Pause / resume |
| **⛶ Fullscreen** / `F` | Fullscreen |
| `C` | Hide/show the top bar (so it never appears in a recording) |

### URL options (handy for a clean capture)

- `ytplucker-ad.html?format=vertical` — start in 9:16 (`?format=landscape` for 16:9, the default).
- `ytplucker-ad.html?clean=1` — hide the control bar from the first frame.
- Combine: `ytplucker-ad.html?format=vertical&clean=1`.

---

## The 21-second storyboard

| Time | Scene | Says |
|---|---|---|
| 0.0–3.0s | Hook | "That video you wanted to **keep**? Screen-recording is over." |
| 3.0–5.5s | Logo | **YT-Plucker** — *pluck videos from YouTube & X, straight to disk* |
| 5.5–9.5s | Browse | Browser screenshot, pulsing download button — **"Browse. Tap. Plucked."** |
| 9.5–14.0s | Quality | Quality picker + a live progress bar filling to *Completed* — **"Every quality. Every site."** |
| 14.0–17.0s | Everywhere | Downloads screenshot + **Phone · Android TV · Desktop** |
| 17.0–21.0s | CTA | **Free · Open source · No ads** → *Get it on GitHub* |

---

## Recording to MP4

### 📁 Where the finished MP4 lands on your device

| How you record | MP4 saves to (on this PC) |
|---|---|
| **Xbox Game Bar** (Win) | `C:\Users\Xyrus\Videos\Captures\` |
| **OBS Studio** | the folder set in **Settings → Output → Recording Path** (default `C:\Users\Xyrus\Videos\`) |
| **macOS** `Shift+Cmd+5` | Desktop by default (change via the recorder's **Options** menu) |
| **Linux / GNOME** | `~/Videos/` |

After recording, rename the two files so they're easy to find, e.g.
`ytplucker-ad-16x9.mp4` and `ytplucker-ad-9x16.mp4`. If you want them kept with the project, drop them
in this folder (`docs/ad/`) — note they're large binaries, so add them to `.gitignore` unless you
deliberately want them committed.

### Easiest — Windows Xbox Game Bar (built in)
1. Open the reel, press `F` (fullscreen) and `C` (hide the bar), then `R` to replay from the top.
2. Press **`Win + Alt + R`** to start recording (or `Win + G` for the overlay). Record one loop (~21s).
3. **The clip lands in `C:\Users\Xyrus\Videos\Captures\`** (Windows' fixed Game Bar folder — it can't
   be changed). Trim to exactly one loop in the Photos app or any editor.

> Game Bar records the whole window at your monitor's resolution. For a clean 16:9 frame, use a
> ~1920×1080 monitor/window; the reel letterboxes on other sizes (that's expected — crop in editing).

### Best quality — OBS Studio (free, recommended for both aspects)
1. **Sources → Display Capture** (or Window Capture on the browser).
2. **Settings → Video → Base & Output (Scaled) Resolution:**
   - **16:9:** `1920 × 1080`
   - **9:16:** `1080 × 1920`  (open the reel with `?format=vertical`)
3. Right-click the source → **Transform → Fit to screen**, then crop to the reel's stage so only the
   `#0F1115` canvas is captured (the reel is already centered/letterboxed to that exact aspect).
4. **Settings → Output → Recording:** set **Recording Format = MP4**, 60 fps, high bitrate
   (~12–20 Mbps), and note the **Recording Path** — that's the folder your MP4 saves to.
5. Fullscreen the reel, `C` to hide the bar, `R` to replay, hit **Start Recording**, capture one loop.
   The MP4 appears in that Recording Path the moment you **Stop Recording**.

Record **twice** — once at 16:9, once with `?format=vertical` at 9:16 — for the two deliverables.

### macOS / Linux
- **macOS:** `Shift + Cmd + 5` → record the browser window (saves to **Desktop** by default — change
  it via the **Options** menu in that same toolbar); or OBS as above.
- **Linux:** OBS as above, or `Shift + Ctrl + Alt + R` (GNOME screen recorder → saves to `~/Videos/`).

---

## Adding music (optional but worth it)

The reel has no audio. Drop the MP4 into any editor (CapCut, DaVinci Resolve, Premiere, Clipchamp)
and lay a track under it. Beat cues are marked in the HTML — hit these for a polished cut:

- **0.0s** low riser under the hook
- **3.0s** BOOM on the logo snap-in
- **5.5 / 9.5 / 14.0s** soft ticks on each scene change
- **17.0s** downbeat on the CTA, hold a pad to the end

Grab royalty-free tracks from YouTube Audio Library, Pixabay Music, or Uppbeat.

---

## Editing the reel

Everything is in the one HTML file:

- **Copy / headlines** — the `<section class="scene">` blocks in the body.
- **Colors** — the `:root` CSS variables at the top (ported from `ui/theme/Theme.kt`; accent red
  `#FF3D3D` on `#0F1115`).
- **Timing** — the `TL` array and `END` in the `<script>` at the bottom (scene start times, in seconds).
- **Screenshots** — embedded as base64 in the `--shot-*` CSS variables. To refresh them after a UI
  change, re-run the inline base64 and paste, or regenerate with:
  ```bash
  # from repo root — prints a data URI you can paste into the --shot-* variable
  node -e "console.log('data:image/jpeg;base64,'+require('fs').readFileSync('docs/screenshots/1-browser.jpg').toString('base64'))"
  ```

## Previewing locally
A helper config lives at `.claude/launch.json` (serves this folder on port 8791). Or just open the
HTML file directly — it needs no server.
