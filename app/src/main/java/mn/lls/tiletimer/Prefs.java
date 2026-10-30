package mn.lls.tiletimer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * All persistent state. One timer "slot" per Quick Settings tile: up to eight
 * preset timers, plus the Custom tile, whose duration is the last time picked.
 *
 * Per slot:
 *   sec_N  - the tile's duration in seconds (user-configurable)
 *   end_N  - epoch millis when the running timer ends, 0 when not running
 *   ring_N - true while the finished timer is ringing
 */
final class Prefs {

    /** Tiles at most, Custom included; eight wide tiles fill a panel page. */
    static final int MAX_TILES = 8;
    static final int CUSTOM_SLOT = MAX_TILES;
    static final int SLOTS = MAX_TILES + 1;
    static final int[] DEFAULT_SECONDS = {300, 600, 900, 1200, 1800, 2700, 3600, 5400, 1800};
    private static final int DEFAULT_TIMER_COUNT = 3;

    private static final String FILE = "tiletimer";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int timerCount(Context c) {
        return sp(c).getInt("timer_count", DEFAULT_TIMER_COUNT);
    }

    static boolean customEnabled(Context c) {
        return sp(c).getBoolean("custom_enabled", false);
    }

    static void setTileSetup(Context c, int timerCount, boolean custom) {
        sp(c).edit().putInt("timer_count", timerCount).putBoolean("custom_enabled", custom).commit();
    }

    static int seconds(Context c, int slot) {
        return sp(c).getInt("sec_" + slot, DEFAULT_SECONDS[slot]);
    }

    static void setSeconds(Context c, int slot, int seconds) {
        sp(c).edit().putInt("sec_" + slot, seconds).commit();
    }

    static long endTime(Context c, int slot) {
        return sp(c).getLong("end_" + slot, 0L);
    }

    static void setEndTime(Context c, int slot, long end) {
        sp(c).edit().putLong("end_" + slot, end).commit();
    }

    static boolean isRinging(Context c, int slot) {
        return sp(c).getBoolean("ring_" + slot, false);
    }

    static void setRinging(Context c, int slot, boolean ringing) {
        sp(c).edit().putBoolean("ring_" + slot, ringing).commit();
    }

    /** Running or ringing. */
    static boolean isActive(Context c, int slot) {
        return endTime(c, slot) > 0 || isRinging(c, slot);
    }

    static void clear(Context c, int slot) {
        sp(c).edit().remove("end_" + slot).remove("ring_" + slot).commit();
    }
}
