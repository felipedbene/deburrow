<h1 align="center">DeBurrow</h1>

<p align="center">A modern, native <strong>Android Gopher client</strong> — Kotlin, Jetpack Compose, Coroutines, Room.</p>

---

## Download

Grab the latest APK from the [**Releases**](../../releases) page, tap it on your phone, and
allow installing from unknown sources. (minSdk 24 / Android 7.0+.)

## Install via Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) auto-updates straight from GitHub Releases.
Add this URL in Obtainium → Add App:

```
https://github.com/felipedbene/deburrow
```

Maintainer notes on signing & store submission (F-Droid / IzzyOnDroid) are in
[DISTRIBUTION.md](DISTRIBUTION.md).

## Features

- Browse Gopher menus, read text files, view images
- **ANSI-color + word-wrap** text viewer (renders colored `.ansi` art; toggle wrap for maps)
- **Veronica / type-7 search** with a query prompt and tappable results
- Address-bar navigation, **bookmarks**, browse **history**, and **search history**
- **Downloads** for binary items, **response caching**, and **TLS** (`gophers://`)
- Opens `gopher://` / `gophers://` links from other apps (even while running)
- Material 3 theming and an adaptive (+ Android-13 themed) launcher icon

## My gopherholes

DeBurrow ships pointing at these — all live, served over Gopher:

| Hole | Address |
|------|---------|
| **gopher-cta** — live Chicago CTA 'L' trains | `gopher://gopher.debene.dev/` |
| **Phlog** — the debene.dev blog over Gopher | `gopher://gopher.debene.dev:7071/` |
| **Ask the Deck** — a live three-card tarot reading | `gopher://gopher.debene.dev:7072/` |

Elsewhere:

- Blog (web): <https://debene.dev>
- CTA tracker (web original): <https://tracker.debene.dev>
- gopher-cta source: <https://github.com/felipedbene/gopher-cta>

## Lineage & attribution

DeBurrow is an **evolution** of **Pocket Gopher**, a J2ME / MIDlet Gopher client written in
2010 by **Felix Pleșoianu** (`felixp7@yahoo.com`), with Gopher URL/menu parsing code and ideas
by **Nuno J. Silva** (`gopher://sdf-eu.org/1/users/njsg`).

The original protocol logic (menu parsing, URL parsing, the socket request/response dance) was
ported to Kotlin and corrected where needed; the entire J2ME LCDUI layer was replaced with a
Compose UI and a modern architecture. The original MIT license and copyright are retained — see
[LICENSE](LICENSE). The original sources (`PocketGopher.java`, `.jar`, `.jad`) are kept in the
repo root for reference.

## Project layout

```
app/src/main/java/dev/debene/gopher/
  protocol/   GopherType, GopherItem, GopherRequest, GopherUrl, GopherParser, GopherClient
  data/       Room DB + DAOs, GopherRepository (cache), LibraryRepository, DownloadStore
  ui/         Compose browser, viewers, bookmarks/history, theme, navigation
  di/         AppContainer (manual DI)
app/src/test/  GopherUrl / GopherParser / AnsiParser unit tests
app/src/main/assets/home.txt   bundled start page (Gopher menu format)
```

## Building

Requires a full JDK 21 (with `javac`) and the Android SDK (platform 35).

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/path/to/a/full/jdk-21"

./gradlew :app:assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # protocol unit tests
./gradlew :app:installDebug         # install on a connected device/emulator
```

Or open the project in Android Studio and Run.

## License

MIT — see [LICENSE](LICENSE).
