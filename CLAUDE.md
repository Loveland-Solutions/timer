# CLAUDE.md — Tile Timer

Android app: one to eight Quick Settings tiles, each a one-tap timer that works from the
lock screen. Target device is a Samsung Galaxy S26 (One UI 8.x, Android 16);
development device is a Google Pixel 10a. Goal: publish on Google Play.

**Start with `HANDOFF.md`.** It holds the decisions still open, the phased plan,
acceptance criteria and the test plan. `README.md` describes the app's behaviour
and the no-IDE build path (optional once Android Studio works).

## Stack and layout
- Plain Java, framework APIs only, **no third-party dependencies** unless the
  handoff says otherwise. Keep it that way; the app's whole value is that it is
  tiny, fast and has nothing to break.
- `app/src/main/java/mn/lls/tiletimer/` — all code (small classes; the
  table in `README.md` says what each does).
- `app/src/main/res/` — layouts, strings, vector icons. No PNGs except what the
  store listing needs.
- Gradle Kotlin DSL. `./gradlew assembleDebug`, `./gradlew installDebug`,
  `./gradlew bundleRelease`.

## Rules
- The code was written without an SDK at hand and compiled only against API-34
  stubs; **nothing has been run on a device yet.** Until Phase 0 of the handoff
  is done, assume every path is untested and verify on hardware, not by reading.
- "Tested" means: built, installed with adb, and the behaviour observed on the
  phone (quote the adb/logcat evidence in your summary). Never report a scenario
  as passing because the code looks right.
- Do not weaken the lock-screen path. Tile → start must keep working with the
  phone locked and must never call `unlockAndRun` except for the one-time
  notifications-permission case.
- No new permissions without saying why in the commit message; Play reviews
  every one. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is to be removed (see handoff).
- Keystores and `keystore.properties` are never committed. `.gitignore` covers
  `/keystore/*.jks`; keep it that way.
- Keep Java source level 8 and avoid lambdas/method references only while
  `build-noide.sh` (dx-based) is still in use; once Android Studio builds are the
  only path, that script and `tools/` can be deleted and the restriction dropped.
- Strings live in `strings.xml`, never inline. English only for 1.0.
- Versioning: `versionCode` increments by 1 per upload to Play; `versionName`
  is semantic (1.0.0). Both live in `app/build.gradle.kts`.
- Commit small, with messages that say what changed and what was verified on
  which device.
