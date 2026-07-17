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

const Pill: React.FC<{ hot?: boolean; children: React.ReactNode }> = ({
  hot,
  children,
}) => (
  <span
    style={{
      fontWeight: 700,
      fontSize: 26,
      padding: "8px 20px",
      borderRadius: 40,
      background: hot ? "rgba(255,61,61,0.14)" : theme.panel2,
      border: `2px solid ${hot ? "rgba(255,61,61,0.5)" : theme.border}`,
      color: hot ? "#ffb3a8" : theme.text,
    }}
  >
    {children}
  </span>
);

const DownloadCard: React.FC<{ frame: number; width: number }> = ({
  frame,
  width,
}) => {
  const p = interpolate(frame, [22, 112], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const pct = Math.round(p * 100);
  const done = p >= 1;
  const etaS = Math.max(0, Math.round(61 * (1 - p)));
  const eta = `${Math.floor(etaS / 60)}:${("0" + (etaS % 60)).slice(-2)}`;
  return (
    <div
      style={{
        width,
        maxWidth: "100%",
        background: theme.panel2,
        border: `2px solid ${theme.border}`,
        borderRadius: 18,
        padding: "22px 24px",
        textAlign: "left",
        boxSizing: "border-box",
      }}
    >
      <div
        style={{
          fontSize: 27,
          fontWeight: 700,
          color: theme.text,
          whiteSpace: "nowrap",
          overflow: "hidden",
          textOverflow: "ellipsis",
        }}
      >
        Absolute Ben 10 Forms Revealed!
      </div>
      <div
        style={{
          height: 11,
          borderRadius: 20,
          background: "#2c313b",
          margin: "18px 0 14px",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            height: "100%",
            width: `${2 + p * 98}%`,
            borderRadius: 20,
            background: `linear-gradient(90deg, ${theme.accent}, #ff7a5c)`,
          }}
        />
      </div>
      <div
        style={{
          fontSize: 24,
          fontWeight: 700,
          color: done ? theme.ok : theme.accent,
        }}
      >
        {done
          ? "Completed · saved to gallery"
          : `${pct}% · 2.63 MB/s · ETA ${eta}`}
      </div>
    </div>
  );
};

export const QualityScene: React.FC<SceneProps> = ({ vertical, dur }) => {
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
            <Phone src={shots.dialog} height={phoneH} />
          </div>
        }
      >
        <div
          style={{
            ...rise(frame, 8),
            display: "flex",
            flexWrap: "wrap",
            gap: 12,
            justifyContent: vertical ? "center" : "flex-start",
          }}
        >
          <Pill hot>4K</Pill>
          <Pill>1080p</Pill>
          <Pill>MP3</Pill>
          <Pill>Playlists</Pill>
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
          Every quality.
          <br />
          <span style={{ color: theme.accent }}>Every site.</span>
        </h2>
        <p
          style={{
            ...rise(frame, 20),
            margin: `0 0 ${base * 18}px`,
            color: theme.dim,
            fontWeight: 500,
            lineHeight: 1.35,
            fontSize: base * 30,
          }}
        >
          YouTube, X &amp; 1800+ sites. Downloads keep running when you leave the
          app.
        </p>
        <div style={{ ...rise(frame, 26), width: "100%", display: "flex", justifyContent: vertical ? "center" : "flex-start" }}>
          <DownloadCard frame={frame} width={base * (vertical ? 620 : 560)} />
        </div>
      </Feature>
    </AbsoluteFill>
  );
};
