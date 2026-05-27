package org.exalm.tabletkat;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * SafeHook
 * --------
 * Drop-in wrappers around XposedHelpers that silently catch NoSuchMethodError /
 * ClassNotFoundError and log them instead of crashing SystemUI.
 *
 * ROOT CAUSE of most TabletKat crashes:
 *   XposedHelpers.findAndHookMethod() throws NoSuchMethodError when the target
 *   method doesn't exist on the current ROM (e.g. TvStatusBar, CM11, AOSPA).
 *   Because the exception was never caught it propagated up and killed the entire
 *   SystemUI process (bootloop on some devices).
 *
 * Usage — replace ALL XposedHelpers.findAndHookMethod calls with SafeHook.method().
 */
public final class SafeHook {

    private static final String TAG = "TabletKat/SafeHook";

    private SafeHook() {}

    // ── Method hooks ──────────────────────────────────────────────────────────

    public static XC_MethodHook.Unhook method(String className, ClassLoader cl,
                                               String methodName, Object... rest) {
        try {
            return XposedHelpers.findAndHookMethod(className, cl, methodName, rest);
        } catch (XposedHelpers.ClassNotFoundError e) {
            Log.w(TAG, "Class missing, skip hook: " + className + "#" + methodName);
        } catch (NoSuchMethodError e) {
            Log.w(TAG, "Method missing, skip hook: " + className + "#" + methodName
                    + " [" + e.getMessage() + "]");
        } catch (Throwable t) {
            Log.e(TAG, "Unexpected error hooking " + className + "#" + methodName, t);
        }
        return null;
    }

    public static XC_MethodHook.Unhook method(Class<?> clazz, String methodName,
                                               Object... rest) {
        try {
            return XposedHelpers.findAndHookMethod(clazz, methodName, rest);
        } catch (NoSuchMethodError e) {
            Log.w(TAG, "Method missing, skip hook: "
                    + clazz.getName() + "#" + methodName + " [" + e.getMessage() + "]");
        } catch (Throwable t) {
            Log.e(TAG, "Error hooking " + clazz.getName() + "#" + methodName, t);
        }
        return null;
    }

    // ── Constructor hooks ─────────────────────────────────────────────────────

    public static XC_MethodHook.Unhook constructor(Class<?> clazz, Object... rest) {
        try {
            return XposedHelpers.findAndHookConstructor(clazz, rest);
        } catch (NoSuchMethodError e) {
            Log.w(TAG, "Constructor missing, skip hook: " + clazz.getName());
        } catch (Throwable t) {
            Log.e(TAG, "Error hooking constructor " + clazz.getName(), t);
        }
        return null;
    }

    // ── Class / field / method reflection helpers ─────────────────────────────

    /** Returns null (not throw) if class not found. */
    public static Class<?> findClass(String name, ClassLoader cl) {
        try {
            return XposedHelpers.findClass(name, cl);
        } catch (XposedHelpers.ClassNotFoundError e) {
            return null;
        }
    }

    public static boolean classExists(String name, ClassLoader cl) {
        return findClass(name, cl) != null;
    }

    /** Get object field — returns null on any error. */
    public static Object getField(Object obj, String field) {
        if (obj == null) return null;
        try {
            return XposedHelpers.getObjectField(obj, field);
        } catch (Throwable t) {
            Log.w(TAG, "getField " + field + ": " + t.getMessage());
            return null;
        }
    }

    public static int getIntField(Object obj, String field, int def) {
        if (obj == null) return def;
        try {
            return XposedHelpers.getIntField(obj, field);
        } catch (Throwable t) {
            return def;
        }
    }

    /** Set object field — silently skips on error. */
    public static void setField(Object obj, String field, Object value) {
        if (obj == null) return;
        try {
            XposedHelpers.setObjectField(obj, field, value);
        } catch (Throwable t) {
            Log.w(TAG, "setField " + field + ": " + t.getMessage());
        }
    }

    public static void setIntField(Object obj, String field, int value) {
        if (obj == null) return;
        try {
            XposedHelpers.setIntField(obj, field, value);
        } catch (Throwable t) {
            Log.w(TAG, "setIntField " + field + ": " + t.getMessage());
        }
    }

    /** Call instance method — returns null on any error. */
    public static Object callMethod(Object obj, String method, Object... args) {
        if (obj == null) return null;
        try {
            return XposedHelpers.callMethod(obj, method, args);
        } catch (Throwable t) {
            Log.w(TAG, "callMethod " + method + ": " + t.getMessage());
            return null;
        }
    }

    /** Call static method — returns null on any error. */
    public static Object callStaticMethod(Class<?> clazz, String method, Object... args) {
        if (clazz == null) return null;
        try {
            return XposedHelpers.callStaticMethod(clazz, method, args);
        } catch (Throwable t) {
            Log.w(TAG, "callStaticMethod " + method + ": " + t.getMessage());
            return null;
        }
    }

    // ── ROM detection ─────────────────────────────────────────────────────────

    public static boolean isTouchWiz(ClassLoader cl) {
        return classExists("com.android.systemui.statusbar.phone.SamsungNavigationBarView", cl)
                || classExists("com.samsung.android.app.SamsungApplication", cl);
    }

    public static boolean isCyanogenMod(ClassLoader cl) {
        return classExists("com.android.systemui.statusbar.phone.NavigationBarViewTaskSwitchHelper", cl);
    }

    public static boolean isParanoidAndroid(ClassLoader cl) {
        return classExists("co.aospa.systemui.ParanoidSystemUI", cl)
                || classExists("com.android.systemui.statusbar.pa.HybridController", cl);
    }

    public static boolean hasTvStatusBar(ClassLoader cl) {
        return classExists("com.android.systemui.statusbar.tv.TvStatusBar", cl);
    }
}
