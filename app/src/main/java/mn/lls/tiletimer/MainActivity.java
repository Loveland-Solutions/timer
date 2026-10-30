package mn.lls.tiletimer;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.Consumer;

/**
 * Settings screen: tile durations, a button that offers to add the tiles to the
 * Quick Settings panel, and a checklist of the permissions the alarm needs.
 */
public class MainActivity extends Activity {

    private static final int REQ_NOTIF = 1;

    private final EditText[] minuteFields = new EditText[Prefs.SLOTS];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifications.ensureChannels(this);

        minuteFields[0] = findViewById(R.id.min1);
        minuteFields[1] = findViewById(R.id.min2);
        minuteFields[2] = findViewById(R.id.min3);
        for (int i = 0; i < Prefs.SLOTS; i++) {
            minuteFields[i].setText(String.valueOf(Prefs.minutes(this, i)));
        }

        Button save = findViewById(R.id.btnSave);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDurations();
            }
        });

        Button addTiles = findViewById(R.id.btnAddTiles);
        addTiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAddTile(0);
            }
        });

        Button test = findViewById(R.id.btnTest);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTest();
            }
        });

        Button fixNotif = findViewById(R.id.btnNotif);
        fixNotif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askNotificationPermission();
            }
        });

        Button fixAlarm = findViewById(R.id.btnAlarm);
        fixAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, true);
            }
        });

        Button fixFsi = findViewById(R.id.btnFsi);
        fixFsi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 34) {
                    openSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, true);
                }
            }
        });

        Button fixBattery = findViewById(R.id.btnBattery);
        fixBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, true);
            }
        });

        if (!hasNotificationPermission()) {
            askNotificationPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChecks();
    }

    // ---- durations ---------------------------------------------------------

    private void saveDurations() {
        for (int i = 0; i < Prefs.SLOTS; i++) {
            int minutes;
            try {
                minutes = Integer.parseInt(minuteFields[i].getText().toString().trim());
            } catch (NumberFormatException e) {
                minutes = 0;
            }
            if (minutes < 1 || minutes > 999) {
                Toast.makeText(this, getString(R.string.toast_bad_minutes, i + 1), Toast.LENGTH_LONG).show();
                return;
            }
            Prefs.setMinutes(this, i, minutes);
        }
        TimerManager.refreshAllTiles(this);
        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
    }

    // ---- quick settings ----------------------------------------------------

    private void requestAddTile(final int slot) {
        if (slot >= Prefs.SLOTS) {
            return;
        }
        StatusBarManager sbm = getSystemService(StatusBarManager.class);
        if (sbm == null) {
            Toast.makeText(this, R.string.toast_add_manually, Toast.LENGTH_LONG).show();
            return;
        }
        ComponentName tile = new ComponentName(this, TimerManager.tileClass(slot));
        CharSequence label = getString(R.string.tile_label, Prefs.minutes(this, slot));
        Icon icon = Icon.createWithResource(this, R.drawable.ic_timer);
        sbm.requestAddTileService(tile, label, icon, getMainExecutor(), new Consumer<Integer>() {
            @Override
            public void accept(Integer result) {
                // Whatever the answer for this one, offer the next tile.
                requestAddTile(slot + 1);
            }
        });
    }

    // ---- checks ------------------------------------------------------------

    private void refreshChecks() {
        TextView tvNotif = findViewById(R.id.tvNotif);
        Button btnNotif = findViewById(R.id.btnNotif);
        boolean notif = hasNotificationPermission();
        tvNotif.setText(notif ? R.string.check_notif_ok : R.string.check_notif_missing);
        btnNotif.setVisibility(notif ? View.GONE : View.VISIBLE);

        TextView tvAlarm = findViewById(R.id.tvAlarm);
        Button btnAlarm = findViewById(R.id.btnAlarm);
        AlarmManager am = getSystemService(AlarmManager.class);
        boolean exact = am.canScheduleExactAlarms();
        tvAlarm.setText(exact ? R.string.check_alarm_ok : R.string.check_alarm_missing);
        btnAlarm.setVisibility(exact ? View.GONE : View.VISIBLE);

        TextView tvFsi = findViewById(R.id.tvFsi);
        Button btnFsi = findViewById(R.id.btnFsi);
        boolean fsi = true;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            fsi = nm.canUseFullScreenIntent();
        }
        tvFsi.setText(fsi ? R.string.check_fsi_ok : R.string.check_fsi_missing);
        btnFsi.setVisibility(fsi ? View.GONE : View.VISIBLE);

        TextView tvBattery = findViewById(R.id.tvBattery);
        Button btnBattery = findViewById(R.id.btnBattery);
        PowerManager pm = getSystemService(PowerManager.class);
        boolean unrestricted = pm.isIgnoringBatteryOptimizations(getPackageName());
        tvBattery.setText(unrestricted ? R.string.check_battery_ok : R.string.check_battery_missing);
        btnBattery.setVisibility(unrestricted ? View.GONE : View.VISIBLE);
    }

    private boolean hasNotificationPermission() {
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void askNotificationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                || !hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF && !hasNotificationPermission()) {
            // Denied twice: the system dialog will not show again, send them to the app's settings page.
            openSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS, false);
        }
        refreshChecks();
    }

    private void openSettings(String action, boolean withPackageUri) {
        Intent i = new Intent(action);
        if (withPackageUri) {
            i.setData(Uri.parse("package:" + getPackageName()));
        } else {
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        }
        try {
            startActivity(i);
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.toast_no_settings_screen, Toast.LENGTH_LONG).show();
        }
    }

    // ---- test --------------------------------------------------------------

    private void runTest() {
        if (!hasNotificationPermission()) {
            askNotificationPermission();
            return;
        }
        if (Prefs.isActive(this, 0)) {
            Toast.makeText(this, R.string.toast_test_busy, Toast.LENGTH_LONG).show();
            return;
        }
        TimerManager.startFor(this, 0, 5_000L);
        Toast.makeText(this, R.string.toast_test_started, Toast.LENGTH_LONG).show();
    }
}
