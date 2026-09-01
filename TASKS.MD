# Velune Android Auto & Deezer Roadmap

Here is the task roadmap for integrating and optimizing the Android Auto experience and Deezer downloads for Velune.

## Completed Tasks

### Android Auto Optimizations
- [x] [PLAYBACK_SERVICE] Update `browsableMediaItem` helper in `MediaLibrarySessionCallback.kt` to support customizable content styling hints. <!-- id: 1 -->
- [x] [PLAYBACK_SERVICE] Modify `onGetChildren` root folder resolution in `MediaLibrarySessionCallback.kt` to style category folders as lists. <!-- id: 2 -->
- [x] [PLAYBACK_SERVICE] Add "Liked Songs" and "Downloaded Songs" shortcuts directly under `MusicService.ROOT` in `MediaLibrarySessionCallback.kt`. <!-- id: 3 -->
- [x] [SECURITY] Disabled Discord RPC features inside `MusicService.kt` to enforce user privacy and stop background Discord worker connections. <!-- id: 6 -->
- [x] [CAR_APP] Added `androidx.car.app:app:1.7.0` dependency. <!-- id: 7 -->
- [x] [CAR_APP] Updated `automotive_app_desc.xml` with `<uses name="template" />`. <!-- id: 8 -->
- [x] [CAR_APP] Declared `VeluneCarAppService` in `AndroidManifest.xml`. <!-- id: 9 -->
- [x] [CAR_APP] Created `VeluneCarAppService.kt` — entry point for Android Auto custom UI. <!-- id: 10 -->
- [x] [CAR_APP] Created `VeluneSession.kt` — custom `ListTemplate` root + `SearchTemplate`. <!-- id: 11 -->
- [x] [CAR_APP] Connected to `MusicService` via `MediaController` for playback control. <!-- id: 12 -->

### Deezer Integration & Downloads
- [x] [DEEZER] Fixed kotlinx.serialization parsing failures (USER_ID and error properties) during Deezer login. <!-- id: 13 -->
- [x] [DEEZER] Added "Download from Deezer" option to SongMenu.kt and YouTubeSongMenu.kt so it's accessible from all list views. <!-- id: 14 -->
- [x] [DEEZER] Implemented custom ID3v2.3 metadata tagger (Title, Artist, Album, Cover Art) in Deezer.kt to tag downloaded files. <!-- id: 15 -->
- [x] [DEEZER] Added physical "Download Deezer" button with loading animations to the Now Playing player screen in PlayerComponents.kt. <!-- id: 16 -->

### Documentation
- [x] [DOCUMENTATION] Update `GEMINI.MD` with progress details and architecture decisions. <!-- id: 4 -->
- [x] [DOCUMENTATION] Update `README.MD` with details on Android Auto features. <!-- id: 5 -->

## Next Steps
- [x] [CAR_APP] V2: Sub-category browsing with database-backed lists. <!-- id: 17 -->
- [x] [CAR_APP] V2: Search result templates and MediaBrowser navigation integration. <!-- id: 18 -->
