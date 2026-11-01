package com.xiaomi.keystoneboot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class KeystoneBootService extends Service {
    private static final String TAG = "KeystoneBoot";
    private static final String CHANNEL_ID = "keystone_boot_channel";
    private static final int NOTIF_ID = 1001;
    private static volatile boolean sRunning = false;

    private Handler mMainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mMainHandler = new Handler(Looper.getMainLooper());
        startForegroundWithNotification("正在准备自动梯形校正...");
    }

    private void startForegroundWithNotification(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "开机自动梯形校正服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("开机自动梯形校正")
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .build();
            startForeground(NOTIF_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final boolean isTest = intent != null && intent.getBooleanExtra("is_test", false);
        final String trigger = intent != null ? intent.getStringExtra("trigger_action") : "unknown";

        if (sRunning) {
            Log.w(TAG, "KeystoneBootService is already running, ignoring duplicate trigger: " + trigger);
            return START_NOT_STICKY;
        }
        sRunning = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wl = null;
                if (pm != null) {
                    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KeystoneBoot::WakeLock");
                    wl.acquire(120 * 1000L);
                }
                try {
                    runKeystoneTask(isTest);
                } catch (Throwable t) {
                    Log.e(TAG, "Error during keystone task", t);
                    KeystoneConfig.saveLastStatus(KeystoneBootService.this, "失败: " + t.getMessage());
                    sendBroadcast(new Intent("com.xiaomi.keystoneboot.ACTION_STATUS_UPDATED"));
                } finally {
                    if (wl != null && wl.isHeld()) {
                        wl.release();
                    }
                    sRunning = false;
                    stopForeground(true);
                    stopSelf();
                }
            }
        }, "KeystoneBootWorker").start();

        return START_NOT_STICKY;
    }

    private void showToast(final String msg) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runKeystoneTask(boolean isTest) throws InterruptedException {
        KeystoneConfig cfg = KeystoneConfig.load(this);
        int delaySec = isTest ? 1 : Math.max(2, cfg.bootDelaySec);
        Log.i(TAG, "Waiting " + delaySec + "s before executing keystone adjustment (mode=" + cfg.mode + ", isTest=" + isTest + ")");
        if (isTest) {
            showToast("1秒后开始执行自动梯形校正...");
        }
        Thread.sleep(delaySec * 1000L);

        // Wait up to 15s for local controller HTTP service (port 6095) if needed
        waitForControllerService(10000);

        showToast("正在应用开机梯形校正参数...");

        // Step 1: Launch START_KEYSTONE activity
        // Creating KeystoneManualManager automatically invokes mFMKeyStoneManager.setKeystoneInit()
        // which triggers hardware ichip_keystone_init (loads saved 4-point Grid 0 & saved AngleV/AngleH).
        Intent ksIntent = new Intent("com.fengmi.projector.imaging.action.START_KEYSTONE");
        ksIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(ksIntent);

        // Wait for KeystoneManualManager window to open and hardware init to complete
        // At this point, initial focus is on the top button: manual_keystone_fast (快速调节)
        Thread.sleep(1600);

        int keyInterval = Math.max(150, cfg.keyIntervalMs);

        if (cfg.mode == KeystoneConfig.MODE_WAKE_SAVED) {
            if (!isTest) {
                // On cold boot, ProjGSensor (DEGREE_INIT) runs SetAngle(0, angleH) which resets angleV to 0
                // and clamps Top-Right [2,0] back to (0, 0).
                // Step A: Enter Fast Angle (mode = 1) and restore angleV = cfg.angleV (default +5)
                if (cfg.angleV != 0) {
                    sendKey("enter", 800);
                    if (cfg.angleV > 0) {
                        sendKeyRepeat("up", cfg.angleV, keyInterval);
                    } else {
                        sendKeyRepeat("down", -cfg.angleV, keyInterval);
                    }
                    Thread.sleep(200);
                    // Exit Fast Angle back to the 3-button menu (focus returns to button 0: 快速调节)
                    sendKey("back", 850);
                }

                // Step B: Enter 4-Corner Fine-Tune (四角微调, button 1) and restore Top-Right [2,0]
                sendKey("down", keyInterval);
                sendKey("enter", 800);
                sendKey("right", keyInterval);
                adjustCurrentCorner(cfg.trUp, cfg.trDown, cfg.trLeft, cfg.trRight, keyInterval);
                Thread.sleep(200);
                sendKey("back", 850);
                sendKey("back", 400);
                String summary = "已恢复冷开机校准位置 (垂直=" + cfg.angleV + ", 右上下" + cfg.trDown + "左" + cfg.trLeft + ")";
                KeystoneConfig.saveLastStatus(this, summary);
                sendBroadcast(new Intent("com.xiaomi.keystoneboot.ACTION_STATUS_UPDATED"));
                showToast("自动梯形校正完成（" + summary + "）");
                Log.i(TAG, summary);
                return;
            } else {
                // In manual test mode (when already calibrated), just enter 4-Corner Fine-Tune
                // and do a 1-step up/down nudge on Top-Right so KeystoneHal reloads Grid 0 without shifting coordinates.
                sendKey("down", keyInterval);
                sendKey("enter", 800);
                sendKey("right", keyInterval);
                sendKey("enter", keyInterval + 100);
                sendKey("up", keyInterval + 100);
                sendKey("down", keyInterval + 100);
                sendKey("back", keyInterval + 100);
                Thread.sleep(200);
                sendKey("back", 850);
                sendKey("back", 400);
                String summary = "已唤醒并保持当前四角校准位置";
                KeystoneConfig.saveLastStatus(this, summary);
                sendBroadcast(new Intent("com.xiaomi.keystoneboot.ACTION_STATUS_UPDATED"));
                showToast("自动梯形校正完成（" + summary + "）");
                Log.i(TAG, summary);
                return;
            }
        }

        if (cfg.mode == KeystoneConfig.MODE_FAST_ANGLE) {
            // Mode 1: Fast Angle adjustment (快速调节)
            // Initial focus in KeystoneManualManager is manual_keystone_fast (button 0)
            if (cfg.resetFirst) {
                // Move from button 0 (快速调节) -> button 1 (四角微调) -> button 2 (重置)
                sendKey("down", keyInterval);
                sendKey("down", keyInterval);
                sendKey("enter", 900); // Execute reset
                // Move back to button 0 (快速调节)
                sendKey("up", keyInterval);
                sendKey("up", keyInterval);
            }

            // Enter Fast Angle KeystoneManualView (mode = 1)
            sendKey("enter", 800);

            // In KeystoneManualView (mode = 1):
            // UP -> angleV + 1, DOWN -> angleV - 1
            // RIGHT -> angleH + 1, LEFT -> angleH - 1
            if (cfg.angleV > 0) {
                sendKeyRepeat("up", cfg.angleV, keyInterval);
            } else if (cfg.angleV < 0) {
                sendKeyRepeat("down", -cfg.angleV, keyInterval);
            }

            if (cfg.angleH > 0) {
                sendKeyRepeat("right", cfg.angleH, keyInterval);
            } else if (cfg.angleH < 0) {
                sendKeyRepeat("left", -cfg.angleH, keyInterval);
            }

            Thread.sleep(300);
            // First BACK exits KeystoneManualView back to KeystoneManualManager (takes ~600ms animation)
            sendKey("back", 800);
            // Second BACK closes KeystoneManualManager
            sendKey("back", 400);

            String summary = "已完成快速角度调节 (垂直=" + cfg.angleV + ", 水平=" + cfg.angleH + ")";
            KeystoneConfig.saveLastStatus(this, summary);
            sendBroadcast(new Intent("com.xiaomi.keystoneboot.ACTION_STATUS_UPDATED"));
            showToast("自动梯形校正完成！");
            Log.i(TAG, summary);
            return;
        }

        if (cfg.mode == KeystoneConfig.MODE_FOUR_CORNER) {
            // Mode 2: 4-Corner Fine-Tune (四角微调)
            // Initial focus in KeystoneManualManager is manual_keystone_fast (button 0)
            if (cfg.resetFirst) {
                // Move down twice to manual_keystone_reset (button 2)
                sendKey("down", keyInterval);
                sendKey("down", keyInterval);
                sendKey("enter", 900); // Execute reset
                // Move up once to manual_keystone_fine (button 1)
                sendKey("up", keyInterval);
            } else {
                // Move down once from manual_keystone_fast (button 0) to manual_keystone_fine (button 1)
                sendKey("down", keyInterval);
            }

            // Enter 4-Corner KeystoneManualView (mode = 0)
            // Initial focused corner is Top-Left (upLeftBtn, select = 1)
            sendKey("enter", 800);

            // 1. Top-Left corner
            adjustCurrentCorner(cfg.tlUp, cfg.tlDown, cfg.tlLeft, cfg.tlRight, keyInterval);

            // 2. Move to Top-Right corner (RIGHT from Top-Left)
            if (hasSteps(cfg.trUp, cfg.trDown, cfg.trLeft, cfg.trRight)
                    || hasSteps(cfg.brUp, cfg.brDown, cfg.brLeft, cfg.brRight)
                    || hasSteps(cfg.blUp, cfg.blDown, cfg.blLeft, cfg.blRight)) {
                sendKey("right", keyInterval);
                adjustCurrentCorner(cfg.trUp, cfg.trDown, cfg.trLeft, cfg.trRight, keyInterval);
            }

            // 3. Move to Bottom-Right corner (DOWN from Top-Right)
            if (hasSteps(cfg.brUp, cfg.brDown, cfg.brLeft, cfg.brRight)
                    || hasSteps(cfg.blUp, cfg.blDown, cfg.blLeft, cfg.blRight)) {
                sendKey("down", keyInterval);
                adjustCurrentCorner(cfg.brUp, cfg.brDown, cfg.brLeft, cfg.brRight, keyInterval);
            }

            // 4. Move to Bottom-Left corner (LEFT from Bottom-Right)
            if (hasSteps(cfg.blUp, cfg.blDown, cfg.blLeft, cfg.blRight)) {
                sendKey("left", keyInterval);
                adjustCurrentCorner(cfg.blUp, cfg.blDown, cfg.blLeft, cfg.blRight, keyInterval);
            }

            Thread.sleep(300);
            // First BACK exits KeystoneManualView back to KeystoneManualManager (takes ~600ms animation)
            sendKey("back", 800);
            // Second BACK closes KeystoneManualManager
            sendKey("back", 400);

            String summary = "已完成四角微调校正";
            KeystoneConfig.saveLastStatus(this, summary);
            sendBroadcast(new Intent("com.xiaomi.keystoneboot.ACTION_STATUS_UPDATED"));
            showToast("自动四角梯形校正完成！");
            Log.i(TAG, summary);
        }
    }

    private boolean hasSteps(int up, int down, int left, int right) {
        return up > 0 || down > 0 || left > 0 || right > 0;
    }

    private void adjustCurrentCorner(int up, int down, int left, int right, int intervalMs) throws InterruptedException {
        if (!hasSteps(up, down, left, right)) {
            return;
        }
        // Press ENTER to select the focused corner (isSelected = true)
        sendKey("enter", intervalMs + 100);
        if (up > 0) {
            sendKeyRepeat("up", up, intervalMs);
        }
        if (down > 0) {
            sendKeyRepeat("down", down, intervalMs);
        }
        if (left > 0) {
            sendKeyRepeat("left", left, intervalMs);
        }
        if (right > 0) {
            sendKeyRepeat("right", right, intervalMs);
        }
        // Press BACK to confirm/deselect the corner (isSelected = false) so D-pad can navigate to next corner
        sendKey("back", intervalMs + 100);
    }

    private void sendKeyRepeat(String keycode, int count, int intervalMs) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            sendKey(keycode, intervalMs);
        }
    }

    private boolean sendKey(String keycode, int sleepAfterMs) throws InterruptedException {
        boolean ok = false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:6095/controller?action=keyevent&keycode=" + keycode);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            try (InputStream is = conn.getInputStream()) {
                byte[] buf = new byte[64];
                while (is.read(buf) != -1) {
                    // consume
                }
            }
            ok = (code == 200);
            Log.d(TAG, "sendKey(" + keycode + ") -> HTTP " + code);
        } catch ( Exception e) {
            Log.w(TAG, "sendKey(" + keycode + ") failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        if (sleepAfterMs > 0) {
            Thread.sleep(sleepAfterMs);
        }
        return ok;
    }

    private void waitForControllerService(long maxWaitMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < maxWaitMs) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:6095/controller?action=getsysteminfo");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                if (conn.getResponseCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            Thread.sleep(1000);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
