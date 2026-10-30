package mn.lls.tiletimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/** Full-screen "Time's up" screen, shown over the lock screen by the ringing notification. */
public class RingingActivity extends Activity {

    private static volatile RingingActivity showing;

    private int slot = -1;

    /** Called when ringing stops from elsewhere (notification button, tile) so the screen goes away. */
    static void closeIfShowing(int slot) {
        RingingActivity a = showing;
        if (a != null && a.slot == slot) {
            a.finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_ringing);

        Button stop = findViewById(R.id.btnStop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (slot >= 0) {
                    RingService.stop(RingingActivity.this, slot);
                }
                finish();
            }
        });

        Button plusOne = findViewById(R.id.btnPlusOne);
        plusOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (slot >= 0) {
                    TimerManager.extend(RingingActivity.this, slot, TimerManager.ONE_MINUTE_MS);
                }
                finish();
            }
        });

        applyIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntent(intent);
    }

    private void applyIntent(Intent intent) {
        slot = intent == null ? -1 : intent.getIntExtra(TimerManager.EXTRA_SLOT, -1);
        TextView subtitle = findViewById(R.id.tvRingSubtitle);
        if (slot >= 0 && slot < Prefs.SLOTS) {
            subtitle.setText(getString(R.string.ring_subtitle, Prefs.minutes(this, slot)));
        } else {
            subtitle.setText("");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showing = this;
        if (slot < 0 || !Prefs.isRinging(this, slot)) {
            // Already stopped from the notification or the tile.
            finish();
        }
    }

    @Override
    protected void onPause() {
        if (showing == this) {
            showing = null;
        }
        super.onPause();
    }
}
