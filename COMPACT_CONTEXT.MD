# COMPACT_CONTEXT.MD — Velune Doped Mappa di Contesto Condensata

> [!NOTE]
> Fai riferimento a questo documento o a `graphify-out/wiki/index.md` per le risposte future sull'architettura, evitando la lettura ripetuta dei file di codice sorgente ove non strettamente richiesto per modifiche dirette.

---

## ⚙️ Architettura del Progetto
*   **Stack**: Android App scritta in Kotlin, gestita con Gradle (Gradle 9.5.1, JDK 21 tramite JBR di Android Studio).
*   **Entry Point UI**: [MainActivity.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/MainActivity.kt) — Container principale basato su Jetpack Compose.
*   **Servizio di Riproduzione**: [MusicService.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/playback/MusicService.kt) — Estende `MediaLibraryService` e gestisce l'istanza di ExoPlayer, crossfade, equalizzatore ed integrazioni.
*   **Media Session Callback**: [MediaLibrarySessionCallback.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/playback/MediaLibrarySessionCallback.kt) — Gestisce la risoluzione dell'albero delle categorie browsable per i client di riproduzione esterni (incluso Android Auto di base).
*   **Android Auto (Custom UI)**: [VeluneCarAppService.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/car/VeluneCarAppService.kt) e [VeluneSession.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/car/VeluneSession.kt) — Forniscono un'interfaccia personalizzata per lo schermo del veicolo usando `ListTemplate` per la navigazione in 2 tap e `SearchTemplate`.

---

## 🎵 Integrazione Deezer (Modulo `:deezer`)
*   **Punto d'Ingresso**: [Deezer.kt](file:///D:/android_player/deezer/src/main/kotlin/com/nikhil/yt/deezer/Deezer.kt) — Gestisce autenticazione basata su ARL token, ricerca brani parallela, decrittazione stream via Blowfish CBC, e download diretto dei brani.
*   **Scrittura Tag ID3v2.3**: Implementata in pure Kotlin all'interno di `Deezer.kt` per incorporare copertine dell'album (APIC) e metadati (titolo TIT2, artista TPE1, album TALB) nell'intestazione del file MP3 decrittato.
*   **Deserializzazione Sicura**: In [GwModels.kt](file:///D:/android_player/deezer/src/main/kotlin/com/nikhil/yt/deezer/models/GwModels.kt), gli ID di brani e utenti (`USER_ID`, `SNG_ID`) sono mappati come `JsonElement` per prevenire crash a causa del cambio di tipo dinamico di Deezer (numeri vs stringhe).
*   **Shortcut Downloader**:
    *   In-player: `@Composable` `DeezerDownloadButton` in [PlayerComponents.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/player/PlayerComponents.kt) (con caricamento animato `VeluneLoader`).
    *   Menu contestuali: [SongMenu.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/menu/SongMenu.kt), [YouTubeSongMenu.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/menu/YouTubeSongMenu.kt), e [PlayerMenu.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/menu/PlayerMenu.kt).

---

## 🗂️ Configurazione e Preferenze
*   **DataStore Keys**: Definite in [PreferenceKeys.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/constants/PreferenceKeys.kt):
    *   `EnableDeezerKey` (Boolean) -> Abilita il download Deezer.
    *   `DeezerArlKey` (String) -> Token ARL di autenticazione.
    *   `DeezerQualityKey` (String) -> Qualità di download desiderata (`MP3_128` default, `MP3_320`, `FLAC`).
*   **Schermata Integrazioni**: [IntegrationScreen.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/screens/settings/IntegrationScreen.kt) — Presenta l'editor multiline (`singleLine = false`) per l'ARL, il selettore di qualità, ed è stata rinominata la sezione Discord a *"Integrazione Discord"*.

---

## 🛠️ Modifiche Grafiche ed Iconografia (Branding "Velune Doped")
*   **Launcher Icon & concept**: Combinazione dei cerchi concentrici di YouTube Music con l'equalizzatore di Deezer ed una siringa obliqua di morfina.
*   **File Icone Sostituiti**:
    *   Colore: `ic_launcher_foreground.xml` e `ic_velune_concept.xml` (Impostazioni).
    *   Monocromatico/Notifiche: `about_splash.xml`, `small_icon.xml`, `app_icon_small.xml`, `app_icon_foreground.xml`, `media3_notification_small_icon.xml`.
*   **Nome App**: Impostato a `"Velune Doped"` sia nel set di risorse `main` ([app_name.xml](file:///D:/android_player/app/src/main/res/values/app_name.xml)) sia in quello di `debug` ([app_name.xml](file:///D:/android_player/app/src/debug/res/values/app_name.xml)).
*   **OTA Updater**: Indirizzato sul fork di rilascio [mich-de/Velune-doped](https://github.com/mich-de/Velune-doped) in [Updater.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/utils/Updater.kt), [ReleaseNotesCard.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/component/ReleaseNotesCard.kt) e [UpdateScreen.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/screens/settings/UpdateScreen.kt) per verificare e scaricare gli aggiornamenti direttamente dal fork dopato.
*   **VU Meter Vintage**: Sostituito il disegno vettoriale con una texture retro retroilluminata ([vu_meter_bg.jpg](file:///D:/android_player/app/src/main/res/drawable/vu_meter_bg.jpg)). Le lancette (rossa per il canale sinistro, blu per il destro) ruotano da un punto di pivot inferiore. Lo switch di attivazione è stato inserito direttamente nei controlli superiori in [PlayerComponents.kt](file:///D:/android_player/app/src/main/kotlin/com/nikhil/yt/ui/player/PlayerComponents.kt) accanto al pulsante di download Deezer.
