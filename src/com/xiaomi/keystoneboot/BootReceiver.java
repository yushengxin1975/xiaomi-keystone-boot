package com.xiaomi.keystoneboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "KeystoneBoot";
    public static final String ACTION_TEST_RUN = "com.xiaomi.keystoneboot.ACTION_TEST_RUN";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        Log.i(TAG, "BootReceiver received action: " + action);

        KeystoneConfig cfg = KeystoneConfig.load(context);
        boolean isTest = ACTION_TEST_RUN.equals(action);
        if (!isTest && !cfg.enabled) {
            Log.i(TAG, "Auto keystone on boot is disabled, skipping.");
            return;
        }

        Intent serviceIntent = new Intent(context, KeystoneBootService.class);
        serviceIntent.putExtra("is_test", isTest);
        serviceIntent.putExtra("trigger_action", action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
