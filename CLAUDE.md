# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

DeBurrow is a native Android **Gopher protocol client** (Kotlin, Jetpack Compose, Coroutines, Room). It is a ground-up rewrite of *Pocket Gopher*, a 2010 J2ME/MIDlet client — the protocol logic (menu/URL parsing, the socket request/response dance) was ported to Kotlin; the entire LCDUI layer was replaced with Compose. The original `PocketGopher.java`/`.jar`/`.jad` are kept in the repo root **for reference only** (not built). Many Kotlin files cite the J2ME method they replace in their KDoc.

## Build & test

Requires a full **JDK 21** (with `javac`) and the **Android SDK (platform 35)**. The app itself compiles to JVM 17 bytecode (`compileSdk`/`targetSdk = 35`, `minSdk = 24`).

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/path/to/a/full/jdk-21"

./gradlew :app:assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # JVM unit tests (protocol + ANSI parser)
./gradlew :app:installDebug         # install on connected device/emulator
```

Run a single test class/method (Gradle test filter):

```bash
./gradlew :app:testDebugUnitTest --tests "dev.debene.gopher.protocol.GopherParserTest"
./gradlew :app:testDebugUnitTest --tests "*GopherUrlTest.parses*"
```

CI (`.github/workflows/ci.yml`) runs `assembleDebug testDebugUnitTest` on push/PR to `main`. Dependencies are pinned in the Gradle version catalog `gradle/libs.versions.toml` — change versions there, not in `build.gradle.kts`.

### Release signing

`assembleRelease` signs with a keystore **only when** the `DEBURROW_KEYSTORE_FILE` / `_PASSWORD` / `DEBURROW_KEY_ALIAS` / `_PASSWORD` env vars are set (CI injects them from secrets). With no keystore the release APK is left **unsigned** (so F-Droid can sign it); for a locally installable build use the debug variant. See `app/build.gradle.kts`.

### F-Droid / reproducibility

This app is distributed via F-Droid, which builds it reproducibly from source. Two build settings exist for that and must not be removed:

- `android { dependenciesInfo { includeInApk = false; includeInBundle = false } }` in `app/build.gradle.kts` — omits the AGP dependency-metadata blob (IzzyOnDroid/F-Droid flag this).
- `distributionSha256Sum` in `gradle/wrapper/gradle-wrapper.properties` — pins the Gradle distribution by checksum; update it alongside the `distributionUrl` whenever the Gradle version changes.

The F-Droid build recipe lives at `fdroid/dev.debene.gopher.yml` (copied into `fdroiddata` for submission — keep this copy in sync with the metadata actually submitted there). Conventions the F-Droid CI enforces, learned the hard way during inclusion (MR [fdroiddata!41663](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/41663)):

- **No `Summary`/`Description` in the recipe** — they're pulled from the upstream Fastlane metadata (`fastlane/metadata/android/en-US/short_description.txt` / `full_description.txt`), which must exist on the release tag.
- **`Builds[].commit` must be a full 40-char commit SHA**, not a `v*` tag or branch (reviewer requirement).
- **`AutoName` must stay in the recipe** — `fdroid checkupdates` regenerates it from the manifest and that CI job fails on any resulting diff. (Only `Summary`/`Description` move to Fastlane; `AutoName` does not.)
- **`AllowedAPKSigningKeys`** is the signing cert's SHA-256 (`apksigner verify --print-certs <signed.apk>`, colons stripped, lowercased) — it opts into Reproducible Builds so F-Droid verifies its build against our key instead of re-signing. One-way door; keep it.
- Keep `versionName`/`versionCode` in sync with `defaultConfig` in `app/build.gradle.kts` and tag releases as `v<versionName>` (e.g. `v2.0.3`). `UpdateCheckMode: Tags` + `AutoUpdateMode: Version` auto-detects new `v*` tags.
- Validate locally before pushing: `fdroid rewritemeta <pkg>` (must produce no diff — canonical form), `fdroid lint <pkg>`, and `check-jsonschema --schemafile <fdroiddata>/schemas/metadata.json <recipe>`. Pin `ruamel.yaml<0.17.22` in the fdroidserver venv or `rewritemeta` reformats long values (e.g. wraps `AllowedAPKSigningKeys`) differently than CI.

## Architecture

Single-activity Compose app. Source lives under `app/src/main/java/dev/debene/gopher/`, organized into four layers. Data flows: **UI → ViewModel → Repository → GopherClient (socket) / Room DAO**.

- **`protocol/`** — pure-Kotlin, Android-free Gopher logic (the most heavily unit-tested part):
  - `GopherType` — RFC 1436 + de-facto type codes. Each maps to a behavioural `Kind` (MENU, TEXT, IMAGE, SEARCH, HTML, BINARY, TELNET, INFO, ERROR); the UI dispatches on `Kind`, not the raw code.
  - `GopherUrl` (parse `gopher://`/`gophers://` addresses), `GopherParser` (parse a menu body into `GopherItem`s), `GopherItem`, `GopherRequest` (host/port/selector/type/`tls`).
  - `GopherClient` — the transport. One `suspend fun fetch(GopherRequest): ByteArray` over a TCP (or `SSLSocket` for `gophers://`) socket, on `Dispatchers.IO`. Coroutine cancellation closes the socket → that's the "Stop" feature. The caller decides how to interpret the returned bytes (menu/text/image).

- **`data/`** — repositories + Room:
  - `GopherRepository` wraps `GopherClient` and transparently caches **menu and text** bodies in Room (10 min TTL, ≤512 KB); images/binaries are never cached. `forceReload` bypasses the cache (used by reload and downloads).
  - `LibraryRepository` — bookmarks, browse history, search history (DAOs in `data/db/`).
  - `DownloadStore` — saves binary items to the system Downloads collection.
  - `data/db/` — `AppDatabase` (Room, `fallbackToDestructiveMigration`, no exported schema), `Entities.kt`, `Daos.kt`.

- **`ui/`** — Compose, unidirectional state:
  - `MainActivity` hosts `AppNavHost` (routes: `browser`, `bookmarks`, `history`). Both ViewModels are scoped to the Activity so the browser keeps its state while the user dips into bookmarks/history.
  - `ui/browser/BrowserViewModel` is the core. It holds the back stack, exposes a single `BrowserUiState` `StateFlow` plus one-shot `events` (a `Channel`), and routes menu taps by `GopherType.Kind` in `open()`. `render()` turns fetched bytes into a `PageContent.{Menu,Text,Image}`. Type-7 (SEARCH) items with no query prompt a dialog before fetching.
  - Viewers: `TextViewer` (uses `AnsiParser` for ANSI-colored art + word-wrap toggle), `ImageViewer`, `MenuList`, `SearchDialog`. `ui/library/` holds the bookmarks/history screens + `LibraryViewModel`. `ui/theme/` is Material 3 theming.

- **`di/AppContainer`** — manual DI graph (deliberately not Hilt). Lazily builds the `AppDatabase`, `GopherClient`, the three repositories, and `DownloadStore`; built once in `GopherApp` (the `Application`). ViewModels get it via `*.factory(container)`.

### Things worth knowing

- The bundled start page is `app/src/main/assets/home.txt` — a literal Gopher **menu** (tab-separated, parsed by `GopherParser`), loaded via `AppContainer.loadHomeMenu()`. Edit it to change the default holes; it defaults to `gopher.debene.dev` holes (see README).
- `gopher://` / `gophers://` deep links from other apps are handled via an intent-filter in `AndroidManifest.xml`; `launchMode="singleTask"` + `onNewIntent` feed links arriving while running into `BrowserViewModel.navigateToAddress` (see `AppNavHost`'s `deepLinks` flow).
- Gopher text bodies are normalised in `BrowserViewModel.normaliseText` (CR/CRLF → LF, strip the trailing lone `.` terminator line).
- Tests are plain JVM JUnit4 (`app/src/test/`) covering `GopherUrl`, `GopherParser`, and `AnsiParser` — keep new protocol logic testable without Android.
