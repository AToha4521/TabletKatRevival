package org.exalm.tabletkat;

public class StatusBarManager {
    public static final int DISABLE_EXPAND = 0x00010000;
    public static final int DISABLE_NOTIFICATION_ICONS = 0x00020000;
    public static final int DISABLE_NOTIFICATION_ALERTS = 0x00040000;
    public static final int DISABLE_NOTIFICATION_TICKER = 0x00080000;
    public static final int DISABLE_SYSTEM_INFO = 0x00100000;
    public static final int DISABLE_HOME = 0x00200000;
    public static final int DISABLE_BACK = 0x00400000;
    public static final int DISABLE_CLOCK = 0x00800000;
    public static final int DISABLE_RECENT = 0x01000000;
    public static final int DISABLE_SEARCH = 0x02000000;

    public static final int NAVIGATION_HINT_BACK_ALT = 1 << 0;

    public static final int WINDOW_STATE_SHOWING = 0;
    public static final int WINDOW_STATE_HIDING = 1;
    public static final int WINDOW_STATE_HIDDEN = 2;

    public static final int WINDOW_STATUS_BAR = 1;
    public static final int WINDOW_NAVIGATION_BAR = 2;
}
