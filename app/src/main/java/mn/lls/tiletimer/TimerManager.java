package mn.lls.tiletimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import java.util.Date;

/**
 * Starts, extends, cancels and fires timers. There is no running service while a
 * timer counts down: the end time is an exact alarm held by the system, and the
 * countdown is drawn by the notification's own chronometer. The app's process only
 * wakes up when something happens (tile tap, notification action, alarm firing).
 */
final class TimerManager {

    static final String EXTRA_SLOT = "slot";

    // PendingIntent request codes, + slot
    static final int REQ_ALARM_BASE = 1000;
    static final int REQ_SHOW_BASE = 1100;
    static final int REQ_OPEN_BASE = 1200;
    static final int REQ_CANCEL_BASE = 1300;
    static final int REQ_PLUS_BASE = 1400;
    static final int REQ_STOP_BASE = 1500;
    static final int REQ_RING_ACT_BASE = 1600;

    static final long ONE_MINUTE_MS = 60_000L;

    private TimerManager() {}

    /** Start the tile's configured duration. */
    static void start(Context c, int slot) {
        startFor(c, slot, Prefs.minutes(c, slot) * ONE_MINUTE_MS);
    }

    /** Start an arbitrary duration on a slot (also used by "+1 min" while ringing and the test button). */
    static void startFor(Context c, int slot, long durationMs) {
        if (Prefs.isRinging(c, slot)) {
            RingService.stop(c, slot);
        }
        long end = System.currentTimeMillis() + durationMs;
        Prefs.setRinging(c, slot, false);
        Prefs.setEndTime(c, slot, end);
        scheduleAlarm(c, slot, end);
        Notifications.ensureChannels(c);
        Notifications.manager(c).notify(Notifications.NOTIF_COUNTDOWN_BASE + slot,
                Notifications.countdown(c, slot, end));
        refreshTile(c, slot);
    }

    /** Cancel a running (or ringing) timer. */
    static void cancel(Context c, int slot) {
        cancelAlarm(c, slot);
        if (Prefs.isRinging(c, slot)) {
            RingService.stop(c, slot);
        }
        Prefs.clear(c, slot);
        Notifications.manager(c).cancel(Notifications.NOTIF_COUNTDOWN_BASE + slot);
        refreshTile(c, slot);
    }

    /** Add time to a running timer; if it is ringing, stop the alarm and run a fresh countdown of extraMs. */
    static void extend(Context c, int slot, long extraMs) {
        if (Prefs.isRinging(c, slot)) {
            startFor(c, slot, extraMs);
            return;
        }
        long end = Prefs.endTime(c, slot);
        if (end <= 0) {
            return;
        }
        long newEnd = Math.max(end, System.currentTimeMillis()) + extraMs;
        Prefs.setEndTime(c, slot, newEnd);
        scheduleAlarm(c, slot, newEnd);
        Notifications.manager(c).notify(Notifications.NOTIF_COUNTDOWN_BASE + slot,
                Notifications.countdown(c, slot, newEnd));
        refreshTile(c, slot);
    }

    /** Called by AlarmReceiver when the exact alarm fires. */
    static void onFired(Context c, int slot) {
        if (Prefs.endTime(c, slot) <= 0) {
            return; // cancelled in the meantime
        }
        Prefs.setEndTime(c, slot, 0L);
        Prefs.setRinging(c, slot, true);
        Notifications.manager(c).cancel(Notifications.NOTIF_COUNTDOWN_BASE + slot);
        Intent i = new Intent(c, RingService.class).putExtra(EXTRA_SLOT, slot);
        c.startForegroundService(i);
        refreshTile(c, slot);
    }

    /** Re-arm alarms after a reboot; timers that should have fired while the phone was off are reported. */
    static void restoreAfterBoot(Context c) {
        long now = System.currentTimeMillis();
        for (int slot = 0; slot < Prefs.SLOTS; slot++) {
            long end = Prefs.endTime(c, slot);
            boolean ringing = Prefs.isRinging(c, slot);
            if (end > now) {
                scheduleAlarm(c, slot, end);
                Notifications.ensureChannels(c);
                Notifications.manager(c).notify(Notifications.NOTIF_COUNTDOWN_BASE + slot,
                        Notifications.countdown(c, slot, end));
            } else if (end > 0 || ringing) {
                Prefs.clear(c, slot);
                Notifications.ensureChannels(c);
                Notifications.manager(c).notify(Notifications.NOTIF_INFO_BASE + slot,
                        Notifications.info(c,
                                c.getString(R.string.info_missed_title),
                                c.getString(R.string.info_missed_text, Prefs.minutes(c, slot))));
            }
            refreshTile(c, slot);
        }
    }

    static void refreshAllTiles(Context c) {
        for (int slot = 0; slot < Prefs.SLOTS; slot++) {
            refreshTile(c, slot);
        }
    }

    static void refreshTile(Context c, int slot) {
        TileService.requestListeningState(c, new ComponentName(c, tileClass(slot)));
    }

    static Class<?> tileClass(int slot) {
        switch (slot) {
            case 0:
                return Tile1Service.class;
            case 1:
                return Tile2Service.class;
            default:
                return Tile3Service.class;
        }
    }

    static String formatTime(Context c, long epochMillis) {
        return android.text.format.DateFormat.getTimeFormat(c).format(new Date(epochMillis));
    }

    // ---- alarms -----------------------------------------------------------

    private static PendingIntent alarmIntent(Context c, int slot) {
        Intent i = new Intent(c, AlarmReceiver.class)
                .setAction(AlarmReceiver.ACTION_FIRE)
                .putExtra(EXTRA_SLOT, slot);
        return PendingIntent.getBroadcast(c, REQ_ALARM_BASE + slot, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void scheduleAlarm(Context c, int slot, long end) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent fire = alarmIntent(c, slot);
        if (am.canScheduleExactAlarms()) {
            Intent show = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent showPi = PendingIntent.getActivity(c, REQ_SHOW_BASE + slot, show,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(end, showPi), fire);
        } else {
            // Should not happen (USE_EXACT_ALARM is declared), but degrade gracefully.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, fire);
        }
    }

    private static void cancelAlarm(Context c, int slot) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.cancel(alarmIntent(c, slot));
    }
}
