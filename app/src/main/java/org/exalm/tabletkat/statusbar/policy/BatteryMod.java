package org.exalm.tabletkat.statusbar.policy;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import org.exalm.tabletkat.TkPrefs;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * BatteryMod
 * ==========
 * Tints the BatteryMeterView to match the Holo theme using multiple fallbacks.
 */
public class BatteryMod {
    private static final String TAG = "TabletKat/BatteryMod";
    private static java.util.Set<Object> sBatteries = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static void initHooks(ClassLoader cl) {
        try {
            final Class<?> batteryMeterViewClass = XposedHelpers.findClass("com.android.systemui.BatteryMeterView", cl);

            // 1. Hook onAttachedToWindow - Very stable way to set initial colors
            XposedHelpers.findAndHookMethod(batteryMeterViewClass, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    sBatteries.add(param.thisObject);
                    applyHoloTint(param.thisObject);
                }
            });

            // 2. Hook updateSettings - Common in KitKat to refresh colors
            try {
                XposedHelpers.findAndHookMethod(batteryMeterViewClass, "updateSettings", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        applyHoloTint(param.thisObject);
                    }
                });
            } catch (Throwable ignored) {}

            // 3. Flexible drawing hook - Use "draw" as a fallback since onDraw might be inherited
            try {
                XposedHelpers.findAndHookMethod(batteryMeterViewClass, "draw", Canvas.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        applyHoloTint(param.thisObject);
                    }
                });
            } catch (Throwable ignored) {}

            // 4. Force color logic - intercept the methods that decide the color
            String[] fillMethods = {"getColorForLevel", "getColorForStatus", "getFillColor"};
            for (String method : fillMethods) {
                try {
                    XposedBridge.hookAllMethods(batteryMeterViewClass, method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isHolo = TkPrefs.isHoloTheme();
                            param.setResult(isHolo ? 0xFF33b5e5 : 0xFFFFFFFF);
                        }
                    });
                } catch (Throwable ignored) {}
            }

            try {
                XposedBridge.hookAllMethods(batteryMeterViewClass, "getFrameColor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(0x44FFFFFF); // Semi-transparent frame
                    }
                });
            } catch (Throwable ignored) {}

        } catch (Throwable t) {
            Log.e(TAG, "Failed to hook BatteryMeterView", t);
        }
    }

    public static void refreshAll() {
        for (Object battery : sBatteries) {
            if (battery instanceof View) {
                View v = (View) battery;
                if (v.isAttachedToWindow()) {
                    applyHoloTint(battery);
                    v.invalidate();
                }
            }
        }
    }

    private static void applyHoloTint(Object batteryView) {
        boolean isHolo = TkPrefs.isHoloTheme();
        int color = isHolo ? 0xFF33b5e5 : 0xFFFFFFFF; // Holo Blue or White
        int frameColor = 0x44FFFFFF; // Always semi-transparent white/grey for frame

        // 1. Tint the Fill (The actual battery level)
        String[] fillFields = {"mBatteryColor", "mChargeColor", "mFillColor", "mLevelColor"};
        for (String field : fillFields) {
            try { XposedHelpers.setIntField(batteryView, field, color); } catch (Throwable ignored) {}
        }

        String[] fillPaints = {"mBatteryPaint", "mFillPaint", "mLevelPaint", "mContentPaint"};
        for (String field : fillPaints) {
            try {
                Paint p = (Paint) XposedHelpers.getObjectField(batteryView, field);
                if (p != null) p.setColor(color);
            } catch (Throwable ignored) {}
        }

        // 2. Tint the Frame (The background/empty part)
        String[] frameFields = {"mFrameColor", "mColor"};
        for (String field : frameFields) {
            try { XposedHelpers.setIntField(batteryView, field, frameColor); } catch (Throwable ignored) {}
        }

        try {
            Paint p = (Paint) XposedHelpers.getObjectField(batteryView, "mFramePaint");
            if (p != null) p.setColor(frameColor);
        } catch (Throwable ignored) {}

        // 3. Misc (Bolt and Text)
        // Holo: White bolt for contrast against blue fill
        // Stock: Black bolt for contrast against white fill
        int boltColor = isHolo ? 0xFFFFFFFF : 0xFF000000;
        try { XposedHelpers.setIntField(batteryView, "mBoltColor", boltColor); } catch (Throwable ignored) {}
        tintPaint(batteryView, "mBoltPaint", boltColor);

        tintPaint(batteryView, "mTextPaint", color);
        
        // Background paint for circle styles
        try {
            Paint p = (Paint) XposedHelpers.getObjectField(batteryView, "mCircleBackPaint");
            if (p != null) p.setColor(color & 0x40FFFFFF);
        } catch (Throwable ignored) {}
    }

    private static void tintPaint(Object obj, String fieldName, int color) {
        try {
            Paint paint = (Paint) XposedHelpers.getObjectField(obj, fieldName);
            if (paint != null) {
                paint.setColor(color);
            }
        } catch (Throwable ignored) {
        }
    }
}
