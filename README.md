# Gopher (android-gopher)

A native, modern **Android Gopher client** built with Kotlin, Jetpack Compose,
Coroutines, and Room.

## Lineage & attribution

This is an **evolution** of **Pocket Gopher**, a J2ME / MIDlet Gopher client written
in 2010 by **Felix Pleșoianu** (`felixp7@yahoo.com`), with Gopher URL/menu parsing
code and ideas by **Nuno J. Silva** (`gopher://sdf-eu.org/1/users/njsg`).

The original protocol logic (menu parsing, URL parsing, the socket request/response
dance) was ported to Kotlin and corrected where needed; the entire J2ME LCDUI layer
was replaced with a Compose UI and a modern architecture. The original MIT license and
copyright are retained — see [LICENSE](LICENSE) — and Felix and Nuno are credited above.
The app now ships under its own identity (`dev.debene.gopher`, "DeBurrow"); the original
sources (`PocketGopher.java`, `.jar`, `.jad`) are kept in the repo root for reference.

## What's new vs. Pocket Gopher

- Kotlin + Jetpack Compose (Material 3) single-Activity UI; LazyColumn scrolling
  replaces manual PgUp/PgDn pagination
- Coroutines/StateFlow instead of raw threads; cancellable loads ("Stop")
- Address-bar navigation, browse history, **bookmarks**, and **search history** (Room)
- **Downloads** for binary item types (scoped storage / MediaStore)
- **Response caching** for menus and text
- **TLS** support (`gophers://`)
- `gopher://` / `gophers://` deep-link handling from other apps

## Project layout

```
app/src/main/java/dev/debene/gopher/
  protocol/   GopherType, GopherItem, GopherRequest, GopherUrl, GopherParser, GopherClient
  data/       Room DB + DAOs, GopherRepository (cache), LibraryRepository, DownloadStore
  ui/         Compose browser, viewers, bookmarks/history, theme, navigation
  di/         AppContainer (manual DI)
app/src/test/  GopherUrl / GopherParser unit tests
app/src/main/assets/home.txt   bundled start page (Gopher menu format)
```

## Building

Requires a JDK with a compiler (`javac`) and the Android SDK (platform 35).

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/path/to/a/full/jdk-21"   # a JRE will not work
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :app:assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # protocol unit tests
./gradlew :app:installDebug         # install on a connected device/emulator
```

Or open the project in Android Studio and Run.

## Releases (CI/CD)

Two GitHub Actions workflows live in `.github/workflows/`:

- **CI** (`ci.yml`) — on every push / PR to `main`: builds the debug APK, runs the unit
  tests, and uploads the APK as a build artifact.
- **Release** (`release.yml`) — on pushing a `v*` tag: runs tests, builds a minified
  release APK, and publishes a **GitHub Release** with `DeBurrow-<tag>.apk` attached.

Cut a release:

```bash
git tag v2.0
git push origin v2.0      # triggers the Release workflow
```

### Signing (optional)

Without any setup the release APK is **debug-signed** — installable for sideloading, but the
signature isn't stable across machines. For a stable signature, create a keystore once and add
it as repo secrets; the workflow picks it up automatically.

```bash
keytool -genkey -v -keystore deburrow.jks -alias deburrow \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 deburrow.jks      # copy this into the KEYSTORE_BASE64 secret
```

Add these in **Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | base64 of `deburrow.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `deburrow` |
| `KEY_PASSWORD` | key password |

(Keystores are git-ignored — never commit `*.jks`.)

## License

MIT — see [LICENSE](LICENSE).
