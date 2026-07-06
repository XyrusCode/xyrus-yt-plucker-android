# youtubedl-android relies on reflection into its bundled Python/yt-dlp bridge.
# Keep its classes intact if minification is ever enabled.
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**
