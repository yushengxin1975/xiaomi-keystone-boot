package com.xiaomi.keystoneboot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class KeystoneConfig {
    private static final String PREF_NAME = "keystone_boot_prefs";

    public static final int MODE_WAKE_SAVED = 0;      // 仅唤醒加载硬件已存参数 (Grid 0 + Angle)
    public static final int MODE_FAST_ANGLE = 1;      // 快速角度调节 (angleV, angleH)
    public static final int MODE_FOUR_CORNER = 2;     // 四角微调 (左上、右上、右下、左下)

    public boolean enabled = true;
    public int bootDelaySec = 12;
    public int keyIntervalMs = 200;
    public int mode = MODE_WAKE_SAVED;
    public boolean resetFirst = false;

    // 快速角度参数 (范围 -45 ~ +45，默认填入投影仪当前读取到的 V=5, H=-2)
    public int angleV = 5;
    public int angleH = -2;

    // 四角微调步数: 默认填入实机拍照校准后的精确步数 (右上: 下30/左8, 左下: 上30/右29)
    public int tlUp = 0, tlDown = 0, tlLeft = 0, tlRight = 0;
    public int trUp = 0, trDown = 30, trLeft = 8, trRight = 0;
    public int brUp = 0, brDown = 0, brLeft = 0, brRight = 0;
    public int blUp = 30, blDown = 0, blLeft = 0, blRight = 29;

    public String lastStatus = "尚未运行";
    public long lastRunTime = 0L;

    private static Context getStorageContext(Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            Context deviceCtx = context.createDeviceProtectedStorageContext();
            if (deviceCtx != null) {
                return deviceCtx;
            }
        }
        return context;
    }

    public static KeystoneConfig load(Context context) {
        SharedPreferences sp = getStorageContext(context).getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        KeystoneConfig cfg = new KeystoneConfig();
        cfg.enabled = sp.getBoolean("enabled", true);
        cfg.bootDelaySec = sp.getInt("bootDelaySec", 12);
        cfg.keyIntervalMs = sp.getInt("keyIntervalMs", 200);
        cfg.mode = sp.getInt("mode", MODE_WAKE_SAVED);
        cfg.resetFirst = sp.getBoolean("resetFirst", false);

        cfg.angleV = sp.getInt("angleV", 5);
        cfg.angleH = sp.getInt("angleH", -2);

        cfg.tlUp = sp.getInt("tlUp", 0);
        cfg.tlDown = sp.getInt("tlDown", 0);
        cfg.tlLeft = sp.getInt("tlLeft", 0);
        cfg.tlRight = sp.getInt("tlRight", 0);

        cfg.trUp = sp.getInt("trUp", 0);
        cfg.trDown = sp.getInt("trDown", 30);
        cfg.trLeft = sp.getInt("trLeft", 8);
        cfg.trRight = sp.getInt("trRight", 0);

        cfg.brUp = sp.getInt("brUp", 0);
        cfg.brDown = sp.getInt("brDown", 0);
        cfg.brLeft = sp.getInt("brLeft", 0);
        cfg.brRight = sp.getInt("brRight", 0);

        cfg.blUp = sp.getInt("blUp", 30);
        cfg.blDown = sp.getInt("blDown", 0);
        cfg.blLeft = sp.getInt("blLeft", 0);
        cfg.blRight = sp.getInt("blRight", 29);

        cfg.lastStatus = sp.getString("lastStatus", "尚未运行");
        cfg.lastRunTime = sp.getLong("lastRunTime", 0L);
        return cfg;
    }

    public void save(Context context) {
        SharedPreferences sp = getStorageContext(context).getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
            .putBoolean("enabled", enabled)
            .putInt("bootDelaySec", bootDelaySec)
            .putInt("keyIntervalMs", keyIntervalMs)
            .putInt("mode", mode)
            .putBoolean("resetFirst", resetFirst)
            .putInt("angleV", angleV)
            .putInt("angleH", angleH)
            .putInt("tlUp", tlUp).putInt("tlDown", tlDown).putInt("tlLeft", tlLeft).putInt("tlRight", tlRight)
            .putInt("trUp", trUp).putInt("trDown", trDown).putInt("trLeft", trLeft).putInt("trRight", trRight)
            .putInt("brUp", brUp).putInt("brDown", brDown).putInt("brLeft", brLeft).putInt("brRight", brRight)
            .putInt("blUp", blUp).putInt("blDown", blDown).putInt("blLeft", blLeft).putInt("blRight", blRight)
            .apply();
    }

    public static void saveLastStatus(Context context, String status) {
        SharedPreferences sp = getStorageContext(context).getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
            .putString("lastStatus", status)
            .putLong("lastRunTime", System.currentTimeMillis())
            .apply();
    }
}
