import React from "react";
import {
  AbsoluteFill,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { theme, shots } from "../theme";
import {
  SceneProps,
  useLayout,
  useSceneOpacity,
  rise,
  Phone,
  Feature,
} from "../components";

const Chip: React.FC<{ bg: string; children: React.ReactNode }> = ({
  bg,
  children,
}) => (
  <span
    style={{
      background: bg,
      color: "#fff",
      fontWeight: 800,
      fontSize: 26,
      padding: "9px 22px",
      borderRadius: 14,
      boxShadow: `0 8px 26px ${bg}55`,
    }}
  >
    {children}
  </span>
);

/** Pulsing ring over the real download FAB in the browser screenshot. */
const FabRing: React.FC<{ size: number }> = ({ size }) => {
  const frame = useCurrentFrame();
  const t = (frame % 45) / 45; // 1.5s loop
  const ringScale = 0.7 + t * 1.5;
  const ringOpacity = interpolate(t, [0, 1], [0.9, 0]);
  return (
    <div
      style={{
        position: "absolute",
        left: "50%",
        top: "79%",
        width: size,
        height: size,
        transform: "translate(-50%,-50%)",
      }}
    >
      <div
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: size * 0.3,
          boxShadow: `0 0 0 4px ${theme.accent}, 0 0 40px 6px rgba(255,61,61,0.6)`,
        }}
      />
      <div
        style={{
          position: "absolute",
          inset: -6,
          borderRadius: size * 0.32,
          border: `3px solid ${theme.accent}`,
          opacity: ringOpacity,
          transform: `scale(${ringScale})`,
        }}
      />
    </div>
  );
};

export const BrowseScene: React.FC<SceneProps> = ({ vertical, dur }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { base } = useLayout();
  const op = useSceneOpacity(dur);
  const phoneH = base * (vertical ? 980 : 780);

  const enter = spring({ frame, fps, config: { damping: 14, stiffness: 90 } });
  const phoneY = interpolate(enter, [0, 1], [40, 0]);

  return (
    <AbsoluteFill style={{ opacity: op }}>
      <Feature
        vertical={vertical}
        phone={
          <div style={{ transform: `translateY(${phoneY}px)`, opacity: enter }}>
            <Phone src={shots.browser} height={phoneH}>
              <FabRing size={base * 90} />
            </Phone>
          </div>
        }
      >
        <div style={{ ...rise(frame, 8), display: "flex", gap: 14 }}>
          <Chip bg={theme.yt}>YouTube</Chip>
          <Chip bg={theme.x}>X / Twitter</Chip>
        </div>
        <h2
          style={{
            ...rise(frame, 14),
            margin: `${base * 14}px 0 ${base * 10}px`,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            lineHeight: 1.05,
            fontSize: base * (vertical ? 66 : 78),
            color: theme.text,
          }}
        >
          Browse. Tap.
          <br />
          <span style={{ color: theme.accent }}>Plucked.</span>
        </h2>
        <p
          style={{
            ...rise(frame, 20),
            margin: 0,
            color: theme.dim,
            fontWeight: 500,
            lineHeight: 1.35,
            fontSize: base * 30,
          }}
        >
          The built-in browser drops a one-tap download button on any video — no
          copy-pasting links.
        </p>
      </Feature>
    </AbsoluteFill>
  );
};
