# CLAUDE.md — Tile Timer

Android app (`mn.lls.tiletimer`, published under Loveland Solutions): one to eight Quick Settings tiles, each a one-tap timer that works from the
lock screen. Target device is a Samsung Galaxy S26 (One UI 8.x, Android 16);
development device is a Google Pixel 10a. Goal: publish on Google Play.

**Start with `HANDOFF.md`.** It holds the decisions still open, the phased plan,
acceptance criteria and the test plan. `README.md` describes the app's behaviour.

## Stack and layout
- Plain Java, framework APIs only, **no third-party dependencies** unless the
  handoff says otherwise. Keep it that way; the app's whole value is that it is
  tiny, fast and has nothing to break.
- `app/src/main/java/mn/lls/tiletimer/` — all code (small classes; the
  table in `README.md` says what each does).
- `app/src/main/res/` — layouts, strings, vector icons. No PNGs except what the
  store listing needs.
- Gradle Kotlin DSL. `./gradlew assembleDebug`, `./gradlew installDebug`,
  `./gradlew bundleRelease`. From a terminal, set `JAVA_HOME` to Android Studio's
  `Contents/jbr/Contents/Home` and `ANDROID_HOME` to `~/Library/Android/sdk`.

## Rules
- The Pixel 10a has run this app; **the S26 has not.** Assume One UI paths are
  untested and verify on hardware, not by reading.
- "Tested" means: built, installed with adb, and the behaviour observed on the
  phone (quote the adb/logcat evidence in your summary). Never report a scenario
  as passing because the code looks right.
- Do not weaken the lock-screen path. Tile → start must keep working with the
  phone locked and must never call `unlockAndRun` except for the one-time
  notifications-permission case.
- No new permissions without saying why in the commit message; Play reviews
  every one. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` stays out (see handoff).
- Keystores and `keystore.properties` are never committed. `.gitignore` covers
  both; keep it that way. Release builds fail without them, by design.
- Tiles declare `ACTIVE_TILE`, so call `TimerManager.refreshTile` after any state
  change. Do not remove it: without it, a locked tap can take over 4 seconds.
- Screens use the system palette tokens in `values/colors.xml`, not fixed colors.
- Strings live in `strings.xml`, never inline. English only for 1.0.
- Versioning: `versionCode` increments by 1 per upload to Play; `versionName`
  is semantic (1.0.0). Nothing has been uploaded yet, so `versionCode` is still 1. Both live in `app/build.gradle.kts`.
- Commit small, with messages that say what changed and what was verified on
  which device.
