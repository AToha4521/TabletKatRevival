package org.exalm.tabletkat;

import android.util.Log;

import de.robv.android.xposed.XSharedPreferences;

/**
 * TkPrefs — centralised, null-safe preference access for all TabletKat modules.
 *
 * Uses XSharedPreferences which is readable from inside SystemUI's process
 * without any IPC.  Call TkPrefs.init() once at the top of handleLoadPackage().
 */
public final class TkPrefs {

    private static final String TAG  = "TabletKat/Prefs";
    public  static final String PKG  = "org.exalm.tabletkat";

    // ── Keys ──────────────────────────────────────────────────────────────────

    // Theme style
    public static final String KEY_THEME_STYLE          = "theme_style";          // "ics"|"jb"|"kitkat"
    // Status bar
    public static final String KEY_HOLO_TINT            = "holo_tint";
    public static final String KEY_STATUS_ICON_STYLE    = "status_icon_style";    // "holo"|"flat"|"stock"
    public static final String KEY_CLOCK_FONT           = "ics_clock_font";
    public static final String KEY_CLOCK_SECONDS        = "clock_seconds";
    public static final String KEY_BATTERY_PERCENT      = "battery_percentage";
    public static final String KEY_BATTERY_STYLE        = "battery_style";        // "bar"|"circle"|"text"
    // Navigation bar extras
    public static final String KEY_NAVBAR_VOLUME        = "navbutton_volume";
    public static final String KEY_NAVBAR_SCREENSHOT    = "navbutton_screenshot";
    public static final String KEY_NAVBAR_POWER         = "navbutton_power";
    public static final String KEY_NAVBAR_EXPANDED      = "navbutton_expanded";   // show/hide expanded menu
    public static final String KEY_NAVBAR_BUTTON_ORDER  = "navbutton_order";      // comma-separated key list
    // Recents
    public static final String KEY_TABLET_RECENTS       = "tablet_recents";
    public static final String KEY_OVERLAY_RECENTS      = "overlay_recents";
    // Misc
    public static final String KEY_DUAL_PANE_SETTINGS   = "dual_pane_settings";
    public static final String KEY_FORCE_TABLET_UI      = "enable_tablet_ui";
    public static final String KEY_LAUNCHER_MOD         = "enable_mod_launcher";
    public static final String KEY_NOTIF_PANEL_COMPACT  = "notif_panel_compact";  // right-anchored panel

    // Theme style constants
    public static final String THEME_HOLO   = "holo";
    public static final String THEME_KITKAT = "kitkat";

    public static final String ACTION_PREFERENCE_CHANGED = "org.exalm.tabletkat.PREFERENCE_CHANGED";

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static XSharedPreferences sPrefs;

    public static void init() {
        try {
            sPrefs = new XSharedPreferences(PKG, PKG + "_preferences");
            // sPrefs.makeWorldReadable();
            Log.i(TAG, "Preferences loaded. Enable Tablet UI = " + isTabletUiEnabled());
        } catch (Throwable t) {
            Log.w(TAG, "Could not load prefs — using defaults: " + t.getMessage());
            sPrefs = null;
        }
    }

    public static boolean isTabletUiEnabled() {
        return getBool(KEY_FORCE_TABLET_UI, false);
    }

    public static boolean isLauncherModEnabled() {
        return getBool(KEY_LAUNCHER_MOD, true);
    }

    private static void reload() {
        if (sPrefs != null) {
            try { sPrefs.reload(); } catch (Throwable ignored) {}
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static String getThemeStyle() {
        String theme = getString(KEY_THEME_STYLE, THEME_HOLO);
        // Map legacy values to Holo
        if ("ics".equals(theme) || "jb".equals(theme)) return THEME_HOLO;
        return theme;
    }

    public static boolean isHoloTheme()   { return THEME_HOLO.equals(getThemeStyle()); }
    public static boolean isKitKatTheme() { return THEME_KITKAT.equals(getThemeStyle()); }

    public static boolean isHoloTintEnabled()       { return getBool(KEY_HOLO_TINT, true); }
    public static boolean isIcsClockFontEnabled()   { return getBool(KEY_CLOCK_FONT, true); }
    public static boolean isClockSecondsEnabled()   { return getBool(KEY_CLOCK_SECONDS, false); }
    public static boolean isBatteryPercentEnabled() { return getBool(KEY_BATTERY_PERCENT, false); }
    public static String  getBatteryStyle()         { return getString(KEY_BATTERY_STYLE, "bar"); }
    public static String  getStatusIconStyle()      { return "stock"; }

    public static boolean isNavVolumeEnabled()      { return getBool(KEY_NAVBAR_VOLUME, false); }
    public static boolean isNavScreenshotEnabled()  { return getBool(KEY_NAVBAR_SCREENSHOT, false); }
    public static boolean isNavPowerEnabled()       { return getBool(KEY_NAVBAR_POWER, false); }
    public static boolean isNavExpandedEnabled()    { return getBool(KEY_NAVBAR_EXPANDED, false); }
    public static String  getNavButtonOrder()       { return getString(KEY_NAVBAR_BUTTON_ORDER, ""); }

    public static boolean isTabletRecentsEnabled()  { return getBool(KEY_TABLET_RECENTS, true); }
    public static boolean isOverlayRecentsEnabled() { return getBool(KEY_OVERLAY_RECENTS, false); }
    public static boolean isDualPaneSettingsEnabled(){ return getBool(KEY_DUAL_PANE_SETTINGS, true); }
    public static boolean isCompactNotifPanelEnabled(){ return getBool(KEY_NOTIF_PANEL_COMPACT, true); }

    // ── Low-level helpers ─────────────────────────────────────────────────────

    public static XSharedPreferences getPrefs() {
        return sPrefs;
    }

    public static boolean getBool(String key, boolean def) {
        if (sPrefs == null) return def;
        reload();
        try { return sPrefs.getBoolean(key, def); }
        catch (Throwable t) { return def; }
    }

    private static String getString(String key, String def) {
        if (sPrefs == null) return def;
        reload();
        try { return sPrefs.getString(key, def); }
        catch (Throwable t) { return def; }
    }
}
