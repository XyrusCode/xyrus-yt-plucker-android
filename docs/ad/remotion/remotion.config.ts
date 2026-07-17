import { Config } from "@remotion/cli/config";

Config.setVideoImageFormat("jpeg");
Config.setOverwriteOutput(true);
// H.264 / MP4 out of the box; Remotion bundles its own ffmpeg.
Config.setCodec("h264");
// Force software GL — headless Chrome otherwise hangs on GPU init on Windows,
// which surfaces as "Timed out after 25000 ms while trying to connect to the browser".
Config.setChromiumOpenGlRenderer("angle");
// Give the browser more room to come up on a cold/AV-scanned first launch.
Config.setDelayRenderTimeoutInMilliseconds(60000);
