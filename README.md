# Tile Timer

One to eight Quick Settings tiles, each one a timer. Swipe down from the top of the
screen — **this works on the lock screen** — and tap a tile. That's the whole
interaction: one swipe, one tap, no unlocking.

- Tap an idle tile → the timer starts (defaults: 5 / 10 / 15 min; change them in the app,
  down to seconds, and choose how many tiles you want).
- The optional **Custom** tile opens a picker, even on the lock screen, to set the time.
- The running countdown is a lock-screen notification with **+1 min** and **Cancel**.
- Tap a running tile → cancels it.
- When time is up: a full-screen **Time's up** screen appears over the lock screen,
  the phone's alarm sound loops on the *alarm* volume (so it rings through silent
  mode), and it vibrates. **Stop** or **+1 min** on that screen or on the notification;
  tapping the tile also stops it. It gives up on its own after 5 minutes.
- The tiles are independent, so several timers can run at once.
- Timers are re-armed after a reboot; one that would have fired while the phone was
  off shows a "Timer missed" notification instead.

No permissions beyond what the alarm needs, no dependencies — plain
Android APIs only. Requires Android 13+ (the Galaxy S26 and Pixel 10a are fine).

## Install on the phone (sideload)

1. Get `app-debug.apk` onto the phone (Quick Share, Google Drive, email it to
   yourself, or USB).
2. Tap the file. The first time, Android asks to allow installs from that app
   (Files / Drive / Chrome) — allow it, then **Install**.
3. Open **Tile Timer** once:
   - allow **notifications** when asked (without them nothing can be shown);
   - tap **Add tiles to Quick Settings** and accept each of the three prompts. If
     the prompts don't appear on One UI, open the quick panel, tap the pencil
     (**Edit**), and drag the three *Tile Timer* buttons in by hand — ideally into
     the first row so they're visible on the first swipe;
   - if **Finish setting up** appears, tap **Fix** on each item. On Samsung, also use
     **App info** to set Battery to Unrestricted;
   - tap **Test: ring in 5 seconds**, lock the phone, and make sure the Time's up
     screen comes up and rings.
4. Set the durations; they save as you type.

Worth knowing on Samsung: the alarm plays at the **Alarm** volume (Settings →
Sounds and vibration → Volume), not the ringtone volume. If Do Not Disturb is used,
check that alarms are allowed in its exceptions. If the phone is ever set to put
"unused apps to sleep", add Tile Timer to the never-sleeping list (Settings →
Battery → Background usage limits).

## Building

Open this folder in Android Studio and let it sync (Gradle 9.3.1, Android Gradle
Plugin 9.1.0, SDK platform 36). Then, from a terminal:

```
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

## How it works (for the curious)

There is **no service running while a timer counts down**. Tapping a tile stores
the end time and hands it to the system as an exact alarm
(`AlarmManager.setAlarmClock`, which survives Doze); the countdown you see is the
notification's own chronometer, drawn by the system. The app's process only wakes
up when something happens — a tile tap, a notification button, or the alarm
firing. When it fires, a short-lived foreground service (`RingService`) loops the
alarm ringtone on the alarm audio stream, vibrates, and posts a notification with a
full-screen intent, which is what brings `RingingActivity` up over the lock screen.

Files:

| File | Role |
|---|---|
| `BaseTileService` + `Tile1`–`8Service`, `TileCustomService` | The Quick Settings tiles |
| `TileConfig` | Which tiles exist; removed tiles have their service disabled |
| `CustomTimerActivity` | The Custom tile's picker, shown over the lock screen |
| `TimerManager` | start / cancel / extend / fire; exact alarms |
| `Notifications` | Channels and the countdown / ringing / info notifications |
| `RingService` | Plays the alarm, vibrates, 5-minute give-up |
| `RingingActivity` | Full-screen Time's up with Stop / +1 min |
| `AlarmReceiver`, `ActionReceiver`, `BootReceiver` | Alarm firing, notification buttons, reboot |
| `MainActivity` | Durations, add-tiles button, permission checklist, test button |
| `Prefs` | The handful of persisted values |

Things Samsung might do differently that are worth a look on the first run: whether
the add-tiles dialog appears (fallback is manual), whether One UI shows the
notification's +1/Cancel buttons without expanding it, and whether the tile's
subtitle ("until 3:52 PM") is shown in the quick panel.
