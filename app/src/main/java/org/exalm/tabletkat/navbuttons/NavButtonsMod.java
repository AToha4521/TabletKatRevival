package org.exalm.tabletkat.navbuttons;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import org.exalm.tabletkat.SafeHook;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkPrefs;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * NavButtonsMod
 * ==============
 * Adds extra quick-action buttons to the navigation bar (right side, tablet-bar style):
 *
 *   [VOL-] [VOL+]  [SCREENSHOT]  [POWER/SLEEP]  [MENU]
 */
public class NavButtonsMod {

    private static final String TAG = TabletKatModule.TAG + "/NavButtons";

    private static final String CLS_NAV_BAR_VIEW =
            "com.android.systemui.statusbar.phone.NavigationBarView";

    private static boolean mSystemMenuVisible = false;

    public static void addHooks(XC_LoadPackage.LoadPackageParam lp) {
        SafeHook.method(CLS_NAV_BAR_VIEW, lp.classLoader,
                "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        TabletKatModule.safeRun("injectNavButtons",
                                () -> injectButtons((View) p.thisObject));
                    }
                });

        SafeHook.method(CLS_NAV_BAR_VIEW, lp.classLoader,
                "setDisabledFlags", int.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        TabletKatModule.safeRun("updateNavButtons",
                                () -> updateButtonVisibility((View) p.thisObject));
                    }
                });

        SafeHook.method(CLS_NAV_BAR_VIEW, lp.classLoader,
                "setMenuVisibility", boolean.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        mSystemMenuVisible = (boolean) p.args[0];
                        TabletKatModule.safeRun("updateMenuVisibility",
                                () -> updateButtonVisibility((View) p.thisObject));
                    }
                });
    }

    public static void injectButtons(View navBar) {
        if (navBar == null) return;
        Context ctx = navBar.getContext();
        if (ctx == null) return;

        ViewGroup navigationArea = findNavigationArea(navBar);
        if (navigationArea == null) return;

        // Check if already injected
        if (navBar.findViewWithTag("tk_vol_down") != null) return;

        // Add buttons directly to the standard navigation layout
        navigationArea.addView(makeVolumeButton(ctx, false));
        navigationArea.addView(makeVolumeButton(ctx, true));
        navigationArea.addView(makeScreenshotButton(ctx));
        navigationArea.addView(makePowerButton(ctx));
        navigationArea.addView(makeLegacyMenuButton(ctx));

        Log.i(TAG, "Extra buttons injected into navigation area");
        updateButtonVisibility(navBar);
    }

    public static void updateButtonVisibility(View root) {
        if (root == null) return;
        
        // Auto-inject if buttons are missing
        if (root.findViewWithTag("tk_vol_down") == null) {
            injectButtons(root);
        }

        boolean vol = TkPrefs.isNavVolumeEnabled();
        toggleTag(root, "tk_vol_down",  vol);
        toggleTag(root, "tk_vol_up",    vol);
        toggleTag(root, "tk_screenshot",TkPrefs.isNavScreenshotEnabled());
        toggleTag(root, "tk_power",     TkPrefs.isNavPowerEnabled());
        
        // The Menu button only shows when the current app has a menu available
        toggleTag(root, "tk_menu",      mSystemMenuVisible);
    }

    private static void toggleTag(View parent, String tag, boolean show) {
        View child = parent.findViewWithTag(tag);
        if (child != null) {
            child.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private static View makeVolumeButton(Context ctx, boolean up) {
        View btn = makeNavButton(ctx, up ? KeyEvent.KEYCODE_VOLUME_UP : KeyEvent.KEYCODE_VOLUME_DOWN);
        btn.setTag(up ? "tk_vol_up" : "tk_vol_down");
        btn.setContentDescription(up ? "Volume Up" : "Volume Down");

        int resId = up ? TkR.drawable.ic_sysbar_volume_up : TkR.drawable.ic_sysbar_volume_down;
        if (resId > 0) ((android.widget.ImageView)btn).setImageResource(resId);
        
        btn.setOnClickListener(v -> {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC, 
                    up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 
                    AudioManager.FLAG_SHOW_UI);
        });
        return btn;
    }

    private static View makeScreenshotButton(Context ctx) {
        View btn = makeNavButton(ctx, 0);
        btn.setTag("tk_screenshot");
        btn.setContentDescription("Screenshot");
        
        int resId = TkR.drawable.ic_sysbar_screenshot;
        if (resId > 0) ((android.widget.ImageView)btn).setImageResource(resId);

        btn.setOnClickListener(v -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                takeScreenshot(ctx);
            }, 500);
        });
        return btn;
    }

    private static void takeScreenshot(Context ctx) {
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Messenger messenger = new Messenger(service);
                Message msg = Message.obtain(null, 1);
                final ServiceConnection myConn = this;
                msg.replyTo = new Messenger(new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        ctx.unbindService(myConn);
                    }
                });
                try { messenger.send(msg); } catch (RemoteException e) {}
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.screenshot.TakeScreenshotService"));
        if (!ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Failed to bind to TakeScreenshotService");
        }
    }

    private static View makePowerButton(Context ctx) {
        View btn = makeNavButton(ctx, 0);
        btn.setTag("tk_power");
        btn.setContentDescription("Sleep / Power");
        
        int resId = TkR.drawable.ic_sysbar_power;
        if (resId == 0) resId = ctx.getResources().getIdentifier("ic_sysbar_power", "drawable", "com.android.systemui");
        if (resId == 0) resId = ctx.getResources().getIdentifier("ic_lock_power_off", "drawable", "android");
        if (resId > 0) ((android.widget.ImageView)btn).setImageResource(resId);

        btn.setOnClickListener(v -> {
            android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                try { XposedHelpers.callMethod(pm, "goToSleep", android.os.SystemClock.uptimeMillis()); }
                catch (Throwable t) { Log.e(TAG, "Sleep failed", t); }
            }
        });

        btn.setOnLongClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            ctx.sendBroadcast(intent);
            try {
                XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.view.WindowManagerGlobal", null), "getWindowManagerService"),
                        "showGlobalActionsDialog");
            } catch (Throwable t) { Log.e(TAG, "Global actions failed", t); }
            return true;
        });
        return btn;
    }

    private static View makeLegacyMenuButton(Context ctx) {
        View btn = makeNavButton(ctx, KeyEvent.KEYCODE_MENU);
        btn.setTag("tk_menu");
        btn.setContentDescription("Menu");
        
        int resId = TkR.drawable.ic_sysbar_menu;
        if (resId == 0) resId = ctx.getResources().getIdentifier("ic_sysbar_menu", "drawable", "com.android.systemui");
        if (resId > 0) ((android.widget.ImageView)btn).setImageResource(resId);

        // Long-press opens TabletKat settings
        btn.setOnLongClickListener(v -> {
            openTkSettings(ctx);
            return true;
        });
        return btn;
    }

    private static void openTkSettings(Context ctx) {
        Intent i = new Intent();
        i.setClassName("org.exalm.tabletkat", "org.exalm.tabletkat.ui.SettingsActivity");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    private static View makeNavButton(Context ctx, int keyCode) {
        View btn = (View) XposedHelpers.newInstance(TabletKatModule.mKeyButtonViewClass, ctx, null);
        
        // Exact 80dp width for perfect alignment with stock buttons
        int width = (int) (80 * ctx.getResources().getDisplayMetrics().density);
        btn.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        
        if (keyCode != 0) XposedHelpers.setIntField(btn, "mCode", keyCode);

        int glowRes = ctx.getResources().getIdentifier("ic_sysbar_highlight", "drawable", "com.android.systemui");
        if (glowRes > 0) {
            Drawable glow = ctx.getResources().getDrawable(glowRes);
            XposedHelpers.setObjectField(btn, "mGlowBG", glow);
            XposedHelpers.setObjectField(btn, "mGlowWidth", glow.getIntrinsicWidth());
            XposedHelpers.setObjectField(btn, "mGlowHeight", glow.getIntrinsicHeight());
        }

        if (btn instanceof android.widget.ImageView) {
            ((android.widget.ImageView) btn).setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            int padding = (int) (4 * ctx.getResources().getDisplayMetrics().density);
            btn.setPadding(0, padding, 0, padding);
        }
        return btn;
    }

    private static ViewGroup findNavigationArea(View navBar) {
        if (!(navBar instanceof ViewGroup)) return null;
        ViewGroup root = (ViewGroup) navBar;
        int navId = root.getResources().getIdentifier("nav_buttons", "id", "com.android.systemui");
        if (navId > 0) {
            View v = root.findViewById(navId);
            if (v instanceof ViewGroup) return (ViewGroup) v;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            Object tag = child.getTag();
            if (tag != null && tag.toString().contains("ends")) return (ViewGroup) child;
        }
        return null;
    }
}
