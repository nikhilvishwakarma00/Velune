# Velune Android Auto & Deezer Optimization Plan

This implementation plan outlines the optimizations and architectural changes implemented to make **Velune** fully optimized for Android Auto (especially on 7-inch touch screens) and to support direct, high-quality audio downloads from Deezer with embedded metadata.

## 1. Context & Architectural Overview

Velune is a Jetpack Compose & Media3-based YouTube Music client.
- `MusicService` extending `MediaLibraryService` manages audio playback.
- `MediaLibrarySessionCallback` handles browse and search queries.
- Android Auto custom template-based service `VeluneCarAppService` and `VeluneSession` are declared to provide a driver-safe, optimized horizontal touch screen experience.

### System Environment Paths
*   **Android SDK Path:** `C:\Users\mdeangelis\AppData\Local\Android\Sdk`
*   **ADB Executable:** [adb.exe](file:///C:/Users/mdeangelis/AppData/Local/Android/Sdk/platform-tools/adb.exe)
*   **Android Studio Executable:** [studio64.exe](file:///C:/Program%20Files/Android/Android%20Studio/bin/studio64.exe)

---

## 2. Completed Optimizations & Implementations

### Phase 1: Android Auto Layout Optimizations
- **Flattened Category Nodes:** Surfaces "Liked Songs" and "Downloaded Songs" directly on the root menu of Android Auto, reducing navigation path to 2 taps.
- **Content Styling:** Root-level abstract directories (Songs, Albums, Artists, Playlists) are styled as compact lists (`CONTENT_STYLE_LIST_ITEM`) to fit 7-inch vehicle touch screens perfectly. Actual albums and playlists retain grid layouts (`CONTENT_STYLE_GRID_ITEM`) to display artwork.
- **Privacy Enforcement:** Discord RPC background tracking is completely disabled to optimize battery usage and prevent arbitrary HTTP requests.

### Phase 2: Android Auto Custom UI (Template-based)
- Created `VeluneCarAppService` and `VeluneSession` using the Android for Cars App Library.
- Implemented `BrowseRootScreen` (`ListTemplate`) and `SearchScreen` (`SearchTemplate`) for seamless category browsing and Voice Search.

### Phase 3: Deezer Integration & Direct Downloads
- **Serialization Bug Fix:** Resolved a kotlinx.serialization parsing bug in `GwModels.kt` by declaring `USER_ID` and `SNG_ID` as `JsonElement` and `error` as `JsonElement?`, allowing the app to successfully parse Deezer's numeric IDs and array error responses without throwing silent exceptions.
- **Menu Downloader Additions:** Exposed "Download from Deezer" option dynamically across all song lists ([SongMenu.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/menu/SongMenu.kt), [YouTubeSongMenu.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/menu/YouTubeSongMenu.kt), [PlayerMenu.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/menu/PlayerMenu.kt)) based on the integration preferences toggled in settings.
- **Now Playing UI Shortcut:** Added a dedicated, stylized action button (`DeezerDownloadButton`) directly on the main player page ([PlayerComponents.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/player/PlayerComponents.kt)) across all 5 styles (V1, V2, V3/V5, V4) with an inline loading spinner (`VeluneLoader`) to provide clear visual feedback during downloads.

### Phase 4: Native ID3v2.3 Tagger
- Implemented a custom pure-Kotlin ID3v2.3 tag encoder inside `Deezer.kt` to write metadata directly.
- Downloads album cover art from Deezer (`track.artworkUrl`) and embeds it in the `"APIC"` (attached picture) frame.
- Writes `"TIT2"` (Title), `"TPE1"` (Artist), and `"TALB"` (Album) text frames.
- Saves tagged files directly to the standard public storage directory (`Music/Deezer/Artist - Title.mp3`) so they are read properly by system players and car dashboard units.
