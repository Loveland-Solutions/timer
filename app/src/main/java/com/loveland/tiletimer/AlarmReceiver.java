package com.loveland.tiletimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Target of the exact alarm: a timer just reached zero. */
public class AlarmReceiver extends BroadcastReceiver {

    static final String ACTION_FIRE = "com.loveland.tiletimer.FIRE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_FIRE.equals(intent.getAction())) {
            return;
        }
        int slot = intent.getIntExtra(TimerManager.EXTRA_SLOT, -1);
        if (slot < 0 || slot >= Prefs.SLOTS) {
            return;
        }
        TimerManager.onFired(context, slot);
    }
}
