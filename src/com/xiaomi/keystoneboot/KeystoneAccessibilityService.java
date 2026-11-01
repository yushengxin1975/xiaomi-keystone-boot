package com.xiaomi.keystoneboot;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class KeystoneAccessibilityService extends AccessibilityService {
    private static final String TAG = "KeystoneBoot";
    private static boolean sBootTriggeredInProcess = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        long uptimeSec = SystemClock.elapsedRealtime() / 1000L;
        Log.i(TAG, "KeystoneAccessibilityService connected (uptime=" + uptimeSec + "s)");

        if (sBootTriggeredInProcess) {
            return;
        }
        sBootTriggeredInProcess = true;

        // Only auto-trigger on cold boot / system restart (uptime < 180 seconds)
        if (uptimeSec > 180L) {
            Log.i(TAG, "Uptime > 180s (" + uptimeSec + "s), skipping accessibility boot trigger.");
            return;
        }

        KeystoneConfig cfg = KeystoneConfig.load(this);
        if (!cfg.enabled) {
            Log.i(TAG, "Auto keystone on boot is disabled, skipping accessibility trigger.");
            return;
        }

        Intent serviceIntent = new Intent(this, KeystoneBootService.class);
        serviceIntent.putExtra("is_test", false);
        serviceIntent.putExtra("trigger_action", "ACCESSIBILITY_BOOT");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No-op: only used for reliable system-level cold-boot startup
    }

    @Override
    public void onInterrupt() {
        // No-op
    }
}
