package com.xiaomi.keystoneboot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private KeystoneConfig mConfig;

    private TextView mStatusView;
    private Button mBtnToggleEnable;
    private TextView mValDelay;
    private TextView mValInterval;
    private Button mBtnMode;
    private Button mBtnResetFirst;

    private LinearLayout mFastAnglePanel;
    private TextView mValAngleV;
    private TextView mValAngleH;

    private LinearLayout mFourCornerPanel;
    private TextView mValTlUp, mValTlDown, mValTlLeft, mValTlRight;
    private TextView mValTrUp, mValTrDown, mValTrLeft, mValTrRight;
    private TextView mValBrUp, mValBrDown, mValBrLeft, mValBrRight;
    private TextView mValBlUp, mValBlDown, mValBlLeft, mValBlRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfig = KeystoneConfig.load(this);
        buildUi();
        refreshUi();
    }

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mConfig = KeystoneConfig.load(MainActivity.this);
            refreshUi();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mStatusReceiver, new IntentFilter("com.xiaomi.keystoneboot.ACTION_STATUS_UPDATED"));
        mConfig = KeystoneConfig.load(this);
        refreshUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mStatusReceiver);
        } catch (Exception ignored) {
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private StateListDrawable createFocusableBg(int normalColor, int focusedColor) {
        StateListDrawable states = new StateListDrawable();

        GradientDrawable focused = new GradientDrawable();
        focused.setColor(focusedColor);
        focused.setCornerRadius(dp(6));
        focused.setStroke(dp(2), Color.WHITE);

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(6));

        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{android.R.attr.state_pressed}, focused);
        states.addState(new int[]{}, normal);
        return states;
    }

    private Button makeButton(String text, int normalColor, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setFocusableInTouchMode(false);
        b.setBackground(createFocusableBg(normalColor, Color.parseColor("#1E88E5")));
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        if (listener != null) {
            b.setOnClickListener(listener);
        }
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#121824"));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(32), dp(24), dp(32), dp(32));
        scroll.addView(root);

        // Title
        TextView title = new TextView(this);
        title.setText("小米/峰米投影仪 · 开机自动梯形校正");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("解决每次断电/冷开机后梯形校正未自动加载或需要自动按固定参数调节的问题（使用遥控器方向键与确认键操作）");
        subtitle.setTextColor(Color.parseColor("#90A4AE"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        // Status banner
        mStatusView = new TextView(this);
        mStatusView.setTextColor(Color.parseColor("#80CBC4"));
        mStatusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#1E293B"));
        statusBg.setCornerRadius(dp(6));
        mStatusView.setBackground(statusBg);
        mStatusView.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.bottomMargin = dp(16);
        root.addView(mStatusView, statusLp);

        // Action bar (Test now / Open native keystone UI)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(14);
        root.addView(actionRow, rowLp);

        Button btnTestNow = makeButton("▶ 立即测试执行梯形校正", Color.parseColor("#2E7D32"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.save(MainActivity.this);
                Intent i = new Intent(MainActivity.this, BootReceiver.class);
                i.setAction(BootReceiver.ACTION_TEST_RUN);
                sendBroadcast(i);
            }
        });
        LinearLayout.LayoutParams btnLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnLp1.rightMargin = dp(10);
        actionRow.addView(btnTestNow, btnLp1);

        Button btnOpenNative = makeButton("⚙ 打开系统原生梯形校正界面", Color.parseColor("#37474F"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent ks = new Intent("com.fengmi.projector.imaging.action.START_KEYSTONE");
                    ks.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(ks);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actionRow.addView(btnOpenNative, btnLp2);

        // Row: Enable toggle & Mode switch
        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(toggleRow, rowLp);

        mBtnToggleEnable = makeButton("", Color.parseColor("#263238"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.enabled = !mConfig.enabled;
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        });
        toggleRow.addView(mBtnToggleEnable, btnLp1);

        mBtnMode = makeButton("", Color.parseColor("#263238"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.mode = (mConfig.mode + 1) % 3;
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        });
        toggleRow.addView(mBtnMode, btnLp2);

        // Row: Boot delay & Key interval & Reset first
        LinearLayout timingRow = new LinearLayout(this);
        timingRow.setOrientation(LinearLayout.HORIZONTAL);
        timingRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(timingRow, rowLp);

        mValDelay = new TextView(this);
        addStepperGroup(timingRow, "开机延迟(秒):", mValDelay, new Runnable() {
            @Override
            public void run() {
                mConfig.bootDelaySec = Math.max(2, mConfig.bootDelaySec - 1);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        }, new Runnable() {
            @Override
            public void run() {
                mConfig.bootDelaySec = Math.min(60, mConfig.bootDelaySec + 1);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        });

        mValInterval = new TextView(this);
        addStepperGroup(timingRow, "  按键间隔(ms):", mValInterval, new Runnable() {
            @Override
            public void run() {
                mConfig.keyIntervalMs = Math.max(150, mConfig.keyIntervalMs - 50);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        }, new Runnable() {
            @Override
            public void run() {
                mConfig.keyIntervalMs = Math.min(1000, mConfig.keyIntervalMs + 50);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        });

        mBtnResetFirst = makeButton("", Color.parseColor("#263238"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.resetFirst = !mConfig.resetFirst;
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        });
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetLp.leftMargin = dp(16);
        timingRow.addView(mBtnResetFirst, resetLp);

        // Panel for Mode 1: Fast Angle
        mFastAnglePanel = new LinearLayout(this);
        mFastAnglePanel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable panelBg1 = new GradientDrawable();
        panelBg1.setColor(Color.parseColor("#1E293B"));
        panelBg1.setCornerRadius(dp(8));
        mFastAnglePanel.setBackground(panelBg1);
        mFastAnglePanel.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(mFastAnglePanel, rowLp);

        TextView faTitle = new TextView(this);
        faTitle.setText("【模式二：快速角度调节参数】正数代表按上/右键步数，负数代表按下/左键步数（当前系统已存值为 垂直=5, 水平=-2）");
        faTitle.setTextColor(Color.parseColor("#B0BEC5"));
        faTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        faTitle.setPadding(0, 0, 0, dp(8));
        mFastAnglePanel.addView(faTitle);

        LinearLayout faRow = new LinearLayout(this);
        faRow.setOrientation(LinearLayout.HORIZONTAL);
        faRow.setGravity(Gravity.CENTER_VERTICAL);
        mFastAnglePanel.addView(faRow);

        mValAngleV = new TextView(this);
        addStepperGroup(faRow, "垂直角度 (上+/下-):", mValAngleV, new Runnable() {
            @Override
            public void run() {
                mConfig.angleV = Math.max(-45, mConfig.angleV - 1);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        }, new Runnable() {
            @Override
            public void run() {
                mConfig.angleV = Math.min(45, mConfig.angleV + 1);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        });

        mValAngleH = new TextView(this);
        addStepperGroup(faRow, "    水平角度 (右+/左-):", mValAngleH, new Runnable() {
            @Override
            public void run() {
                mConfig.angleH = Math.max(-45, mConfig.angleH - 1);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        }, new Runnable() {
            @Override
            public void run() {
                mConfig.angleH = Math.min(45, mConfig.angleH + 1);
                mConfig.save(MainActivity.this);
                refreshUi();
            }
        });

        // Panel for Mode 2: Four Corner
        mFourCornerPanel = new LinearLayout(this);
        mFourCornerPanel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable panelBg2 = new GradientDrawable();
        panelBg2.setColor(Color.parseColor("#1E293B"));
        panelBg2.setCornerRadius(dp(8));
        mFourCornerPanel.setBackground(panelBg2);
        mFourCornerPanel.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(mFourCornerPanel, rowLp);

        TextView fcTitle = new TextView(this);
        fcTitle.setText("【模式三：四角微调按键步数】设置每个角自动按上/下/左/右的步数（0表示该方向不按键）");
        fcTitle.setTextColor(Color.parseColor("#B0BEC5"));
        fcTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        fcTitle.setPadding(0, 0, 0, dp(8));
        mFourCornerPanel.addView(fcTitle);

        mValTlUp = new TextView(this);
        mValTlDown = new TextView(this);
        mValTlLeft = new TextView(this);
        mValTlRight = new TextView(this);
        addCornerRow(mFourCornerPanel, "左上角:", mValTlUp, mValTlDown, mValTlLeft, mValTlRight, 0);

        mValTrUp = new TextView(this);
        mValTrDown = new TextView(this);
        mValTrLeft = new TextView(this);
        mValTrRight = new TextView(this);
        addCornerRow(mFourCornerPanel, "右上角:", mValTrUp, mValTrDown, mValTrLeft, mValTrRight, 1);

        mValBrUp = new TextView(this);
        mValBrDown = new TextView(this);
        mValBrLeft = new TextView(this);
        mValBrRight = new TextView(this);
        addCornerRow(mFourCornerPanel, "右下角:", mValBrUp, mValBrDown, mValBrLeft, mValBrRight, 2);

        mValBlUp = new TextView(this);
        mValBlDown = new TextView(this);
        mValBlLeft = new TextView(this);
        mValBlRight = new TextView(this);
        addCornerRow(mFourCornerPanel, "左下角:", mValBlUp, mValBlDown, mValBlLeft, mValBlRight, 3);

        setContentView(scroll);
        btnTestNow.requestFocus();
    }

    private void addStepperGroup(LinearLayout parent, String labelText, TextView valueTv,
                                 final Runnable onMinus, final Runnable onPlus) {
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setPadding(0, 0, dp(6), 0);
        parent.addView(label);

        Button btnMinus = makeButton("-", Color.parseColor("#37474F"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMinus.run();
            }
        });
        LinearLayout.LayoutParams smallBtnLp = new LinearLayout.LayoutParams(dp(44), dp(38));
        parent.addView(btnMinus, smallBtnLp);

        valueTv.setTextColor(Color.parseColor("#FFD54F"));
        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        valueTv.setGravity(Gravity.CENTER);
        valueTv.setMinWidth(dp(44));
        parent.addView(valueTv);

        Button btnPlus = makeButton("+", Color.parseColor("#37474F"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlus.run();
            }
        });
        parent.addView(btnPlus, smallBtnLp);
    }

    private void addCornerRow(LinearLayout parent, String cornerName,
                              TextView tvUp, TextView tvDown, TextView tvLeft, TextView tvRight,
                              final int cornerIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        parent.addView(row, lp);

        TextView title = new TextView(this);
        title.setText(cornerName);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setMinWidth(dp(68));
        row.addView(title);

        addStepperGroup(row, "上:", tvUp,
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 0, -1); } },
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 0, 1); } });
        addStepperGroup(row, "  下:", tvDown,
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 1, -1); } },
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 1, 1); } });
        addStepperGroup(row, "  左:", tvLeft,
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 2, -1); } },
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 2, 1); } });
        addStepperGroup(row, "  右:", tvRight,
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 3, -1); } },
                new Runnable() { @Override public void run() { adjustCornerVal(cornerIndex, 3, 1); } });
    }

    private void adjustCornerVal(int cornerIndex, int dir, int delta) {
        if (cornerIndex == 0) {
            if (dir == 0) mConfig.tlUp = Math.max(0, mConfig.tlUp + delta);
            if (dir == 1) mConfig.tlDown = Math.max(0, mConfig.tlDown + delta);
            if (dir == 2) mConfig.tlLeft = Math.max(0, mConfig.tlLeft + delta);
            if (dir == 3) mConfig.tlRight = Math.max(0, mConfig.tlRight + delta);
        } else if (cornerIndex == 1) {
            if (dir == 0) mConfig.trUp = Math.max(0, mConfig.trUp + delta);
            if (dir == 1) mConfig.trDown = Math.max(0, mConfig.trDown + delta);
            if (dir == 2) mConfig.trLeft = Math.max(0, mConfig.trLeft + delta);
            if (dir == 3) mConfig.trRight = Math.max(0, mConfig.trRight + delta);
        } else if (cornerIndex == 2) {
            if (dir == 0) mConfig.brUp = Math.max(0, mConfig.brUp + delta);
            if (dir == 1) mConfig.brDown = Math.max(0, mConfig.brDown + delta);
            if (dir == 2) mConfig.brLeft = Math.max(0, mConfig.brLeft + delta);
            if (dir == 3) mConfig.brRight = Math.max(0, mConfig.brRight + delta);
        } else if (cornerIndex == 3) {
            if (dir == 0) mConfig.blUp = Math.max(0, mConfig.blUp + delta);
            if (dir == 1) mConfig.blDown = Math.max(0, mConfig.blDown + delta);
            if (dir == 2) mConfig.blLeft = Math.max(0, mConfig.blLeft + delta);
            if (dir == 3) mConfig.blRight = Math.max(0, mConfig.blRight + delta);
        }
        mConfig.save(this);
        refreshUi();
    }

    private void refreshUi() {
        String timeStr = mConfig.lastRunTime > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(mConfig.lastRunTime))
                : "尚无记录";
        mStatusView.setText("最近一次自动执行状态: " + mConfig.lastStatus + "  （时间: " + timeStr + "）");

        mBtnToggleEnable.setText(mConfig.enabled ? "开机自启状态: [已启用 ✓]" : "开机自启状态: [已关闭 ✗]");

        String modeText;
        if (mConfig.mode == KeystoneConfig.MODE_WAKE_SAVED) {
            modeText = "执行模式: [模式一：唤醒加载系统已存梯形数据(推荐)]";
        } else if (mConfig.mode == KeystoneConfig.MODE_FAST_ANGLE) {
            modeText = "执行模式: [模式二：按快速垂直/水平角度调节]";
        } else {
            modeText = "执行模式: [模式三：按四角微调步数调节]";
        }
        mBtnMode.setText(modeText);

        mValDelay.setText(String.valueOf(mConfig.bootDelaySec));
        mValInterval.setText(String.valueOf(mConfig.keyIntervalMs));
        mBtnResetFirst.setText(mConfig.resetFirst ? "调节前先重置: [是]" : "调节前先重置: [否]");

        mFastAnglePanel.setVisibility(mConfig.mode == KeystoneConfig.MODE_FAST_ANGLE ? View.VISIBLE : View.GONE);
        mFourCornerPanel.setVisibility(mConfig.mode == KeystoneConfig.MODE_FOUR_CORNER ? View.VISIBLE : View.GONE);

        mValAngleV.setText(String.valueOf(mConfig.angleV));
        mValAngleH.setText(String.valueOf(mConfig.angleH));

        mValTlUp.setText(String.valueOf(mConfig.tlUp));
        mValTlDown.setText(String.valueOf(mConfig.tlDown));
        mValTlLeft.setText(String.valueOf(mConfig.tlLeft));
        mValTlRight.setText(String.valueOf(mConfig.tlRight));

        mValTrUp.setText(String.valueOf(mConfig.trUp));
        mValTrDown.setText(String.valueOf(mConfig.trDown));
        mValTrLeft.setText(String.valueOf(mConfig.trLeft));
        mValTrRight.setText(String.valueOf(mConfig.trRight));

        mValBrUp.setText(String.valueOf(mConfig.brUp));
        mValBrDown.setText(String.valueOf(mConfig.brDown));
        mValBrLeft.setText(String.valueOf(mConfig.brLeft));
        mValBrRight.setText(String.valueOf(mConfig.brRight));

        mValBlUp.setText(String.valueOf(mConfig.blUp));
        mValBlDown.setText(String.valueOf(mConfig.blDown));
        mValBlLeft.setText(String.valueOf(mConfig.blLeft));
        mValBlRight.setText(String.valueOf(mConfig.blRight));
    }
}
