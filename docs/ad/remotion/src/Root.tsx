import { Composition } from "remotion";
import { Promo } from "./Promo";
import { FPS } from "./theme";

// 21s @ 30fps = 630 frames
const DURATION = 630;

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="Ad-Landscape"
        component={Promo}
        durationInFrames={DURATION}
        fps={FPS}
        width={1920}
        height={1080}
        defaultProps={{ vertical: false }}
      />
      <Composition
        id="Ad-Vertical"
        component={Promo}
        durationInFrames={DURATION}
        fps={FPS}
        width={1080}
        height={1920}
        defaultProps={{ vertical: true }}
      />
    </>
  );
};
