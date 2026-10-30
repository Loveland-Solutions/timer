package mn.lls.tiletimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

/** Notification channels and builders. */
final class Notifications {

    static final String CH_COUNTDOWN = "countdown";
    static final String CH_RING = "ring";
    static final String CH_INFO = "info";

    static final int NOTIF_COUNTDOWN_BASE = 100;   // + slot
    static final int NOTIF_RING_BASE = 200;        // + slot
    static final int NOTIF_INFO_BASE = 300;        // + slot

    private static final int PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    private static final int ACCENT = 0xFF2E7D32;

    private Notifications() {}

    static NotificationManager manager(Context c) {
        return c.getSystemService(NotificationManager.class);
    }

    static void ensureChannels(Context c) {
        NotificationManager nm = manager(c);

        NotificationChannel countdown = new NotificationChannel(
                CH_COUNTDOWN, c.getString(R.string.channel_countdown), NotificationManager.IMPORTANCE_DEFAULT);
        countdown.setDescription(c.getString(R.string.channel_countdown_desc));
        countdown.setSound(null, null);
        countdown.enableVibration(false);
        countdown.setShowBadge(false);
        countdown.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(countdown);

        // The alarm sound is played by RingService on the alarm stream, so this
        // channel is deliberately silent; its job is the full-screen alert.
        NotificationChannel ring = new NotificationChannel(
                CH_RING, c.getString(R.string.channel_ring), NotificationManager.IMPORTANCE_HIGH);
        ring.setDescription(c.getString(R.string.channel_ring_desc));
        ring.setSound(null, null);
        ring.enableVibration(false);
        ring.setShowBadge(false);
        ring.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ring);

        NotificationChannel info = new NotificationChannel(
                CH_INFO, c.getString(R.string.channel_info), NotificationManager.IMPORTANCE_DEFAULT);
        info.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(info);
    }

    /** Ongoing notification for a running timer; the system draws the countdown. */
    static Notification countdown(Context c, int slot, long end) {
        int minutes = Prefs.minutes(c, slot);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi = PendingIntent.getActivity(c, TimerManager.REQ_OPEN_BASE + slot, open, PI_FLAGS);

        Notification.Builder b = new Notification.Builder(c, CH_COUNTDOWN)
                .setSmallIcon(R.drawable.ic_timer)
                .setColor(ACCENT)
                .setContentTitle(c.getString(R.string.notif_countdown_title, minutes))
                .setContentText(c.getString(R.string.notif_countdown_text, TimerManager.formatTime(c, end)))
                .setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(openPi)
                .addAction(action(c, R.string.action_plus_one,
                        ActionReceiver.ACTION_PLUS, TimerManager.REQ_PLUS_BASE + slot, slot))
                .addAction(action(c, R.string.action_cancel,
                        ActionReceiver.ACTION_CANCEL, TimerManager.REQ_CANCEL_BASE + slot, slot));
        return b.build();
    }

    /** Full-screen "time's up" notification used by RingService while ringing. */
    static Notification ringing(Context c, int slot) {
        int minutes = Prefs.minutes(c, slot);
        Intent act = new Intent(c, RingingActivity.class)
                .putExtra(TimerManager.EXTRA_SLOT, slot)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(
                c, TimerManager.REQ_RING_ACT_BASE + slot, act, PI_FLAGS);

        Notification.Builder b = new Notification.Builder(c, CH_RING)
                .setSmallIcon(R.drawable.ic_timer)
                .setColor(ACCENT)
                .setContentTitle(c.getString(R.string.notif_ring_title))
                .setContentText(c.getString(R.string.notif_ring_text, minutes))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreen, true)
                .setContentIntent(fullScreen)
                .addAction(action(c, R.string.action_stop,
                        ActionReceiver.ACTION_STOP_RING, TimerManager.REQ_STOP_BASE + slot, slot))
                .addAction(action(c, R.string.action_plus_one,
                        ActionReceiver.ACTION_PLUS, TimerManager.REQ_PLUS_BASE + slot, slot));
        return b.build();
    }

    /** Plain notification, e.g. "timer finished while you were away". */
    static Notification info(Context c, String title, String text) {
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi = PendingIntent.getActivity(c, TimerManager.REQ_OPEN_BASE + 50, open, PI_FLAGS);
        return new Notification.Builder(c, CH_INFO)
                .setSmallIcon(R.drawable.ic_timer)
                .setColor(ACCENT)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .build();
    }

    private static Notification.Action action(Context c, int labelRes, String action, int requestCode, int slot) {
        Intent i = new Intent(c, ActionReceiver.class)
                .setAction(action)
                .putExtra(TimerManager.EXTRA_SLOT, slot);
        PendingIntent pi = PendingIntent.getBroadcast(c, requestCode, i, PI_FLAGS);
        return new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_timer), c.getString(labelRes), pi).build();
    }
}
