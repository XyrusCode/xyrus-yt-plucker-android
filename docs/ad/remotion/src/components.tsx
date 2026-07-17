import React from "react";
import {
  AbsoluteFill,
  Img,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { loadFont } from "@remotion/google-fonts/Inter";
import { theme, SHOT_RATIO } from "./theme";

export const { fontFamily } = loadFont("normal", {
  weights: ["500", "600", "700", "800"],
  subsets: ["latin"],
  ignoreTooManyRequestsWarning: true,
});

export type SceneProps = { vertical: boolean; dur: number };

/** Physical scale unit relative to the 1080px short side (== 1 for both formats). */
export const useLayout = () => {
  const { width, height } = useVideoConfig();
  const vertical = height > width;
  const base = Math.min(width, height) / 1080;
  return { width, height, vertical, base };
};

/** Scene container opacity — soft cross-fade against the shared background. */
export const useSceneOpacity = (dur: number, fadeIn = 8, fadeOut = 9) => {
  const f = useCurrentFrame();
  return interpolate(f, [0, fadeIn, dur - fadeOut, dur], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
};

/** Fade + rise entrance for a single element, starting at `delay`. */
export const rise = (frame: number, delay: number, dist = 26) => {
  const p = interpolate(frame, [delay, delay + 14], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  return { opacity: p, transform: `translateY(${(1 - p) * dist}px)` };
};

/** The app's red play-badge logo, drawn in pure CSS. */
export const Badge: React.FC<{ size: number; glow?: number }> = ({
  size,
  glow = 0,
}) => (
  <div
    style={{
      width: size,
      height: size,
      borderRadius: size * 0.25,
      background: theme.accent,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      boxShadow: `0 ${size * 0.1}px ${size * 0.5 + glow}px rgba(255,61,61,${
        0.45 + glow / 260
      }), inset 0 0 0 ${Math.max(1, size * 0.012)}px rgba(255,255,255,.1)`,
    }}
  >
    <div
      style={{
        width: 0,
        height: 0,
        marginLeft: size * 0.07,
        borderTop: `${size * 0.23}px solid transparent`,
        borderBottom: `${size * 0.23}px solid transparent`,
        borderLeft: `${size * 0.38}px solid #fff`,
      }}
    />
  </div>
);

/** A device frame wrapping one of the app screenshots. */
export const Phone: React.FC<{
  src: string;
  height: number;
  children?: React.ReactNode;
}> = ({ src, height, children }) => {
  const width = height * SHOT_RATIO;
  const border = Math.max(4, Math.round(height * 0.012));
  return (
    <div
      style={{
        position: "relative",
        height,
        width,
        flex: "0 0 auto",
        borderRadius: height * 0.055,
        border: `${border}px solid #23272f`,
        background: "#000",
        overflow: "hidden",
        boxShadow:
          "0 40px 90px rgba(0,0,0,.6), 0 0 0 2px rgba(52,57,68,.5)",
      }}
    >
      <Img
        src={staticFile(src)}
        style={{
          position: "absolute",
          inset: 0,
          width: "100%",
          height: "100%",
          objectFit: "cover",
          objectPosition: "top center",
        }}
      />
      {children}
    </div>
  );
};

/** Feature-scene layout: phone + copy, side-by-side (landscape) or stacked (vertical). */
export const Feature: React.FC<{
  vertical: boolean;
  phone: React.ReactNode;
  children: React.ReactNode;
}> = ({ vertical, phone, children }) => (
  <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
    <div
      style={{
        display: "flex",
        flexDirection: vertical ? "column" : "row",
        alignItems: "center",
        justifyContent: "center",
        gap: vertical ? 48 : 96,
        padding: vertical ? "0 70px" : "0 120px",
        width: "100%",
        height: "100%",
        boxSizing: "border-box",
      }}
    >
      {phone}
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: vertical ? "center" : "flex-start",
          textAlign: vertical ? "center" : "left",
          maxWidth: vertical ? 860 : 660,
        }}
      >
        {children}
      </div>
    </div>
  </AbsoluteFill>
);

/** Subtle radial depth glow shared by every scene. */
export const Backdrop: React.FC<{ color?: string }> = ({
  color = "rgba(255,61,61,0.10)",
}) => (
  <AbsoluteFill
    style={{
      background: `radial-gradient(60% 45% at 50% 42%, ${color}, transparent 70%)`,
    }}
  />
);
