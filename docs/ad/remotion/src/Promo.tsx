import React from "react";
import { AbsoluteFill, Sequence } from "remotion";
import { theme } from "./theme";
import { fontFamily } from "./components";
import { HookScene } from "./scenes/HookScene";
import { LogoScene } from "./scenes/LogoScene";
import { BrowseScene } from "./scenes/BrowseScene";
import { QualityScene } from "./scenes/QualityScene";
import { PlatformsScene } from "./scenes/PlatformsScene";
import { CtaScene } from "./scenes/CtaScene";

// scene starts + lengths (frames @30fps) — mirrors the HTML reel storyboard
const S = [
  { from: 0, dur: 90, C: HookScene }, //   0.0 – 3.0s
  { from: 90, dur: 75, C: LogoScene }, //  3.0 – 5.5s
  { from: 165, dur: 120, C: BrowseScene }, // 5.5 – 9.5s
  { from: 285, dur: 135, C: QualityScene }, // 9.5 – 14.0s
  { from: 420, dur: 90, C: PlatformsScene }, // 14.0 – 17.0s
  { from: 510, dur: 120, C: CtaScene }, // 17.0 – 21.0s
];

export const Promo: React.FC<{ vertical: boolean }> = ({ vertical }) => {
  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(80% 60% at 50% 42%, #14171d 0%, ${theme.bg} 55%, #0b0d10 100%)`,
        fontFamily,
      }}
    >
      {S.map(({ from, dur, C }, i) => (
        <Sequence key={i} from={from} durationInFrames={dur} name={C.name}>
          <C vertical={vertical} dur={dur} />
        </Sequence>
      ))}
    </AbsoluteFill>
  );
};
