import React from "react";
import {
  AbsoluteFill,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { theme } from "../theme";
import {
  SceneProps,
  useLayout,
  useSceneOpacity,
  rise,
  Badge,
  Backdrop,
} from "../components";

export const LogoScene: React.FC<SceneProps> = ({ dur }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { base, vertical } = useLayout();
  const op = useSceneOpacity(dur);

  const s = spring({ frame, fps, config: { damping: 11, stiffness: 120 } });
  const scale = interpolate(s, [0, 1], [0.7, 1]);
  const glow = (Math.sin(frame / 8) * 0.5 + 0.5) * 42;

  return (
    <AbsoluteFill
      style={{
        opacity: op,
        alignItems: "center",
        justifyContent: "center",
        flexDirection: "column",
        gap: base * 20,
      }}
    >
      <Backdrop />
      <div style={{ transform: `scale(${scale})` }}>
        <Badge size={base * 175} glow={glow} />
      </div>
      <div
        style={{
          ...rise(frame, 10),
          fontSize: base * (vertical ? 96 : 108),
          fontWeight: 800,
          letterSpacing: "-0.03em",
          color: theme.text,
          marginTop: base * 8,
        }}
      >
        YT<span style={{ color: theme.accent }}>-</span>Plucker
      </div>
      <div
        style={{
          ...rise(frame, 16),
          fontSize: base * 32,
          fontWeight: 600,
          color: theme.dim,
        }}
      >
        pluck videos from YouTube &amp; X — straight to disk
      </div>
    </AbsoluteFill>
  );
};
