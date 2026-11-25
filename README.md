# LIFE OS
**One Life. One Intelligence. Total Control.**

A private, offline-first personal operating system for Android, built for tablets first (it adapts down to phones).
Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore.

> **Status: Phase 1 of 8 complete** — architecture, design system, adaptive navigation, a real-data dashboard,
> and working settings (name, theme, motion). Finance and Tasks arrive in Phase 2; their screens say so honestly.

---

## What works in Phase 1

| Area | State |
|---|---|
| Project, Gradle (Kotlin DSL + version catalog), CI | ✅ |
| Glass design system: colour, type, spacing tokens; cards, buttons, inputs, dialogs, charts, loading/empty/error states | ✅ |
| Adaptive navigation: **floating side rail on tablets** (≥600dp), floating bottom bar on phones | ✅ |
| Dashboard reading only stored records, 8 widgets, show/hide/reorder/reset, pull-to-refresh, reminders sheet | ✅ |
| Room schema v1 for **every** module (25 tables, FKs, indices, exported schema) | ✅ |
| Settings: profile name, Dark/Light/System theme, motion (follow system / full / reduced), About | ✅ |
| Splash screen (androidx core-splashscreen) and adaptive + themed app icon | ✅ |
| LIFE AI animated orb; command engine | 🔜 Phase 5 |
| Finance, Tasks, Notes | 🔜 Phase 2 |
| Family, Health, Calendar, Reminders/notifications | 🔜 Phase 3 |
| Documents, Goals, Analytics | 🔜 Phase 4 |
| Security, encrypted backup/restore, export | 🔜 Phase 6 |

On a fresh install the dashboard shows empty states — it never shows sample numbers.

## Tablet layout
- **≥ 840dp** (landscape tablets): side rail + 3-column staggered dashboard, two-column Settings.
- **600–839dp** (portrait tablets): side rail + 2-column dashboard.
- **< 600dp** (phones): floating bottom bar + single column.

The activity handles rotation and multi-window resizing without restarting.

---

## Build the APK

### Option A — GitHub Actions (no Android Studio needed)
1. Create a GitHub repo (e.g. `LifeOS`) and push this folder to `main`.
2. Open **Actions → Build LIFE OS APK**. Each push runs the unit tests and builds a debug APK.
3. Download the `LifeOS-debug-apk` artifact, unzip, copy `app-debug.apk` to the tablet and install
   (allow "Install unknown apps" for your file manager when Android asks).

### Option B — Android Studio
1. Android Studio Ladybug (2024.2) or newer, JDK 17.
2. *File → Open* this folder; let Gradle sync.
3. Run the `app` configuration on your tablet (USB debugging on) or an emulator.
4. Command line: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

### Tests
```
./gradlew testDebugUnitTest          # JVM unit tests (formatting, time, dashboard maths, validation)
./gradlew connectedDebugAndroidTest  # Room + Compose UI tests on a connected device/emulator
```

## Release signing
1. Create a keystore once (keep it safe — losing it means you can't update the app):
   ```
   keytool -genkeypair -v -keystore lifeos-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias lifeos
   ```
2. Create `keystore.properties` in the project root (it's git-ignored):
   ```
   storeFile=lifeos-release.jks
   storePassword=…
   keyAlias=lifeos
   keyPassword=…
   ```
3. Signed APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/`.
   App Bundle for Play: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/`.

Never commit the keystore or passwords.

---

## Pinned dependency versions
Kotlin 2.1.0 · KSP 2.1.0-1.0.29 · AGP 8.7.3 · Gradle 8.11.1 · Compose BOM 2024.12.01 (Material 3 1.3.1) ·
Navigation 2.8.5 · Lifecycle 2.8.7 · Hilt 2.54 · Room 2.6.1 · DataStore 1.1.1 · Coroutines 1.9.0 ·
kotlinx-serialization 1.7.3 · core-splashscreen 1.0.1. compileSdk/targetSdk 35, minSdk 26 (Android 8.0).
All live in `gradle/libs.versions.toml`. This set was chosen because it's known to work together; upgrade as a group.

## Privacy
- No `INTERNET` permission is declared — the app physically cannot send data anywhere.
- No account, analytics, ads or Firebase.
- Android's automatic cloud backup and device-transfer copy of app data are disabled; an encrypted, user-controlled backup arrives in Phase 6.

See `docs/ARCHITECTURE.md` for design decisions.
