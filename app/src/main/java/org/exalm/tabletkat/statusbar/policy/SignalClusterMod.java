package org.exalm.tabletkat.statusbar.policy;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.exalm.tabletkat.TkPrefs;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * SignalClusterMod
 * ================
 * Tints the stock SignalClusterView icons to match Holo Blue.
 * No layout or icon replacements.
 */
public class SignalClusterMod {
    private static final String TAG = "TabletKat/SignalCluster";

    private static java.util.Set<View> sClusters = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static void initHooks(ClassLoader cl) {
        try {
            final Class<?> signalClusterClass = XposedHelpers.findClass("com.android.systemui.statusbar.SignalClusterView", cl);

            XposedHelpers.findAndHookMethod(signalClusterClass, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    sClusters.add((View) param.thisObject);
                    applyTheme((View) param.thisObject);
                }
            });

            XposedHelpers.findAndHookMethod(signalClusterClass, "apply", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    applyTheme((View) param.thisObject);
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Failed to hook SignalClusterView", t);
        }
    }

    public static void refreshAll() {
        for (View cluster : sClusters) {
            if (cluster != null && cluster.isAttachedToWindow()) {
                applyTheme(cluster);
            }
        }
    }

    private static void applyTheme(View cluster) {
        try {
            boolean isHolo = TkPrefs.isHoloTheme();
            int tint = isHolo ? 0xFF33b5e5 : 0; // Holo Blue or Clear

            if (cluster instanceof ViewGroup) {
                tintRecursive((ViewGroup) cluster, tint);
            }
        } catch (Throwable t) {
            // Never let this hook crash SystemUI
        }
    }

    private static void tintRecursive(ViewGroup parent, int color) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                if (color != 0) {
                    iv.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP);
                    iv.setImageAlpha(255);
                } else {
                    iv.setColorFilter(null);
                }
            } else if (child instanceof ViewGroup) {
                tintRecursive((ViewGroup) child, color);
            }
        }
    }
}
