# Impervir for Android

Native Android app for **Impervir Social Agent**: an AI agent that runs a founder's social media (X, LinkedIn, Threads, Telegram) and shows which posts made money.

This is the Android version, separate from the iOS design. Same product, data and brand, built as a native Android app.

## Stack

- Kotlin 2.0, Jetpack Compose (BOM 2024.12.01), Material 3
- Navigation Compose 2.8 with type-safe routes
- Edge-to-edge, predictive back, splash screen API, adaptive and themed launcher icon
- Light and dark theme; optional wallpaper colors (Material You) on Android 12+
- Fonts bundled: Geist (UI and numbers, tabular figures) and Instrument Serif (headlines)
- minSdk 26, targetSdk 35

## Run

Open the folder in Android Studio (Ladybug or newer) and run the `app` configuration, or:

```sh
./gradlew installDebug
```

## Structure

```
app/src/main/java/com/impervir/agent/
  data/          models, sample data (persona: Maya Chen), in-memory repository
  navigation/    routes, NavHost, bottom navigation (Home, Talk, Posts, Inbox, Analytics)
  ui/theme/      colors, type, shapes, motion
  ui/components/ shared pieces: cards, count-up numbers, "+ Add" pills, official platform logos
  ui/ember/      Ember, the mascot, drawn in Compose with 15 emotions
  feature/       one package per screen: home, talk, voice, writing, posts, editor,
                 calendar, inbox, analytics, postdetail, report, yourvoice, settings, onboarding
```

## Design rules

- Show only each screen's main actions. Optional features stay hidden until the user adds them ("+ Add" pills, a Customize sheet, or the ••• menu).
- Official platform and store logos only (Simple Icons, CC0).
- Every number uses Geist with tabular figures; serif only for word headlines.
- Ember has floating hands and no belly window; its mood follows the context.

The app currently runs on sample data. `ImpervirRepository` is the single place to connect a real backend.

Fonts are licensed under the SIL Open Font License.
