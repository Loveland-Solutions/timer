package mn.lls.tiletimer;

import android.app.Activity;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;

/** Android 15+ draws behind the system bars; keep every screen's content clear of them. */
final class EdgeToEdge {

    private EdgeToEdge() {}

    static void pad(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            }
        });
    }
}
