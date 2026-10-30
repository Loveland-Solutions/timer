package mn.lls.tiletimer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.util.Locale;

/** Opened by the Custom tile: pick a time and start. Works over the lock screen. */
public class CustomTimerActivity extends Activity {

    private static final int MAX_HOURS = 16;
    private static final int WHEEL_TEXT_SP = 44;

    private NumberPicker hours;
    private NumberPicker minutes;
    private NumberPicker seconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);
        EdgeToEdge.pad(this);

        int last = Prefs.seconds(this, Prefs.CUSTOM_SLOT);
        hours = findViewById(R.id.pickHours);
        minutes = findViewById(R.id.pickMinutes);
        seconds = findViewById(R.id.pickSeconds);
        setUpPicker(hours, MAX_HOURS, Math.min(last / 3600, MAX_HOURS));
        setUpPicker(minutes, 59, last / 60 % 60);
        setUpPicker(seconds, 59, last % 60);

        View.OnClickListener chip = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int preset = Integer.parseInt((String) v.getTag());
                hours.setValue(0);
                minutes.setValue(preset / 60);
                seconds.setValue(0);
            }
        };
        findViewById(R.id.chip1).setOnClickListener(chip);
        findViewById(R.id.chip5).setOnClickListener(chip);
        findViewById(R.id.chip10).setOnClickListener(chip);
        findViewById(R.id.chip30).setOnClickListener(chip);

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
        picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int number) {
                return String.format(Locale.US, "%02d", number);
            }
        });
        picker.setTextSize(getResources().getDisplayMetrics().scaledDensity * WHEEL_TEXT_SP);
        picker.setTextColor(getColor(R.color.lock_fg));
        picker.setSelectionDividerHeight(0);
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
