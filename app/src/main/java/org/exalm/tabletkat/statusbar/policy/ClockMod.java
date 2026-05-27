package org.exalm.tabletkat.statusbar.policy;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.TextView;

import org.exalm.tabletkat.TkPrefs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * ClockMod
 * ========
 * Fixes the "far away" gap in ICS font mode and ensures consistent seconds
 * across all status bar clock instances.
 */
public class ClockMod {
    private static final String TAG = "TabletKat/ClockMod";
    
    private static final WeakHashMap<TextView, Boolean> mClocks = new WeakHashMap<>();

    public static void initHooks(ClassLoader cl) {
        try {
            final Class<?> clockClass = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", cl);

            XposedHelpers.findAndHookMethod(clockClass, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    TextView clock = (TextView) param.thisObject;
                    mClocks.put(clock, true);
                    startTicker(clock);
                }
            });

            XposedHelpers.findAndHookMethod(clockClass, "onDetachedFromWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mClocks.remove((TextView) param.thisObject);
                }
            });

            XposedHelpers.findAndHookMethod(clockClass, "getSmallTime", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!TkPrefs.isClockSecondsEnabled()) return;

                    TextView clock = (TextView) param.thisObject;
                    Context context = clock.getContext();
                    boolean is24 = DateFormat.is24HourFormat(context);
                    Locale locale = Locale.getDefault();
                    
                    // 1. Use a pattern WITHOUT a space before 'a' to fix the ICS font gap
                    String pattern = is24 ? "H:mm:ss" : "h:mm:ssa";
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
                    
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                    String result = sdf.format(calendar.getTime());

                    if (!is24) {
                        // 2. Small AM/PM styling
                        int amPmIndex = result.indexOf("AM");
                        if (amPmIndex == -1) amPmIndex = result.indexOf("PM");
                        
                        if (amPmIndex != -1) {
                            SpannableStringBuilder sb = new SpannableStringBuilder(result);
                            // Scale down AM/PM to 0.7x
                            sb.setSpan(new RelativeSizeSpan(0.7f), amPmIndex, result.length(), 
                                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                            
                            // 3. For ICS font specifically, add a tiny manual space if needed, 
                            // or just keep it tight as it looks more "Tablet-native".
                            param.setResult(sb);
                            return;
                        }
                    }
                    param.setResult(result);
                }
            });

        } catch (Throwable t) {
            Log.e(TAG, "Failed to hook Clock", t);
        }
    }

    public static void refreshAllClocks() {
        boolean isHolo = TkPrefs.isHoloTheme();
        int color = isHolo ? 0xFF33b5e5 : 0xFFFFFFFF;

        for (TextView clock : mClocks.keySet()) {
            if (clock != null) {
                clock.setTextColor(color);
                startTicker(clock);
                try {
                    XposedHelpers.callMethod(clock, "updateClock");
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void startTicker(final TextView clock) {
        Handler handler = clock.getHandler();
        if (handler == null) return;

        Runnable oldTicker = (Runnable) XposedHelpers.getAdditionalInstanceField(clock, "tk_ticker");
        if (oldTicker != null) {
            handler.removeCallbacks(oldTicker);
        }
        
        if (TkPrefs.isClockSecondsEnabled()) {
            Runnable ticker = new Runnable() {
                @Override
                public void run() {
                    if (mClocks.containsKey(clock) && TkPrefs.isClockSecondsEnabled()) {
                        try {
                            XposedHelpers.callMethod(clock, "updateClock");
                        } catch (Throwable ignored) {}
                        
                        Handler h = clock.getHandler();
                        if (h != null) {
                            h.postAtTime(this, SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
                        }
                    }
                }
            };
            XposedHelpers.setAdditionalInstanceField(clock, "tk_ticker", ticker);
            handler.postAtTime(ticker, SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
        }
    }
}
