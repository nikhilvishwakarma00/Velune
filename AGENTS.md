# Velune — Playback + AA fork

## Branch workflow
- `origin` = `git@github.com:mich-de/velune-doped.git`
- `upstream` = `git@github.com:nikhilvishwakarma00/Velune.git`
- Feature branch only, never `main`. Sync: `git fetch upstream && git rebase upstream/main`

## Build
SSL cert blocks `gradlew`. Use cached Gradle directly:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.6.1-all\4xnwe4ed5w7qqynisxnnvegss\gradle-9.6.1\bin\gradle.bat" assembleArm64Debug
```

ADB: `C:\Users\mdeangelis\AppData\Local\Android\Sdk\platform-tools\adb.exe`
Debug package: `com.nikhil.yt.debug` → `run-as com.nikhil.yt.debug`
Tests: `gradlew :app:testArm64DebugUnitTest`

## Modules
9 lib modules (all pure Kotlin JVM — no Android SDK):

| Module | Type | Purpose |
|--------|------|---------|
| `:app` | android-app | UI, playback, DI |
| `:innertube` | jvm | YouTube InnerTube API |
| `:kugou` | jvm | KuGou lyrics |
| `:lrclib` | jvm | LrcLib lyrics |
| `:lastfm` | jvm | Last.fm scrobbling |
| `:simpmusic` | jvm | SimpMusic lyrics |
| `:betterlyrics` | jvm | TTML lyrics |
| `:canvas` | jvm | Animated artwork |
| `:kizzy` | jvm | Discord RPC |
| `:deezer` | jvm | Deezer ARL auth + download |

**Kotlin JVM modules cannot use `android.util.Log`** — use file-based logging via `File.appendText` or `println`.

## Gradle quirks
- `ksp.incremental=false` — fixes `Storage already registered` crash
- `16kPageAlignment=true` — required for Pixel 10 (Android 15+)
- Separate `ioScope` and `scope` in `MusicService` — `collectLatest(ioScope)` not `scope`
- **Versions** in `gradle/libs.versions.toml` — keep at latest stable. Check Maven Central / Jetpack releases page before edits. Update Gradle wrapper via `gradlew wrapper --gradle-version=LATEST`.

## Lyrics
- 6 providers: SimpMusic, BetterLyrics, LrcLib, KuGou, YouTubeSubtitle, YouTubeLyrics
- `ShowLyricsKey` default = `true` (MusicService pre-fetches on song change)
- **Do not save `LYRICS_NOT_FOUND` to DB** — causes permanent "no lyrics" cache with no retry
- `LyricsScreen.kt` auto-fetches on open; `MusicService.kt` pre-fetches on play
- Lyrics only visible in full-screen `LyricsScreen` (bottom sheet), not in main player view

## PlayerMenu
- Deezer button at line 648 is **unconditional** (no `if` guard)
- **Do NOT add `userScrollEnabled = !isPortrait`** back — that blocked scrolling in portrait mode
- Two Deezer download entry points: `PlayerMenu.kt:648` (menu) + `PlayerComponents.kt` (player top actions)

## Deezer (`:deezer` module)
- ARL-based auth (256-char hex string), 128kbps MP3 download
- File-based logging: `Deezer.setLogDir(context.cacheDir)` → `cacheDir/deezer_log.txt`
- Retrieve log: `adb exec-out run-as com.nikhil.yt.debug cat /data/data/com.nikhil.yt.debug/cache/deezer_log.txt`
- Download path: `Music/Deezer/` via MediaStore `RELATIVE_PATH` (API 29+)

## Android Auto (`feat/android-auto-ui`)
- `car/VeluneCarAppService.kt` → `VeluneSession.kt`
- `VeluneSession.kt`: `ListTemplate` (root categories) + `SearchTemplate`
- Playback via `MediaController.setMediaItems()` → resolved by `onSetMediaItems` in `MediaLibrarySessionCallback`
- `MediaController` **is** `Player` (implements the interface)
- No `Player.UNSET_TIME` — use `C.TIME_UNSET`
- `SearchTemplate.Builder` requires explicit `SearchCallback` object (no lambda)
- **AA search fix**: `SongItem.toMediaItem()` sets `mediaId = videoId` (no `song/` prefix). AA search items reach `onSetMediaItems` with plain videoId. Handler at `MediaLibrarySessionCallback.kt:1144` checks `!mediaId.contains("/")` and sets `isPlayable = true` to allow playback.
- `ResolvingDataSource.Factory` (`MusicService.kt:4084`) resolves streams: `dataSpec.key` (= `customCacheKey` = videoId) → `YTPlayerUtils.playerResponseForPlayback` → stream URL

## Version constraints (GRADLE QUIET DOWNGRADE)
- **DO NOT upgrade Kotlin** past 2.3.10 — Kotlin 2.4.x Compose compiler generates stable hashes (`graphicsLayer-56HxDYs$default` etc.) that don't exist in any published Compose runtime (1.10.x, 1.11.x). Causes `NoSuchMethodError: graphicsLayer` on launch.
- **DO NOT upgrade Compose** past 1.10.4 — Compose 1.11.x artifacts have same hashes but only work with specific Kotlin versions. 1.10.4 matches JetBrains Compose 1.10.1 (transitive from `reorderable`, `shimmer`, `coil`, `backdrop`).
- Always match Compose ↔ Kotlin ↔ KSP. Current tested: Kotlin 2.3.10, Compose 1.10.4, KSP 2.3.6.

## Key files
| File | Purpose |
|------|---------|
| `playback/MusicService.kt` | ~4800-line MediaLibraryService — ExoPlayer, crossfade, equalizer, auto-mix, together |
| `playback/MediaLibrarySessionCallback.kt` | Media3 session callback — AA root items, search, `onSetMediaItems` |
| `playback/PlayerConnection.kt` | UI↔Service bridge via StateFlow |
| `car/VeluneSession.kt` | AA custom UI screens |
| `extensions/MediaItemExt.kt` | `SongItem.toMediaItem()` — mediaId = videoId with no prefix |
| `lyrics/LyricsHelper.kt` | Provider orchestration, LRU cache, romanization |
| `deezer/Deezer.kt` | ARL auth, search, StripeDecryptor download |
| `ui/menu/PlayerMenu.kt` | Song context menu — Deezer button at line 648 |
| `ui/player/PlayerComponents.kt` | Player top actions — Deezer download icon |
