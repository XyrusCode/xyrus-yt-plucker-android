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

const Tag: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <span
    style={{
      fontSize: 26,
      fontWeight: 700,
      color: theme.dim,
      border: `2px solid ${theme.border}`,
      borderRadius: 40,
      padding: "8px 22px",
      background: "#12151b",
    }}
  >
    {children}
  </span>
);

export const CtaScene: React.FC<SceneProps> = ({ dur }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { base, vertical } = useLayout();
  const op = useSceneOpacity(dur);

  const s = spring({ frame, fps, config: { damping: 11, stiffness: 120 } });
  const scale = interpolate(s, [0, 1], [0.7, 1]);
  const glow = (Math.sin(frame / 8) * 0.5 + 0.5) * 42;

  const btn = spring({
    frame: frame - 15,
    fps,
    config: { damping: 12, stiffness: 140 },
  });

  return (
    <AbsoluteFill
      style={{
        opacity: op,
        alignItems: "center",
        justifyContent: "center",
        flexDirection: "column",
        gap: base * 16,
      }}
    >
      <Backdrop color="rgba(255,61,61,0.16)" />
      <div style={{ transform: `scale(${scale})` }}>
        <Badge size={base * 130} glow={glow} />
      </div>
      <div
        style={{
          ...rise(frame, 8),
          fontSize: base * (vertical ? 90 : 104),
          fontWeight: 800,
          letterSpacing: "-0.03em",
          color: theme.text,
          marginTop: base * 6,
        }}
      >
        YT<span style={{ color: theme.accent }}>-</span>Plucker
      </div>
      <div style={{ ...rise(frame, 12), display: "flex", gap: 12 }}>
        <Tag>Free</Tag>
        <Tag>Open source</Tag>
        <Tag>No ads</Tag>
      </div>
      <div
        style={{
          transform: `scale(${btn})`,
          marginTop: base * 10,
          fontSize: base * 38,
          fontWeight: 800,
          color: "#fff",
          background: theme.accent,
          padding: "20px 46px",
          borderRadius: 50,
          boxShadow: "0 16px 50px rgba(255,61,61,0.5)",
        }}
      >
        Get it on GitHub
      </div>
      <div
        style={{
          ...rise(frame, 20),
          marginTop: base * 12,
          fontSize: base * 28,
          fontWeight: 600,
          color: theme.dim,
        }}
      >
        github.com/XyrusCode · <b style={{ color: theme.text }}>xyrus.code.ytplucker</b>
      </div>
    </AbsoluteFill>
  );
};
