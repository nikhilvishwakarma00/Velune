
<div align="center">

 <img src="https://raw.githubusercontent.com/mich-de/Velune-doped/main/fastlane/metadata/android/en-US/images/icon.png" width="110" />

</div>

# 💉 velune-doped
<div align="center">

<pre>
██╗   ██╗███████╗██╗     ██╗   ██╗███╗   ██╗███████╗
██║   ██║██╔════╝██║     ██║   ██║████╗  ██║██╔════╝
██║   ██║█████╗  ██║     ██║   ██║██╔██╗ ██║█████╗
╚██╗ ██╔╝██╔══╝  ██║     ██║   ██║██║╚██╗██║██╔══╝
 ╚████╔╝ ███████╗███████╗╚██████╔╝██║ ╚████║███████╗
  ╚═══╝  ╚══════╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝
</pre>

**Velune** — YouTube Music client • No Ads • No Subscription • Full Control

<a href="#-android-auto-custom-ui">Android Auto</a> • <a href="#-deezer-downloader">Deezer</a> • <a href="#-whats-different">What's Different</a>

</div>

---

## 🚗 Android Auto Custom UI

Template‑based Android Auto via `androidx.car.app:1.7.0`, optimized for 7‑inch horizontal screens.

| Feature | Description |
|---------|-------------|
| `CarAppService` + `VeluneSession` | Replaces AA's default UI with custom templates |
| `ListTemplate` | Category browsing: Songs, Artists, Albums, Playlists, Liked Songs |
| `SearchTemplate` | Voice search via Google Assistant |
| 2‑tap playback | Liked Songs & Downloaded Songs accessible directly from root |

---

## 📥 Deezer Downloader

Download FLAC, MP3 320kbps, or MP3 128kbps directly from Deezer's CDN via ARL‑based auth — embedded ID3v2.3 tags, album art, saved to `Music/Deezer/`.

| Feature | Description |
|---------|-------------|
| **Quality Selector** | Choose between `MP3_128`, `MP3_320`, or `FLAC` in settings. Automatic fallback to lower quality if not supported by ARL account type. |
| **Multiline ARL Input** | Multiline text area in integrations panel for comfortable pasting of 192/256-character tokens. |
| Song context menu | `PlayerMenu.kt` — "Download from Deezer ($quality)" |
| Player top bar | `PlayerComponents.kt` — download icon next to menu |

---

## 💉 What's Different

This fork adds **everything above** plus fixes and quality‑of‑life improvements:

| Area | Change |
|------|--------|
| **Player scrolling** | Removed `userScrollEnabled = !isPortrait` — the player menu was unscrollable in portrait mode, hiding the Deezer button |
| **Lyrics caching** | Never save `LYRICS_NOT_FOUND` to DB — avoids permanent "no lyrics" lock after transient network errors |
| **Lyrics auto‑fetch** | `ShowLyricsKey` defaults to `true` — lyrics pre‑fetched on song change |
| **Lyrics fetch lifecycle** | Fixed `rememberCoroutineScope().launch` leak inside `LaunchedEffect` — fetch is now properly scoped |
| **CarAppService** | Full `ListTemplate` + `SearchTemplate` custom UI (V1) |
| **AA content styles** | Root categories use `CONTENT_STYLE_LIST_ITEM` for 7‑inch compact display |
| **Deezer module** | `:deezer` JVM module — file‑based logging, MediaStore save, StripeDecryptor |
| **Kizzy Discord RPC** | Disabled by default (privacy / battery) |
| **New Branding** | "Velune Doped" name, hybrid syringe/equalizer launcher and monochrome status bar/notification icons |
| **Gradle Builds** | Output APKs are automatically named with `Velune_Doped_v<version>` base name |

---

## ✨ Features (upstream + fork)

| Category | Features |
|----------|----------|
| 🎵 Core | Ad‑free, full library sync, offline caching, background playback |
| 🔊 Audio | Gapless, crossfade, silence skipping, EBU R128 normalization, tempo/pitch, system EQ |
| 🎤 Lyrics | 6 providers, word‑by‑word sync, romanization (JP/KO), translation |
| 🎨 UI | Material You, synced lyrics, personalized home, year‑in‑review stats |
| 🚗 Car | AA template‑based UI, voice search, media browsing |
| 📥 Download | Deezer 128kbps MP3 with ID3v2.3 + album art |
| 🤝 Together | LAN + Online synchronized listening |
| 📊 Scrobbling | Last.fm + ListenBrainz |

---

## 🧠 Architecture

MVVM + UDF • Compose + M3 • Media3 ExoPlayer • Hilt DI • Room • Ktor • Coil 3

```
velune-doped/
├── app/                    ← Android app (UI, playback, DI, car)
├── innertube/              ← YouTube InnerTube API client
├── deezer/                 ← Deezer ARL auth + MP3 download
├── lrclib/                 ← LRC lyrics
├── kugou/                  ← KuGou lyrics
├── simpmusic/              ← SimpMusic lyrics
├── betterlyrics/           ← TTML lyrics
├── lastfm/                 ← Last.fm scrobbling
├── kizzy/                  ← Discord RPC (disabled)
└── canvas/                 ← Animated artwork
```

---

## 🔗 Original

Fork of [Velune](https://github.com/nikhilvishwakarma00/Velune) by Nikhil. Upstream features preserved.

## ⚖️ Legal

Independent client. Not affiliated with YouTube, Google, or Deezer.  
Licensed under **GPL-3.0**.
