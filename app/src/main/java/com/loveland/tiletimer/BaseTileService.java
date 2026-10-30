package com.loveland.tiletimer;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * One Quick Settings tile = one timer slot.
 *
 * Tap while idle    -> start the tile's duration
 * Tap while running -> cancel
 * Tap while ringing -> stop the alarm
 *
 * onClick is delivered even when the phone is locked; nothing here needs the
 * device unlocked, so unlockAndRun is deliberately not used for the timer itself.
 */
public abstract class BaseTileService extends TileService {

    protected abstract int slot();

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        final int slot = slot();

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // First run only: notifications have not been allowed yet, so the
            // countdown and the alarm screen could not be shown. Open the app.
            unlockAndRun(new Runnable() {
                @Override
                public void run() {
                    openApp();
                }
            });
            return;
        }

        if (Prefs.isRinging(this, slot)) {
            RingService.stop(this, slot);
        } else if (Prefs.endTime(this, slot) > 0) {
            TimerManager.cancel(this, slot);
        } else {
            TimerManager.start(this, slot);
        }
        updateTile();
    }

    @SuppressWarnings("deprecation")
    private void openApp() {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            startActivityAndCollapse(pi);
        } else {
            startActivityAndCollapse(i);
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        int slot = slot();
        int minutes = Prefs.minutes(this, slot);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_timer));
        tile.setLabel(getString(R.string.tile_label, minutes));

        if (Prefs.isRinging(this, slot)) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setSubtitle(getString(R.string.tile_sub_ringing));
        } else {
            long end = Prefs.endTime(this, slot);
            if (end > 0) {
                tile.setState(Tile.STATE_ACTIVE);
                tile.setSubtitle(getString(R.string.tile_sub_running, TimerManager.formatTime(this, end)));
            } else {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setSubtitle(getString(R.string.tile_sub_idle));
            }
        }
        tile.updateTile();
    }
}
