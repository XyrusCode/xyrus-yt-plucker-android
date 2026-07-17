# YT-Plucker Promo — Remotion (real MP4)

Motion-graphics ad for YT-Plucker built **entirely in code** with
[Remotion](https://remotion.dev) — React/TypeScript components animated with
`useCurrentFrame()`, `spring()`, and `interpolate()`, rendered to a real H.264
MP4. No AI video, no stock footage, no editor. Same six-scene story as the
[HTML reel](../ytplucker-ad.html), but this pipeline outputs actual video files.

## Output

Rendered files land in **`out/`** (next to this README):

| Composition | File | Size |
|---|---|---|
| `Ad-Landscape` | `out/ytplucker-ad-16x9.mp4` | 1920×1080, 21s, 30fps |
| `Ad-Vertical` | `out/ytplucker-ad-9x16.mp4` | 1080×1920, 21s, 30fps |

Full path on this machine:
`C:\Users\Xyrus\Desktop\XyrusCode\Xyrus-YT-Plucker\docs\ad\remotion\out\`

The videos are **silent** (motion graphics only) — add music in any editor if you
want, or wire audio into the timeline (see Remotion's `<Audio>`).

## Commands

```bash
cd docs/ad/remotion
npm install                 # once

# preview a single frame before committing to a full render (fast)
npx remotion still Ad-Landscape out/frame.png --frame=300

# render the two videos
npm run render              # both 16:9 and 9:16
npm run render:landscape    # just 16:9
npm run render:vertical     # just 9:16

# live editing with a timeline + hot reload in the browser
npm run studio
```

`npx remotion compositions` lists the composition ids.

## Anatomy

```
docs/ad/remotion/
├── package.json           deps: remotion, @remotion/cli, @remotion/google-fonts, react
├── remotion.config.ts     H.264 / MP4 output settings
├── public/                the three app screenshots (via staticFile())
└── src/
    ├── index.ts           registerRoot()
    ├── Root.tsx           <Composition> Ad-Landscape (1920×1080) + Ad-Vertical (1080×1920)
    ├── theme.ts           brand palette (from ui/theme/Theme.kt) + screenshot names
    ├── components.tsx      shared: fonts, Phone frame, Badge logo, Feature layout, fades
    ├── Promo.tsx          master timeline — stitches scenes with <Sequence>
    └── scenes/
        ├── HookScene.tsx       0.0–3.0s
        ├── LogoScene.tsx       3.0–5.5s
        ├── BrowseScene.tsx     5.5–9.5s   (browser shot + pulsing download button)
        ├── QualityScene.tsx    9.5–14.0s  (quality pills + live filling download card)
        ├── PlatformsScene.tsx  14.0–17.0s (Phone · Android TV · Desktop)
        └── CtaScene.tsx        17.0–21.0s
```

## Editing

- **Copy / headlines** — the `scenes/*.tsx` files.
- **Colors / fonts** — `theme.ts` (accent red `#FF3D3D` on `#0F1115`).
- **Pacing** — the `S` array in `Promo.tsx` (scene `from` + `dur`, in frames @30fps)
  and `DURATION` in `Root.tsx`.
- **Screenshots** — replace the JPGs in `public/`; they're referenced by name in
  `theme.ts`.

Both formats share one set of scene components; they adapt (row vs. stacked
layout, sizes) from the composition dimensions, so a copy edit updates both.
