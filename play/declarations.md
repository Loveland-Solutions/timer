# Play Console answers (App content)

**Privacy policy URL:** https://loveland-solutions.github.io/timer/play/privacy-policy.html

**Ads:** No.

**App access:** All functionality is available without special access.

**Content rating:** Category Utility or Productivity. Answer No to the content questions. Expect "Everyone".

**Target audience:** 18 and over.

**Data safety:** No data collected. No data shared.

**Foreground service:** Type `mediaPlayback`. Purpose: "Plays the alarm sound when a timer the user started reaches zero. If the system stopped it, the user would miss the timer." Video: [DEMO VIDEO URL], showing a tile tap, the ring, and Stop.

**Full-screen intent:** The app's core function is alarms (timers). If access is not granted, it prompts the user and still alerts with sound, vibration and a heads-up notification.

**Exact alarms:** `USE_EXACT_ALARM` is for alarm and timer apps, and this is a timer app. There is no separate form today. Keep this for an appeal if a reviewer asks.

**Other permissions, if asked:** `RECEIVE_BOOT_COMPLETED` re-arms running timers after a restart. `VIBRATE` and `WAKE_LOCK` are used while the alarm rings. `POST_NOTIFICATIONS` shows the countdown and alarm.
