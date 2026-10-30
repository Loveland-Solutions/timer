package mn.lls.tiletimer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

/** Opened by the Custom tile: pick a time and start. Works over the lock screen. */
public class CustomTimerActivity extends Activity {

    private static final int MAX_HOURS = 16;

    private NumberPicker hours;
    private NumberPicker minutes;
    private NumberPicker seconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);

        int last = Prefs.seconds(this, Prefs.CUSTOM_SLOT);
        hours = findViewById(R.id.pickHours);
        minutes = findViewById(R.id.pickMinutes);
        seconds = findViewById(R.id.pickSeconds);
        setUpPicker(hours, MAX_HOURS, Math.min(last / 3600, MAX_HOURS));
        setUpPicker(minutes, 59, last / 60 % 60);
        setUpPicker(seconds, 59, last % 60);

        Button start = findViewById(R.id.btnCustomStart);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();
            }
        });

        Button cancel = findViewById(R.id.btnCustomCancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setUpPicker(NumberPicker picker, int max, int value) {
        picker.setMinValue(0);
        picker.setMaxValue(max);
        picker.setValue(value);
        // Wheels only: no keyboard over the lock screen.
        picker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    private void startTimer() {
        int total = hours.getValue() * 3600 + minutes.getValue() * 60 + seconds.getValue();
        if (total < 1) {
            Toast.makeText(this, R.string.toast_custom_zero, Toast.LENGTH_SHORT).show();
            return;
        }
        Prefs.setSeconds(this, Prefs.CUSTOM_SLOT, total);
        TimerManager.startFor(this, Prefs.CUSTOM_SLOT, total * 1000L);
        finish();
    }
}
