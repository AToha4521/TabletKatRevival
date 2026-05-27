package org.exalm.tabletkat.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.exalm.tabletkat.IMod;
import org.exalm.tabletkat.R;
import org.exalm.tabletkat.TkPrefs;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class LauncherMod implements IMod {
    private Class mHoloImageViewClass;
    private static final String TAG = "TabletKat/LauncherMod";

    private int drawable_divider_launcher_holo;
    private int drawable_ic_home_all_apps;

    private int id_dock_divider;
    private int id_drag_target_bar;
    private int id_hotseat;
    private int id_paged_view_indicator;
    private int id_qsb_divider;
    private int id_qsb_search_bar;
    private int id_search_button;
    private int id_voice_button;
    private int id_voice_button_proxy;
    private int id_workspace;

    private String mCurrentPackage;

    public void addHooks(String packageName, ClassLoader cl) {
        mCurrentPackage = packageName;
        final String launcherPkg = packageName.equals("com.android.launcher") ? packageName + "2" : packageName;
        
        try {
            mHoloImageViewClass = XposedHelpers.findClass(launcherPkg + ".HolographicImageView", cl);
        } catch (Throwable t) {
            mHoloImageViewClass = null;
        }

        // Register restart listener
        try {
            XposedHelpers.findAndHookMethod(launcherPkg + ".Launcher", cl, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Activity activity = (Activity) param.thisObject;
                    IntentFilter filter = new IntentFilter(TkPrefs.ACTION_PREFERENCE_CHANGED);
                    activity.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String key = intent.getStringExtra("key");
                            if (TkPrefs.KEY_LAUNCHER_MOD.equals(key)) {
                                Log.i(TAG, "Launcher preference changed. Refreshing...");
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }
                    }, filter);
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Could not hook Launcher.onCreate for restart listener", t);
        }
    }

    @Override
    public void addHooks(ClassLoader cl) { }

    private void removeView(View container, int id) {
        View v = container.findViewById(id);
        if (v == null || !(v.getParent() instanceof ViewGroup)) return;
        ((ViewGroup) v.getParent()).removeView(v);
    }

    @Override
    public void initResources(XResources res, XModuleResources res2) {
        if (!TkPrefs.isLauncherModEnabled()) return;

        String pkg = res.getPackageName();

        res.setReplacement(pkg, "bool", "allow_rotation", true);
        res.setReplacement(pkg, "bool", "config_workspaceFadeAdjacentScreens", true);
        res.setReplacement(pkg, "bool", "config_useDropTargetDownTransition", true);
        res.setReplacement(pkg, "bool", "is_large_screen", true);

        res.setReplacement(pkg, "dimen", "button_bar_height", 0);
        res.setReplacement(pkg, "dimen", "button_bar_height_top_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_height_bottom_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_height_plus_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_width_left_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_width_right_padding", 0);

        res.setReplacement(pkg, "integer", "cell_count_x", res2.fwd(R.integer.launcher2_cell_count_x));
        res.setReplacement(pkg, "integer", "cell_count_y", res2.fwd(R.integer.launcher2_cell_count_y));

        res.setReplacement(pkg, "dimen", "cell_layout_left_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_left_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_left_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_left_padding_land));
        res.setReplacement(pkg, "dimen", "cell_layout_right_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_right_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_right_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_right_padding_land));
        res.setReplacement(pkg, "dimen", "cell_layout_top_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_top_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_top_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_top_padding_land));
        res.setReplacement(pkg, "dimen", "cell_layout_bottom_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_bottom_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_bottom_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_bottom_padding_land));

        res.setReplacement(pkg, "dimen", "qsb_bar_height", res2.fwd(R.dimen.launcher2_qsb_bar_height));
        res.setReplacement(pkg, "dimen", "qsb_bar_height_inset", res2.fwd(R.dimen.launcher2_qsb_bar_height_inset));
        res.setReplacement(pkg, "dimen", "search_bar_height", res2.fwd(R.dimen.launcher2_qsb_bar_height));

        res.setReplacement(pkg, "dimen", "workspace_top_padding_land", res2.fwd(R.dimen.launcher2_workspace_top_padding));
        res.setReplacement(pkg, "dimen", "workspace_top_padding_port", res2.fwd(R.dimen.launcher2_workspace_top_padding));
        res.setReplacement(pkg, "dimen", "workspace_bottom_padding_land", 0);
        res.setReplacement(pkg, "dimen", "workspace_bottom_padding_port", 0);
        res.setReplacement(pkg, "dimen", "workspace_left_padding_land", 0);
        res.setReplacement(pkg, "dimen", "workspace_left_padding_port", 0);
        res.setReplacement(pkg, "dimen", "workspace_right_padding_land", 0);
        res.setReplacement(pkg, "dimen", "workspace_right_padding_port", 0);

        res.setReplacement(pkg, "dimen", "workspace_width_gap_land", res2.fwd(R.dimen.launcher2_workspace_width_gap_land));
        res.setReplacement(pkg, "dimen", "workspace_width_gap_port", res2.fwd(R.dimen.launcher2_workspace_width_gap_port));
        res.setReplacement(pkg, "dimen", "workspace_height_gap_land", res2.fwd(R.dimen.launcher2_workspace_height_gap_land));
        res.setReplacement(pkg, "dimen", "workspace_height_gap_port", res2.fwd(R.dimen.launcher2_workspace_height_gap_port));

        res.setReplacement(pkg, "drawable", "workspace_bg", new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(Color.TRANSPARENT);
            }
        });

        res.setReplacement(pkg, "drawable", "divider_launcher_holo", res2.fwd(R.drawable.launcher2_divider_launcher_holo));
        drawable_divider_launcher_holo = res.getIdentifier("divider_launcher_holo", "drawable", pkg);
        drawable_ic_home_all_apps = res.addResource(res2, res2.fwd(R.drawable.ic_home_all_apps_holo_dark).getId());

        id_dock_divider = res.getIdentifier("dock_divider", "id", pkg);
        id_drag_target_bar = res.getIdentifier("drag_target_bar", "id", pkg);
        id_hotseat = res.getIdentifier("hotseat", "id", pkg);
        id_paged_view_indicator = res.getIdentifier("paged_view_indicator", "id", pkg);
        id_qsb_divider = res.getIdentifier("qsb_divider", "id", pkg);
        id_qsb_search_bar = res.getIdentifier("qsb_search_bar", "id", pkg);
        id_search_button = res.getIdentifier("search_button", "id", pkg);
        id_voice_button = res.getIdentifier("voice_button", "id", pkg);
        id_voice_button_proxy = res.getIdentifier("voice_button_proxy", "id", pkg);
        id_workspace = res.getIdentifier("workspace", "id", pkg);

        res.hookLayout(pkg, "layout", "launcher", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable {
                View v = layoutInflatedParam.view;

                DisplayMetrics d = v.getResources().getDisplayMetrics();
                int dimen_qsb_bar_height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, d);
                int workspace_padding_top = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, d);

                View hotseat = v.findViewById(id_hotseat);
                if (hotseat != null) hotseat.setVisibility(View.GONE);

                removeView(v, id_dock_divider);
                removeView(v, id_paged_view_indicator);
                removeView(v, id_qsb_divider);

                View voiceProxy = v.findViewById(id_voice_button_proxy);
                if (voiceProxy != null) {
                    voiceProxy.setClickable(false);
                    voiceProxy.setFocusable(false);
                }

                View workspace = v.findViewById(id_workspace);
                if (workspace != null) {
                    workspace.setPadding(0, workspace_padding_top, 0, 0);
                }

                View dragTarget = v.findViewById(id_drag_target_bar);
                if (dragTarget != null && dragTarget.getParent() instanceof ViewGroup) {
                    ViewGroup qsbContainer = (ViewGroup) dragTarget.getParent();

                    qsbContainer.setPadding(0, 0, 0, 0);
                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) qsbContainer.getLayoutParams();
                    p.height = dimen_qsb_bar_height;
                    p.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    p.gravity = Gravity.TOP;

                    qsbContainer.setLayoutParams(p);

                    View qsb = v.findViewById(id_qsb_search_bar);
                    if (qsb != null) {
                        RelativeLayout l = new RelativeLayout(qsbContainer.getContext());
                        l.setId(qsb.getId());
                        qsbContainer.removeView(qsb);
                        qsbContainer.addView(l, 0);
                        createTabletActionBar(l);
                    }
                    XposedHelpers.callMethod(qsbContainer, "onFinishInflate");
                }
            }
        });
    }

    private void createTabletActionBar(RelativeLayout l){
        DisplayMetrics d = l.getResources().getDisplayMetrics();
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, d);
        Class<?> imageClass = mHoloImageViewClass != null ? mHoloImageViewClass : ImageView.class;

        ImageView searchButton = (ImageView) XposedHelpers.newInstance(imageClass, l.getContext());
        searchButton.setImageResource(drawable_ic_home_all_apps);
        searchButton.setAdjustViewBounds(true);
        searchButton.setFocusable(true);
        searchButton.setClickable(true);
        searchButton.setId(id_search_button);
        RelativeLayout.LayoutParams searchParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        searchParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        searchParams.addRule(RelativeLayout.CENTER_VERTICAL);
        searchButton.setLayoutParams(searchParams);
        searchButton.setPadding(padding, 0, padding, 0);
        searchButton.setOnClickListener(v -> XposedHelpers.callMethod(v.getContext(), "onClickSearchButton", v));
        l.addView(searchButton);

        ImageView searchDivider = new ImageButton(l.getContext());
        searchDivider.setImageResource(drawable_divider_launcher_holo);
        searchDivider.setId(TkR.id.battery_text);
        searchDivider.setBackground(null);
        searchDivider.setClickable(true);
        searchDivider.setFocusable(false);
        RelativeLayout.LayoutParams divParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        divParams.addRule(RelativeLayout.END_OF, id_search_button);
        searchDivider.setLayoutParams(divParams);
        searchDivider.setOnClickListener(v -> XposedHelpers.callMethod(v.getContext(), "onClickSearchButton", v));
        l.addView(searchDivider);

        ImageView voiceButton = (ImageView) XposedHelpers.newInstance(imageClass, l.getContext());
        voiceButton.setImageResource(drawable_ic_home_all_apps);
        voiceButton.setAdjustViewBounds(true);
        voiceButton.setFocusable(true);
        voiceButton.setClickable(true);
        voiceButton.setId(id_voice_button);
        RelativeLayout.LayoutParams voiceParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        voiceParams.addRule(RelativeLayout.END_OF, TkR.id.battery_text);
        voiceButton.setLayoutParams(voiceParams);
        voiceButton.setPadding(padding, 0, padding, 0);
        voiceButton.setOnClickListener(v -> XposedHelpers.callMethod(v.getContext(), "onClickVoiceButton", v));
        l.addView(voiceButton);

        ImageView allAppsButton = (ImageView) XposedHelpers.newInstance(imageClass, l.getContext());
        allAppsButton.setImageResource(drawable_ic_home_all_apps);
        allAppsButton.setAdjustViewBounds(true);
        allAppsButton.setFocusable(true);
        allAppsButton.setClickable(true);
        RelativeLayout.LayoutParams allAppsParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        allAppsParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        allAppsParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        allAppsButton.setLayoutParams(allAppsParams);
        allAppsButton.setPadding(padding, 0, padding, 0);
        allAppsButton.setOnClickListener(v -> XposedHelpers.callMethod(v.getContext(), "onClickAllAppsButton", v));
        l.addView(allAppsButton);
    }

    public static boolean isSupported(String s){
        return s.equals("com.android.launcher") || s.equals("com.android.launcher2");
    }

    public String getPackage() {
        return mCurrentPackage;
    }
}
