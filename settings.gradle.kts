pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // youtubedl-android (yt-dlp + ffmpeg + aria2c packaging) is published on JitPack.
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "Xyrus-YT-Plucker"
include(":app")
