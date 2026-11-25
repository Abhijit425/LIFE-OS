# LIFE OS
**One Life. One Intelligence. Total Control.**

A private, offline-first personal operating system for Android, built for tablets first (it adapts down to phones).
Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore.

> **Status: v1.0.0, all phases built.** Every module works offline on your own data. A fresh install starts empty; it never shows sample numbers.

## Features

| Module | What you can do |
|---|---|
| **Dashboard** | Greeting, life overview, 8 widgets from real records: show, hide, reorder, reset, pull to refresh |
| **LIFE AI** | Offline command engine (rule-based, not generative): spending and income questions, category breakdown, budget status, add expense/income, reminders, tasks (add, complete, delete with confirmation), savings goals, appointments, schedule, weekly summary, notes. Conversation history is saved. |
| **Finance (₹)** | Income and expenses, categories, accounts, cards, loans with EMI reminders, monthly budgets, recurring transactions, savings goals, donut and 6-month charts. Manual only; it never connects to banks. |
| **Tasks** | Priorities, due dates and times, reminders, recurrence, subtasks, categories, search and filters, Kanban board, projects with milestones and progress |
| **Notes** | Rich text (Markdown subset with preview), checklists, tags, categories, pin, archive, search, links to tasks and projects |
| **Family** | Profiles, automatic yearly birthday reminders, emergency contacts with one-tap dial, notes |
| **Health** | Water, sleep, weight and exercise logs; health notes; medicine reminders (doses stored exactly as entered); appointments with a 1-hour alert; per-person tracking. Not medical advice. |
| **Calendar** | Month, week and day views; events; all reminders (bill, birthday, general and more) with recurrence |
| **Documents** | SAF import of PDF, images and text. Store as a reference, a private copy or a Keystore-encrypted copy. Folders, tags, categories, search, expiry reminders, in-app preview (PDF pages, images, text) |
| **Goals** | Goals with automatic 25/50/75/100% milestones, habits with streaks, weekly check-ins, achievement badges |
| **Insights** | Weekly report, tasks per day, spending breakdown, habit consistency |
| **Settings** | Profile, theme, motion, reminder delivery checks, biometric app lock, encrypted backup and restore, JSON/CSV export, optional GGUF model import |

### Reminders
AlarmManager uses exact alarms when Android allows them (Android 12+ asks for "Alarms & reminders") and inexact alarms otherwise. Alarms are rebuilt from the database after a reboot, app update, clock change or time-zone change, and a WorkManager job re-checks them twice a day. Some phone makers (Vivo, Oppo, Xiaomi and others) aggressively stop background work. **Settings → Reminder delivery** links to the battery setting to change.

### Local LLM: what's included
LIFE AI's basic mode is the rule-based engine above and is always available. **This build lets you import a GGUF model and checks your device's RAM and storage, but it does not include the llama.cpp inference engine.** An imported model is stored but not used, and the app says so. Adding llama.cpp means compiling native code with the NDK. It's a planned next step and wasn't shipped half-working.

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
