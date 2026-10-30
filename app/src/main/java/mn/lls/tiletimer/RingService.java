package mn.lls.tiletimer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;

/**
 * Foreground service that rings when a timer finishes: loops the phone's alarm
 * sound on the ALARM stream (so it plays through silent mode and, unless alarms
 * are muted in Do Not Disturb, through DND too), vibrates, and posts the
 * full-screen "Time's up" notification. Stops itself after RING_TIMEOUT_MS.
 */
public class RingService extends Service {

    private static final long RING_TIMEOUT_MS = 5 * 60_000L;

    /** The running instance, so receivers/activities in this process can reach it directly. */
    private static volatile RingService current;

    private final boolean[] ringingSlots = new boolean[Prefs.SLOTS];
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = new Runnable() {
        @Override
        public void run() {
            onTimeout();
        }
    };

    private Ringtone ringtone;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private int foregroundSlot = -1;

    /** Stop ringing for one slot and clear its state. Safe to call from anywhere in the app. */
    static void stop(Context c, int slot) {
        Prefs.clear(c, slot);
        RingService s = current;
        if (s != null) {
            s.stopSlot(slot);
        } else {
            Notifications.manager(c).cancel(Notifications.NOTIF_RING_BASE + slot);
        }
        RingingActivity.closeIfShowing(slot);
        TimerManager.refreshTile(c, slot);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        current = this;
        Notifications.ensureChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int slot = intent == null ? -1 : intent.getIntExtra(TimerManager.EXTRA_SLOT, -1);
        if (slot < 0 || slot >= Prefs.SLOTS) {
            if (!anyRinging()) {
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        if (foregroundSlot < 0) {
            foregroundSlot = slot;
            startForeground(Notifications.NOTIF_RING_BASE + slot, Notifications.ringing(this, slot),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            Notifications.manager(this).notify(Notifications.NOTIF_RING_BASE + slot,
                    Notifications.ringing(this, slot));
        }
        ringingSlots[slot] = true;

        startAlerting();
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, RING_TIMEOUT_MS);
        return START_NOT_STICKY;
    }

    private void stopSlot(int slot) {
        ringingSlots[slot] = false;
        if (!anyRinging()) {
            stopAlerting();
            handler.removeCallbacks(timeout);
            stopForeground(STOP_FOREGROUND_REMOVE);
            Notifications.manager(this).cancel(Notifications.NOTIF_RING_BASE + slot);
            foregroundSlot = -1;
            stopSelf();
            return;
        }
        if (slot == foregroundSlot) {
            // Another slot is still ringing: move the foreground notification to it first.
            int other = firstRinging();
            foregroundSlot = other;
            startForeground(Notifications.NOTIF_RING_BASE + other, Notifications.ringing(this, other),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }
        Notifications.manager(this).cancel(Notifications.NOTIF_RING_BASE + slot);
    }

    private void onTimeout() {
        for (int slot = 0; slot < Prefs.SLOTS; slot++) {
            if (ringingSlots[slot]) {
                int minutes = Prefs.minutes(this, slot);
                Prefs.clear(this, slot);
                Notifications.manager(this).notify(Notifications.NOTIF_INFO_BASE + slot,
                        Notifications.info(this,
                                getString(R.string.info_timeout_title, minutes),
                                getString(R.string.info_timeout_text)));
                RingingActivity.closeIfShowing(slot);
                TimerManager.refreshTile(this, slot);
            }
        }
        stopAlerting();
        stopForeground(STOP_FOREGROUND_REMOVE);
        for (int slot = 0; slot < Prefs.SLOTS; slot++) {
            ringingSlots[slot] = false;
            Notifications.manager(this).cancel(Notifications.NOTIF_RING_BASE + slot);
        }
        foregroundSlot = -1;
        stopSelf();
    }

    private boolean anyRinging() {
        return firstRinging() >= 0;
    }

    private int firstRinging() {
        for (int i = 0; i < Prefs.SLOTS; i++) {
            if (ringingSlots[i]) {
                return i;
            }
        }
        return -1;
    }

    private void startAlerting() {
        if (wakeLock == null) {
            PowerManager pm = getSystemService(PowerManager.class);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tiletimer:ring");
            wakeLock.acquire(RING_TIMEOUT_MS + 10_000L);
        }

        if (ringtone == null || !ringtone.isPlaying()) {
            Ringtone r = loadRingtone();
            if (r != null) {
                r.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                r.setLooping(true);
                r.play();
                ringtone = r;
            }
        }

        if (vibrator == null) {
            VibratorManager vm = getSystemService(VibratorManager.class);
            vibrator = vm.getDefaultVibrator();
            if (vibrator.hasVibrator()) {
                VibrationEffect effect = VibrationEffect.createWaveform(new long[]{0, 600, 400}, 0);
                VibrationAttributes attrs = new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build();
                vibrator.vibrate(effect, attrs);
            }
        }
    }

    private Ringtone loadRingtone() {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            uri = Settings.System.DEFAULT_ALARM_ALERT_URI;
        }
        Ringtone r = RingtoneManager.getRingtone(this, uri);
        if (r == null) {
            r = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_NOTIFICATION_URI);
        }
        return r;
    }

    private void stopAlerting() {
        if (ringtone != null) {
            try {
                ringtone.stop();
            } catch (RuntimeException ignored) {
                // nothing useful to do
            }
            ringtone = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            wakeLock = null;
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopAlerting();
        if (current == this) {
            current = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
