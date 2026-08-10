# GYM PRO — Native Android

A finished native Android rewrite of the **Personal Gym Tracker** web app
(`index.html`, the single-file PWA that used to ship inside a Capacitor shell).
Built with **Kotlin + Jetpack Compose + Material 3**, fully offline, single-user,
with **Room** (structured data) + **DataStore** (settings) persistence.

No backend, no accounts, no ads, no telemetry — every byte stays on the device,
exactly like the original's localStorage architecture.

## Building

```bash
./gradlew assembleDebug        # build the APK
./gradlew installDebug         # install on a connected device/emulator
./gradlew testDebugUnitTest    # pure-JVM unit tests (streak, PR, goals, parsers)
./gradlew connectedDebugAndroidTest  # E2E flow + screenshots on an emulator
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

CI (`.github/workflows/android-build.yml`) builds the APK, runs the unit tests,
runs the instrumented E2E suite on a headless emulator, and uploads the APK,
test reports, and screen-by-screen screenshots as downloadable artifacts.

## Feature map (original → native)

| Original feature (web app) | Native implementation |
|---|---|
| Header: GYM PRO logo, 🔥 streak pill, weight/BF pill, theme toggle | `MainActivity.kt` → `GymHeader` |
| Home — "This Week" dots + N/6 days | `ui/home/HomeScreen.kt` → `WeekCard` |
| Home — Consistency calendar (Gym/Diet modes, month nav, TODAY jump, note dots, per-day colors) | `ui/home/HomeScreen.kt` → `CalendarCard` |
| Home — Body Stats tiles + weight log + delta | `ui/home/HomeScreen.kt` → `BodyStatsCard` |
| Home — Tools (Navy BF, BF graph, Wt graph) | `ui/home/HomeScreen.kt` → `ToolsCard` + `ui/modals/BfModal.kt`, `GraphModal.kt` |
| Home — Today's Mood (5 emoji) | `ui/home/HomeScreen.kt` → `MoodCard` |
| Home — Data: Backup / Restore | `ui/home/HomeScreen.kt` → `DataCard`, `data/repo/BackupRepository.kt` |
| Per-day History modal (note, workout, diet tags, mood) | `ui/modals/HistoryModal.kt` |
| Progress graph modal (Latest/Peak/Change + line chart + tagline) | `ui/modals/GraphModal.kt`, `ui/components/Charts.kt` |
| Train — day title + day scroller (Sun–Sat, has-log dots) | `ui/train/TrainScreen.kt` → `DayScroller` |
| Train — routine switcher pill + Cardio button | `ui/train/TrainScreen.kt` → `RoutinePill`, `CardioModal` |
| Train — REST timer (60/90/120/180 presets, overlay pill + ring, vibrate) | `ui/components/Overlays.kt` → `TimerOverlay`, `ui/AppViewModel.kt` |
| Train — exercise cards (target, last session, Copy Last, FORM link, steppers, Est 1RM, volume bar, PR badge) | `ui/train/TrainScreen.kt` → `ExerciseCard` |
| Train — LOG COMPLETE WORKOUT (history + attendance + PR toasts + confetti) | `data/repo/WorkoutRepository.kt` → `finishWorkout` |
| Train — REST day screen (breathing 🧘) | `ui/train/TrainScreen.kt` → `RestDayCard` |
| Routines manager (create / rename / delete / switch) | `ui/train/TrainScreen.kt` → `RoutineManagerModal`, `data/repo/RoutineRepository.kt` |
| Exercise manager (add / edit / remove, name + target + link) | `ui/train/TrainScreen.kt` → `ExerciseManagerModal` |
| **AI Prompt generator** (byte-identical prompt + schema) | `domain/AIPromptBuilder.kt`, `ui/train/TrainScreen.kt` → `AiPromptModal` |
| JSON import (Format A direct days / Format B AI array) | `domain/WorkoutJsonParser.kt` → `ImportModal` |
| Cardio log (11 types, duration/calories/distance/notes, history, delete) | `ui/train/TrainScreen.kt` → `CardioModal` |
| Fuel — protein bar (green/amber/red), food checklist, quick-add protein | `ui/fuel/FuelScreen.kt` → `ProteinCard` |
| Fuel — goal editor (protein + water) | `ui/fuel/FuelScreen.kt` → `GoalModal` |
| Fuel — water glasses tracker | `ui/fuel/FuelScreen.kt` → `WaterCard` |
| Fuel — supplements (daily / alt-day / weekly schedule) | `ui/fuel/FuelScreen.kt` → `SuppsCard`, `data/repo/DietRepository.kt` |
| Fuel — food/supplement manager | `ui/fuel/FuelScreen.kt` → `DietManagerModal` |
| Fuel — 7-day protein trend chart | `ui/fuel/FuelScreen.kt` → `ProteinTrendCard` |
| (additive) 7-day water trend chart | `ui/fuel/FuelScreen.kt` → `WaterTrendCard` |
| Streak calculation | `domain/StreakCalculator.kt` |
| PR detection (est 1RM = w×(1+r/30)) | `domain/PrDetector.kt` |
| US Navy body-fat formula | `domain/BodyFatCalculator.kt` |
| Toasts (info/success/PR) + confetti cannon | `ui/components/Overlays.kt` → `ToastHost`, `ConfettiOverlay` |
| Backup JSON (localStorage-compatible format) | `data/repo/BackupRepository.kt` |

## Data & backup format

The export file is **byte-compatible with the web app's backup format**: a flat
JSON object keyed exactly like the original `localStorage` (`routines`, `gatt`,
`h_<exerciseId>`, `t_<exerciseId>_<date>`, `dlog`, `bw_log`, `bfLog`,
`water_<date>`, `mood_<date>`, `note_<date>`, `dc_<date>`, `de_<date>`,
`cardioLog`, `dietConfig`, `suppConfig`, `proteinGoal`, `waterTarget`,
`timerDuration`, `theme`, `bfHeight`, `uwt`, `activeRoutineId`). A backup from
this app restores into the web app and vice versa.

One deliberate difference: dates are stored in the **device's local timezone**
(the web app used UTC via `toISOString()`, which could lag local midnight).

## Tests

- `app/src/test` — pure JVM unit tests: `StreakCalculatorTest`,
  `PrDetectorTest`, `GoalMathTest`, `BodyFatCalculatorTest`,
  `WorkoutJsonParserTest`, `AIPromptBuilderTest`, `DatesTest`.
- `app/src/androidTest` — instrumented tests on an emulator:
  - `AppE2eTest` — the full Definition-of-Done loop: create routine → log
    workout → PR celebration → calendar + streak → diet/water/mood/notes →
    export → wipe → import → identical state → theme persistence.
  - `AppScreenshotTest` — captures every screen and modal to PNGs (uploaded as
    the `gym-pro-screenshots` artifact).
