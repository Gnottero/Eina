# Eina

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/version-0.1.0-ff5b62)

Eina is a local-first Android workout tracker built for planning routines, logging training sessions, and understanding progress without turning a workout into a spreadsheet.

It combines a focused live-workout experience with flexible set types, automatic rest timing, personal-record tracking, an animated exercise library, and clear weekly and long-term statistics.

## Features

- **Live workout tracking** — record weight, repetitions, duration, or distance while a session is in progress.
- **Reusable routines** — plan exercises, per-set targets, rest times, notes, supersets, and linked Spotify or YouTube Music playlists.
- **Flexible set types** — normal, warm-up, failure, and drop sets, with correct numbering, load suggestions, rest timing, volume, and personal-record behavior.
- **Persistent rest timer** — continues outside the workout screen and schedules a system alarm for reliable sound and vibration feedback.
- **Supersets** — group exercises into rounds and start recovery only when the corresponding round is complete.
- **Progress insights** — weekly dashboard, training calendar, volume charts, streaks, body-weight history, and exercise-specific progress.
- **Personal records** — automatically detects records according to the exercise's load type.
- **Workout history** — review, edit, annotate, share, or turn a completed session into a new routine.
- **Exercise library** — 197 bundled exercises with muscle groups, equipment, instructions, and local animated media.
- **Custom exercises** — create and edit movements with custom details and images.
- **Routine sharing** — import and export routines as portable JSON files, including custom exercises and their media.
- **Health Connect integration** — attach heart-rate samples and calorie estimates recorded during a completed workout.
- **Multilingual interface** — English, Italian, and French.

## Training model

Eina supports several ways of recording an exercise:

| Type | Recorded values |
| --- | --- |
| Free weight / machine stack | Load and repetitions |
| Bodyweight | Repetitions and a body-weight snapshot |
| Bodyweight with added load | Added load, repetitions, and body weight |
| Assisted | Assistance, repetitions, and body weight |
| Time based | Duration |
| Distance based | Distance and time |

Warm-up sets remain outside workout volume and personal records, while still participating correctly in load suggestions and rest timing.

## Tech stack

- Kotlin 2.0.21
- Jetpack Compose with Material 3
- MVVM with `StateFlow`
- Room for local persistence
- Koin for dependency injection
- Kotlin coroutines
- Android Health Connect
- Coil for local exercise media
- Custom Compose Canvas charts

The application requires Android 8.0 (API 26) or newer and targets API 35.

## Getting started

### Prerequisites

- Android Studio with Android SDK 35
- JDK 17 or 21
- An Android device or emulator running API 26 or newer

### Clone the repository

```bash
git clone https://github.com/Gnottero/Eina.git
cd Eina
```

### Configure Java

The repository currently defines `org.gradle.java.home` in `gradle.properties`. Update it to the absolute path of a compatible JDK on your machine, or override it for an individual command:

```bash
./gradlew -Dorg.gradle.java.home=/absolute/path/to/jdk testDebugUnitTest
```

### Configure the debug keystore

The Android configuration expects a debug keystore at:

```text
${user.home}/.config/.android/debug.keystore
```

If Android Studio has already generated the standard debug keystore, macOS and Linux users can copy it to the expected location:

```bash
mkdir -p ~/.config/.android
cp ~/.android/debug.keystore ~/.config/.android/debug.keystore
```

Alternatively, update the debug signing configuration in `app/build.gradle.kts` for your local environment. Never commit a production signing key.

### Build and install

```bash
./gradlew assembleDebug
./gradlew installDebug
```

The generated APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Tests

Run the local unit-test suite:

```bash
./gradlew testDebugUnitTest
```

With an emulator or device connected, run the instrumented Room tests:

```bash
./gradlew connectedDebugAndroidTest
```

## Project structure

```text
app/src/main/
├── assets/                 Exercise data, animations, and font licenses
├── java/com/eina/app/
│   ├── data/
│   │   ├── db/             Room entities, DAOs, converters, and migrations
│   │   ├── health/         Health Connect integration
│   │   ├── prefs/          Local application settings
│   │   ├── repository/     Data access and workout operations
│   │   ├── seed/           Bundled exercise-library import
│   │   └── transfer/       Routine import/export and custom media
│   ├── domain/             Volume, records, progress, supersets, and sync rules
│   ├── di/                 Koin modules
│   └── ui/                 Compose screens, components, feedback, and theme
└── res/                    Android resources and translations
```

## Data and privacy

Workout history, routines, body-weight measurements, and settings are stored locally on the device. Health data is read only after Android grants the required Health Connect permissions and can be disabled from Eina's settings.

Routine files and workout images leave the device only when the user explicitly uses a share or export action.

## Development status

Eina is currently under active development. The application version in this repository is `0.1.0`, so interfaces and data formats may continue to evolve.

## License

Eina is open-source software released under the [MIT License](LICENSE).
