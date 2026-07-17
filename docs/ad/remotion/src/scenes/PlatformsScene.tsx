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

const ICONS: Record<string, React.ReactNode> = {
  phone: (
    <>
      <rect x="7" y="2" width="10" height="20" rx="2" />
      <line x1="11" y1="18" x2="13" y2="18" />
    </>
  ),
  tv: (
    <>
      <rect x="2" y="4" width="20" height="13" rx="2" />
      <line x1="8" y1="21" x2="16" y2="21" />
    </>
  ),
  desktop: (
    <>
      <rect x="2" y="4" width="20" height="12" rx="2" />
      <line x1="8" y1="20" x2="16" y2="20" />
      <line x1="12" y1="16" x2="12" y2="20" />
    </>
  ),
};

const Device: React.FC<{
  icon: keyof typeof ICONS;
  label: string;
  hot?: boolean;
  delay: number;
}> = ({ icon, label, hot, delay }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const s = spring({
    frame: frame - delay,
    fps,
    config: { damping: 12, stiffness: 130 },
  });
  const box = 88;
  return (
    <div
      style={{
        transform: `scale(${s})`,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 14,
        color: hot ? theme.accent : theme.dim,
        fontWeight: 700,
        fontSize: 28,
      }}
    >
      <div
        style={{
          width: box,
          height: box,
          borderRadius: 22,
          background: hot ? "rgba(255,61,61,0.12)" : theme.panel2,
          border: `2px solid ${hot ? "rgba(255,61,61,0.55)" : theme.border}`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <svg
          width={box * 0.56}
          height={box * 0.56}
          viewBox="0 0 24 24"
          fill="none"
          stroke={hot ? "#fff" : theme.text}
          strokeWidth={1.7}
        >
          {ICONS[icon]}
        </svg>
      </div>
      {label}
    </div>
  );
};

export const PlatformsScene: React.FC<SceneProps> = ({ vertical, dur }) => {
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
            <Phone src={shots.downloads} height={phoneH} />
          </div>
        }
      >
        <h2
          style={{
            ...rise(frame, 6),
            margin: `0 0 ${base * 12}px`,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            lineHeight: 1.05,
            fontSize: base * (vertical ? 66 : 78),
            color: theme.text,
          }}
        >
          Saved to your
          <br />
          <span style={{ color: theme.accent }}>gallery.</span>
        </h2>
        <p
          style={{
            ...rise(frame, 12),
            margin: 0,
            color: theme.dim,
            fontWeight: 500,
            lineHeight: 1.35,
            fontSize: base * 30,
          }}
        >
          Finished files land in Movies / Music / Pictures — ready to watch
          anywhere.
        </p>
        <div style={{ display: "flex", gap: vertical ? 40 : 34, marginTop: base * 34 }}>
          <Device icon="phone" label="Phone" delay={12} />
          <Device icon="tv" label="Android TV" hot delay={18} />
          <Device icon="desktop" label="Desktop" delay={24} />
        </div>
      </Feature>
    </AbsoluteFill>
  );
};
