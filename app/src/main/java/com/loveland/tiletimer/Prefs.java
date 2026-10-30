package com.loveland.tiletimer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * All persistent state. Three timer "slots", one per Quick Settings tile.
 *
 * Per slot:
 *   min_N  - the tile's duration in minutes (user-configurable, defaults 5/10/15)
 *   end_N  - epoch millis when the running timer ends, 0 when not running
 *   ring_N - true while the finished timer is ringing
 */
final class Prefs {

    static final int SLOTS = 3;
    static final int[] DEFAULT_MINUTES = {5, 10, 15};

    private static final String FILE = "tiletimer";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int minutes(Context c, int slot) {
        return sp(c).getInt("min_" + slot, DEFAULT_MINUTES[slot]);
    }

    static void setMinutes(Context c, int slot, int minutes) {
        sp(c).edit().putInt("min_" + slot, minutes).commit();
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
