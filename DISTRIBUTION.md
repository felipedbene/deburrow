# Distributing DeBurrow

This covers signing and the three ways to get DeBurrow into people's hands. All of these
are valid because DeBurrow is FOSS (MIT), has no trackers or proprietary dependencies, and
publishes tagged releases on GitHub.

---

## 1. Stable release signing (do this first)

Your GitHub Releases are built by `.github/workflows/release.yml`. With no keystore the
release APK is left **unsigned** (so F-Droid can sign it). For your *own* distribution
(Obtainium / IzzyOnDroid / direct download) you want a **stable signature** so updates
install over each other cleanly.

Generate a keystore once (keep it safe and private — it is git-ignored):

```bash
keytool -genkey -v -keystore deburrow.jks -alias deburrow \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 deburrow.jks    # copy this whole string
```

Add four repository secrets in **GitHub → Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | the base64 string from above |
| `KEYSTORE_PASSWORD` | the keystore password you chose |
| `KEY_ALIAS` | `deburrow` |
| `KEY_PASSWORD` | the key password you chose |

Re-cut a release and it will be properly signed:

```bash
git tag v2.0.1 && git push origin v2.0.1
```

> Keep `deburrow.jks` and its passwords backed up. Losing the key means future updates
> can't install over installed copies.

---

## 2. Obtainium (zero submission — available now)

[Obtainium](https://github.com/ImranR98/Obtainium) installs and auto-updates apps straight
from GitHub Releases. Tell users to add this URL in Obtainium:

```
https://github.com/felipedbene/deburrow
```

That's it — no store, no review. (Requires the stable signing above so updates apply.)

---

## 3. IzzyOnDroid (easy F-Droid-compatible repo)

[IzzyOnDroid](https://apt.izzysoft.de/fdroid/) mirrors your **signed GitHub release APK**
and is a one-click repo to add in any F-Droid client. Open a "Request for Packaging" issue
at <https://gitlab.com/IzzyOnDroid/repo/-/issues> with the text below.

<details>
<summary>Ready-to-paste RFP</summary>

```
Title: RFP: DeBurrow (dev.debene.gopher)

- App name: DeBurrow
- Package ID: dev.debene.gopher
- Source code: https://github.com/felipedbene/deburrow
- License: MIT
- Issue tracker: https://github.com/felipedbene/deburrow/issues
- Releases: https://github.com/felipedbene/deburrow/releases (signed APK attached per tag)
- Summary: A modern, native Android client for the Gopher protocol (Kotlin + Jetpack Compose).
- FOSS, no trackers, no ads, no proprietary dependencies (AndroidX, Compose, Coroutines,
  Room, Coil). Fastlane metadata is in the repo at fastlane/metadata/android/en-US.

Please consider adding DeBurrow to the IzzyOnDroid repo. Thanks!
```
</details>

IzzyOnDroid picks up the Fastlane metadata (`fastlane/metadata/android/en-US`) for the
listing automatically.

---

## 4. F-Droid official (most reach)

F-Droid builds from source on their servers and **signs with the F-Droid key** (so a
F-Droid install and your GitHub install won't cross-update — that's expected).

1. Fork <https://gitlab.com/fdroid/fdroiddata>.
2. Copy [`fdroid/dev.debene.gopher.yml`](fdroid/dev.debene.gopher.yml) to
   `metadata/dev.debene.gopher.yml` in your fork.
3. Open a merge request.

A reviewer will run the build. If it fails on **JDK toolchain provisioning** (the Gradle
daemon JVM is pinned to 21 via `gradle/gradle-daemon-jvm.properties`, and the
`foojay-resolver-convention` plugin in `settings.gradle.kts` tries to download a toolchain
when none matches), the fix is to ensure the build uses a JDK 21 — or remove those two so
the build uses the server's default JDK. Mention this in the MR if asked.

---

## Why DeBurrow qualifies

- **License:** MIT (FOSS)
- **No anti-features:** no trackers/analytics, no ads, no Google Play Services/Firebase
- **Dependencies:** all FOSS (AndroidX, Jetpack Compose, kotlinx-coroutines, Room, Coil)
- **Reproducibility:** standard Gradle build, source public, tagged releases
