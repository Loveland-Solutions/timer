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
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.Consumer;

/**
 * Settings screen: tile durations, a button that offers to add the tiles to the
 * Quick Settings panel, and a checklist of the permissions the alarm needs.
 */
public class MainActivity extends Activity {

    private static final int REQ_NOTIF = 1;

    private final EditText[] minuteFields = new EditText[Prefs.MAX_TILES];
    private final EditText[] secondFields = new EditText[Prefs.MAX_TILES];
    private LinearLayout durationRows;
    private TextView tvTileCount;
    private Button btnFewer;
    private Button btnMore;
    private Switch cbCustom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.pad(this);
        Notifications.ensureChannels(this);

        durationRows = findViewById(R.id.durationRows);
        tvTileCount = findViewById(R.id.tvTileCount);
        btnFewer = findViewById(R.id.btnFewer);
        btnMore = findViewById(R.id.btnMore);
        cbCustom = findViewById(R.id.cbCustom);
        cbCustom.setChecked(Prefs.customEnabled(this));
        rebuildDurationRows();
        refreshTileControls();

        btnFewer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTimerCount(-1);
            }
        });

        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTimerCount(1);
            }
        });

        cbCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCustomTile(cbCustom.isChecked());
            }
        });

        Button addTiles = findViewById(R.id.btnAddTiles);
        addTiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAddTile(0, true);
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
                openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true);
            }
        });

        TextView about = findViewById(R.id.tvAbout);
        about.setText(getString(R.string.about_line, versionName()));

        if (!hasNotificationPermission()) {
            askNotificationPermission();
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChecks();
    }

    // ---- durations ---------------------------------------------------------

    /** Durations save as they are typed; a time that isn't valid is simply not kept. */
    private void saveDuration(int i) {
        int minutes = parseField(minuteFields[i]);
        int rest = parseField(secondFields[i]);
        int seconds = minutes * 60 + rest;
        if (minutes < 0 || minutes > 999 || rest < 0 || rest > 59 || seconds < 1) {
            return;
        }
        Prefs.setSeconds(this, i, seconds);
        TimerManager.refreshTile(this, i);
    }

    /** An empty box counts as 0; anything unreadable as -1. */
    private int parseField(EditText field) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---- quick settings ----------------------------------------------------

    private void changeTimerCount(int delta) {
        int count = Prefs.timerCount(this) + delta;
        TileConfig.apply(this, count, Prefs.customEnabled(this));
        rebuildDurationRows();
        refreshTileControls();
        if (delta > 0) {
            requestAddTile(count - 1, false);
        }
    }

    private void setCustomTile(boolean on) {
        TileConfig.apply(this, Prefs.timerCount(this), on);
        refreshTileControls();
        if (on) {
            requestAddTile(Prefs.CUSTOM_SLOT, false);
        }
    }

    private void rebuildDurationRows() {
        durationRows.removeAllViews();
        int count = Prefs.timerCount(this);
        for (int i = 0; i < count; i++) {
            View row = getLayoutInflater().inflate(R.layout.row_duration, durationRows, false);
            TextView label = row.findViewById(R.id.tvRowLabel);
            label.setText(getString(R.string.tile_row, i + 1));
            minuteFields[i] = row.findViewById(R.id.etRowMinutes);
            secondFields[i] = row.findViewById(R.id.etRowSeconds);
            // Rows share ids, so Android must not restore them; prefs are the source of truth.
            minuteFields[i].setSaveEnabled(false);
            secondFields[i].setSaveEnabled(false);
            minuteFields[i].setContentDescription(getString(R.string.desc_row_minutes, i + 1));
            secondFields[i].setContentDescription(getString(R.string.desc_row_seconds, i + 1));
            int seconds = Prefs.seconds(this, i);
            minuteFields[i].setText(String.valueOf(seconds / 60));
            secondFields[i].setText(String.valueOf(seconds % 60));
            final int index = i;
            TextWatcher saver = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    saveDuration(index);
                }
            };
            minuteFields[i].addTextChangedListener(saver);
            secondFields[i].addTextChangedListener(saver);
            durationRows.addView(row);
        }
    }

    private void refreshTileControls() {
        int count = Prefs.timerCount(this);
        boolean custom = Prefs.customEnabled(this);
        tvTileCount.setText(String.valueOf(count));
        btnFewer.setEnabled(count > 1);
        btnMore.setEnabled(count + (custom ? 1 : 0) < Prefs.MAX_TILES);
        cbCustom.setEnabled(custom || count < Prefs.MAX_TILES);
    }

    /** Offer one tile; with chain, carry on through the remaining enabled tiles. */
    private void requestAddTile(final int slot, final boolean chain) {
        if (slot >= Prefs.SLOTS) {
            return;
        }
        if (!TileConfig.isEnabled(this, slot)) {
            if (chain) {
                requestAddTile(slot + 1, true);
            }
            return;
        }
        StatusBarManager sbm = getSystemService(StatusBarManager.class);
        if (sbm == null) {
            Toast.makeText(this, R.string.toast_add_manually, Toast.LENGTH_LONG).show();
            return;
        }
        ComponentName tile = new ComponentName(this, TimerManager.tileClass(slot));
        CharSequence label = slot == Prefs.CUSTOM_SLOT
                ? getString(R.string.tile_custom_label)
                : TimerManager.formatDuration(this, Prefs.seconds(this, slot));
        Icon icon = Icon.createWithResource(this, R.drawable.ic_timer);
        try {
            sbm.requestAddTileService(tile, label, icon, getMainExecutor(), new Consumer<Integer>() {
                @Override
                public void accept(Integer result) {
                    // Whatever the answer for this one, offer the next tile.
                    if (chain) {
                        requestAddTile(slot + 1, true);
                    }
                }
            });
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.toast_add_manually, Toast.LENGTH_LONG).show();
        }
    }

    // ---- checks ------------------------------------------------------------

    private void refreshChecks() {
        boolean notif = hasNotificationPermission();
        boolean exact = getSystemService(AlarmManager.class).canScheduleExactAlarms();
        boolean fsi = true;
        if (Build.VERSION.SDK_INT >= 34) {
            fsi = getSystemService(NotificationManager.class).canUseFullScreenIntent();
        }
        findViewById(R.id.rowNotif).setVisibility(notif ? View.GONE : View.VISIBLE);
        findViewById(R.id.rowAlarm).setVisibility(exact ? View.GONE : View.VISIBLE);
        findViewById(R.id.rowFsi).setVisibility(fsi ? View.GONE : View.VISIBLE);
        findViewById(R.id.cardSetup).setVisibility(notif && exact && fsi ? View.GONE : View.VISIBLE);
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
