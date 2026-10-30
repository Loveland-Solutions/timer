package mn.lls.tiletimer;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Which tiles exist. An app cannot remove a tile from the Quick Settings panel
 * directly, so a removed tile has its service disabled, which takes it off the
 * panel and out of the tile picker.
 */
final class TileConfig {

    private TileConfig() {}

    static boolean isEnabled(Context c, int slot) {
        if (slot == Prefs.CUSTOM_SLOT) {
            return Prefs.customEnabled(c);
        }
        return slot < Prefs.timerCount(c);
    }

    /** Apply a new tile setup. A timer on a removed tile is cancelled. */
    static void apply(Context c, int timerCount, boolean custom) {
        Prefs.setTileSetup(c, timerCount, custom);
        PackageManager pm = c.getPackageManager();
        for (int slot = 0; slot < Prefs.SLOTS; slot++) {
            boolean on = isEnabled(c, slot);
            if (!on && Prefs.isActive(c, slot)) {
                TimerManager.cancel(c, slot);
            }
            pm.setComponentEnabledSetting(new ComponentName(c, TimerManager.tileClass(slot)),
                    on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
