package org.exalm.tabletkat;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import org.exalm.tabletkat.navbuttons.NavButtonsMod;
import org.exalm.tabletkat.recent.TabletRecentsMod;
import org.exalm.tabletkat.settings.MultiPaneSettingsMod;
import org.exalm.tabletkat.statusbar.policy.SignalClusterMod;
import org.exalm.tabletkat.statusbar.tablet.TabletStatusBarMod;
import org.exalm.tabletkat.launcher.LauncherMod;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class TabletKatModule
        implements IXposedHookLoadPackage,
                   IXposedHookZygoteInit,
                   IXposedHookInitPackageResources {

    public static final String TAG = "TabletKat";

    public static final String CLS_BASE_SBAR   = "com.android.systemui.statusbar.BaseStatusBar";
    public static final String CLS_PHONE_SBAR  = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    public static final String CLS_TV_SBAR     = "com.android.systemui.statusbar.tv.TvStatusBar";
    public static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    public static final String SETTINGS_PACKAGE = "com.android.settings";

    public static String MODULE_PATH;

    private static Object mSystemUI;
    private static BroadcastReceiver mReceiver;
    private static ClassLoader mClassLoader;
    private static Boolean mHasNavigationBar;
    private static XSharedPreferences pref;

    public static Class mActivityManagerNativeClass;
    public static Class mBaseStatusBarClass;
    public static Class mBaseStatusBarHClass;
    public static Class mBatteryControllerClass;
    public static Class mBatteryMeterViewClass;
    public static Class mBluetoothControllerClass;
    public static Class mBrightnessControllerClass;
    public static Class mClockClass;
    public static Class mComAndroidInternalRDrawableClass;
    public static Class mComAndroidInternalRStringClass;
    public static Class mComAndroidInternalRStyleClass;
    public static Class mDateViewClass;
    public static Class mDelegateViewHelperClass;
    public static Class mExpandHelperClass;
    public static Class mExpandHelperCallbackClass;
    public static Class mGlowPadViewClass;
    public static Class mKeyButtonViewClass;
    public static Class mLocationControllerClass;
    public static Class mNetworkControllerClass;
    public static Class mNotificationDataEntryClass;
    public static Class mNotificationRowLayoutClass;
    public static Class mPhoneStatusBarClass;
    public static Class mPhoneStatusBarPolicyClass;
    public static Class mQuickSettingsClass;
    public static Class mRecentTasksLoaderClass;
    public static Class mStatusBarIconClass;
    public static Class mStatusBarIconViewClass;
    public static Class mStatusBarManagerClass;
    public static Class mSystemUIClass;
    public static Class mToggleSliderClass;
    public static Class mTvStatusBarClass;
    public static Class mWindowManagerGlobalClass;
    public static Class mWindowManagerLayoutParamsClass;

    public static TabletKatModule self;
    private static TabletStatusBarMod statusBarMod;
    public static TabletRecentsMod recentsMod;
    private static LauncherMod launcherMod;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        self = this;
        MODULE_PATH = startupParam.modulePath;
        pref = new XSharedPreferences("org.exalm.tabletkat", "org.exalm.tabletkat_preferences");
        TkPrefs.init();

        Class c = SafeHook.findClass("com.android.internal.policy.impl.PhoneWindowManager", null);
        if (c == null) return;

        final XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, null);

        XposedHelpers.findAndHookMethod(c, "getNonDecorDisplayWidth", int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                setNavigationBarProperties(methodHookParam, shouldUseTabletUI(null));
                if (shouldUseTabletUI(null)) {
                    return (Integer) methodHookParam.args[0];
                }
                return XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
            }
        });
        XposedHelpers.findAndHookMethod(c, "getNonDecorDisplayHeight", int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                setNavigationBarProperties(methodHookParam, shouldUseTabletUI(null));
                if (shouldUseTabletUI(null)){
                    int fullHeight = (Integer) methodHookParam.args[1];
                    int rotation = XposedHelpers.getIntField(methodHookParam.thisObject, "mSeascapeRotation");
                    int[] mNavigationBarHeightForRotation = (int[]) XposedHelpers.getObjectField(methodHookParam.thisObject, "mNavigationBarHeightForRotation");
                    return fullHeight - mNavigationBarHeightForRotation[rotation];
                }
                return XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
            }
        });
        XposedHelpers.findAndHookMethod(c, "getConfigDisplayHeight", int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int fullWidth = (Integer) methodHookParam.args[0];
                int fullHeight = (Integer) methodHookParam.args[1];
                int rotation = (Integer) methodHookParam.args[2];
                int base = (Integer) XposedHelpers.callMethod(methodHookParam.thisObject, "getNonDecorDisplayHeight", fullWidth, fullHeight, rotation);
                if (shouldUseTabletUI(null)) {
                    return base;
                }
                return base + res2.getDimensionPixelSize(R.dimen.status_bar_height);
            }
        });
        XposedHelpers.findAndHookMethod(c, "setInitialDisplaySize", Display.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                Display display = (Display) param.args[0];
                if (mContext == null || (Integer)XposedHelpers.callMethod(display, "getDisplayId") != 0) { // Display.DEFAULT_DISPLAY
                    return;
                }
                XposedHelpers.setIntField(param.thisObject, "mStatusBarHeight", res2.getDimensionPixelSize(R.dimen.status_bar_height));
            }
        });

        try {
            XResources.setSystemWideReplacement("android", "layout", "status_bar_latest_event_ticker", res2.fwd(R.layout.status_bar_latest_event_ticker));
            XResources.setSystemWideReplacement("android", "layout", "status_bar_latest_event_ticker_large_icon", res2.fwd(R.layout.status_bar_latest_event_ticker_large_icon));
        } catch (Throwable e){}

        XResources.setSystemWideReplacement("android", "dimen", "status_bar_height", new CustomDimenReplacement() {
            @Override
            protected float getValue() {
                if (shouldUseTabletUI(null)) {
                    return 0;
                }
                return res2.getDimension(R.dimen.status_bar_height);
            }
        });
    }

    private void setNavigationBarProperties(XC_MethodHook.MethodHookParam param, boolean b) {
        int width = (Integer) param.args[0];
        int height = (Integer) param.args[1];
        int shortSize = Math.min(width, height);
        Display d = (Display) XposedHelpers.getObjectField(param.thisObject, "mDisplay");
        if (d == null){
            return;
        }
        Object info = XposedHelpers.newInstance(SafeHook.findClass("android.view.DisplayInfo", mClassLoader));
        XposedHelpers.callMethod(d, "getDisplayInfo", info);
        int density = XposedHelpers.getIntField(info, "logicalDensityDpi");

        int shortSizeDp = shortSize * 160 / density;

        XposedHelpers.setBooleanField(param.thisObject, "mNavigationBarCanMove", shortSizeDp < 600 && !b);

        if (mHasNavigationBar == null) {
            mHasNavigationBar = XposedHelpers.getBooleanField(param.thisObject, "mHasNavigationBar");
        }
        XposedHelpers.setBooleanField(param.thisObject, "mHasNavigationBar", b || mHasNavigationBar);
    }

    private boolean shouldUseTabletUI(Configuration conf) {
        return TkPrefs.isTabletUiEnabled();
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam res)
            throws Throwable {
        if (res.packageName.equals(SETTINGS_PACKAGE)) {
            MultiPaneSettingsMod.hookBreadcrumbs(res.res);
        }
        if (LauncherMod.isSupported(res.packageName)) {
            if (launcherMod == null) launcherMod = new LauncherMod();
            XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, res.res);
            SystemR.init(res.res, res2);
            TkR.init(res.res, res2);
            launcherMod.initResources(res.res, res2);
            return;
        }
        if (!res.packageName.equals(SYSTEMUI_PACKAGE)) return;
        try {
            XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, res.res);
            SystemR.init(res.res, res2);
            TkR.init(res.res, res2);

            if (statusBarMod == null) statusBarMod = new TabletStatusBarMod();
            statusBarMod.initResources(res.res, res2);

            if (recentsMod == null) recentsMod = new TabletRecentsMod();
            recentsMod.initResources(res.res, res2);

        } catch (Throwable t) {
            Log.e(TAG, "handleInitPackageResources failed", t);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        mClassLoader = lp.classLoader;
        TkPrefs.init();

        if (lp.packageName.equals(SYSTEMUI_PACKAGE)) {
            Log.i(TAG, "Loading package: " + lp.packageName);
            handleSystemUI(lp);
        } else if (lp.packageName.equals(SETTINGS_PACKAGE)) {
            safeRun("SettingsMain", () -> new MultiPaneSettingsMod().addHooks(lp.classLoader));
        } else if (LauncherMod.isSupported(lp.packageName)) {
            if (launcherMod == null) launcherMod = new LauncherMod();
            launcherMod.addHooks(lp.packageName, lp.classLoader);
        }
    }

    private void handleSystemUI(XC_LoadPackage.LoadPackageParam lp) {
        initClasses(lp.classLoader);

        if (statusBarMod == null) statusBarMod = new TabletStatusBarMod();
        if (recentsMod == null) recentsMod = new TabletRecentsMod();

        statusBarMod.addHooks(lp.classLoader);
        recentsMod.addHooks(lp.classLoader);
        NavButtonsMod.addHooks(lp);
        SignalClusterMod.initHooks(lp.classLoader);
        org.exalm.tabletkat.statusbar.policy.ClockMod.initHooks(lp.classLoader);
        org.exalm.tabletkat.statusbar.policy.BatteryMod.initHooks(lp.classLoader);

        try {
            Class<?> systemBarsClass = SafeHook.findClass(
                    "com.android.systemui.statusbar.SystemBars", lp.classLoader);
            if (systemBarsClass == null) {
                Log.e(TAG, "SystemBars class not found");
                return;
            }
            
            XposedHelpers.findAndHookMethod(systemBarsClass, "createStatusBarFromConfig", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mSystemUI = param.thisObject;
                    Context ctx = (Context) XposedHelpers.getObjectField(mSystemUI, "mContext");
                    refreshReceiver(ctx);
                    
                    if (!TkPrefs.isTabletUiEnabled()) {
                        Log.i(TAG, "createStatusBarFromConfig: Tablet UI disabled, continuing with stock");
                        return;
                    }

                    String clsName = CLS_TV_SBAR;
                    if (!SafeHook.classExists(clsName, lp.classLoader)) {
                        Log.w(TAG, "createStatusBarFromConfig: " + clsName + " not found! Cannot force Tablet UI.");
                        return;
                    }

                    Log.i(TAG, "createStatusBarFromConfig: forcing " + clsName + " for Tablet UI");
                    
                    try {
                        Object sbar = createStatusBar(clsName, ctx, 
                                XposedHelpers.getObjectField(mSystemUI, "mComponents"));
                        XposedHelpers.setObjectField(mSystemUI, "mStatusBar", sbar);
                        param.setResult(null); 
                    } catch (Throwable t) {
                        Log.e(TAG, "Failed to create status bar: " + clsName, t);
                        // Do NOT set result to null here, let it try stock as fallback
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "SystemBars hook failed", t);
        }
    }

    private void initClasses(ClassLoader cl) {
        mActivityManagerNativeClass = SafeHook.findClass("android.app.ActivityManagerNative", cl);
        mBaseStatusBarClass = SafeHook.findClass("com.android.systemui.statusbar.BaseStatusBar", cl);
        mBaseStatusBarHClass = SafeHook.findClass("com.android.systemui.statusbar.BaseStatusBar$H", cl);
        mBatteryControllerClass = SafeHook.findClass("com.android.systemui.statusbar.policy.BatteryController", cl);
        mBatteryMeterViewClass = SafeHook.findClass("com.android.systemui.BatteryMeterView", cl);
        mBluetoothControllerClass = SafeHook.findClass("com.android.systemui.statusbar.policy.BluetoothController", cl);
        mBrightnessControllerClass = SafeHook.findClass("com.android.systemui.settings.BrightnessController", cl);
        mClockClass = SafeHook.findClass("com.android.systemui.statusbar.policy.Clock", cl);
        mComAndroidInternalRDrawableClass = SafeHook.findClass("com.android.internal.R$drawable", cl);
        mComAndroidInternalRStringClass = SafeHook.findClass("com.android.internal.R$string", cl);
        mComAndroidInternalRStyleClass = SafeHook.findClass("com.android.internal.R$style", cl);
        mDateViewClass = SafeHook.findClass("com.android.systemui.statusbar.policy.DateView", cl);
        mDelegateViewHelperClass = SafeHook.findClass("com.android.systemui.statusbar.DelegateViewHelper", cl);
        mExpandHelperClass = SafeHook.findClass("com.android.systemui.ExpandHelper", cl);
        mExpandHelperCallbackClass = SafeHook.findClass("com.android.systemui.ExpandHelper$Callback", cl);
        mGlowPadViewClass = SafeHook.findClass("com.android.internal.widget.multiwaveview.GlowPadView", cl);
        mKeyButtonViewClass = SafeHook.findClass("com.android.systemui.statusbar.policy.KeyButtonView", cl);
        mLocationControllerClass = SafeHook.findClass("com.android.systemui.statusbar.policy.LocationController", cl);
        mNetworkControllerClass = SafeHook.findClass("com.android.systemui.statusbar.policy.NetworkController", cl);
        mNotificationDataEntryClass = SafeHook.findClass("com.android.systemui.statusbar.NotificationData$Entry", cl);
        mNotificationRowLayoutClass = SafeHook.findClass("com.android.systemui.statusbar.policy.NotificationRowLayout", cl);
        mPhoneStatusBarClass = SafeHook.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", cl);
        mPhoneStatusBarPolicyClass = SafeHook.findClass("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", cl);
        mRecentTasksLoaderClass = SafeHook.findClass("com.android.systemui.recent.RecentTasksLoader", cl);
        mStatusBarIconClass = SafeHook.findClass("com.android.internal.statusbar.StatusBarIcon", cl);
        mStatusBarIconViewClass = SafeHook.findClass("com.android.systemui.statusbar.StatusBarIconView", cl);
        mStatusBarManagerClass = SafeHook.findClass("android.app.StatusBarManager", cl);
        mSystemUIClass = SafeHook.findClass("com.android.systemui.SystemUI", cl);
        mToggleSliderClass = SafeHook.findClass("com.android.systemui.settings.ToggleSlider", cl);
        mTvStatusBarClass = SafeHook.findClass("com.android.systemui.statusbar.tv.TvStatusBar", cl);
        mWindowManagerGlobalClass = SafeHook.findClass("android.view.WindowManagerGlobal", cl);
        mWindowManagerLayoutParamsClass = SafeHook.findClass("android.view.WindowManager$LayoutParams", cl);
    }

    private Object createStatusBar(String clsName, Context ctx, Object components) throws Exception {
        Class<?> cls = mClassLoader.loadClass(clsName);
        Object sbar = cls.newInstance();
        XposedHelpers.setObjectField(sbar, "mContext", ctx);
        XposedHelpers.setObjectField(sbar, "mComponents", components);
        
        statusBarMod.init(sbar);
        recentsMod.destroy();
        XposedHelpers.callMethod(sbar, "start");
        statusBarMod.onStart();
        recentsMod.setBar(sbar, statusBarMod);
        recentsMod.registerReceiver(ctx);
        recentsMod.createPanel(sbar, statusBarMod);

        Log.i(TAG, "Started status bar: " + clsName);
        return sbar;
    }

    private void refreshReceiver(Context c) {
        if (mReceiver != null) return;
        mReceiver = registerReceiver(c, new OnPreferenceChangedListener() {
            @Override
            public void onPreferenceChanged(String key, boolean value) {
                if (key.equals("enable_tablet_ui")) {
                    refreshSystemUI();
                }
            }
            @Override
            public void onPreferenceChanged(String key, int value) {}
            @Override
            public void init(XSharedPreferences pref) {}
        });
    }

    public static BroadcastReceiver registerReceiver(Context c, final OnPreferenceChangedListener l){
        IntentFilter f = new IntentFilter();
        f.addAction(TkPrefs.ACTION_PREFERENCE_CHANGED);
        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String key = intent.getStringExtra("key");
                if (intent.hasExtra("boolValue")){
                    boolean boolValue = intent.getBooleanExtra("boolValue", false);
                    l.onPreferenceChanged(key, boolValue);
                }
                if (intent.hasExtra("intValue")){
                    int intValue = intent.getIntExtra("intValue", 0);
                    l.onPreferenceChanged(key, intValue);
                }
                if (intent.hasExtra("stringValue")){
                    String stringValue = intent.getStringExtra("stringValue");
                    l.onPreferenceChanged(key, stringValue);
                }
            }
        };
        l.init(TkPrefs.getPrefs());
        c.registerReceiver(r, f);
        return r;
    }

    public static void refreshSystemUI() {
        if (mSystemUI == null) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                XposedHelpers.callMethod(mSystemUI, "createStatusBarFromConfig");
            } catch (Throwable t) {
                Log.e(TAG, "refreshSystemUI failed", t);
            }
        }, 500); 
    }

    public static Object invokeOriginalMethod(XC_MethodHook.MethodHookParam param) throws IllegalAccessException, InvocationTargetException{
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
    }

    public static boolean shouldUseTabletRecents(){
        return isModEnabled("recents");
    }

    public static boolean shouldForceBreadcrumbs() {
        return isModEnabled("settings") && TkPrefs.getBool("force_breadcrumbs", true);
    }

    public static boolean shouldUseLightTheme() {
        return isModEnabled("settings") && TkPrefs.getBool("settings_light_theme", false);
    }

    private static boolean isModEnabled(String id) {
        return TkPrefs.getBool("enable_mod_" + id, true);
    }

    public static void safeRun(String label, Runnable block) {
        try {
            block.run();
        } catch (Throwable t) {
            Log.e(TAG, label + " failed (non-fatal):", t);
        }
    }
}
