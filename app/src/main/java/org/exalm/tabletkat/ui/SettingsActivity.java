package org.exalm.tabletkat.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.exalm.tabletkat.TkPrefs;

import java.util.List;

/**
 * SettingsActivity
 * =================
 * TabletKat Revival — main settings screen.
 *
 * Restored Dual-Pane layout for tablets. Uses PreferenceActivity with Headers
 * to provide a stable, standard tablet experience.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!onIsMultiPane()) {
            // Setup world readability for Xposed
            try {
                getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            } catch (Throwable ignored) {}

            // For Phone mode (single pane), load everything into one list
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_general);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_navbar);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_recents);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_launcher);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_about);
            
            // Add manual tools to the bottom of the list
            addMiscItems();
        }
    }

    private void addMiscItems() {
        Preference restart = new Preference(this);
        restart.setTitle("Restart Bar (Hard Kill)");
        restart.setSummary("Kills SystemUI process to apply changes (requires root)");
        restart.setOnPreferenceClickListener(p -> {
            restartSystemUI();
            return true;
        });
        getPreferenceScreen().addPreference(restart);

        Preference reset = new Preference(this);
        reset.setTitle("EMERGENCY RESET");
        reset.setSummary("Wipe all settings and soft reboot");
        reset.setOnPreferenceClickListener(p -> {
            emergencyReset();
            return true;
        });
        getPreferenceScreen().addPreference(reset);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(org.exalm.tabletkat.R.xml.pref_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return fragmentName.startsWith("org.exalm.tabletkat.ui.SettingsActivity");
    }

    // ── Fragments ─────────────────────────────────────────────────────────────

    public static abstract class BaseFragment extends PreferenceFragment 
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            updateSummaries();
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updateSummaries();
            fixPermissions();

            Object value = prefs.getAll().get(key);
            Intent i = new Intent(TkPrefs.ACTION_PREFERENCE_CHANGED);
            i.putExtra("key", key);
            if (value instanceof Boolean) i.putExtra("boolValue", (Boolean) value);
            else if (value instanceof Integer) i.putExtra("intValue", (Integer) value);
            else i.putExtra("stringValue", String.valueOf(value));
            getActivity().sendBroadcast(i);
        }

        protected abstract void updateSummaries();

        private void fixPermissions() {
            try {
                java.io.File sharedPrefsDir = new java.io.File(getActivity().getApplicationInfo().dataDir, "shared_prefs");
                java.io.File prefsFile = new java.io.File(sharedPrefsDir, getActivity().getPackageName() + "_preferences.xml");
                if (prefsFile.exists()) {
                    prefsFile.setReadable(true, false);
                }
            } catch (Throwable ignored) {}
        }
    }

    public static class GeneralPreferenceFragment extends BaseFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_general);
        }

        @Override
        protected void updateSummaries() {
            Preference themeP = findPreference(TkPrefs.KEY_THEME_STYLE);
            if (themeP instanceof ListPreference) {
                themeP.setSummary(((ListPreference) themeP).getEntry());
            }
            Preference battP = findPreference(TkPrefs.KEY_BATTERY_STYLE);
            if (battP instanceof ListPreference) {
                battP.setSummary(((ListPreference) battP).getEntry());
            }
        }
    }

    public static class NavBarPreferenceFragment extends BaseFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_navbar);
        }

        @Override
        protected void updateSummaries() {}
    }

    public static class RecentsPreferenceFragment extends BaseFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_recents);
        }

        @Override
        protected void updateSummaries() {}
    }

    public static class LauncherPreferenceFragment extends BaseFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_launcher);
        }

        @Override
        protected void updateSummaries() {}
    }

    public static class AboutPreferenceFragment extends BaseFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(org.exalm.tabletkat.R.xml.pref_about);
            
            // Add tools to the bottom of the About section in tablet mode
            Preference restart = new Preference(getActivity());
            restart.setTitle("Restart Bar (Hard Kill)");
            restart.setSummary("Kills SystemUI process");
            restart.setOnPreferenceClickListener(p -> {
                ((SettingsActivity)getActivity()).restartSystemUI();
                return true;
            });
            getPreferenceScreen().addPreference(restart);

            Preference reset = new Preference(getActivity());
            reset.setTitle("EMERGENCY RESET");
            reset.setSummary("Wipe settings and soft reboot");
            reset.setOnPreferenceClickListener(p -> {
                ((SettingsActivity)getActivity()).emergencyReset();
                return true;
            });
            getPreferenceScreen().addPreference(reset);
        }

        @Override
        protected void updateSummaries() {}
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private void restartSystemUI() {
        try {
            String cmd = "am force-stop com.android.systemui || " +
                    "kill $(ps | grep com.android.systemui | tr -s ' ' | cut -d ' ' -f 2)";
            Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            Toast.makeText(this, "Killing SystemUI...", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Log.e("TabletKat", "Restart failed", t);
        }
    }

    private void emergencyReset() {
        new AlertDialog.Builder(this)
                .setTitle("Emergency Reset")
                .setMessage("Wipe all settings and reboot?")
                .setPositiveButton("Reset", (d, w) -> {
                    PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();
                    fixPermissions();
                    try {
                        Runtime.getRuntime().exec(new String[]{"su", "-c", "setprop ctl.restart zygote"});
                    } catch (Throwable ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    public void fixPermissions() {
        try {
            java.io.File sharedPrefsDir = new java.io.File(getApplicationInfo().dataDir, "shared_prefs");
            java.io.File prefsFile = new java.io.File(sharedPrefsDir, getPackageName() + "_preferences.xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Throwable ignored) {}
    }
}
