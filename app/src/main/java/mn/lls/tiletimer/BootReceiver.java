package mn.lls.tiletimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Exact alarms do not survive a reboot, so re-arm any timer that is still in the future. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        TimerManager.restoreAfterBoot(context);
    }
}
