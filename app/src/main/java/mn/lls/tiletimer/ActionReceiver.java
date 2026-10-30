package mn.lls.tiletimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Notification action buttons: Cancel, +1 min, Stop. */
public class ActionReceiver extends BroadcastReceiver {

    static final String ACTION_CANCEL = "mn.lls.tiletimer.CANCEL";
    static final String ACTION_PLUS = "mn.lls.tiletimer.PLUS";
    static final String ACTION_STOP_RING = "mn.lls.tiletimer.STOP_RING";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        int slot = intent.getIntExtra(TimerManager.EXTRA_SLOT, -1);
        if (slot < 0 || slot >= Prefs.SLOTS) {
            return;
        }
        String action = intent.getAction();
        if (ACTION_CANCEL.equals(action)) {
            TimerManager.cancel(context, slot);
        } else if (ACTION_PLUS.equals(action)) {
            TimerManager.extend(context, slot, TimerManager.ONE_MINUTE_MS);
        } else if (ACTION_STOP_RING.equals(action)) {
            RingService.stop(context, slot);
        }
    }
}
