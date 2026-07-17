// Brand kit — ported verbatim from the Android app's ui/theme/Theme.kt
export const theme = {
  bg: "#0F1115",
  panel: "#181B22",
  panel2: "#1F232C",
  border: "#2A2F3A",
  text: "#E8EAF0",
  dim: "#9AA1AF",
  accent: "#FF3D3D",
  accent2: "#B32020",
  ok: "#3ECF6E",
  warn: "#F0B429",
  yt: "#FF0000",
  x: "#1DA1F2",
} as const;

// Screenshots live in public/ and are referenced via staticFile()
export const shots = {
  browser: "1-browser.jpg",
  dialog: "2-download-dialog.jpg",
  downloads: "3-downloads.jpg",
} as const;

// Native screenshot aspect ratio (720 x 1600)
export const SHOT_RATIO = 720 / 1600;

export const FPS = 30;
