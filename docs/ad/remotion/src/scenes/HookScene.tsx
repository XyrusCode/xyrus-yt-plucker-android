import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { SceneProps, useLayout, useSceneOpacity, rise, Backdrop } from "../components";

export const HookScene: React.FC<SceneProps> = ({ dur }) => {
  const frame = useCurrentFrame();
  const { base, vertical } = useLayout();
  const op = useSceneOpacity(dur);
  const underline = interpolate(frame, [24, 38], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill
      style={{
        opacity: op,
        alignItems: "center",
        justifyContent: "center",
        flexDirection: "column",
        padding: "0 8%",
        textAlign: "center",
      }}
    >
      <Backdrop color="rgba(255,61,61,0.06)" />
      <div
        style={{
          ...rise(frame, 2),
          color: theme.accent,
          fontWeight: 800,
          letterSpacing: base * 12,
          fontSize: base * 22,
          textTransform: "uppercase",
          marginBottom: base * 22,
        }}
      >
        Stop
      </div>
      <h1
        style={{
          ...rise(frame, 6),
          margin: 0,
          fontWeight: 800,
          letterSpacing: "-0.02em",
          lineHeight: 1.05,
          fontSize: base * (vertical ? 92 : 108),
          color: theme.text,
        }}
      >
        That video you
        <br />
        wanted to{" "}
        <span style={{ position: "relative", whiteSpace: "nowrap" }}>
          keep
          <span
            style={{
              position: "absolute",
              left: "-0.03em",
              right: "-0.03em",
              bottom: "0.04em",
              height: "0.12em",
              borderRadius: 6,
              background: `linear-gradient(90deg, ${theme.accent}, #ff7a5c)`,
              transformOrigin: "left center",
              transform: `scaleX(${underline})`,
            }}
          />
        </span>
        ?
      </h1>
      <p
        style={{
          ...rise(frame, 32),
          marginTop: base * 34,
          marginBottom: 0,
          color: theme.dim,
          fontWeight: 600,
          fontSize: base * 34,
        }}
      >
        Screen-recording is over.
      </p>
    </AbsoluteFill>
  );
};
