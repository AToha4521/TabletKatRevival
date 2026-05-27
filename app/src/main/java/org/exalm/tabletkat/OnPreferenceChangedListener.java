package org.exalm.tabletkat;

import de.robv.android.xposed.XSharedPreferences;

public interface OnPreferenceChangedListener {
    default void onPreferenceChanged(String key, boolean value) {}

    default void onPreferenceChanged(String key, int value) {}

    default void onPreferenceChanged(String key, String value) {}

    default void init(XSharedPreferences pref) {}
}
