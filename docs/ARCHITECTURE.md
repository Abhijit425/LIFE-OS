# LIFE OS — Architecture

## Decisions

1. **Single Gradle module, feature-sliced packages.** `core / data / domain / di / navigation / feature/*`.
   Clean-architecture boundaries are enforced by package and interface (`domain` has no Android or Room
   imports). Splitting into Gradle modules later is mechanical; doing it now would slow builds and add risk
   while the app is still taking shape.
2. **MVVM + unidirectional data flow.** Room DAOs expose `Flow`s → repositories combine them → ViewModels
   expose `StateFlow<UiState>` → Compose collects with `collectAsStateWithLifecycle`.
3. **Schema-first.** Room v1 already contains every table the spec lists (plus `goals`), so later phases add
   DAOs and screens rather than migrations. Future changes bump the version and add to `Migrations.ALL`.
   **No destructive fallback migration** — a missing migration fails loudly instead of wiping user data.
4. **Money is `Long` paise; time is UTC `Instant`; calendar dates are `LocalDate`.** "Today" and "this month"
   are computed from the *current* zone on every refresh, so time-zone changes and midnight roll-over are
   handled (dashboard refreshes on resume and pull-to-refresh).
5. **Indian number formatting is hand-written** (`IndianFormat`) for identical output on every device.
6. **No fake data.** Every dashboard number comes from a DAO query. Modules not yet built show what they
   will do and which phase delivers them — no inert buttons.
7. **Adaptive layout by window width**, not device type: rail ≥ 600dp, bottom bar below. Works in
   split-screen and on foldables.
8. **Glass effect without backdrop blur.** Compose can't blur content *behind* a view. The background's light
   orbs are blurred (RenderEffect, API 31+; soft gradients below), and cards are translucent layers with a
   light-catching gradient border. This stays smooth on mid-range tablets.
9. **Reduced motion** is honoured from the system "Remove animations" setting or an in-app override; all
   infinite animations and entrance effects switch off.
10. **LIFE AI honesty.** Basic mode (Phase 5) is a rule-based command engine and is labelled as such.
    Optional generative mode loads a user-imported GGUF model via llama.cpp (JNI) — never bundled in the APK.

## Package map
```
com.lifeos.app
├── LifeOsApp, MainActivity            Hilt app, splash, edge-to-edge, theme from DataStore
├── core
│   ├── designsystem/theme             Color, Type, Dimens (spacing, radii, width classes), Theme
│   ├── designsystem/component         GlassCard, LifeBackground, buttons, text field, dialog,
│   │                                  states, AiOrb, ProgressRing, GlowProgressBar, AnimatedBarChart
│   ├── format                         IndianFormat, TimeFormat
│   └── time                           TimeProvider, day/month ranges, greetings
├── data
│   ├── local                          LifeOsDatabase, Converters, entity/*, dao/*
│   └── repository                     Dashboard, Profile, Preferences implementations
├── domain
│   ├── model                          Enums, dashboard models
│   ├── repository                     Repository interfaces
│   └── usecase                        DashboardMath (pure, unit-tested)
├── di                                 Hilt modules
├── navigation                         Type-safe routes, rail/bottom bar, app shell
└── feature
    ├── dashboard                      Screen, ViewModel, widgets, customise & reminders sheets
    ├── assistant                      LIFE AI (orb; engine in Phase 5)
    ├── more, settings, module         More grid, Settings, phase-status screens
```

## Database (v1)
user_profile · family_members · dashboard_widgets · accounts · transactions · budgets · savings_goals ·
projects · milestones · tasks · subtasks · calendar_events · reminders · health_records ·
medication_reminders · appointments · document_folders · documents · notes · note_tags · goals · habits ·
habit_entries · ai_conversations · ai_messages

Delete rules: deleting a family member or account **unlinks** money records (SET NULL) rather than
deleting them; deleting a task, event or document removes only its own reminders (CASCADE).
